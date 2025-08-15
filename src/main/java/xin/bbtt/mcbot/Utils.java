/*
 * # Copyright (C) 2025 huangdihd
 * #
 * # This program is free software: you can redistribute it and/or modify
 * # it under the terms of the GNU General Public License as published by
 * # the Free Software Foundation, either version 3 of the License, or
 * # (at your option) any later version.
 * #
 * # This program is distributed in the hope that it will be useful,
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * # GNU General Public License for more details.
 * #
 * # You should have received a copy of the GNU General Public License
 * # along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package xin.bbtt.mcbot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final String ANSI_GARBAGE = "░";                 // §k（伪乱码）
    private static final String ANSI_BOLD = "\u001B[1m";            // §l
    private static final String ANSI_STRIKETHROUGH = "\u001B[9m";   // §m
    private static final String ANSI_UNDERLINE = "\u001B[4m";       // §n
    private static final String ANSI_ITALIC = "\u001B[3m";          // §o
    private static final String ANSI_RESET = "\u001B[97m";          // §r

    private static final Map<Character, String> FORMAT_CODES = new HashMap<>();

    private static final Map<NamedTextColor, String> colorCodeMap = new HashMap<>();

    static {
        colorCodeMap.put(NamedTextColor.BLACK, "§0");
        colorCodeMap.put(NamedTextColor.DARK_BLUE, "§1");
        colorCodeMap.put(NamedTextColor.DARK_GREEN, "§2");
        colorCodeMap.put(NamedTextColor.DARK_AQUA, "§3");
        colorCodeMap.put(NamedTextColor.DARK_RED, "§4");
        colorCodeMap.put(NamedTextColor.DARK_PURPLE, "§5");
        colorCodeMap.put(NamedTextColor.GOLD, "§6");
        colorCodeMap.put(NamedTextColor.GRAY, "§7");
        colorCodeMap.put(NamedTextColor.DARK_GRAY, "§8");
        colorCodeMap.put(NamedTextColor.BLUE, "§9");
        colorCodeMap.put(NamedTextColor.GREEN, "§a");
        colorCodeMap.put(NamedTextColor.AQUA, "§b");
        colorCodeMap.put(NamedTextColor.RED, "§c");
        colorCodeMap.put(NamedTextColor.LIGHT_PURPLE, "§d");
        colorCodeMap.put(NamedTextColor.YELLOW, "§e");
        colorCodeMap.put(NamedTextColor.WHITE, "§f");
    }

    static {
        FORMAT_CODES.put('k', ANSI_GARBAGE);
        FORMAT_CODES.put('l', ANSI_BOLD);
        FORMAT_CODES.put('m', ANSI_STRIKETHROUGH);
        FORMAT_CODES.put('n', ANSI_UNDERLINE);
        FORMAT_CODES.put('o', ANSI_ITALIC);
        FORMAT_CODES.put('r', ANSI_RESET);
    }

    private static final String[] ANSI_COLORS = {
            "\u001B[30m", "\u001B[34m", "\u001B[32m", "\u001B[36m", // §0-§3
            "\u001B[31m", "\u001B[35m", "\u001B[33m", "\u001B[37m", // §4-§7
            "\u001B[90m", "\u001B[94m", "\u001B[92m", "\u001B[96m", // §8-§b
            "\u001B[91m", "\u001B[95m", "\u001B[93m", "\u001B[97m"  // §c-§f
    };

    public static String getStyleAnsi(TextComponent text) {
        StringBuilder sb = new StringBuilder();
        if (text.style().hasDecoration(TextDecoration.BOLD)) sb.append("§l");
        if (text.style().hasDecoration(TextDecoration.ITALIC)) sb.append("§o");
        if (text.style().hasDecoration(TextDecoration.UNDERLINED)) sb.append("§n");
        if (text.style().hasDecoration(TextDecoration.STRIKETHROUGH)) sb.append("§m");
        if (text.style().hasDecoration(TextDecoration.OBFUSCATED)) sb.append("░"); // §k 无法用 ANSI 实现
        return sb.toString();
    }

    public static String toString(Component component) {
        return String.join("", toStrings(component));
    }

    public static ArrayList<String> toStrings(Component component) {
        return toStrings(component, null);
    }

    public static ArrayList<String> toStrings(Component component, NamedTextColor defaultColor) {
        ArrayList<String> result = new ArrayList<>();

        if (component instanceof TranslatableComponent translatable) {
            String text = LangManager.get(translatable.key());
            if (!text.isEmpty()) {
                result.add(text);
            }
        }
        else if (component instanceof TextComponent textComponent) {
            String content = textComponent.content();
            TextColor textColor = textComponent.color();
            StringBuilder colorCode = new StringBuilder();
            if (textColor instanceof NamedTextColor namedTextColor) {
                colorCode.append(colorCodeMap.getOrDefault(namedTextColor, ""));
                defaultColor = namedTextColor;
            }
            else {
                colorCode.append(colorCodeMap.getOrDefault(defaultColor, ""));
            }
            colorCode.append(getStyleAnsi(textComponent));
            colorCode.append(content);
            result.add(colorCode.toString());
        }

        for (Component child : component.children()) {
            result.addAll(toStrings(child, defaultColor));
        }

        return result;
    }

    public static String parseColors(String text) {
        text = text.replace("§r§", "§"); // 合并重复重置

        Pattern pattern = Pattern.compile("§([0-9a-fk-or])");
        Matcher matcher = pattern.matcher(text);

        StringBuilder result = new StringBuilder();

        result.append("\u001B[97m");
        int lastIndex = 0;

        while (matcher.find()) {
            result.append(text, lastIndex, matcher.start()); // 普通文本
            char code = matcher.group(1).charAt(0);
            lastIndex = matcher.end();

            if (FORMAT_CODES.containsKey(code)) {
                result.append(FORMAT_CODES.get(code));
            } else {
                int index = Integer.parseInt(String.valueOf(code), 16);
                if (index >= 0 && index < ANSI_COLORS.length) {
                    result.append(ANSI_COLORS[index]);
                }
            }
        }

        result.append(text.substring(lastIndex));
        result.append("\u001B[0m");
        return result.toString();
    }
}
