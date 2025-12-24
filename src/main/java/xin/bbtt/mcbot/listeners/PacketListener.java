package xin.bbtt.mcbot.listeners;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.events.ReceivePacketEvent;

public class PacketListener extends SessionAdapter {
    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(packet instanceof MinecraftPacket minecraftPacket)) return;
        ReceivePacketEvent<?> event = new ReceivePacketEvent<>(minecraftPacket, MinecraftPacket.class);
        Bot.Instance.getPluginManager().events().callEvent(event);
    }
}
