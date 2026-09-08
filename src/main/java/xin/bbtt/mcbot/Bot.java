/*
 * Copyright (C) 2024-2026 huangdihd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package xin.bbtt.mcbot;

import lombok.Getter;
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
import xin.bbtt.mcbot.events.ConnectEvent;
import xin.bbtt.mcbot.events.DisconnectEvent;
import xin.bbtt.mcbot.listeners.*;
import xin.bbtt.mcbot.plugin.Plugin;
import xin.bbtt.mcbot.plugin.PluginManager;
import xin.bbtt.mcbot.telemetry.TelemetryManager;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static xin.bbtt.mcbot.Utils.parseColors;

public class Bot {
    private static final Logger log = LoggerFactory.getLogger(Bot.class.getSimpleName());
    @Getter
    private volatile boolean running = false;
    @Getter
    private MinecraftProtocol protocol;
    @Getter
    private volatile ClientSession session;
    @Getter
    private Thread mainThread;
    @Getter
    private TelemetryManager telemetryManager = new TelemetryManager();
    @Getter
    private BotConfig config;
    @Getter
    private final PluginManager pluginManager;
    @Getter
    private ProxyInfo proxyInfo;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Bot-Scheduler");
        thread.setDaemon(true);
        return thread;
    });
    @Getter
    private final Queue<String> toBeSentMessages = new ConcurrentLinkedQueue<>();
    public static final Bot INSTANCE = new Bot();
    @Getter
    private volatile Server server = null;

    public void setServer(Server server) {
        this.server = server;

        if (server == null) {
            return;
        }

        CompletableFuture<Server> future = serverReadyFuture;
        if (future != null) {
            future.complete(server);
        }
    }

    public final Map<UUID, GameProfile> players = new ConcurrentHashMap<>();
    private final PacketListener packetListener = new PacketListener();
    private final ServerRecorder serverRecorder = new ServerRecorder();
    private final ChatMessagePrinter chatMessagePrinter = new ChatMessagePrinter();
    private final MessageSender messageSender = new MessageSender();
    private final BlockChangedAckRecorder blockChangedAckRecorder = new BlockChangedAckRecorder();
    private final ServerMembersChangedMessagePrinter serverMembersChangedMessagePrinter = new ServerMembersChangedMessagePrinter();
    private final CommandsRecorder commandsRecorder = new CommandsRecorder();
    private volatile CompletableFuture<Server> serverReadyFuture;
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
        telemetryManager.configure(config.getConfigData().getTelemetry());
        var proxy = config.getConfigData().getProxy();
        if (proxy.isEnable()) {
            if (proxy.getInfo() == null) {
                log.error(LangManager.get("xinbot.proxy.error.no_info"));
            } else {
                proxyInfo = proxy.getInfo().toMcProtocolLibProxyInfo();
            }
        }
        log.info(LangManager.get("xinbot.bot.starting", protocol.getProfile().getName()));

        scheduler.execute(this::connect);
        
        getInput();
    }

    public void stop() {
        try {
            running = false;
            telemetryManager.shutdown();
            scheduler.shutdownNow();
            if (session != null) {
                disconnect(LangManager.get("xinbot.bot.stopped"));
            }
            pluginManager.unloadPlugins();
        }
        catch (Exception e) {
            log.error(LangManager.get("xinbot.bot.error.stopping"), e);
        }
        finally {
            if (mainThread != null) {
                mainThread.interrupt();
            }
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

    private void connect() {
        if (!running) {
            return;
        }

        setServer(null);

        CompletableFuture<Server> readyFuture = new CompletableFuture<>();
        serverReadyFuture = readyFuture;

        ClientNetworkSession newSession = new ClientNetworkSession(
            pluginManager.getMetaPlugin().getServerSocketAddress(),
            protocol,
            DefaultPacketHandlerExecutor.createExecutor(),
            null,
            proxyInfo
        );

        session = newSession;

        newSession.addListener(new SessionAdapter() {
            @Override
            public void disconnected(DisconnectedEvent event) {
                readyFuture.completeExceptionally(
                    new IllegalStateException(
                        Utils.toString(event.getReason())
                    )
                );

                onDisconnect(newSession, event.getReason());
            }
        });

        newSession.addListener(packetListener);
        newSession.addListener(serverRecorder);
        newSession.addListener(chatMessagePrinter);
        newSession.addListener(messageSender);
        newSession.addListener(blockChangedAckRecorder);
        newSession.addListener(serverMembersChangedMessagePrinter);
        newSession.addListener(commandsRecorder);

        try {
            pluginManager.enableAll();

            log.info(LangManager.get("xinbot.bot.connecting"));
            pluginManager.events().callEvent(new ConnectEvent());

            newSession.connect();

            Server connectedServer = readyFuture.get(
                config.getConfigData().getReconnectTimeout(),
                TimeUnit.MILLISECONDS
            );

            if (running && session == newSession) {
                log.info(
                    LangManager.get(
                        "xinbot.bot.connection.completed",
                        connectedServer
                    )
                );
            }
        } catch (TimeoutException e) {
            if (session == newSession) {
                newSession.disconnect(
                    LangManager.get("xinbot.bot.connection.timed.out")
                );
            }
        } catch (ExecutionException e) {
            if (running && session == newSession) {
                log.warn("Connection failed", e.getCause());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            readyFuture.completeExceptionally(e);

            if (session == newSession) {
                log.error("Failed to initialize connection", e);

                if (newSession.isConnected()) {
                    newSession.disconnect(e.getMessage());
                } else {
                    onDisconnect(
                        newSession,
                        Component.text(
                            e.getMessage() == null
                                ? e.getClass().getSimpleName()
                                : e.getMessage()
                        )
                    );
                }
            }
        } finally {
            if (serverReadyFuture.equals(readyFuture)) {
                serverReadyFuture = null;
            }
        }
    }

    private void onDisconnect(
        ClientSession disconnectedSession,
        Component reason
    ) {
        disconnectedSession.removeListener(packetListener);
        disconnectedSession.removeListener(serverRecorder);
        disconnectedSession.removeListener(chatMessagePrinter);
        disconnectedSession.removeListener(messageSender);
        disconnectedSession.removeListener(blockChangedAckRecorder);
        disconnectedSession.removeListener(serverMembersChangedMessagePrinter);
        disconnectedSession.removeListener(commandsRecorder);

        // 这是旧连接的延迟回调，不应该影响当前连接
        if (session != disconnectedSession) {
            return;
        }

        session = null;

        DisconnectEvent event = new DisconnectEvent(reason);
        pluginManager.events().callEvent(event);

        String reasonStr = Utils.toString(reason);
        String translatedReason = reasonStr;

        if (reasonStr.toLowerCase().contains("timed out")) {
            translatedReason = LangManager.get("xinbot.disconnect.timeout");
        } else if (reasonStr.toLowerCase().contains("end of stream")) {
            translatedReason = LangManager.get(
                "xinbot.disconnect.endOfStream"
            );
        }

        log.info(
            LangManager.get(
                "xinbot.bot.disconnect.reason",
                parseColors(translatedReason)
            )
        );

        players.clear();
        pluginManager.disableAll();
        setServer(null);

        if (!running) {
            return;
        }

        protocol = AccountLoader.getProtocol();

        long delay = config.getConfigData().getReconnectDelay();

        if (delay > 0) {
            log.info(LangManager.get("xinbot.bot.reconnecting", delay));
        }

        scheduler.schedule(
            this::connect,
            Math.max(0, delay),
            TimeUnit.MILLISECONDS
        );
    }

    public void disconnect(String reason){
        ClientSession currentSession = session;

        if (currentSession != null) {
            currentSession.disconnect(reason);
        }
    }

    public void reloadConfig(String configPath) throws Exception {
        config.loadFromFile(configPath);
        Xinbot.configPath = configPath;
        
        config.getConfigData().setAccount(AccountLoader.init(config.getConfigData().getAccount()));
        config.saveToFile();

        var proxy = config.getConfigData().getProxy();
        if (proxy.isEnable() && proxy.getInfo() != null) {
            proxyInfo = proxy.getInfo().toMcProtocolLibProxyInfo();
        } else {
            if (proxy.isEnable()) {
                log.error(LangManager.get("xinbot.proxy.error.no_info"));
            }
            proxyInfo = null;
        }

        if (session != null && session.isConnected()) {
            disconnect(LangManager.get("xinbot.command.reload.disconnect", "Reloading config..."));
        }
        telemetryManager.configure(config.getConfigData().getTelemetry());
    }

    @SuppressWarnings("unused")
    public void addPacketListener(SessionListener listener, Plugin plugin){
        getPluginManager().addListener(listener, plugin);
    }

    @SuppressWarnings("unused")
    public void removePacketListener(SessionListener listener, Plugin plugin){
        getPluginManager().removeListener(listener, plugin);
    }

    public void sendCommand(String command) {
        toBeSentMessages.add("/" + command);
    }

    public void sendChatMessage(String message) {
        if (message.startsWith("/")) {
            message = "\\" + message;
        }
        toBeSentMessages.add(message);
    }

    @SuppressWarnings("unused")
    public int getAndIncreaseSequence() {
        return this.sequence.getAndAdd(1);
    }
}
