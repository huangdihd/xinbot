/*
 * # Copyright (C) 2025 huangdihd
 * #
 * # This program is free software: you can redistribute it and/or modify
 * # it under the terms of the GNU General Public License as published by
 * # the Free Software Foundation, either version 3 of the License, or
 * # (at your option) any later version.
 * #
 * # This program is distributed in the hope that it will be useful,
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * # GNU General Public License for more details.
 * #
 * # You should have received a copy of the GNU General Public License
 * # along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package xin.bbtt.mcbot.JLine;

import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import xin.bbtt.mcbot.Bot;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class JLineServerCompleter implements Completer {
    private static final AtomicInteger transactionId = new AtomicInteger();

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String buffer = line.line();

        boolean isRootCommand = buffer.startsWith("/") && !buffer.contains(" ");
        String query = isRootCommand ? buffer.substring(1) : buffer;

        int id = transactionId.incrementAndGet();
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        Bot.Instance.addPacketListener(new CommandSuggestionsProcessor(future, id));
        Bot.Instance.getSession().send(new ServerboundCommandSuggestionPacket(id, query));
        List<String> results;
        try {
            results = future.get(200, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            results = List.of();
        }
        for (String result : results) {
            candidates.add(new Candidate((isRootCommand ? "/" : "") + result));
        }
    }
}

