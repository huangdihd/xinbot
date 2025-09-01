package xin.bbtt.mcbot.commands.command;

import xin.bbtt.mcbot.command.Command;

public class DisconnectCommand extends Command {

    @Override
    public String getName() {
        return "disconnect";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"disconnect", "dc"};
    }

    @Override
    public String getDescription() {
        return "A command to disconnect with server.";
    }

    @Override
    public String getUsage() {
        return "disconnect";
    }
}
