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
import xin.bbtt.mcbot.command.Command;

import java.io.File;
import java.net.SocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Getter
public class RegisteredMetaPlugin extends RegisteredPlugin {

    public RegisteredMetaPlugin(String name, String version, String mainClass, List<String> depends, File file, URL url, MetaPlugin plugin, Map<String, Command> commands) {
        super(name, version, mainClass, depends, file, url, plugin, PluginType.META_PLUGIN, commands);
    }

    @Override
    public MetaPlugin getPlugin() {
        return (MetaPlugin) super.getPlugin();
    }

    public SocketAddress getServerSocketAddress() {
        return getPlugin().getServerSocketAddress();
    }
}
