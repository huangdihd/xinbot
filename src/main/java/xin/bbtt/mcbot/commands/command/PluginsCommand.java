package xin.bbtt.mcbot.commands.command;

import xin.bbtt.mcbot.command.Command;

public class PluginsCommand extends Command {

    @Override
    public String getName() {
        return "plugins";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"plugins"};
    }

    @Override
    public String getDescription() {
        return "A command to show all plugins";
    }

    @Override
    public String getUsage() {
        return "plugins";
    }
}
