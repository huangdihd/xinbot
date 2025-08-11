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

