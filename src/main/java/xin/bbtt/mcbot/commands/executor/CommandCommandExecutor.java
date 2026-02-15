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

package xin.bbtt.mcbot.commands.executor;

import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.TabHighlightExecutor;
import xin.bbtt.mcbot.listeners.CommandSuggestionsListener;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static xin.bbtt.mcbot.Utils.parseHighlight;
import static xin.bbtt.mcbot.listeners.CommandsRecorder.rootCommands;

public class CommandCommandExecutor extends TabHighlightExecutor {
    private static int transactionId = 0;

    @Override
    public void onCommand(Command command, String label, String[] args) {
        String cmd = String.join(" ", args);
        Bot.Instance.sendCommand(cmd);
    }

    @Override
    public List<String> onTabComplete(Command command, String label, String[] args) {
        if (args.length == 1 && args[0].isEmpty()) {
            return rootCommands;
        }
        String cmd = String.join(" ", args);
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        Bot.Instance.getSession().addListener(new CommandSuggestionsListener(future, transactionId));
        Bot.Instance.getSession().send(new ServerboundCommandSuggestionPacket(transactionId, cmd));
        List<String> results;
        try {
            results = future.get(200, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            results = List.of();
        }
        transactionId++;
        return results;
    }

    @Override
    public AttributedString onHighlight(Command cmd, String label, String[] args) {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.append(args[0], rootCommands.contains(args[0]) ?
            AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE) :
            AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)
        );
        if (args.length == 1) return builder.toAttributedString();
        String[] commandArgs = Arrays.stream(args).toList().subList(1, args.length).toArray(new String[0]);
        builder.append(" ");
        builder.append(parseHighlight(commandArgs));
        return builder.toAttributedString();
    }
}
