package xin.bbtt.mcbot.commands.executor;

import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.CommandExecutor;

public class DisconnectExecutor extends CommandExecutor {
    @Override
    public void onCommand(Command command, String label, String[] args) {
        Bot.Instance.disconnect("Disconnected by command");
    }
}
