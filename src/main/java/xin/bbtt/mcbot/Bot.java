/*
 *   Copyright (C) 2024-2026 huangdihd
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
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.ProxyInfo;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.geysermc.mcprotocollib.network.netty.DefaultPacketHandlerExecutor;
import org.geysermc.mcprotocollib.network.session.ClientNetworkSession;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.jLine.CLI;
import xin.bbtt.mcbot.auth.AccountLoader;
import xin.bbtt.mcbot.config.BotConfig;
import xin.bbtt.mcbot.events.DisconnectEvent;
import xin.bbtt.mcbot.listeners.*;
import xin.bbtt.mcbot.plugin.Plugin;
import xin.bbtt.mcbot.plugin.PluginManager;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static xin.bbtt.mcbot.Utils.parseColors;

public class Bot {
    private static final Logger log = LoggerFactory.getLogger(Bot.class.getSimpleName());
    @Getter
    private volatile boolean running = false;
    @Getter
    private MinecraftProtocol protocol;
    @Getter
    private ClientSession session;
    private Thread mainThread;
    @Getter
    private BotConfig config;
    @Getter
    private final PluginManager pluginManager;
    @Getter
    private ProxyInfo proxyInfo;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Bot-Scheduler");
        t.setDaemon(true);
        return t;
    });

    public final ArrayList<String> to_be_sent_messages = new ArrayList<>();
    public final static Bot Instance = new Bot();
    @Getter
    @Setter
    private Server server = null;
    public final Map<UUID, GameProfile> players = new HashMap<>();
    private final PacketListener packetListener = new PacketListener();
    private final ServerRecorder serverRecorder = new ServerRecorder();
    private final ChatMessagePrinter chatMessagePrinter = new ChatMessagePrinter();
    private final MessageSender messageSender = new MessageSender();
    private final BlockChangedAckRecorder blockChangedAckRecorder = new BlockChangedAckRecorder();
    private final ServerMembersChangedMessagePrinter serverMembersChangedMessagePrinter = new ServerMembersChangedMessagePrinter();
    private final CommandsRecorder commandsRecorder = new CommandsRecorder();
    @Getter
    private final AtomicInteger sequence = new AtomicInteger(0);

    private Bot() {
        this.pluginManager = new PluginManager();
    }

    public void init(BotConfig config) {
        this.config = config;
        this.pluginManager.loadPlugins(this.config.getConfigData().getPlugin().getDirectory());
    }

    public void start() {
        mainThread = Thread.currentThread();

        long metaCount = pluginManager.countMetaPlugins();
        if (metaCount != 1) {
            log.error(LangManager.get("xinbot.metaplugin.error.count", metaCount));
            running = false;
            return;
        }

        running = true;
        protocol = AccountLoader.getProtocol();
        if (config.getConfigData().getProxy().isEnable()) {
            proxyInfo = config.getConfigData().getProxy().getInfo().toMcProtocolLibProxyInfo();
        }
        log.info(LangManager.get("xinbot.bot.starting", protocol.getProfile().getName()));
        connect();
        getInput();
    }

    public void stop() {
        try {
            running = false;
            scheduler.shutdownNow();
            disconnect(LangManager.get("xinbot.bot.stopped"));
            pluginManager.unloadPlugins();
        }
        catch (Exception e) {
            log.error(LangManager.get("xinbot.bot.error.stopping"), e);
        }
        finally {
            mainThread.interrupt();
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
                break;
            }
            catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            if (input == null || input.isEmpty()) continue;
            this.getPluginManager().commands().callCommand(input);
        }
    }

    private void connect(){
        session = new ClientNetworkSession( pluginManager.getMetaPlugin().getServerSocketAddress(), protocol, DefaultPacketHandlerExecutor.createExecutor(), null, proxyInfo);
        session.addListener(new SessionAdapter() {
            @Override
            public void disconnected(DisconnectedEvent event) {
                onDisconnect(event.getReason());
            }
        });
        session.addListener(packetListener);
        session.addListener(serverRecorder);
        session.addListener(chatMessagePrinter);
        session.addListener(messageSender);
        session.addListener(blockChangedAckRecorder);
        session.addListener(serverMembersChangedMessagePrinter);
        session.addListener(commandsRecorder);
        pluginManager.enableAll();
        log.info(LangManager.get("xinbot.bot.connecting"));
        session.connect();
        long start_time = System.currentTimeMillis();
        while (server == null && running){
            if (System.currentTimeMillis() - start_time > config.getConfigData().getReconnectTimeout()) {
                disconnect(LangManager.get("xinbot.bot.connection.timed.out"));
                break;
            }
        }
        log.info(LangManager.get("xinbot.bot.connection.completed"));
    }

    private void onDisconnect(Component reason) {
        DisconnectEvent event = new DisconnectEvent(reason);
        getPluginManager().events().callEvent(event);
        log.info(parseColors(Utils.toString(reason)));

        players.clear();
        pluginManager.disableAll();
        session.removeListener(packetListener);
        session.removeListener(serverRecorder);
        session.removeListener(chatMessagePrinter);
        session.removeListener(messageSender);
        session.removeListener(serverMembersChangedMessagePrinter);
        session.removeListener(commandsRecorder);
        server = null;
        if (!running) return;

        protocol = AccountLoader.getProtocol();

        long delay = config.getConfigData().getReconnectDelay();
        if (delay > 0) {
            log.info(LangManager.get("xinbot.bot.reconnecting", delay));
            scheduler.schedule(() -> {
                if (running) connect();
            }, delay, TimeUnit.MILLISECONDS);
        } else {
            connect();
        }
    }

    public void disconnect(String reason){
        session.disconnect(reason);
    }

    public void addPacketListener(SessionListener listener, Plugin plugin){
        getPluginManager().addListener(listener, plugin);
    }

    @SuppressWarnings("unused")
    public void removePacketListener(SessionListener listener, Plugin plugin){
        getPluginManager().removeListener(listener, plugin);
    }

    public void sendCommand(String command) {
        to_be_sent_messages.add("/" + command);
    }

    public void sendChatMessage(String message) {
        if (message.startsWith("/")) {
            message = "\\" + message;
        }
        to_be_sent_messages.add(message);
    }

    public int getAndIncreaseSequence() {
        return this.sequence.getAndAdd(1);
    }
}
