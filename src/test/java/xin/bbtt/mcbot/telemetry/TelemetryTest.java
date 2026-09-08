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
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        // IVs start at offset 6 with length 12 and must never repeat
        assertTrue(first.length >= 18 && second.length >= 18);
        boolean sameIv = true;
        for (int i = 6; i < 18; i++) {
            if (first[i] != second[i]) {
                sameIv = false;
                break;
            }
        }
        assertTrue(!sameIv, "random IV should differ between envelopes");
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
}
