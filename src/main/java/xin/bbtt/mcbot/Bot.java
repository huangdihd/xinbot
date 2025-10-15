/*
 *   Copyright (C) 2024-2025 huangdihd
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xin.bbtt.mcbot;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.ProxyInfo;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.geysermc.mcprotocollib.network.tcp.TcpClientSession;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.jLine.CLI;
import xin.bbtt.mcbot.auth.AccountLoader;
import xin.bbtt.mcbot.config.BotConfig;
import xin.bbtt.mcbot.events.DisconnectEvent;
import xin.bbtt.mcbot.plugin.Plugin;
import xin.bbtt.mcbot.plugin.PluginManager;

import java.util.*;

public class Bot {
    private static final Logger log = LoggerFactory.getLogger(Bot.class.getSimpleName());
    @Getter
    private volatile boolean running = false;
    @Getter
    private MinecraftProtocol protocol;
    @Getter
    private Session session;
    private final Thread inputThread = new Thread(this::getInput);
    @Getter
    private BotConfig config;
    @Getter
    private final PluginManager pluginManager;
    @Getter
    private ProxyInfo proxyInfo;

    public final ArrayList<String> to_be_sent_messages = new ArrayList<>();
    public final static Bot Instance = new Bot();
    @Getter
    @Setter
    private Server server = null;
    public boolean login = false;
    public final Map<UUID, GameProfile> players = new HashMap<>();

    private Bot() {
        this.pluginManager = new PluginManager();
    }

    public void init(BotConfig config) {
        this.config = config;
        this.pluginManager.loadPlugin(new XinbotPlugin());
        this.pluginManager.loadPlugins(this.config.getConfigData().getPlugin().getDirectory());
        this.inputThread.setDaemon(true);
        this.inputThread.start();
    }

    public void start() {
        running = true;
        protocol = AccountLoader.getProtocol();
        if (config.getConfigData().getProxy().isEnable()) {
            proxyInfo = config.getConfigData().getProxy().getInfo().toMcProtocolLibProxyInfo();
        }
        session = new TcpClientSession("2b2t.xin", 25565, protocol, proxyInfo);
        login = false;
        log.info("Starting bot with username: {}", protocol.getProfile().getName());
        mainLoop();
    }

    public void stop() {
        try {
            pluginManager.disableAll();
            pluginManager.unloadPlugins();
            running = false;
            disconnect("Bot stopped.");
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        finally {
            inputThread.interrupt();
            Thread.currentThread().interrupt();
        }
    }

    private void mainLoop() {
        connect();
        while (!Thread.currentThread().isInterrupted() && running) {
            if (Bot.Instance.getConfig().getConfigData().getAdvances().isEnableHighStability()) {
                if (session.isConnected()) continue;
                players.clear();
                pluginManager.disableAll();
                connect();
            }
            Thread.onSpinWait();
        }
    }

    private void getInput() {
        while (!Thread.currentThread().isInterrupted() && running && CLI.getLineReader() != null) {
            String input = null;
            try {
                input = CLI.getLineReader().readLine("> ");
            }
            catch (UserInterruptException | EndOfFileException e) {
                this.stop();
            }
            catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            if (input == null || input.isEmpty()) continue;
            this.getPluginManager().commands().callCommand(input);
        }
    }

    private void on_disconnect(Component reason) {
        if (!running) return;
        DisconnectEvent event = new DisconnectEvent(reason);
        getPluginManager().events().callEvent(event);
        if (Bot.Instance.getConfig().getConfigData().getAdvances().isEnableHighStability()) return;
        players.clear();
        pluginManager.disableAll();
        server = null;
        connect();
    }

    private void connect(){
        session = new TcpClientSession("2b2t.xin", 25565, protocol, proxyInfo);
        session.addListener(new SessionAdapter() {
        @Override
        public void disconnected(DisconnectedEvent event) {
            on_disconnect(event.getReason());
        }
    });
        pluginManager.enableAll();
        log.info("connecting.");
        session.connect();
        long start_time = System.currentTimeMillis();
        while (server == null && !running){
            if (System.currentTimeMillis() - start_time > 2000) {
                disconnect("connect timed out.");
                break;
            }
        }
        log.info("connect complete.");
    }

    public void disconnect(String reason){
        session.disconnect(reason);
    }

    public void addPacketListener(SessionListener listener, Plugin plugin){
        getPluginManager().addListener(listener, plugin);
    }

    @SuppressWarnings("unused")
    public void removeListener(SessionListener listener, Plugin plugin){
        getPluginManager().removeListener(listener, plugin);
    }

    public void sendCommand(String command) {
        to_be_sent_messages.add("/" + command);
    }

    public void sendChatMessage(String message) {
        to_be_sent_messages.add(message);
    }
}
