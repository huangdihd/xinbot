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
import xin.bbtt.mcbot.Xinbot;
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.TabHighlightExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ReloadCommandExecutor extends TabHighlightExecutor {
    private static final Logger log = LoggerFactory.getLogger(ReloadCommandExecutor.class.getSimpleName());

    @Override
    public void onCommand(Command command, String label, String[] args) {
        String configPath = Xinbot.configPath;

        if (args.length > 0) {
            configPath = args[0];
            if (!configPath.endsWith(".conf")) {
                log.error(LangManager.get("xinbot.command.reload.invalid_extension"));
                return;
            }
        }

        File configFile = new File(configPath);
        if (!configFile.exists() || !configFile.isFile()) {
            log.error(LangManager.get("xinbot.config.file.not_found", configPath));
            return;
        }

        try {
            Bot.INSTANCE.reloadConfig(configPath);
            log.info(LangManager.get("xinbot.command.reload.success", configPath));
        } catch (Exception e) {
            log.error(LangManager.get("xinbot.command.reload.failed", configPath), e);
        }
    }

    @Override
    public List<String> onTabComplete(Command command, String label, String[] args) {
        if (args.length > 1) return List.of();
        
        List<String> confFiles = new ArrayList<>();
        File currentDir = new File(".");
        File[] files = currentDir.listFiles((dir, name) -> name.endsWith(".conf"));
        
        if (files != null) {
            for (File file : files) {
                confFiles.add(file.getName());
            }
        }
        
        return confFiles;
    }

    @Override
    public AttributedStyle[] onHighlight(Command command, String label, String[] args) {
        AttributedStyle[] styles = new AttributedStyle[args.length];
        if (args.length == 0) return styles;

        if (args[0].endsWith(".conf") && new File(args[0]).exists()) {
            styles[0] = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
        } else {
            styles[0] = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
        }
        
        for (int i = 1; i < args.length; i++) {
            styles[i] = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
        }
        return styles;
    }
}
