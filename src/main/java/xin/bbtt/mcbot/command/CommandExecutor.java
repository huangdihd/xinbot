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

package xin.bbtt.mcbot.command;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.List;

public abstract class CommandExecutor {
    public abstract void onCommand(Command command, String label, String[] args);
    public List<String> onTabComplete(Command cmd, String label, String[] args) {
        return List.of();
    }
    public AttributedString onHighlight(Command cmd, String label, String[] args) {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        for (String token : args) {
            builder
                .append(token, AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                .append(" ");
        }
        return builder.toAttributedString();
    }
}
