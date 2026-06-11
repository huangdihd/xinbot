package xin.bbtt.mcbot.plugin;

import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import xin.bbtt.mcbot.Server;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

public class DummyMetaPlugin implements MetaPlugin {

    // Shared lifecycle event log so tests can assert ordering across plugins.
    public static final List<String> events = new ArrayList<>();

    @Override
    public void onLoad() {
        events.add("meta-load");
    }

    @Override
    public void onEnable() {
        events.add("meta-enable");
    }

    @Override
    public void onDisable() {
        events.add("meta-disable");
    }

    @Override
    public void onUnload() {
        events.add("meta-unload");
    }

    @Override
    public SocketAddress getServerSocketAddress() {
        return null;
    }

    @Override
    public Server getServer(ClientboundLoginPacket loginPacket) {
        return null;
    }

    public static void reset() {
        events.clear();
    }
}
