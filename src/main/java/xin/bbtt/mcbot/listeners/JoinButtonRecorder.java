package xin.bbtt.mcbot.listeners;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.Server;

public class JoinButtonRecorder extends SessionAdapter {

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(packet instanceof ClientboundContainerSetSlotPacket containerSetSlotPacket)) return;
        if (Bot.Instance.getServer().equals(Server.Xin)) return;
        if (!containerSetSlotPacket.toString().contains("加入游戏")) return;
        AutoLoginListener.join_button_slot = containerSetSlotPacket.getSlot() % 9;
    }
}
