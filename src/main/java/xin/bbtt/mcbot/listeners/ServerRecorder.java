package xin.bbtt.mcbot.listeners;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.Server;

public class ServerRecorder extends SessionAdapter {

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(packet instanceof ClientboundLoginPacket loginPacket)) return;
        if (loginPacket.toString().contains(", gameMode=ADVENTURE")) Bot.Instance.setServer(Server.Login);
        if (loginPacket.toString().contains(", gameMode=SURVIVAL")) Bot.Instance.setServer(Server.Xin);
    }
}
