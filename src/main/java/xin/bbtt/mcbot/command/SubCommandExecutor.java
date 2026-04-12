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


import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubCommandExecutor extends TabHighlightExecutor {
    private final Map<String, CommandExecutor> subCommandMap = new HashMap<>();
    private final Logger log = LoggerFactory.getLogger(CommandManager.class.getSimpleName());

    public void registerSubCommand(String name, CommandExecutor executor) {
        subCommandMap.put(name.toLowerCase(), executor);
    }

    @Override
    public void onCommand(Command command, String label, String[] args) {
        if (args.length == 0) {
            onNoSubCommand(command, label);
            return;
        }

        String subLabel = args[0].toLowerCase();
        CommandExecutor executor = subCommandMap.get(subLabel);

        if (executor != null) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            executor.onCommand(command, subLabel, subArgs);
        } else {
            onUnknownSubCommand(command, label, subLabel);
        }
    }

    @Override
    public List<String> onTabComplete(Command command, String label, String[] args) {
        if (args.length <= 1) {
            List<String> completions = new ArrayList<>();
            String current = args.length == 1 ? args[0].toLowerCase() : "";
            for (String subName : subCommandMap.keySet()) {
                if (subName.startsWith(current)) {
                    completions.add(subName);
                }
            }
            return completions;
        } else {
            String subLabel = args[0].toLowerCase();
            CommandExecutor executor = subCommandMap.get(subLabel);
            if (executor != null) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return executor.onTabComplete(command, subLabel, subArgs);
            }
        }
        return List.of();
    }

    @Override
    public AttributedStyle[] onHighlight(Command command, String label, String[] args) {
        if (args.length == 0) {
            return new AttributedStyle[0];
        }
        
        String subLabel = args[0].toLowerCase();
        CommandExecutor executor = subCommandMap.get(subLabel);
        
        AttributedStyle[] result = new AttributedStyle[args.length];
        
        if (executor != null) {
            result[0] = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
            
            if (args.length > 1) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                AttributedStyle[] subHighlight = executor.onHighlight(command, subLabel, subArgs);
                System.arraycopy(subHighlight, 0, result, 1, Math.min(subHighlight.length, args.length - 1));
            }
        } else {
            for (int i = 0; i < args.length; i++) {
                result[i] = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
            }
        }
        return result;
    }

    protected void onNoSubCommand(Command command, String label) {
        log.error("Missing sub-command. Available sub-commands: {}", String.join(", ", subCommandMap.keySet()));
    }

    protected void onUnknownSubCommand(Command command, String label, String unknownSub) {
        log.error("Unknown sub-command: {}. Available sub-commands: {}", unknownSub, String.join(", ", subCommandMap.keySet()));
    }
}
