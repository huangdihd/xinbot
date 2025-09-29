/*
 *   Copyright (C) 2024-2025 huangdihd
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.RegisteredCommand;
import xin.bbtt.mcbot.command.TabExecutor;
import xin.bbtt.mcbot.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class HelpCommandExecutor extends TabExecutor {
    private final Logger log = LoggerFactory.getLogger(HelpCommandExecutor.class.getSimpleName());

    private void printCommandHelp(Command command, Plugin plugin) {
        log.info("Command: {}", command.getName());
        log.info("\tPlugin: {}", plugin.getName());
        log.info("\tAliases: [ {} ]", String.join(", ", command.getAliases()));
        log.info("\tDescription: {}", command.getDescription());
        log.info("\tUsage: {}", command.getUsage());
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
            log.info("no such command: {}", args[0]);
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
}
