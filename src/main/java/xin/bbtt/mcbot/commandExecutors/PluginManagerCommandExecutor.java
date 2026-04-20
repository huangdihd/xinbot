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

package xin.bbtt.mcbot.commandExecutors;

import org.jetbrains.annotations.Nullable;

import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.LangManager;
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.CommandExecutor;
import xin.bbtt.mcbot.command.SubCommandExecutor;
import xin.bbtt.mcbot.plugin.RegisteredPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static xin.bbtt.mcbot.Utils.parseConditionalHighlight;
import static xin.bbtt.mcbot.Utils.parseContainHighlight;

public class PluginManagerCommandExecutor extends SubCommandExecutor {
    private final static Logger log = LoggerFactory.getLogger(PluginManagerCommandExecutor.class.getSimpleName());

    public PluginManagerCommandExecutor() {
        registerSubCommand("list", new ListCommand());
        registerSubCommand("load", new LoadCommand());
        registerSubCommand("unload", new UnloadCommand());
        registerSubCommand("reload", new ReloadCommand());
        registerSubCommand("enable", new EnableCommand());
        registerSubCommand("disable", new DisableCommand());
        registerSubCommand("re-enable", new ReEnableCommand());
        registerSubCommand("tree", new TreeCommand());
    }

    @Override
    protected void onNoSubCommand(Command command, String label) {
        log.error(LangManager.get("xinbot.plugin.command.usage"));
    }

    @Nullable
    private static RegisteredPlugin findPlugin(String pluginName) {
        RegisteredPlugin plugin = Bot.Instance.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            log.error(LangManager.get("xinbot.plugin.not.found.name", pluginName));
        }
        return plugin;
    }

    private static class TreeCommand extends CommandExecutor {
        @Override
        public void onCommand(Command command, String label, String[] args) {
            log.info(LangManager.get("xinbot.plugin.tree.header"));
            java.util.Map<String, java.util.List<String>> deps = Bot.Instance.getPluginManager().getPluginDependencies();
            
            // Build dependents map (who depends on me)
            java.util.Map<String, java.util.List<String>> dependents = new java.util.HashMap<>();
            for (RegisteredPlugin plugin : Bot.Instance.getPluginManager().getPlugins()) {
                dependents.putIfAbsent(plugin.getName(), new java.util.ArrayList<>());
            }
            
            for (java.util.Map.Entry<String, java.util.List<String>> entry : deps.entrySet()) {
                String dependentPlugin = entry.getKey();
                for (String dependency : entry.getValue()) {
                    dependents.computeIfAbsent(dependency, k -> new java.util.ArrayList<>()).add(dependentPlugin);
                }
            }
            
            // Find plugins with inDegree == 0 (no loaded dependencies)
            for (RegisteredPlugin plugin : Bot.Instance.getPluginManager().getPlugins()) {
                java.util.List<String> myDeps = deps.getOrDefault(plugin.getName(), java.util.Collections.emptyList());
                boolean hasLoadedDeps = false;
                for (String dep : myDeps) {
                    if (Bot.Instance.getPluginManager().isPluginLoaded(dep)) {
                        hasLoadedDeps = true;
                        break;
                    }
                }
                
                if (!hasLoadedDeps) {
                    printTree(plugin.getName(), dependents, "", true, new java.util.HashSet<>());
                }
            }
        }

        private void printTree(String pluginName, java.util.Map<String, java.util.List<String>> dependents, String prefix, boolean isTail, java.util.Set<String> visited) {
            log.info(prefix + (isTail ? "└── " : "├── ") + pluginName);
            if (visited.contains(pluginName)) {
                log.info(prefix + (isTail ? "    " : "│   ") + "└── [Circular Reference]");
                return;
            }
            visited.add(pluginName);
            java.util.List<String> children = dependents.getOrDefault(pluginName, java.util.Collections.emptyList());
            for (int i = 0; i < children.size(); i++) {
                printTree(children.get(i), dependents, prefix + (isTail ? "    " : "│   "), i == children.size() - 1, new java.util.HashSet<>(visited));
            }
        }
    }

    private static class ListCommand extends CommandExecutor {
        @Override
        public void onCommand(Command command, String label, String[] args) {
            log.info(LangManager.get("xinbot.plugin.list.header"));
            for (RegisteredPlugin plugin : Bot.Instance.getPluginManager().getPlugins()) {
                log.info(LangManager.get("xinbot.plugin.list.item", plugin.getName(), plugin.getVersion()));
            }
        }
    }

    private static class LoadCommand extends CommandExecutor {
        @Override
        public void onCommand(Command command, String label, String[] args) {
            if (args.length < 1) {
                log.error(LangManager.get("xinbot.plugin.command.load.usage"));
                return;
            }
            File dir = new File(Bot.Instance.getConfig().getConfigData().getPlugin().getDirectory());
            if (!dir.exists() || !dir.isDirectory()) {
                log.error(LangManager.get("xinbot.plugin.dir.not.found"));
                return;
            }
            File file = new File(dir, args[0]);
            if (!file.exists() || !file.isFile()) {
                log.error(LangManager.get("xinbot.plugin.file.not.found"));
                return;
            }
            try {
                Bot.Instance.getPluginManager().loadPlugin(file);
            } catch (Exception e) {
                log.error(LangManager.get("xinbot.plugin.load.failed", file.getName()), e);
            }
        }

        @Override
        public List<String> onTabComplete(Command cmd, String label, String[] args) {
            File dir = new File(Bot.Instance.getConfig().getConfigData().getPlugin().getDirectory());
            if (!dir.exists() || !dir.isDirectory()) {
                return List.of();
            }

            File[] files = dir.listFiles((dir1, name) -> name.endsWith(".jar"));
            if (files == null || files.length == 0) {
                return List.of();
            }
            List<String> result = new ArrayList<>(Stream.of(files).map(File::getName).toList());
            result.removeAll(List.of(args));
            return result;
        }

        @Override
        public AttributedStyle[] onHighlight(Command cmd, String label, String[] args) {
            File dir = new File(Bot.Instance.getConfig().getConfigData().getPlugin().getDirectory());
            File[] filesArray = dir.listFiles((dir1, name) -> name.endsWith(".jar"));
            if (filesArray == null) {
                return parseContainHighlight(args, List.of());
            }
            List<String> pluginFils = Arrays.stream(filesArray).map(File::getName).toList();
            return parseContainHighlight(args, pluginFils);
        }
    }

    private static class UnloadCommand extends CommandExecutor {
        @Override
        public void onCommand(Command command, String label, String[] args) {
            if (args.length < 1) {
                log.error(LangManager.get("xinbot.plugin.command.unload.usage"));
                return;
            }
            for (String pluginName : args) {
                if (pluginName.equals("XinbotPlugin")) {
                    log.error(LangManager.get("xinbot.plugin.unload.xinbot.denied"));
                    continue;
                }
                RegisteredPlugin plugin = findPlugin(pluginName);
                if (plugin == null) continue;
                try {
                    Bot.Instance.getPluginManager().unloadPlugin(plugin);
                } catch (Exception e) {
                    log.error(LangManager.get("xinbot.plugin.unload.failed", plugin.getName()), e);
                }
            }
        }

        @Override
        public List<String> onTabComplete(Command cmd, String label, String[] args) {
            List<String> result = new ArrayList<>(Bot.Instance.getPluginManager().getPlugins().stream().map(RegisteredPlugin::getName).toList());
            result.removeAll(List.of(args));
            result.remove("XinbotPlugin");
            return result;
        }

        @Override
        public AttributedStyle[] onHighlight(Command cmd, String label, String[] args) {
            Predicate<String> isPluginLoaded = Bot.Instance.getPluginManager()::isPluginLoaded;
            return parseConditionalHighlight(args, isPluginLoaded.and(plugin -> !plugin.equals("XinbotPlugin")));
        }
    }

    private static class ReloadCommand extends CommandExecutor {
        @Override
        public void onCommand(Command command, String label, String[] args) {
            if (args.length < 1) {
                log.error(LangManager.get("xinbot.plugin.command.reload.usage"));
                return;
            }
            for (String pluginName : args) {
                RegisteredPlugin plugin = findPlugin(pluginName);
                if (plugin == null) continue;
                try {
                    Bot.Instance.getPluginManager().unloadPlugin(plugin);
                } catch (Exception e) {
                    log.error(LangManager.get("xinbot.plugin.unload.failed", plugin.getName()), e);
                    continue;
                }
                try {
                    Bot.Instance.getPluginManager().loadPlugin(plugin);
                } catch (Exception e) {
                    log.error(LangManager.get("xinbot.plugin.load.failed", plugin.getName()), e);
                }
            }
        }

        @Override
        public List<String> onTabComplete(Command cmd, String label, String[] args) {
            List<String> result = new ArrayList<>(Bot.Instance.getPluginManager().getPlugins().stream().map(RegisteredPlugin::getName).toList());
            result.removeAll(List.of(args));
            return result;
        }

        @Override
        public AttributedStyle[] onHighlight(Command cmd, String label, String[] args) {
            Predicate<String> isPluginLoaded = Bot.Instance.getPluginManager()::isPluginLoaded;
            return parseConditionalHighlight(args, isPluginLoaded);
        }
    }

    private static class EnableCommand extends CommandExecutor {
        @Override
        public void onCommand(Command command, String label, String[] args) {
            if (args.length < 1) {
                log.error(LangManager.get("xinbot.plugin.command.enable.usage"));
            }
            for (String pluginName : args) {
                RegisteredPlugin plugin = findPlugin(pluginName);
                if (plugin == null) continue;
                try {
                    Bot.Instance.getPluginManager().enablePlugin(plugin);
                } catch (Exception e) {
                    log.error(LangManager.get("xinbot.plugin.enable.failed", plugin.getName()), e);
                }
            }
        }

        @Override
        public List<String> onTabComplete(Command cmd, String label, String[] args) {
            List<String> result = new ArrayList<>(Bot.Instance.getPluginManager().getPlugins().stream().map(RegisteredPlugin::getName).toList());
            result.removeAll(List.of(args));
            return result;
        }

        @Override
        public AttributedStyle[] onHighlight(Command cmd, String label, String[] args) {
            Predicate<String> isPluginLoaded = Bot.Instance.getPluginManager()::isPluginLoaded;
            return parseConditionalHighlight(args, isPluginLoaded);
        }
    }

    private static class DisableCommand extends CommandExecutor {
        @Override
        public void onCommand(Command command, String label, String[] args) {
            if (args.length < 1) {
                log.error(LangManager.get("xinbot.plugin.command.disable.usage"));
            }
            for (String pluginName : args) {
                if (pluginName.equals("XinbotPlugin")) {
                    log.error(LangManager.get("xinbot.plugin.disable.xinbot.denied"));
                    continue;
                }
                RegisteredPlugin plugin = findPlugin(pluginName);
                if (plugin == null) continue;
                try {
                    Bot.Instance.getPluginManager().disablePlugin(plugin);
                } catch (Exception e) {
                    log.error(LangManager.get("xinbot.plugin.disable.failed", plugin.getName()), e);
                }
            }
        }

        @Override
        public List<String> onTabComplete(Command cmd, String label, String[] args) {
            List<String> result = new ArrayList<>(Bot.Instance.getPluginManager().getPlugins().stream().map(RegisteredPlugin::getName).toList());
            result.removeAll(List.of(args));
            result.remove("XinbotPlugin");
            return result;
        }

        @Override
        public AttributedStyle[] onHighlight(Command cmd, String label, String[] args) {
            Predicate<String> isPluginLoaded = Bot.Instance.getPluginManager()::isPluginLoaded;
            return parseConditionalHighlight(args, isPluginLoaded.and(plugin -> !plugin.equals("XinbotPlugin")));
        }
    }

    private static class ReEnableCommand extends CommandExecutor {
        @Override
        public void onCommand(Command command, String label, String[] args) {
            if (args.length < 1) {
                log.error(LangManager.get("xinbot.plugin.command.reenable.usage"));
            }
            for (String pluginName : args) {
                RegisteredPlugin plugin = findPlugin(pluginName);
                if (plugin == null) continue;
                try {
                    Bot.Instance.getPluginManager().disablePlugin(plugin);
                } catch (Exception e) {
                    log.error(LangManager.get("xinbot.plugin.disable.failed", plugin.getName()), e);
                    continue;
                }
                try {
                    Bot.Instance.getPluginManager().enablePlugin(plugin);
                } catch (Exception e) {
                    log.error(LangManager.get("xinbot.plugin.enable.failed", plugin.getName()), e);
                }
            }
        }

        @Override
        public List<String> onTabComplete(Command cmd, String label, String[] args) {
            List<String> result = new ArrayList<>(Bot.Instance.getPluginManager().getPlugins().stream().map(RegisteredPlugin::getName).toList());
            result.removeAll(List.of(args));
            return result;
        }

        @Override
        public AttributedStyle[] onHighlight(Command cmd, String label, String[] args) {
            Predicate<String> isPluginLoaded = Bot.Instance.getPluginManager()::isPluginLoaded;
            return parseConditionalHighlight(args, isPluginLoaded);
        }
    }
}
