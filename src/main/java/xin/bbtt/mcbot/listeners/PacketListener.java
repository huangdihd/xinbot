package xin.bbtt.mcbot.listeners;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.events.ReceivePacketEvent;

public class PacketListener extends SessionAdapter {
    @Override
    public void packetReceived(Session session, Packet packet) {
        ReceivePacketEvent<? extends Packet> event = new ReceivePacketEvent<>(packet);
        Bot.Instance.getPluginManager().events().callEvent(event);
    }
}
