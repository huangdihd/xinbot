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
        return "PluginManager list / PluginManager load <plugin file name1> <plugin file name2> ... / PluginManager unload <plugin name1> <plugin name2> ... / PluginManager reload <plugin name1> <plugin name2> ... / PluginManager enable <plugin name1> <plugin name2> ... / PluginManager disable <plugin name1> <plugin name2> ... / PluginManager re-enable <plugin name1> <plugin name2> ...";
    }
}
