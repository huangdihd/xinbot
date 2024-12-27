package xin.bbtt.mcbot;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final String ANSI_RESET = "\u001B[97m";

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

    public static ArrayList<String> toStrings(Component component, String DefaultColorCode) {
        ArrayList<String> result = new ArrayList<>();
        if (component.toString().startsWith("TranslatableComponentImpl")) {
            result.add(component.toString().split("key=\"")[1].split("\"")[0]);
            return result;
        }
        String content = component.toString().replaceFirst("^TextComponentImpl\\{content=\"", "").split("\", style=")[0];
        String color = component.toString().split("color=")[1].split(", ")[0];
        result.add(colorMap.get(color));
        if (!content.isEmpty()) result.add(content);
        if (component.children().isEmpty()) {
            return result;
        }
        component.children().forEach(sub_component -> result.addAll(toStrings(sub_component, colorMap.get(color))));
        if (!color.equals("null")) result.add(DefaultColorCode);
        return result;
    }

    public static String parseColors(String text) {
        text = text.replace("§r§", "§");

        Pattern pattern = Pattern.compile("§([0-9a-fr])");
        Matcher matcher = pattern.matcher(text);

        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String code = matcher.group(1);
            if ("r".equals(code)) {
                matcher.appendReplacement(result, ANSI_RESET);
            } else {
                int colorCode = Integer.parseInt(code, 16);
                String ansiColor = ANSI_COLORS[colorCode];
                matcher.appendReplacement(result, ansiColor);
            }
        }
        matcher.appendTail(result);
        return "\u001B[97m " +  result + "\u001B[0m";
    }
}
