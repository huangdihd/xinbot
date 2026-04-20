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


import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.LangManager;
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.CommandExecutor;
import xin.bbtt.mcbot.command.RegisteredCommand;
import xin.bbtt.mcbot.command.SubCommandExecutor;
import xin.bbtt.mcbot.command.TabHighlightExecutor;
import xin.bbtt.mcbot.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HelpCommandExecutor extends TabHighlightExecutor {
    private final Logger log = LoggerFactory.getLogger(HelpCommandExecutor.class.getSimpleName());

    private void printCommandHelp(RegisteredCommand cmd) {
        Command command = cmd.command();
        Plugin plugin = cmd.plugin();
        log.info(LangManager.get("xinbot.command.help.name", command.getName()));
        log.info(LangManager.get("xinbot.command.help.plugin", Bot.Instance.getPluginManager().getPluginName(plugin)));
        log.info(LangManager.get("xinbot.command.help.aliases", String.join(", ", command.getAliases())));
        log.info(LangManager.get("xinbot.command.help.description", command.getDescription()));
        log.info(LangManager.get("xinbot.command.help.usage", command.getUsage()));

        if (cmd.executor() instanceof SubCommandExecutor subExec) {
            printSubCommands(subExec, "\t");
        }
    }

    private void printSubCommands(SubCommandExecutor executor, String prefix) {
        if (!executor.getSubCommands().isEmpty()) {
            log.info(prefix + LangManager.get("xinbot.command.help.subcommands", "Subcommands:"));
            for (Map.Entry<String, CommandExecutor> entry : executor.getSubCommands().entrySet()) {
                log.info(prefix + "  - " + entry.getKey());
                if (entry.getValue() instanceof SubCommandExecutor subExec) {
                    printSubCommands(subExec, prefix + "  ");
                }
            }
        }
    }

    @Override
    public void onCommand(Command command, String label, String[] args) {
        xin.bbtt.mcbot.command.CommandManager cm = Bot.Instance.getPluginManager().commands();
        if (args.length == 0) {
            // Print built-in commands
            for (RegisteredCommand cmd : cm.getCommandsByPlugin(null)) {
                printCommandHelp(cmd);
            }
            // Print plugin commands
            for (xin.bbtt.mcbot.plugin.RegisteredPlugin rp : Bot.Instance.getPluginManager().getPlugins()) {
                for (RegisteredCommand cmd : cm.getCommandsByPlugin(rp.getPlugin())) {
                    printCommandHelp(cmd);
                }
            }
        }
        else if (args.length == 1) {
            // Search built-in commands
            for (RegisteredCommand cmd : cm.getCommandsByPlugin(null)) {
                if (cmd.command().getName().equalsIgnoreCase(args[0])) {
                    printCommandHelp(cmd);
                    return;
                }
            }
            // Search plugin commands
            for (xin.bbtt.mcbot.plugin.RegisteredPlugin rp : Bot.Instance.getPluginManager().getPlugins()) {
                for (RegisteredCommand cmd : cm.getCommandsByPlugin(rp.getPlugin())) {
                    if (!cmd.command().getName().equalsIgnoreCase(args[0])) continue;
                    printCommandHelp(cmd);
                    return;
                }
            }
            log.info(LangManager.get("xinbot.command.not.found", args[0]));
        }
    }

    @Override
    public List<String> onTabComplete(Command command, String label, String[] args) {
        if (args.length > 1) return List.of();
        List<String> list = new ArrayList<>();
        xin.bbtt.mcbot.command.CommandManager cm = Bot.Instance.getPluginManager().commands();
        
        // Add built-in commands
        for (RegisteredCommand cmd : cm.getCommandsByPlugin(null)) {
            list.add(cmd.command().getName());
        }
        // Add plugin commands
        for (xin.bbtt.mcbot.plugin.RegisteredPlugin rp : Bot.Instance.getPluginManager().getPlugins()) {
            for (RegisteredCommand cmd : cm.getCommandsByPlugin(rp.getPlugin())) {
                list.add(cmd.command().getName());
            }
        }
        return list;
    }

    @Override
    public AttributedStyle[] onHighlight(Command command, String label, String[] args) {
        AttributedStyle[] styles = new AttributedStyle[args.length];
        if (args.length == 0) return styles;

        if (Bot.Instance.getPluginManager().commands().getCommandByLabel(args[0]) == null) {
            styles[0] = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
        } else {
            styles[0] = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
        }
        for (int i = 1; i < args.length; i++) {
            styles[i] = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
        }
        return styles;
    }
}
