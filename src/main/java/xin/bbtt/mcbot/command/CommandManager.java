/*
 *   Copyright (C) 2024-2025 huangdihd
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.plugin.Plugin;

import java.util.*;

public class CommandManager {
    private static final Logger log = LoggerFactory.getLogger(CommandManager.class.getSimpleName());

    final Marker commandErrorMarker = MarkerFactory.getMarker("[CommandError]");

    private final Map<Plugin, List<RegisteredCommand>> byPlugin = new HashMap<>();

    private static List<String> tokenize(String commandLine) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean insideQuotedSection = false;
        char currentQuoteChar = 0;

        for (int charIndex = 0; charIndex < commandLine.length(); charIndex++) {
            char currentChar = commandLine.charAt(charIndex);

            if (insideQuotedSection) {
                if (currentChar == '\\' && charIndex + 1 < commandLine.length()) {
                    currentToken.append(commandLine.charAt(++charIndex));
                } else if (currentChar == currentQuoteChar) {
                    insideQuotedSection = false;
                } else {
                    currentToken.append(currentChar);
                }
            } else {
                if (Character.isWhitespace(currentChar)) {
                    if (!currentToken.isEmpty()) {
                        tokens.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                } else if (currentChar == '"' || currentChar == '\'') {
                    insideQuotedSection = true;
                    currentQuoteChar = currentChar;
                } else {
                    currentToken.append(currentChar);
                }
            }
        }
        if (!currentToken.isEmpty()) {
            tokens.add(currentToken.toString());
        }
        if (!commandLine.isEmpty() && Character.isWhitespace(commandLine.charAt(commandLine.length() - 1))) {
            tokens.add("");
        }
        return tokens;
    }

    private RegisteredCommand getCommandByLabel(String label) {
        List<RegisteredCommand> commandList = new ArrayList<>(List.of());
        for (List<RegisteredCommand> commands : byPlugin.values()) {
            commandList.addAll(commands);
        }
        String commandName = label;

        if (label.contains(":")) {
            String[] parts = label.split(":");
            if (parts.length == 2) {
                String pluginName = parts[0];
                commandName = parts[1];
                Plugin plugin = Bot.Instance.getPluginManager().getPlugin(pluginName);
                if (plugin != null) {
                    commandList = byPlugin.get(plugin);
                }
            }
        }

        for (RegisteredCommand registeredCommand : commandList) {
            if (Arrays.asList(registeredCommand.command().getAliases()).contains(commandName)) {
                return registeredCommand;
            }
        }

        return null;
    }

    public Collection<RegisteredCommand> getCommandsByPlugin(Plugin plugin) {
        return byPlugin.getOrDefault(plugin, new ArrayList<>());
    }

    public void registerCommand(Command command, CommandExecutor executor, Plugin plugin) {
        RegisteredCommand registeredCommand = new RegisteredCommand(plugin, command, executor);
        byPlugin.computeIfAbsent(plugin, k -> new ArrayList<>()).add(registeredCommand);
    }

    public void unregisterAll(Plugin plugin) {
        byPlugin.remove(plugin);
    }

    public void callCommand(String command) {
        List<String> tokens = tokenize(command);
        if (tokens.isEmpty()) return;

        String label = tokens.get(0);
        String[] args = tokens.size() > 1
                ? tokens.subList(1, tokens.size()).toArray(new String[0])
                : new String[0];

        RegisteredCommand registeredCommand = getCommandByLabel(label);


        if (registeredCommand == null) {
            log.warn(commandErrorMarker, "No command found for command {}", label);
            return;
        }

        if (args.length > 0 && args[args.length - 1].isEmpty()) {
            args = Arrays.copyOfRange(args, 0, args.length - 1);
        }

        try {
            registeredCommand.callCommand(label, args);
        }
        catch (Exception e) {
            log.error(commandErrorMarker, "Error while executing command {}", command, e);
        }
    }

    public List<String> getCommandNames(String prefix) {
        List<String> names = new ArrayList<>();
        List<RegisteredCommand> commandList = new ArrayList<>(List.of());
        for (List<RegisteredCommand> commands : byPlugin.values()) {
            commandList.addAll(commands);
        }
        prefix = prefix.toLowerCase();
        for (RegisteredCommand registeredCommand : commandList) {
            for (String commandName : registeredCommand.command().getAliases()) {
                if (registeredCommand.plugin().getName().toLowerCase().startsWith(prefix)) {
                    names.add(registeredCommand.plugin().getName() + ":" + commandName);
                }
                if (commandName.toLowerCase().startsWith(prefix)) {
                    names.add(commandName);
                }
            }
        }
        return names;
    }

    public List<String> callComplete(String command) {
        List<String> tokens = tokenize(command);
        if (tokens.isEmpty()) return List.of();

        if (tokens.size() == 1) {
            return getCommandNames(tokens.get(0));
        }

        if (command.endsWith(" ")) {
            tokens.add("");
        }

        String label = tokens.get(0);
        String[] args = tokens.subList(1, tokens.size()).toArray(new String[0]);

        RegisteredCommand registeredCommand = getCommandByLabel(label);

        if (registeredCommand == null) {
            return List.of();
        }

        if (args.length > 0 && args[args.length - 1].isEmpty()) {
            args = Arrays.copyOfRange(args, 0, args.length - 1);
        }

        try {
            return registeredCommand.callComplete(label, args);
        }
        catch (Exception e) {
            log.error(commandErrorMarker, "Error while Complete command {}", command, e);
        }
        return List.of();
    }
}
