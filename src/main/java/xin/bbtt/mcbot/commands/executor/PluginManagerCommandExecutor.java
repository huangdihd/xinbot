package xin.bbtt.mcbot.commands.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.TabExecutor;
import xin.bbtt.mcbot.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PluginManagerCommandExecutor extends TabExecutor {
    private final static Logger log = LoggerFactory.getLogger(ListCommandExecutor.class.getSimpleName());

    @Override
    public void onCommand(Command command, String label, String[] args) {
        if (args.length == 0) {
            log.error("PluginManager list / PluginManager load <plugin file name> / PluginManager unload <plugin name> / PluginManager reload <plugin name> / PluginManager enable <plugin name> / PluginManager disable <plugin name> / PluginManager reenable <plugin name>");
            return;
        }
        switch (args[0]) {
            case "list" -> {
                log.info("Plugins:");
                for (Plugin plugin : Bot.Instance.getPluginManager().getPlugins()) {
                    log.info(plugin.getName());
                }
            }
            case "load" -> {
                if (args.length < 2) {
                    log.error("PluginManager load <plugin file name>");
                    return;
                }
                File dir = new File(Bot.Instance.getConfig().getPlugin().getDirectory());
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
            case "unload" -> {
                if (args.length < 2) {
                    log.error("PluginManager unload <plugin name>");
                    return;
                }
                Plugin plugin = Bot.Instance.getPluginManager().getPlugin(args[1]);
                if (plugin == null) {
                    log.error("Plugin not found.");
                    return;
                }
                try {
                    Bot.Instance.getPluginManager().unloadPlugin(plugin);
                } catch (Exception e) {
                    log.error("Failed to unload plugin: {}", plugin.getName(), e);
                }
            }
            case "reload" -> {
                if (args.length < 2) {
                    log.error("PluginManager reload <plugin name>");
                    return;
                }
                Plugin plugin = Bot.Instance.getPluginManager().getPlugin(args[1]);
                if (plugin == null) {
                    log.error("Plugin not found.");
                    return;
                }
                Bot.Instance.getPluginManager().unloadPlugin(plugin);
                try {
                    Bot.Instance.getPluginManager().loadPlugin(plugin);
                } catch (Exception e) {
                    log.error("Failed to load plugin: {}", plugin.getName(), e);
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(Command cmd, String label, String[] args) {
        if  (args.length >= 2) {
            switch (args[0]) {
                case "disable", "unload", "reload", "enable", "reenable" -> {
                    List<String> result = new ArrayList<>(Bot.Instance.getPluginManager().getPlugins().stream().map(Plugin::getName).toList());
                    result.removeAll(List.of(args).subList(1, args.length));
                    return result;
                }
                case "load" -> {
                    File dir = new File(Bot.Instance.getConfig().getPlugin().getDirectory());
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
        return List.of("list", "load", "unload", "reload", "enable", "disable", "reenable");
    }
}
