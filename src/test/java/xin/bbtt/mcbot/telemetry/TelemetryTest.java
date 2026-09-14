/*
 * Copyright (C) 2026 huangdihd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package xin.bbtt.mcbot.telemetry;

import org.junit.jupiter.api.Test;
import xin.bbtt.mcbot.config.BotConfigData;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryTest {

    /** 32 ASCII bytes, i.e. a valid 32-byte AES-256 key (not real Base64).
     * Shared with InteropVectorTest and the telemetry server's test suite. */
    static final byte[] TEST_KEY =
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);

    @Test
    void envelopeRoundTripReturnsOriginalPlaintext() throws Exception {
        byte[] plaintext = "{\"type\":\"heartbeat\"}".getBytes(StandardCharsets.UTF_8);
        byte[] envelope = TelemetryManager.buildEnvelope(
                TelemetryManager.TYPE_HEARTBEAT, plaintext, TEST_KEY);
        assertArrayEquals(plaintext, TelemetryManager.decryptEnvelope(envelope, TEST_KEY));
    }

    @Test
    void envelopeCarriesMagicVersionAndType() throws Exception {
        byte[] envelope = TelemetryManager.buildEnvelope(
                TelemetryManager.TYPE_CRASH,
                "crash".getBytes(StandardCharsets.UTF_8),
                TEST_KEY
        );
        assertEquals('X', envelope[0]);
        assertEquals('B', envelope[1]);
        assertEquals('T', envelope[2]);
        assertEquals('L', envelope[3]);
        assertEquals(TelemetryManager.PROTOCOL_VERSION, envelope[4]);
        assertEquals(TelemetryManager.TYPE_CRASH, envelope[5]);
    }

    @Test
    void everyEnvelopeUsesAFreshIv() throws Exception {
        byte[] payload = "same".getBytes(StandardCharsets.UTF_8);
        byte[] first = TelemetryManager.buildEnvelope(TelemetryManager.TYPE_HEARTBEAT, payload, TEST_KEY);
        byte[] second = TelemetryManager.buildEnvelope(TelemetryManager.TYPE_HEARTBEAT, payload, TEST_KEY);
        // IVs live at offsets 6..17 and must never repeat across envelopes
        assertFalse(Arrays.equals(Arrays.copyOfRange(first, 6, 18),
                        Arrays.copyOfRange(second, 6, 18)),
                "random IV should differ between envelopes");
    }

    @Test
    void tamperedCiphertextFailsAuthentication() throws Exception {
        byte[] envelope = TelemetryManager.buildEnvelope(
                TelemetryManager.TYPE_HEARTBEAT,
                "payload".getBytes(StandardCharsets.UTF_8),
                TEST_KEY
        );
        envelope[envelope.length - 1] ^= 0x01; // flip one bit of the auth tag / ciphertext
        assertThrows(Exception.class, () -> TelemetryManager.decryptEnvelope(envelope, TEST_KEY));
    }

    @Test
    void mutatedTypeByteFailsAuthentication() throws Exception {
        byte[] envelope = TelemetryManager.buildEnvelope(
                TelemetryManager.TYPE_HEARTBEAT,
                "{\"type\":\"heartbeat\"}".getBytes(StandardCharsets.UTF_8),
                TEST_KEY
        );
        // The type byte is part of the GCM AAD, so flipping heartbeat -> crash
        // must invalidate the tag even though ciphertext and IV are untouched.
        envelope[5] = TelemetryManager.TYPE_CRASH;
        assertThrows(Exception.class, () -> TelemetryManager.decryptEnvelope(envelope, TEST_KEY));
    }

    @Test
    void envelopeDecryptedWithWrongKeyFailsAuthentication() throws Exception {
        byte[] envelope = TelemetryManager.buildEnvelope(
                TelemetryManager.TYPE_HEARTBEAT,
                "payload".getBytes(StandardCharsets.UTF_8),
                TEST_KEY
        );
        byte[] otherKey = "fedcba9876543210fedcba9876543210".getBytes(StandardCharsets.UTF_8);
        assertThrows(Exception.class, () -> TelemetryManager.decryptEnvelope(envelope, otherKey));
    }

    @Test
    void envelopeWithBadMagicIsRejected() {
        byte[] envelope = new byte[18];
        envelope[4] = TelemetryManager.PROTOCOL_VERSION;
        assertThrows(IllegalArgumentException.class,
                () -> TelemetryManager.decryptEnvelope(envelope, TEST_KEY));
    }

    @Test
    void parseKeyAcceptsBase64OfExactly32Bytes() {
        String encoded = Base64.getEncoder().encodeToString(TEST_KEY);
        assertArrayEquals(TEST_KEY, TelemetryManager.parseKey(encoded));
        // Whitespace around the value is tolerated
        assertArrayEquals(TEST_KEY, TelemetryManager.parseKey("  " + encoded + "\n"));
    }

    @Test
    void parseKeyRejectsBlankMalformedAndWrongLengthValues() {
        assertThrows(IllegalArgumentException.class, () -> TelemetryManager.parseKey(null));
        assertThrows(IllegalArgumentException.class, () -> TelemetryManager.parseKey(""));
        assertThrows(IllegalArgumentException.class, () -> TelemetryManager.parseKey("   "));
        // Not Base64 at all
        assertThrows(IllegalArgumentException.class,
                () -> TelemetryManager.parseKey("!!!not-base64!!!"));
        // Valid Base64 but the wrong decoded length (16 bytes, not 32)
        String shortKey = Base64.getEncoder().encodeToString("short-key-16b!".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> TelemetryManager.parseKey(shortKey));
    }

    @Test
    void redactNumbersMasksFourOrMoreDigitsIncludingNegatives() {
        // Positive and negative runs (leading minus included) are all masked with ****
        assertEquals("player at ****, 64, ****",
                TelemetryManager.redactNumbers("player at 1234, 64, -98765"));
        assertEquals("line Bot.java:**** threw at **** ms",
                TelemetryManager.redactNumbers("line Bot.java:10240 threw at 12345 ms"));
        assertEquals("\"****\" and \"****\"",
                TelemetryManager.redactNumbers("\"-1234\" and \"567890\""));
    }

    @Test
    void redactNumbersKeepsShortNumbersAndPlainText() {
        // Coordinates that are all under 4 digits are not player-identifying, keep them
        assertEquals("at 64, 255, 7 and -123",
                TelemetryManager.redactNumbers("at 64, 255, 7 and -123"));
        assertEquals("no digits here", TelemetryManager.redactNumbers("no digits here"));
        assertEquals("", TelemetryManager.redactNumbers(""));
        assertNull(TelemetryManager.redactNumbers(null));
    }

    @Test
    void redactSecretsReplacesConfiguredPasswordsWithSixStars() {
        List<String> secrets = List.of("s3cret", "pa55w0rd");
        assertEquals("login ****** ok ******",
                TelemetryManager.redactSecrets("login s3cret ok pa55w0rd", secrets));
        assertEquals("no match here",
                TelemetryManager.redactSecrets("no match here", secrets));
        assertEquals("plain", TelemetryManager.redactSecrets("plain", List.of()));
        assertNull(TelemetryManager.redactSecrets(null, secrets));
    }

    @Test
    void redactCrashTextMasksPasswordWholeBeforeNumberRule() {
        // Password "abcd1234" itself holds 4+ digits: secret replacement must run
        // first so the whole password becomes ******, not a digit-broken fragment.
        assertEquals("token ****** tail",
                TelemetryManager.redactCrashText("token abcd1234 tail", List.of("abcd1234")));
        // Without secrets the combined helper still applies the number redaction.
        assertEquals("numbers only: ****",
                TelemetryManager.redactCrashText("numbers only: 12345", List.of()));
    }

    @Test
    void telemetryConfigDefaultsToDisabledUdpLocalhostWithoutKey() {
        BotConfigData.Telemetry telemetry = new BotConfigData.Telemetry();
        // Opt-in: telemetry stays off unless the user explicitly enables it
        assertFalse(telemetry.isEnable());
        assertEquals("udp", telemetry.getMode());
        assertEquals("127.0.0.1", telemetry.getIp());
        assertEquals(9000, telemetry.getPort());
        // No bundled secret either: telemetry fails closed until one is configured
        assertTrue(telemetry.getKey().isEmpty());
    }

    @Test
    void payloadOptionsMirrorTheConfiguredPrivacySwitches() {
        BotConfigData.Telemetry telemetry = new BotConfigData.Telemetry();
        // Every per-field switch defaults to on so existing reports stay unchanged
        TelemetryManager.PayloadOptions defaults = TelemetryManager.PayloadOptions.of(telemetry);
        assertTrue(defaults.bot() && defaults.server() && defaults.state()
                && defaults.players() && defaults.uptime() && defaults.system());

        telemetry.setSendBot(false);
        telemetry.setSendState(false);
        telemetry.setSendSystem(false);
        TelemetryManager.PayloadOptions options = TelemetryManager.PayloadOptions.of(telemetry);
        assertFalse(options.bot());
        assertFalse(options.state());
        assertFalse(options.system());
        assertTrue(options.server());
        assertTrue(options.players());
        assertTrue(options.uptime());
    }

    @Test
    void filterPayloadDropsDisabledFieldsAndKeepsTheRest() {
        Map<String, Object> full = new LinkedHashMap<>();
        full.put("type", "heartbeat");
        full.put("timestamp_ms", 1L);
        full.put("version", "1.0");
        full.put("bot", "Steve");
        full.put("server", "mc.example:25565");
        full.put("online", true);
        full.put("state", "Game");
        full.put("players", 3);
        full.put("uptime_ms", 42L);
        full.put("heap_used_bytes", 1L);
        full.put("heap_max_bytes", 2L);
        full.put("os_name", "Windows");
        full.put("os_arch", "amd64");
        full.put("java_version", "17");

        TelemetryManager.PayloadOptions options = new TelemetryManager.PayloadOptions(
                false /* bot */, true /* server */, false /* state */,
                true /* players */, true /* uptime */, false /* system */);
        Map<String, Object> filtered = TelemetryManager.filterPayload(full, options);

        assertFalse(filtered.containsKey("bot"));
        assertFalse(filtered.containsKey("online"));
        assertFalse(filtered.containsKey("state"));
        assertFalse(filtered.containsKey("heap_used_bytes"));
        assertFalse(filtered.containsKey("os_name"));
        assertFalse(filtered.containsKey("java_version"));
        assertTrue(filtered.containsKey("server"));
        assertTrue(filtered.containsKey("players"));
        assertTrue(filtered.containsKey("uptime_ms"));
        assertEquals("heartbeat", filtered.get("type"));
        assertEquals("1.0", filtered.get("version"));
        // The input map must not be mutated
        assertTrue(full.containsKey("bot"));
        assertEquals(14, full.size());
    }

    @Test
    void filterPayloadAlwaysKeepsProtocolAndCrashDetailFields() {
        Map<String, Object> full = new LinkedHashMap<>();
        full.put("type", "crash");
        full.put("timestamp_ms", 1L);
        full.put("version", "1.0");
        full.put("bot", "Steve");
        full.put("server", "mc.example:25565");
        full.put("online", true);
        full.put("state", "Game");
        full.put("players", 3);
        full.put("uptime_ms", 42L);
        full.put("thread_name", "main");
        full.put("exception", "java.lang.Error: boom");
        full.put("stack_trace", "at ...");

        // Everything privacy-related switched off: crash details must still go out
        TelemetryManager.PayloadOptions none =
                new TelemetryManager.PayloadOptions(false, false, false, false, false, false);
        Map<String, Object> filtered = TelemetryManager.filterPayload(full, none);

        assertTrue(filtered.containsKey("type"));
        assertTrue(filtered.containsKey("timestamp_ms"));
        assertTrue(filtered.containsKey("version"));
        assertTrue(filtered.containsKey("thread_name"));
        assertTrue(filtered.containsKey("exception"));
        assertTrue(filtered.containsKey("stack_trace"));
        assertFalse(filtered.containsKey("bot"));
        assertFalse(filtered.containsKey("players"));
        assertFalse(filtered.containsKey("uptime_ms"));
        assertEquals(6, filtered.size());
    }

    @Test
    void keyRequestIsJustTheSixByteControlHeader() {
        byte[] request = TelemetryManager.buildKeyRequest();
        assertEquals(6, request.length);
        assertEquals('X', request[0]);
        assertEquals('B', request[1]);
        assertEquals('T', request[2]);
        assertEquals('L', request[3]);
        assertEquals(TelemetryManager.PROTOCOL_VERSION, request[4]);
        assertEquals(TelemetryManager.TYPE_KEY_REQUEST, request[5]);
    }

    @Test
    void parseKeyResponseDecodesTheBase64Body() {
        byte[] reply = keyResponseFor(TEST_KEY);
        assertArrayEquals(TEST_KEY, TelemetryManager.parseKeyResponse(reply));
    }

    @Test
    void parseKeyResponseRejectsMalformedReplies() {
        // Wrong magic, wrong type and truncated replies are all rejected
        byte[] badMagic = keyResponseFor(TEST_KEY);
        badMagic[3] = 'X';
        assertThrows(IllegalArgumentException.class,
                () -> TelemetryManager.parseKeyResponse(badMagic));
        byte[] wrongType = keyResponseFor(TEST_KEY);
        wrongType[5] = TelemetryManager.TYPE_KEY_REQUEST;
        assertThrows(IllegalArgumentException.class,
                () -> TelemetryManager.parseKeyResponse(wrongType));
        assertThrows(IllegalArgumentException.class,
                () -> TelemetryManager.parseKeyResponse(null));
        assertThrows(IllegalArgumentException.class,
                () -> TelemetryManager.parseKeyResponse(new byte[6]));
        // Body that is not Base64 of 32 bytes fails the key parser too
        byte[] junk = new byte[6 + 5];
        System.arraycopy(TelemetryManager.MAGIC, 0, junk, 0, 4);
        junk[4] = TelemetryManager.PROTOCOL_VERSION;
        junk[5] = TelemetryManager.TYPE_KEY_RESPONSE;
        System.arraycopy("nope!".getBytes(StandardCharsets.UTF_8), 0, junk, 6, 5);
        assertThrows(IllegalArgumentException.class,
                () -> TelemetryManager.parseKeyResponse(junk));
    }

    /** Builds a plaintext key response: header + Base64 of the given key. */
    private static byte[] keyResponseFor(byte[] key) {
        byte[] body = Base64.getEncoder().encodeToString(key)
                .getBytes(StandardCharsets.UTF_8);
        byte[] reply = new byte[6 + body.length];
        System.arraycopy(TelemetryManager.MAGIC, 0, reply, 0, 4);
        reply[4] = TelemetryManager.PROTOCOL_VERSION;
        reply[5] = TelemetryManager.TYPE_KEY_RESPONSE;
        System.arraycopy(body, 0, reply, 6, body.length);
        return reply;
    }
}
