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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts Adventure components to terminal ANSI text without losing RGB colors. */
public final class ComponentParser {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final Pattern TRANSLATION_PLACEHOLDER = Pattern.compile("%%|%(?:(\\d+)\\$)?s");
    private static final String LEGACY_CODES = "0123456789abcdef";
    private static final List<NamedTextColor> LEGACY_COLORS = List.of(
        NamedTextColor.BLACK, NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GREEN,
        NamedTextColor.DARK_AQUA, NamedTextColor.DARK_RED, NamedTextColor.DARK_PURPLE,
        NamedTextColor.GOLD, NamedTextColor.GRAY, NamedTextColor.DARK_GRAY,
        NamedTextColor.BLUE, NamedTextColor.GREEN, NamedTextColor.AQUA,
        NamedTextColor.RED, NamedTextColor.LIGHT_PURPLE, NamedTextColor.YELLOW,
        NamedTextColor.WHITE
    );

    private static final Map<NamedTextColor, String> NAMED_ANSI_COLORS = Map.ofEntries(
        Map.entry(NamedTextColor.BLACK, "\u001B[30m"),
        Map.entry(NamedTextColor.DARK_BLUE, "\u001B[34m"),
        Map.entry(NamedTextColor.DARK_GREEN, "\u001B[32m"),
        Map.entry(NamedTextColor.DARK_AQUA, "\u001B[36m"),
        Map.entry(NamedTextColor.DARK_RED, "\u001B[31m"),
        Map.entry(NamedTextColor.DARK_PURPLE, "\u001B[35m"),
        Map.entry(NamedTextColor.GOLD, "\u001B[33m"),
        Map.entry(NamedTextColor.GRAY, "\u001B[37m"),
        Map.entry(NamedTextColor.DARK_GRAY, "\u001B[90m"),
        Map.entry(NamedTextColor.BLUE, "\u001B[94m"),
        Map.entry(NamedTextColor.GREEN, "\u001B[92m"),
        Map.entry(NamedTextColor.AQUA, "\u001B[96m"),
        Map.entry(NamedTextColor.RED, "\u001B[91m"),
        Map.entry(NamedTextColor.LIGHT_PURPLE, "\u001B[95m"),
        Map.entry(NamedTextColor.YELLOW, "\u001B[93m"),
        Map.entry(NamedTextColor.WHITE, "\u001B[97m")
    );

    private ComponentParser() {
    }

    /**
     * Renders a component tree as ANSI text. Component styles are inherited,
     * custom colors use ANSI truecolor, and each line is independently styled.
     */
    public static String toAnsi(Component component) {
        List<TextRun> runs = new ArrayList<>();
        appendComponent(runs, component, StyleState.DEFAULT);

        StringBuilder result = new StringBuilder();
        StyleState renderedStyle = null;
        for (TextRun run : runs) {
            if (!run.style().equals(renderedStyle)) {
                result.append(toAnsi(run.style()));
                renderedStyle = run.style();
            }
            appendText(result, run.text(), run.style());
        }
        return result.append(ANSI_RESET).toString();
    }

    private static void appendComponent(List<TextRun> runs, Component component, StyleState parentStyle) {
        StyleState style = parentStyle.merge(component);

        if (component instanceof TranslatableComponent translatable) {
            appendTranslation(runs, translatable, style);
        } else if (component instanceof TextComponent textComponent) {
            appendLegacyText(runs, textComponent.content(), style);
        }

        for (Component child : component.children()) {
            appendComponent(runs, child, style);
        }
    }

    private static void appendTranslation(
        List<TextRun> runs,
        TranslatableComponent component,
        StyleState style
    ) {
        String template = LangManager.get(component.key());
        Matcher matcher = TRANSLATION_PLACEHOLDER.matcher(template);
        int sequentialArgument = 0;
        int lastIndex = 0;

        while (matcher.find()) {
            appendLegacyText(runs, template.substring(lastIndex, matcher.start()), style);
            if ("%%".equals(matcher.group())) {
                appendLegacyText(runs, "%", style);
            } else {
                int argumentIndex;
                if (matcher.group(1) == null) {
                    argumentIndex = sequentialArgument++;
                } else {
                    argumentIndex = Integer.parseInt(matcher.group(1)) - 1;
                }
                if (argumentIndex >= 0 && argumentIndex < component.arguments().size()) {
                    appendComponent(runs, component.arguments().get(argumentIndex).asComponent(), style);
                } else {
                    appendLegacyText(runs, matcher.group(), style);
                }
            }
            lastIndex = matcher.end();
        }
        appendLegacyText(runs, template.substring(lastIndex), style);
    }

    private static void appendLegacyText(List<TextRun> runs, String text, StyleState baseStyle) {
        StyleState style = baseStyle;
        int lastIndex = 0;

        for (int index = 0; index + 1 < text.length(); index++) {
            if (text.charAt(index) != '§') {
                continue;
            }

            char code = Character.toLowerCase(text.charAt(index + 1));
            TextColor hexColor = readLegacyHexColor(text, index);
            if (hexColor != null) {
                appendRun(runs, text.substring(lastIndex, index), style);
                style = style.withColor(hexColor).withoutDecorations();
                index += 13;
                lastIndex = index + 1;
                continue;
            }

            int colorIndex = LEGACY_CODES.indexOf(code);
            if (colorIndex >= 0) {
                appendRun(runs, text.substring(lastIndex, index), style);
                style = style.withColor(namedColor(colorIndex)).withoutDecorations();
            } else if (code == 'k' || code == 'l' || code == 'm' || code == 'n' || code == 'o') {
                appendRun(runs, text.substring(lastIndex, index), style);
                style = style.withDecoration(code);
            } else if (code == 'r') {
                appendRun(runs, text.substring(lastIndex, index), style);
                style = baseStyle;
            } else {
                continue;
            }

            index++;
            lastIndex = index + 1;
        }
        appendRun(runs, text.substring(lastIndex), style);
    }

    private static TextColor readLegacyHexColor(String text, int start) {
        if (start + 13 >= text.length() || Character.toLowerCase(text.charAt(start + 1)) != 'x') {
            return null;
        }

        StringBuilder hex = new StringBuilder(6);
        for (int index = start + 2; index < start + 14; index += 2) {
            if (text.charAt(index) != '§' || Character.digit(text.charAt(index + 1), 16) < 0) {
                return null;
            }
            hex.append(text.charAt(index + 1));
        }
        return TextColor.color(Integer.parseInt(hex.toString(), 16));
    }

    private static NamedTextColor namedColor(int index) {
        return LEGACY_COLORS.get(index);
    }

    private static void appendRun(List<TextRun> runs, String text, StyleState style) {
        if (!text.isEmpty()) {
            runs.add(new TextRun(text, style));
        }
    }

    private static void appendText(StringBuilder result, String text, StyleState style) {
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '\n') {
                result.append(ANSI_RESET).append('\n').append(toAnsi(style));
            } else {
                result.append(character);
            }
        }
    }

    private static String toAnsi(StyleState style) {
        StringBuilder result = new StringBuilder(ANSI_RESET);
        String namedColor = null;
        if (style.color() instanceof NamedTextColor color) {
            namedColor = NAMED_ANSI_COLORS.get(color);
        }
        if (namedColor != null) {
            result.append(namedColor);
        } else {
            int value = style.color().value();
            result.append("\u001B[38;2;")
                .append(value >> 16 & 0xff).append(';')
                .append(value >> 8 & 0xff).append(';')
                .append(value & 0xff).append('m');
        }
        if (style.bold()) result.append("\u001B[1m");
        if (style.italic()) result.append("\u001B[3m");
        if (style.underlined()) result.append("\u001B[4m");
        if (style.strikethrough()) result.append("\u001B[9m");
        if (style.obfuscated()) result.append('░');
        return result.toString();
    }

    private record TextRun(String text, StyleState style) {
    }

    private record StyleState(
        TextColor color,
        boolean bold,
        boolean italic,
        boolean underlined,
        boolean strikethrough,
        boolean obfuscated
    ) {
        private static final StyleState DEFAULT = new StyleState(
            NamedTextColor.WHITE, false, false, false, false, false
        );

        private StyleState merge(Component component) {
            return new StyleState(
                component.color() == null ? color : component.color(),
                decoration(component, TextDecoration.BOLD, bold),
                decoration(component, TextDecoration.ITALIC, italic),
                decoration(component, TextDecoration.UNDERLINED, underlined),
                decoration(component, TextDecoration.STRIKETHROUGH, strikethrough),
                decoration(component, TextDecoration.OBFUSCATED, obfuscated)
            );
        }

        private StyleState withColor(TextColor newColor) {
            return new StyleState(newColor, bold, italic, underlined, strikethrough, obfuscated);
        }

        private StyleState withoutDecorations() {
            return new StyleState(color, false, false, false, false, false);
        }

        private StyleState withDecoration(char code) {
            return new StyleState(
                color,
                bold || code == 'l',
                italic || code == 'o',
                underlined || code == 'n',
                strikethrough || code == 'm',
                obfuscated || code == 'k'
            );
        }

        private static boolean decoration(Component component, TextDecoration decoration, boolean inherited) {
            return switch (component.decoration(decoration)) {
                case TRUE -> true;
                case FALSE -> false;
                case NOT_SET -> inherited;
            };
        }
    }
}
