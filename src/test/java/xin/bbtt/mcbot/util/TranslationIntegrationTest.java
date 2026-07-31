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

package xin.bbtt.mcbot.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xin.bbtt.mcbot.LangManager;
import xin.bbtt.mcbot.Utils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TranslationIntegrationTest {

    @BeforeEach
    void setUp() {
        LangManager.clear();
    }

    @Test
    void testTranslatableComponentToString() {
        // Add a translation template
        LangManager.addTranslations(Map.of("chat.type.text", "<%s> %s"));
        
        // Create a translatable component: <Alice> Hello World
        Component component = Component.translatable("chat.type.text", 
                Component.text("Alice"), 
                Component.text("Hello World"));
        
        String result = Utils.toString(component);
        
        // Adventure components might add default colors if not careful, 
        // but Utils.toString only adds codes if present.
        // chat.type.text -> <Alice> Hello World
        assertThat(result).isEqualTo("<Alice> Hello World");
    }

    @Test
    void testColorPreservationInTranslation() {
        LangManager.addTranslations(Map.of("test.color", "Colored: %s"));
        
        // Create a translatable component with colored argument
        Component component = Component.translatable("test.color", 
                Component.text("Red").color(NamedTextColor.RED));
        
        String result = Utils.toString(component);
        
        // §c is the code for RED
        assertThat(result).isEqualTo("Colored: §cRed");
    }
}
