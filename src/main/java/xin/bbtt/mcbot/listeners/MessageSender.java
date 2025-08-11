package xin.bbtt.mcbot.listeners;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.Server;
import xin.bbtt.mcbot.events.SendChatMessageEvent;
import xin.bbtt.mcbot.events.SendCommandEvent;

import java.time.Instant;
import java.util.BitSet;

public class MessageSender extends SessionAdapter {
    private static Long last_send_time = System.currentTimeMillis();

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (System.currentTimeMillis() - last_send_time < 3000 && Bot.Instance.getServer() == Server.Xin) return;
        if (Bot.Instance.getProtocol().getOutboundState() != ProtocolState.GAME) return;
        if (Bot.Instance.getProtocol().getInboundState() != ProtocolState.GAME) return;
        if (Bot.Instance.to_be_sent_messages.isEmpty()) return;
        if (Bot.Instance.to_be_sent_messages.get(0).startsWith("/")) {
            String command = Bot.Instance.to_be_sent_messages.get(0).replaceFirst("/", "");
            SendCommandEvent sendCommandEvent = new SendCommandEvent(command);
            Bot.Instance.getPluginManager().events().callEvent(sendCommandEvent);
            if (!sendCommandEvent.isDefaultActionCancelled())
                session.send(new ServerboundChatCommandPacket(sendCommandEvent.getCommand()));
        }
        else {
            String message = Bot.Instance.to_be_sent_messages.get(0);
            SendChatMessageEvent sendChatMessageEvent = new SendChatMessageEvent(message);
            Bot.Instance.getPluginManager().events().callEvent(sendChatMessageEvent);
            if (!sendChatMessageEvent.isDefaultActionCancelled())
                session.send(
                        new ServerboundChatPacket(
                                sendChatMessageEvent.getMessage(),
                                Instant.now().toEpochMilli(),
                                0L,
                                null,
                                0,
                                new BitSet()
                        )
                );
        }
        last_send_time = System.currentTimeMillis();
        Bot.Instance.to_be_sent_messages.remove(0);
    }
}
