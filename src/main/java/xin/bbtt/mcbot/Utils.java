package xin.bbtt.mcbot;

import net.kyori.adventure.text.Component;

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

    public static Map<String, String> colorMap = new HashMap<>();

    static {
        colorMap.put("NamedTextColor{name=\"black\"", "§0");
        colorMap.put("NamedTextColor{name=\"dark_blue\"", "§1");
        colorMap.put("NamedTextColor{name=\"dark_green\"", "§2");
        colorMap.put("NamedTextColor{name=\"dark_aqua\"", "§3");
        colorMap.put("NamedTextColor{name=\"dark_red\"", "§4");
        colorMap.put("NamedTextColor{name=\"dark_purple\"", "§5");
        colorMap.put("NamedTextColor{name=\"gold\"", "§6");
        colorMap.put("NamedTextColor{name=\"gray\"", "§7");
        colorMap.put("NamedTextColor{name=\"dark_gray\"", "§8");
        colorMap.put("NamedTextColor{name=\"blue\"", "§9");
        colorMap.put("NamedTextColor{name=\"green\"", "§a");
        colorMap.put("NamedTextColor{name=\"aqua\"", "§b");
        colorMap.put("NamedTextColor{name=\"red\"", "§c");
        colorMap.put("NamedTextColor{name=\"light_purple\"", "§d");
        colorMap.put("NamedTextColor{name=\"yellow\"", "§e");
        colorMap.put("NamedTextColor{name=\"white\"", "§f");
        colorMap.put("null", "");
    }

    public static String toString(Component component) {
        return String.join("", toStrings(component));
    }

    public static ArrayList<String> toStrings(Component component) {
        return toStrings(component, "§r");
    }

    public static ArrayList<String> toStrings(Component component, String defaultColorCode) {
        ArrayList<String> result = new ArrayList<>();

        String serialized = component.toString();

        // 处理 TranslatableComponentImpl 特例
        if (serialized.startsWith("TranslatableComponentImpl")) {
            String key = serialized.split("key=\"")[1].split("\"")[0];
            result.add(key);
            return result;
        }

        // 获取内容
        String content = serialized.replaceFirst("^TextComponentImpl\\{content=\"", "").split("\", style=")[0];

        // 获取颜色
        String color = "null";
        if (serialized.contains("color=")) {
            color = serialized.split("color=")[1].split(",")[0];
        }

        result.add(colorMap.getOrDefault(color, ""));
        if (!content.isEmpty()) result.add(content);

        if (!component.children().isEmpty()) {
            for (Component child : component.children()) {
                result.addAll(toStrings(child, colorMap.getOrDefault(color, "")));
            }
        }

        if (!"null".equals(color)) result.add(defaultColorCode);
        return result;
    }

    public static String parseColors(String text) {
        text = text.replace("§r§", "§");

        Pattern pattern = Pattern.compile("§([0-9a-fk-or])");
        Matcher matcher = pattern.matcher(text);

        StringBuilder result = new StringBuilder();
        int lastIndex = 0;

        while (matcher.find()) {
            result.append(text, lastIndex, matcher.start()); // 添加前面的普通字符
            String code = matcher.group(1);
            lastIndex = matcher.end();

            if (FORMAT_CODES.containsKey(code.charAt(0))) {
                result.append(FORMAT_CODES.get(code.charAt(0)));
            } else {
                int index = Integer.parseInt(code, 16);
                if (index >= 0 && index < ANSI_COLORS.length) {
                    result.append(ANSI_COLORS[index]);
                }
            }
        }

        result.append(text.substring(lastIndex));
        return "\u001B[97m " + result + "\u001B[0m";
    }
}
