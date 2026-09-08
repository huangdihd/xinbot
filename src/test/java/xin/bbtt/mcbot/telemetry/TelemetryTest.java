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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryTest {

    @Test
    void envelopeRoundTripReturnsOriginalPlaintext() throws Exception {
        byte[] plaintext = "{\"type\":\"heartbeat\"}".getBytes(StandardCharsets.UTF_8);
        byte[] envelope = TelemetryManager.buildEnvelope(TelemetryManager.TYPE_HEARTBEAT, plaintext);
        assertArrayEquals(plaintext, TelemetryManager.decryptEnvelope(envelope));
    }

    @Test
    void envelopeCarriesMagicVersionAndType() throws Exception {
        byte[] envelope = TelemetryManager.buildEnvelope(
                TelemetryManager.TYPE_CRASH,
                "crash".getBytes(StandardCharsets.UTF_8)
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
        byte[] first = TelemetryManager.buildEnvelope(TelemetryManager.TYPE_HEARTBEAT, payload);
        byte[] second = TelemetryManager.buildEnvelope(TelemetryManager.TYPE_HEARTBEAT, payload);
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
                "payload".getBytes(StandardCharsets.UTF_8)
        );
        envelope[envelope.length - 1] ^= 0x01; // flip one bit of the auth tag / ciphertext
        assertThrows(Exception.class, () -> TelemetryManager.decryptEnvelope(envelope));
    }

    @Test
    void envelopeWithBadMagicIsRejected() {
        byte[] envelope = new byte[18];
        envelope[4] = TelemetryManager.PROTOCOL_VERSION;
        assertThrows(IllegalArgumentException.class, () -> TelemetryManager.decryptEnvelope(envelope));
    }

    @Test
    void telemetryConfigDefaultsToEnabledUdpLocalhost() {
        BotConfigData.Telemetry telemetry = new BotConfigData.Telemetry();
        assertTrue(telemetry.isEnable());
        assertEquals("udp", telemetry.getMode());
        assertEquals("127.0.0.1", telemetry.getIp());
        assertEquals(9000, telemetry.getPort());
    }
}
