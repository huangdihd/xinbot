package xin.bbtt.mcbot;

import lombok.Getter;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.geysermc.mcprotocollib.network.tcp.TcpClientSession;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.JLine.CLI;
import xin.bbtt.mcbot.auth.AccountLoader;
import xin.bbtt.mcbot.config.BotConfig;

import java.time.Instant;
import java.util.*;

public class Bot {
    private static final Logger log = LoggerFactory.getLogger(Bot.class.getSimpleName());
    @Getter
    private volatile boolean is_running = false;
    public MinecraftProtocol protocol;
    public Session session;
    private final Thread main_thread = new Thread(this::main_loop);
    private final Thread input_thread = new Thread(this::get_input);
    @Getter
    private BotConfig config;
    @Getter
    private final PluginManager pluginManager;

    public final ArrayList<String> to_be_sent_messages = new ArrayList<>();
    public static Bot Instance = new Bot();
    public Server server = null;
    public boolean login = false;
    public final Map<UUID, GameProfile> players = new HashMap<>();

    private Bot() {
        this.pluginManager = new PluginManager();
    }

    public void init(BotConfig config) {
        this.config = config;
        this.pluginManager.loadPlugin(new DefaultPlugin());
        this.pluginManager.loadPlugins(this.config.getPlugin().getDirectory());
        this.input_thread.start();
    }

    public void start() {
        is_running = true;
        protocol = AccountLoader.getProtocol();
        session = new TcpClientSession("2b2t.xin", 25565, protocol);
        login = false;
        log.info("Starting bot with username: {}", protocol.getProfile().getName());
        main_thread.start();
    }

    public void stop() {
        main_thread.interrupt();
        input_thread.interrupt();
        disconnect("Bot stopped.");
        is_running = false;
    }

    private void main_loop() {
        connect();
        while (!Thread.currentThread().isInterrupted() && is_running) {
            if (Bot.Instance.getConfig().getAdvances().isEnableHighStability()) {
                if (session.isConnected()) continue;
                pluginManager.disableAll();
                connect();
            }
            Thread.onSpinWait();
        }
    }

    private void get_input() {
        while (!Thread.currentThread().isInterrupted() && is_running && CLI.lineReader != null) {
            String input = null;
            try {
                input = CLI.lineReader.readLine("> ");
            }
            catch (UserInterruptException | EndOfFileException e) {
                this.stop();
            }
            catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            if (input == null || input.isEmpty()) continue;
            this.sendChatMessage(input);
        }
    }

    private void on_disconnect() {
        if (!is_running) return;
        pluginManager.disableAll();
        server = null;
        connect();
    }

    private void connect(){
        session = new TcpClientSession("2b2t.xin", 25565, protocol);
        if (!Bot.Instance.getConfig().getAdvances().isEnableHighStability())
            session.addListener(new SessionAdapter() {
            @Override
            public void disconnected(DisconnectedEvent event) {
                on_disconnect();
            }
        });
        pluginManager.enableAll();
        log.info("connecting.");
        session.connect();
        long start_time = System.currentTimeMillis();
        while (server == null && !is_running){
            if (System.currentTimeMillis() - start_time > 2000) {
                disconnect("connect timed out.");
                break;
            }
        }
        log.info("connect complete.");
    }

    private void disconnect(String reason){
        session.disconnect(reason);
    }

    public void addListener(SessionListener listener){
        session.addListener(listener);
    }

    public void removeListener(SessionListener listener){
        session.removeListener(listener);
    }

    public void sendCommand(String command) {
        to_be_sent_messages.add("/" + command);
    }

    public void sendChatMessage(String message) {
        to_be_sent_messages.add(message);
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
