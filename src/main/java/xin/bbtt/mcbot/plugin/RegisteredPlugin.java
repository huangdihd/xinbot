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

package xin.bbtt.mcbot.plugin;

import lombok.Getter;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Getter
public class RegisteredPlugin {
    private final String name;
    private final String version;
    private final String mainClass;
    private final List<String> depends;
    private final File file;
    private final URL url;
    private final PluginType type;
    private final Plugin plugin;

    public RegisteredPlugin(String name, String version, String mainClass, List<String> depends, File file, URL url, Plugin plugin, PluginType type) {
        this.name = name;
        this.version = version != null ? version : "1.0.0";
        this.mainClass = mainClass;
        this.depends = depends != null ? depends : new ArrayList<>();
        this.file = file;
        this.url = url;
        this.type = type;
        this.plugin = plugin;
    }
}
