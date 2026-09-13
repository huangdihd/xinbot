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
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xin.bbtt.mcbot.ComponentParser;
import xin.bbtt.mcbot.LangManager;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentParserTest {
    private static final String RESET = "\u001B[0m";

    @BeforeEach
    void setUp() {
        LangManager.clear();
    }

    @Test
    void rendersCustomRgbColorAsAnsiTruecolor() {
        Component component = Component.text("RGB").color(TextColor.color(0x12ab34));

        assertThat(ComponentParser.toAnsi(component))
            .isEqualTo(RESET + "\u001B[38;2;18;171;52mRGB" + RESET);
    }

    @Test
    void inheritsStylesAndHonorsExplicitDecorationRemoval() {
        Component component = Component.text("parent")
            .color(TextColor.color(0x12ab34))
            .decorate(TextDecoration.BOLD)
            .append(Component.text("child").decoration(TextDecoration.BOLD, false))
            .append(Component.text("sibling"));

        assertThat(ComponentParser.toAnsi(component)).isEqualTo(
            RESET + "\u001B[38;2;18;171;52m\u001B[1mparent"
                + RESET + "\u001B[38;2;18;171;52mchild"
                + RESET + "\u001B[38;2;18;171;52m\u001B[1msibling"
                + RESET
        );
    }

    @Test
    void preservesComponentStylesInTranslatedArguments() {
        LangManager.addTranslations(Map.of("test.colors", "%2$s then %1$s"));
        Component component = Component.translatable(
            "test.colors",
            Component.text("red").color(NamedTextColor.RED),
            Component.text("rgb").color(TextColor.color(0x010203))
        );

        assertThat(ComponentParser.toAnsi(component)).isEqualTo(
            RESET + "\u001B[38;2;1;2;3mrgb"
                + RESET + "\u001B[97m then "
                + RESET + "\u001B[91mred"
                + RESET
        );
    }

    @Test
    void parsesLegacyHexColorsEmbeddedInTextComponents() {
        Component component = Component.text("before §x§1§2§a§b§3§4after");

        assertThat(ComponentParser.toAnsi(component)).isEqualTo(
            RESET + "\u001B[97mbefore "
                + RESET + "\u001B[38;2;18;171;52mafter"
                + RESET
        );
    }

    @Test
    void reappliesStylesAfterNewlines() {
        Component component = Component.text("first\nsecond")
            .color(NamedTextColor.AQUA)
            .decorate(TextDecoration.ITALIC);

        String style = RESET + "\u001B[96m\u001B[3m";
        assertThat(ComponentParser.toAnsi(component))
            .isEqualTo(style + "first" + RESET + "\n" + style + "second" + RESET);
    }
}
