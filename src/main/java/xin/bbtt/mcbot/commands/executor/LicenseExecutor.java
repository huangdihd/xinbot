package xin.bbtt.mcbot.commands.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Xinbot;
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.CommandExecutor;

import java.util.Arrays;

public class LicenseExecutor extends CommandExecutor {
    private static final Logger log = LoggerFactory.getLogger(LicenseExecutor.class.getSimpleName());
    @Override
    public void onCommand(Command command, String label, String[] args) {
        Arrays.stream(Xinbot.license.split("\n")).toList().forEach(log::info);
    }
}
