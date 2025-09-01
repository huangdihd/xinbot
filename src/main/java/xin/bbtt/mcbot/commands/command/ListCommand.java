package xin.bbtt.mcbot.commands.command;

import xin.bbtt.mcbot.command.Command;

public class ListCommand extends Command {

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"list"};
    }

    @Override
    public String getDescription() {
        return "Shows the list of players";
    }

    @Override
    public String getUsage() {
        return "list / list uuid";
    }
}
