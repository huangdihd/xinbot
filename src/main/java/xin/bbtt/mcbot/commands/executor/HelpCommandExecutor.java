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

package xin.bbtt.mcbot.commands.executor;


import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.LangManager;
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.RegisteredCommand;
import xin.bbtt.mcbot.command.TabHighlightExecutor;
import xin.bbtt.mcbot.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class HelpCommandExecutor extends TabHighlightExecutor {
    private final Logger log = LoggerFactory.getLogger(HelpCommandExecutor.class.getSimpleName());

    private void printCommandHelp(Command command, Plugin plugin) {
        log.info(LangManager.get("command.help.name", command.getName()));
        log.info(LangManager.get("command.help.plugin", plugin.getName()));
        log.info(LangManager.get("command.help.aliases", String.join(", ", command.getAliases())));
        log.info(LangManager.get("command.help.description", command.getDescription()));
        log.info(LangManager.get("command.help.usage", command.getUsage()));
    }

    @Override
    public void onCommand(Command command, String label, String[] args) {
        if (args.length == 0) {
            for (Plugin plugin : Bot.Instance.getPluginManager().getPlugins()) {
                for (RegisteredCommand cmd : Bot.Instance.getPluginManager().commands().getCommandsByPlugin(plugin)) {
                    printCommandHelp(cmd.command(), plugin);
                }
            }
        }
        else if (args.length == 1) {
            for (Plugin plugin : Bot.Instance.getPluginManager().getPlugins()) {
                for (RegisteredCommand cmd : Bot.Instance.getPluginManager().commands().getCommandsByPlugin(plugin)) {
                    if (!cmd.command().getName().equalsIgnoreCase(args[0])) continue;
                    printCommandHelp(cmd.command(), plugin);
                    return;
                }
            }
            log.info(LangManager.get("command.not.found", args[0]));
        }
    }

    @Override
    public List<String> onTabComplete(Command command, String label, String[] args) {
        if (args.length > 1) return List.of();
        List<String> list = new ArrayList<>();
        for (Plugin plugin : Bot.Instance.getPluginManager().getPlugins()) {
            for (RegisteredCommand cmd : Bot.Instance.getPluginManager().commands().getCommandsByPlugin(plugin)) {
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
