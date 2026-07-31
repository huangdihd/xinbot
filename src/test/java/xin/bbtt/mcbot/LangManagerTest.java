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

package xin.bbtt.mcbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LangManagerTest {

    @BeforeEach
    void setUp() {
        LangManager.clear();
    }

    @Test
    void testBasicGetAndAdd() {
        LangManager.addTranslations(Map.of("test.key", "Hello World"));
        assertThat(LangManager.get("test.key")).isEqualTo("Hello World");
    }

    @Test
    void testGetFallbackToKey() {
        assertThat(LangManager.get("non.existent.key")).isEqualTo("non.existent.key");
    }

    @Test
    void testFormatGet() {
        LangManager.addTranslations(Map.of("test.format", "Hello %s, you have %d messages."));
        assertThat(LangManager.get("test.format", "Alice", 5)).isEqualTo("Hello Alice, you have 5 messages.");
    }

    @Test
    void testFormatGetFallbackOnError() {
        LangManager.addTranslations(Map.of("test.format.error", "Hello %d"));
        // Invalid arguments for %d
        assertThat(LangManager.get("test.format.error", "Alice")).isEqualTo("Hello %d");
    }
}
