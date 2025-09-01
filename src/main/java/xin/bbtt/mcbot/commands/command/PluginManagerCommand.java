package xin.bbtt.mcbot.commands.command;

import xin.bbtt.mcbot.command.Command;

public class PluginManagerCommand extends Command {
    @Override
    public String getName() {
        return "PluginManager";
    }

    @Override
    public String[] getAliases() {
        return new String[] { "PluginManager", "pm" };
    }

    @Override
    public String getDescription() {
        return "A command to manage plugins.";
    }

    @Override
    public String getUsage() {
        return "PluginManager list / PluginManager load <plugin file name> / PluginManager unload <plugin name> / PluginManager reload <plugin name> / PluginManager enable <plugin name> / PluginManager disable <plugin name> / PluginManager reenable <plugin name>";
    }
}
