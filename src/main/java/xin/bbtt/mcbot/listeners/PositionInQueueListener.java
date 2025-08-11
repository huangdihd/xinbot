package xin.bbtt.mcbot.listeners;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import xin.bbtt.mcbot.Utils;

public class PositionInQueueListener extends SessionAdapter {
    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(packet instanceof ClientboundSystemChatPacket systemChatPacket)) return;
        if (systemChatPacket.isOverlay()) {
            if (Utils.toString(systemChatPacket.getContent()).startsWith("§0§lPosition in queue: §6§l")) {
                AutoLoginListener.last_action_time = System.currentTimeMillis();
            }
        }
    }
}
