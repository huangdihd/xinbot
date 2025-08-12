/*
 * # Copyright (C) 2025 huangdihd
 * #
 * # This program is free software: you can redistribute it and/or modify
 * # it under the terms of the GNU General Public License as published by
 * # the Free Software Foundation, either version 3 of the License, or
 * # (at your option) any later version.
 * #
 * # This program is distributed in the hope that it will be useful,
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * # GNU General Public License for more details.
 * #
 * # You should have received a copy of the GNU General Public License
 * # along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package xin.bbtt.mcbot.command;

import xin.bbtt.mcbot.plugin.Plugin;

import java.util.List;

public record RegisteredCommand(Plugin plugin, Command command, CommandExecutor executor) {
    void callCommand(String label, String[] args) {
        executor.onCommand(command, label, args);
    }
    List<String> callComplete(String label, String[] args) {
        return executor.onTabComplete(command, label, args);

    }

}
