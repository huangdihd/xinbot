/*
 *   Copyright (C) 2026 huangdihd
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

package xin.bbtt.mcbot.commands.command;

import xin.bbtt.mcbot.command.Command;

public class CommandCommand extends Command {

    @Override
    public String getName() {
        return "command";
    }

    @Override
    public String[] getAliases() {
        return new String[] { "command" , "cmd" };
    }

    @Override
    public String getDescription() {
        return "A command to send commands.";
    }

    @Override
    public String getUsage() {
        return "command command";
    }
}
