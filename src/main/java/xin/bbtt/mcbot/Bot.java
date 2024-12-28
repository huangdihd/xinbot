package xin.bbtt.mcbot;

import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.geysermc.mcprotocollib.network.tcp.TcpClientSession;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Bot {
    private static final Logger log = LoggerFactory.getLogger(Bot.class);
    private boolean is_running = false;
    private MinecraftProtocol protocol;
    private Session session;
    private final Thread thread = new Thread(this::main_loop);
    private final BotProfile botProfile;
    private final PluginManager pluginManager;

    public static Bot Instance = new Bot();
    public Server server;
    public boolean login = false;
    public final Map<UUID, GameProfile> players = new HashMap<>();

    private Bot() {
        this.botProfile = new BotProfile();
        this.pluginManager = new PluginManager();
    }

    public void init() {
        this.pluginManager.loadPlugin(new DefaultPlugin());
        this.pluginManager.loadPlugins(this.botProfile.getPluginsDirectory());
    }

    public void start() {
        is_running = true;
        protocol = new MinecraftProtocol(botProfile.getUsername());
        session = new TcpClientSession("2b2t.xin", 25565, protocol);
        login = false;
        log.info(this.botProfile.toString());
        thread.start();
    }

    public void stop() {
        thread.interrupt();
        disconnect("Bot stopped.");
        is_running = false;
    }

    private void main_loop() {
        connect();
        while (is_running) {
            if (!session.isConnected()) {
                pluginManager.disableAll();
                connect();
            }
        }
    }

    private void connect(){
        session = new TcpClientSession("2b2t.xin", 25565, protocol);
        pluginManager.enableAll();
        log.info("connecting.");
        session.connect();
        long start_time = Instant.now().toEpochMilli();
        while (!session.isConnected()){
            if (System.currentTimeMillis() - start_time > 2000) break;
        }
    }

    private void disconnect(String reason){
        session.disconnect(reason);
    }

    public boolean isRunning() {
        return is_running;
    }

    public BotProfile getBotProfile() {
        return botProfile;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public void addListener(SessionListener listener){
        session.addListener(listener);
    }

    public void removeListener(SessionListener listener){
        session.removeListener(listener);
    }

    public void sendCommand(String command) {
        session.send(new ServerboundChatCommandPacket(command));
    }

    public void sendChatMessage(String message) {
        session.send(
                new ServerboundChatPacket(
                    message,
                    Instant.now().toEpochMilli(),
                    0L,
                    null,
                    0,
                    new BitSet()
                )
        );
    }

    public void setCarriedItem(int slot) {
        if (slot > 9 || slot < 0) return;
        session.send(new ServerboundSetCarriedItemPacket(slot));
    }

    public void useItemWithMainHand(float yRot, float xRot) {
        session.send(
                new ServerboundUseItemPacket(
                    Hand.MAIN_HAND,
                    (int) Instant.now().toEpochMilli(),
                    yRot,
                    xRot
                )
        );
    }
}
