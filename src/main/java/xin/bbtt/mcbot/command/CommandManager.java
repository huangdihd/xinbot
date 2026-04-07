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

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.plugin.Plugin;

import java.util.*;

import static xin.bbtt.mcbot.Utils.parseHighlight;

public class CommandManager {
    private static final Logger log = LoggerFactory.getLogger(CommandManager.class.getSimpleName());

    final Marker commandErrorMarker = MarkerFactory.getMarker("[CommandError]");

    private final Map<Plugin, List<RegisteredCommand>> byPlugin = new HashMap<>();

    public static List<String> tokenize(String commandLine) {
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

    public RegisteredCommand getCommandByLabel(String label) {
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

        if (tokens.isEmpty()) return getCommandNames("");
        if (tokens.size() == 1) {
            return getCommandNames(tokens.get(0));
        }

        String label = tokens.get(0);
        String[] args = tokens.subList(1, tokens.size()).toArray(new String[0]);

        RegisteredCommand registeredCommand = getCommandByLabel(label);

        if (registeredCommand == null) {
            return List.of();
        }

        try {
            return registeredCommand.callComplete(label, args);
        }
        catch (Exception e) {
            log.error(commandErrorMarker, "Error while Complete command {}", command, e);
        }
        return List.of();
    }

    private static class Token {
        final String value;
        final int start;
        final int end;

        Token(String value, int start, int end) {
            this.value = value;
            this.start = start;
            this.end = end;
        }
    }

    private static List<Token> tokenizeDetailed(String commandLine) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean insideQuotedSection = false;
        char currentQuoteChar = 0;
        int tokenStart = -1;

        for (int charIndex = 0; charIndex < commandLine.length(); charIndex++) {
            char currentChar = commandLine.charAt(charIndex);

            if (insideQuotedSection) {
                if (currentChar == '\\' && charIndex + 1 < commandLine.length()) {
                    currentToken.append(commandLine.charAt(++charIndex));
                } else if (currentChar == currentQuoteChar) {
                    insideQuotedSection = false;
                    tokens.add(new Token(currentToken.toString(), tokenStart, charIndex + 1));
                    currentToken.setLength(0);
                    tokenStart = -1;
                } else {
                    currentToken.append(currentChar);
                }
            } else {
                if (Character.isWhitespace(currentChar)) {
                    if (tokenStart != -1) {
                        tokens.add(new Token(currentToken.toString(), tokenStart, charIndex));
                        currentToken.setLength(0);
                        tokenStart = -1;
                    }
                } else if (currentChar == '"' || currentChar == '\'') {
                    insideQuotedSection = true;
                    currentQuoteChar = currentChar;
                    tokenStart = charIndex;
                } else {
                    if (tokenStart == -1) {
                        tokenStart = charIndex;
                    }
                    currentToken.append(currentChar);
                }
            }
        }
        if (tokenStart != -1) {
            tokens.add(new Token(currentToken.toString(), tokenStart, commandLine.length()));
        }
        if (!commandLine.isEmpty() && Character.isWhitespace(commandLine.charAt(commandLine.length() - 1))) {
            tokens.add(new Token("", commandLine.length(), commandLine.length()));
        }
        return tokens;
    }

    public AttributedString callHighlight(String command) {
        final AttributedStringBuilder builder = new AttributedStringBuilder();
        List<Token> tokens = tokenizeDetailed(command);
        if (tokens.isEmpty()) {
            builder.append(command);
            return builder.toAttributedString();
        }

        Token labelToken = tokens.get(0);
        String label = labelToken.value;
        RegisteredCommand registeredCommand = getCommandByLabel(label);

        // Prefix spaces
        builder.append(command.substring(0, labelToken.start));

        // Label
        AttributedStyle labelStyle = (registeredCommand == null)
                ? AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)
                : AttributedStyle.DEFAULT;
        builder.append(command.substring(labelToken.start, labelToken.end), labelStyle);

        if (tokens.size() == 1) {
            builder.append(command.substring(labelToken.end));
            return builder.toAttributedString();
        }

        String[] argValues = tokens.subList(1, tokens.size()).stream().map(t -> t.value).toArray(String[]::new);
        AttributedStyle[] highlightedStyles;

        if (registeredCommand == null) {
            highlightedStyles = parseHighlight(argValues);
        } else {
            highlightedStyles = registeredCommand.callHighlight(label, argValues);
        }

        int lastPos = labelToken.end;

        for (int i = 1; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            // Append gaps (spaces) between tokens
            builder.append(command.substring(lastPos, t.start));

            AttributedStyle style = (i - 1 < highlightedStyles.length) ? highlightedStyles[i - 1] : AttributedStyle.DEFAULT;

            builder.append(command.substring(t.start, t.end), style);
            lastPos = t.end;
        }

        // Trailing spaces
        builder.append(command.substring(lastPos));

        return builder.toAttributedString();
    }
}
