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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.CommandExecutor;

import java.util.stream.Collectors;


public class PluginsCommandExecutor extends CommandExecutor {
    private final static Logger log = LoggerFactory.getLogger(PluginsCommandExecutor.class.getSimpleName());
    @Override
    public void onCommand(Command command, String label, String[] args) {
        log.info("There are {} plugins loaded: {}",
                Bot.Instance.getPluginManager().getPlugins().size(),
                Bot.Instance.getPluginManager().getPlugins().parallelStream().map(
                        (plugin -> plugin.getName() + "(" + plugin.getVersion() + ")")
                ).collect(Collectors.joining(", "))
        );
    }
}
