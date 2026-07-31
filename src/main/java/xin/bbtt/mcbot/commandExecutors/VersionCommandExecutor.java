/*
 * Copyright (C) 2024-2026 huangdihd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package xin.bbtt.mcbot.commandExecutors;

import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.LangManager;
import xin.bbtt.mcbot.Utils;
import xin.bbtt.mcbot.Xinbot;
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.TabHighlightExecutor;
import xin.bbtt.mcbot.plugin.RegisteredPlugin;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class VersionCommandExecutor extends TabHighlightExecutor {
    private static final Logger log = LoggerFactory.getLogger(VersionCommandExecutor.class);

    @Override
    public void onCommand(Command command, String label, String[] args) {
        if (args.length == 0) {
            log.info(LangManager.get("xinbot.version", Xinbot.VERSION));
        } else {
            String pluginName = args[0];
            RegisteredPlugin rp = Bot.INSTANCE.getPluginManager().getPlugin(pluginName);
            if (rp != null) {
                log.info(LangManager.get("xinbot.version", rp.getVersion()));
            } else {
                log.error(LangManager.get("xinbot.plugin.not.found.name", pluginName));
            }
        }
    }

    @Override
    public List<String> onTabComplete(Command cmd, String label, String[] args) {
        if (args.length == 1) {
            String lastArg = args[0].toLowerCase();
            return Bot.INSTANCE.getPluginManager().getPlugins().stream()
                    .map(RegisteredPlugin::getName)
                    .filter(name -> name.toLowerCase().startsWith(lastArg))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public AttributedStyle[] onHighlight(Command cmd, String label, String[] args) {
        return Utils.parseConditionalHighlight(args, Bot.INSTANCE.getPluginManager()::isPluginLoaded);
    }
}
