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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Interoperability vector writer: builds real heartbeat/crash envelopes with the
 * actual client implementation ({@link TelemetryManager#buildEnvelope}) and writes
 * them into the telemetry server's test resources so that the server's
 * {@code ClientInteropTest} can decode them. Skipped when the target directory
 * does not exist (e.g. this project is checked out without the server next to it).
 */
class InteropVectorTest {

    private static final String HEARTBEAT_JSON = """
            {"type":"heartbeat","timestamp_ms":1700000000000,"version":"2.4.2-RELEASE",
             "bot":"interop-bot","online":true,"state":"Game",
             "server":"interop.example.com:25565","players":2,"uptime_ms":60000,
             "heap_used_bytes":1048576,"heap_max_bytes":4294967296,
             "os_name":"Linux","os_arch":"amd64","java_version":"21"}
            """;

    private static final String CRASH_JSON = """
            {"type":"crash","timestamp_ms":1700000000001,"version":"2.4.2-RELEASE",
             "bot":"interop-bot","online":false,"state":"Game",
             "server":"interop.example.com:25565","players":0,"uptime_ms":60001,
             "thread_name":"main","exception":"java.lang.NullPointerException: interop vector",
             "stack_trace":"java.lang.NullPointerException: interop vector\\n\\tat xin.bbtt.mcbot.Bot.start(Bot.java:1)"}
            """;

    @Test
    void writeInteropVectorsForTelemetryServer() throws Exception {
        Path dir = vectorDir();
        Assumptions.assumeTrue(Files.isDirectory(dir),
                "Interop vector directory not found (expected " + dir
                        + "): telemetry server is not checked out next to this project");
        Files.write(dir.resolve("client-heartbeat.bin"),
                TelemetryManager.buildEnvelope(
                        TelemetryManager.TYPE_HEARTBEAT,
                        HEARTBEAT_JSON.getBytes(StandardCharsets.UTF_8),
                        TelemetryTest.TEST_KEY));
        Files.write(dir.resolve("client-crash.bin"),
                TelemetryManager.buildEnvelope(
                        TelemetryManager.TYPE_CRASH,
                        CRASH_JSON.getBytes(StandardCharsets.UTF_8),
                        TelemetryTest.TEST_KEY));
    }

    /** Defaults to the telemetry server's test resources; override with -Dinterop.vector.dir=... */
    private static Path vectorDir() {
        String custom = System.getProperty("interop.vector.dir");
        if (custom != null && !custom.isBlank()) {
            return Paths.get(custom);
        }
        return Paths.get("../XinBotTelemetry/src/test/resources/interop");
    }
}
