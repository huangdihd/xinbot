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

package xin.bbtt.mcbot.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Covers the first-run telemetry opt-in rewrite (see {@link BotConfig#replaceTelemetryEnabled}). */
class BotConfigTest {

    private static final String SAMPLE = """
            {
                "account" : {
                    "name" : "[Bot name]"
                },
                "proxy" : {
                    "enable" : false,               // Whether enable proxy
                    "info" : {
                        "type" : ""
                    }
                },
                "telemetry" : {
                    "enable" : false,               // Opt-in comment
                    "key" : ""
                }
            }
            """;

    @Test
    void turnsTelemetryOnAndKeepsCommentsAndOtherSections() {
        String updated = BotConfig.replaceTelemetryEnabled(SAMPLE, true);
        assertNotNull(updated);
        assertTrue(updated.contains("\"enable\" : true"));
        // The proxy block and the comments must survive untouched
        assertTrue(updated.contains("\"enable\" : false"));
        assertTrue(updated.contains("// Opt-in comment"));
        assertTrue(updated.contains("// Whether enable proxy"));
        int proxyEnable = updated.indexOf("\"enable\" : false");
        int telemetryEnable = updated.indexOf("\"enable\" : true");
        assertTrue(proxyEnable >= 0 && proxyEnable < telemetryEnable,
                "only telemetry.enable is rewritten, proxy.enable is not");
    }

    @Test
    void turnsTelemetryBackOffAgain() {
        String once = BotConfig.replaceTelemetryEnabled(SAMPLE, true);
        String twice = BotConfig.replaceTelemetryEnabled(once, false);
        assertNotNull(twice);
        assertFalse(twice.contains("\"enable\" : true"));
        assertTrue(twice.contains("\"enable\" : false"));
        // The value formatting (whitespace, trailing comment) stays consistent
        assertTrue(twice.contains("\"enable\" : false,               // Opt-in comment"));
    }

    @Test
    void returnsNullWhenTelemetryBlockIsMissing() {
        String withoutTelemetry = """
                {
                    "proxy" : {
                        "enable" : false
                    }
                }
                """;
        assertNull(BotConfig.replaceTelemetryEnabled(withoutTelemetry, true));
    }

    @Test
    void returnsNullWhenTelemetryHasNoEnableKey() {
        String withoutEnable = """
                {
                    "telemetry" : {
                        "key" : ""
                    }
                }
                """;
        assertNull(BotConfig.replaceTelemetryEnabled(withoutEnable, true));
    }

    @Test
    void refusesToRewriteNonBooleanValues() {
        String quoted = """
                {
                    "telemetry" : {
                        "enable" : "false",
                        "key" : ""
                    }
                }
                """;
        assertNull(BotConfig.replaceTelemetryEnabled(quoted, true));
    }
}
