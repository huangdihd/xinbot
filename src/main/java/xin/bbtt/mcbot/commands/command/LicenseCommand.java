package xin.bbtt.mcbot.commands.command;

import xin.bbtt.mcbot.command.Command;

public class LicenseCommand extends Command {
    @Override
    public String getName() {
        return "license";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"license", "lic"};
    }

    @Override
    public String getDescription() {
        return "Show the license of Xinbot";
    }

    @Override
    public String getUsage() {
        return "license";
    }
}
