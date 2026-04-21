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

package xin.bbtt.mcbot.command;

import lombok.Getter;

public class Command {
    @Getter
    private final String name;
    @Getter
    private final String[] aliases;
    private final String description;
    private final String usage;

    public Command(String name, String[] aliases, String description, String usage) {
        this.name = name;
        java.util.List<String> aliasList = new java.util.ArrayList<>();
        aliasList.add(name);
        if (aliases != null) {
            for (String alias : aliases) {
                if (aliasList.stream().noneMatch(a -> a.equalsIgnoreCase(alias))) {
                    aliasList.add(alias);
                }
            }
        }
        this.aliases = aliasList.toArray(new String[0]);
        this.description = description != null ? description : "";
        this.usage = usage != null ? usage : "";
    }

    public String getDescription() {
        return xin.bbtt.mcbot.LangManager.get(description);
    }

    public String getUsage() {
        return xin.bbtt.mcbot.LangManager.get(usage);
    }
}
