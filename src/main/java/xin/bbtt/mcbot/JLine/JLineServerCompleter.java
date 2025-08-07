package xin.bbtt.mcbot.JLine;

import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import xin.bbtt.mcbot.Bot;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class JLineServerCompleter implements Completer {
    private static final AtomicInteger transactionId = new AtomicInteger();
    private final Map<String, List<String>> cachedResults = new ConcurrentHashMap<>();

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String buffer = line.line();

        boolean isRootCommand = buffer.startsWith("/") && !buffer.contains(" ");
        String query = isRootCommand ? buffer.substring(1) : buffer;

        List<String> cached = cachedResults.getOrDefault(buffer, List.of());
        for (String suggestion : cached) {
            candidates.add(new Candidate(isRootCommand ? "/" + suggestion : suggestion));
        }

        int id = transactionId.incrementAndGet();
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        Bot.Instance.session.addListener(new CommandSuggestionsProcessor(future, id));
        Bot.Instance.session.send(new ServerboundCommandSuggestionPacket(id, query));

        future.thenAccept(results -> {
            cachedResults.put(buffer, results);
            reader.callWidget("redisplay");
        });
    }
}

