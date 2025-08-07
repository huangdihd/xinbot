package xin.bbtt.mcbot.JLine;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandSuggestionsPacket;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CommandSuggestionsProcessor extends SessionAdapter {
    private final CompletableFuture<List<String>> future;
    private final int transactionId;

    public CommandSuggestionsProcessor(CompletableFuture<List<String>> future, int transactionId) {
        super();
        this.future = future;
        this.transactionId = transactionId;
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(packet instanceof ClientboundCommandSuggestionsPacket commandSuggestionsPacket)) return;
        if (commandSuggestionsPacket.getTransactionId() != transactionId) return;
        List<String> result = List.of(commandSuggestionsPacket.getMatches());
        future.complete(result);
        session.removeListener(this);
    }
}
