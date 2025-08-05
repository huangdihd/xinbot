package xin.bbtt.mcbot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
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

    public static String ansiColor(int r, int g, int b) {
        return String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
    }

    public static String getStyleAnsi(TextComponent text) {
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(text.style().hasDecoration(TextDecoration.BOLD))) sb.append("\u001B[1m");
        if (Boolean.TRUE.equals(text.style().hasDecoration(TextDecoration.ITALIC))) sb.append("\u001B[3m");
        if (Boolean.TRUE.equals(text.style().hasDecoration(TextDecoration.UNDERLINED))) sb.append("\u001B[4m");
        if (Boolean.TRUE.equals(text.style().hasDecoration(TextDecoration.STRIKETHROUGH))) sb.append("\u001B[9m");
        if (Boolean.TRUE.equals(text.style().hasDecoration(TextDecoration.OBFUSCATED))) sb.append("░"); // §k 无法用 ANSI 实现
        return sb.toString();
    }

    public static String toString(Component component) {
        return String.join("", toStrings(component));
    }

    public static ArrayList<String> toStrings(Component component) {
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
            StringBuilder ANSICode = new StringBuilder();
            if (textColor != null) {
                ANSICode.append(ansiColor(
                        textColor.red(), textColor.green(), textColor.blue()));
                ANSICode.append(getStyleAnsi(textComponent));
            }
            ANSICode.append(parseColors(content));
            if (!ANSICode.toString().isEmpty()) {
                result.add(ANSICode.toString());
            }
        }

        for (Component child : component.children()) {
            result.addAll(toStrings(child));
        }

        return result;
    }

    public static String parseColors(String text) {
        text = text.replace("§r§", "§"); // 合并重复重置

        Pattern pattern = Pattern.compile("§([0-9a-fk-or])");
        Matcher matcher = pattern.matcher(text);

        StringBuilder result = new StringBuilder();
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
        return result.toString();
    }
}
