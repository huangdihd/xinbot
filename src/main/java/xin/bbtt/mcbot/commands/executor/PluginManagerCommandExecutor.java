/*
 *   Copyright (C) 2024-2026 huangdihd
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xin.bbtt.mcbot.commands.executor;

import org.jetbrains.annotations.Nullable;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.TabHighlightExecutor;
import xin.bbtt.mcbot.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static xin.bbtt.mcbot.Utils.parseConditionalHighlight;
import static xin.bbtt.mcbot.Utils.parseContainHighlight;

public class PluginManagerCommandExecutor extends TabHighlightExecutor {
    private final static Logger log = LoggerFactory.getLogger(PluginManagerCommandExecutor.class.getSimpleName());

    private void listPlugins() {
        log.info("Plugins:");
        for (Plugin plugin : Bot.Instance.getPluginManager().getPlugins()) {
            log.info("{}({})", plugin.getName(), plugin.getVersion());
        }
    }

    private void loadPlugins(String[] args) {
        if (args.length < 2) {
            log.error("PluginManager load <plugin file name1> <plugin file name2> ...");
            return;
        }
        File dir = new File(Bot.Instance.getConfig().getConfigData().getPlugin().getDirectory());
        if (!dir.exists() || !dir.isDirectory()) {
            log.error("Plugin directory not found.");
            return;
        }
        File file = new File(dir, args[1]);
        if (!file.exists() || !file.isFile()) {
            log.error("Plugin file not found.");
            return;
        }
        try {
            Bot.Instance.getPluginManager().loadPlugin(file);
        } catch (Exception e) {
            log.error("Failed to load plugin: {}", file.getName(), e);
        }
    }

    @Nullable
    private Plugin findPlugin(String pluginName) {
        Plugin plugin = Bot.Instance.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            log.error("Plugin {} not found.",  pluginName);
        }
        return plugin;
    }

    private void unloadPlugins(String[] args) {
        if (args.length < 2) {
            log.error("PluginManager unload <plugin name1> <plugin name2> ...");
            return;
        }
        for (String pluginName : Arrays.asList(args).subList(1, args.length)) {
            if (pluginName.equals("XinbotPlugin")) {
                log.error("Failed to unload plugin: XinbotPlugin because unloading the XinbotPlugin by commands is not allowed.");
                continue;
            }
            Plugin plugin = findPlugin(pluginName);
            if (plugin == null) continue;
            try {
                Bot.Instance.getPluginManager().unloadPlugin(plugin);
            } catch (Exception e) {
                log.error("Failed to unload plugin: {}", plugin.getName(), e);
            }
        }
    }

    private void reloadPlugins(String[] args) {
        if (args.length < 2) {
            log.error("PluginManager reload <plugin name1> <plugin name2> ...");
            return;
        }
        for (String pluginName : Arrays.asList(args).subList(1, args.length)) {
            Plugin plugin = findPlugin(pluginName);
            if (plugin == null) continue;
            try {
                Bot.Instance.getPluginManager().unloadPlugin(plugin);
            } catch (Exception e) {
                log.error("Failed to unload plugin: {}", plugin.getName(), e);
                continue;
            }
            try {
                Bot.Instance.getPluginManager().loadPlugin(plugin);
            } catch (Exception e) {
                log.error("Failed to load plugin: {}", plugin.getName(), e);
            }
        }
    }

    private void enablePlugins(String[] args) {
        if (args.length < 2) {
            log.error("PluginManager enable <plugin name1> <plugin name2> ...");
        }
        for (String pluginName : Arrays.asList(args).subList(1, args.length)) {
            Plugin plugin = findPlugin(pluginName);
            if (plugin == null) continue;
            try {
                Bot.Instance.getPluginManager().enablePlugin(plugin);
            } catch (Exception e) {
                log.error("Failed to enable plugin: {}", plugin.getName(), e);
            }
        }
    }

    private void disablePlugins(String[] args) {
        if (args.length < 2) {
            log.error("PluginManager disable <plugin name1> <plugin name2> ...");
        }
        for (String pluginName : Arrays.asList(args).subList(1, args.length)) {
            if (pluginName.equals("XinbotPlugin")) {
                log.error("Failed to disable plugin: XinbotPlugin because disabling the XinbotPlugin by commands is not allowed.");
                continue;
            }
            Plugin plugin = findPlugin(pluginName);
            if (plugin == null) continue;
            try {
                Bot.Instance.getPluginManager().disablePlugin(plugin);
            } catch (Exception e) {
                log.error("Failed to disable plugin: {}", plugin.getName(), e);
            }
        }
    }

    private void reEnablePlugins(String[] args) {
        if (args.length < 2) {
            log.error("PluginManager re-enable <plugin name1> <plugin name2> ...");
        }
        for (String pluginName : Arrays.asList(args).subList(1, args.length)) {
            Plugin plugin = findPlugin(pluginName);
            if (plugin == null) continue;
            try {
                Bot.Instance.getPluginManager().disablePlugin(plugin);
            } catch (Exception e) {
                log.error("Failed to disable plugin: {}", plugin.getName(), e);
                continue;
            }
            try {
                Bot.Instance.getPluginManager().enablePlugin(plugin);
            } catch (Exception e) {
                log.error("Failed to enable plugin: {}", plugin.getName(), e);
            }
        }
    }

    @Override
    public void onCommand(Command command, String label, String[] args) {
        if (args.length == 0) {
            // Show usage
            log.error("PluginManager list / PluginManager load <plugin file name1> <plugin file name2> ... / PluginManager unload <plugin name1> <plugin name2> ... / PluginManager reload <plugin name1> <plugin name2> ... / PluginManager enable <plugin name1> <plugin name2> ... / PluginManager disable <plugin name1> <plugin name2> ... / PluginManager re-enable <plugin name1> <plugin name2> ...");
            return;
        }
        // Call sub-command handler
        switch (args[0]) {
            case "list" -> listPlugins();
            case "load" -> loadPlugins(args);
            case "unload" -> unloadPlugins(args);
            case "reload" -> reloadPlugins(args);
            case "enable" -> enablePlugins(args);
            case "disable" -> disablePlugins(args);
            case "re-enable" -> reEnablePlugins(args);
        }
    }

    @Override
    public List<String> onTabComplete(Command cmd, String label, String[] args) {
        if (args.length == 1) return List.of("list", "load", "unload", "reload", "enable", "disable", "re-enable");
        switch (args[0]) {
            case "reload", "enable", "re-enable" -> {
                List<String> result = new ArrayList<>(Bot.Instance.getPluginManager().getPlugins().stream().map(Plugin::getName).toList());
                result.removeAll(List.of(args).subList(1, args.length));
                return result;
            }
            case "disable", "unload" -> {
                List<String> result = new ArrayList<>(Bot.Instance.getPluginManager().getPlugins().stream().map(Plugin::getName).toList());
                result.removeAll(List.of(args).subList(1, args.length));
                result.remove("XinbotPlugin");
                return result;
            }
            case "load" -> {
                File dir = new File(Bot.Instance.getConfig().getConfigData().getPlugin().getDirectory());
                if (!dir.exists() || !dir.isDirectory()) {
                    return List.of();
                }

                File[] files = dir.listFiles((dir1, name) -> name.endsWith(".jar"));
                if (files == null || files.length == 0) {
                    return List.of();
                }
                List<String> result = new ArrayList<>(Stream.of(files).map(File::getName).toList());
                result.removeAll(List.of(args).subList(1, args.length));
                return result;
            }
            default -> {
                return List.of();
            }
        }
    }

    @Override
    public AttributedString onHighlight(Command cmd, String label, String[] args) {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        String operate = args[0];

        builder
            .append(args[0],
                List.of(
                    "list", "load", "unload", "reload", "enable", "disable", "re-enable"
                ).contains(args[0]) ?
                AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE) :
                AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)
            )
            .append(" ");

        if (args.length == 1) return builder.toAttributedString();
        String[] plugins = Arrays.copyOfRange(args, 1, args.length);
        Predicate<String> isPluginLoaded = Bot.Instance.getPluginManager()::isPluginLoaded;

        switch (operate) {
            case "reload", "enable", "re-enable" ->
                builder.append(
                    parseConditionalHighlight(plugins, isPluginLoaded)
                );
            case "disable", "unload" ->
                builder.append(
                    parseConditionalHighlight(plugins, isPluginLoaded.and(plugin -> !plugin.equals("XinbotPlugin")))
                );
            case "load" -> {
                File dir = new File(Bot.Instance.getConfig().getConfigData().getPlugin().getDirectory());
                List<String> pluginFils = Arrays.stream(
                    Objects.requireNonNull(
                        dir.listFiles((dir1, name) -> name.endsWith(".jar"))
                    )
                ).map(File::getName).toList();
                builder.append(parseContainHighlight(plugins, pluginFils));
            }
            default -> builder.append(parseConditionalHighlight(plugins, plugin -> false));
        }
        return builder.toAttributedString();
    }
}
