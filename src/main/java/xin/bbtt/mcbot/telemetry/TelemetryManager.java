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

package xin.bbtt.mcbot.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.LangManager;
import xin.bbtt.mcbot.Xinbot;
import xin.bbtt.mcbot.config.BotConfigData;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sends encrypted heartbeat and crash-report payloads to a telemetry server.
 *
 * <p>Wire format (identical for both transports):
 * <pre>
 *   [0..3]   magic "XBTL"
 *   [4]      protocol version (1)
 *   [5]      message type (1 = heartbeat, 2 = crash report)
 *   [6..17]  AES-GCM 12-byte IV
 *   [18..]   AES-128-GCM ciphertext (plain JSON payload + 16-byte auth tag)
 * </pre>
 * The fixed encryption key ({@link #ENCRYPTION_KEY}) must match on the receiver.
 * UDP mode sends the raw envelope as one datagram; HTTP mode POSTs the same
 * envelope to {@code http://<ip>:<port>/telemetry} as
 * {@code application/octet-stream}.
 */
public class TelemetryManager implements Thread.UncaughtExceptionHandler {

    /** Envelope magic bytes, lets a receiver identify Xinbot telemetry packets. */
    public static final byte[] MAGIC = {'X', 'B', 'T', 'L'};
    /** Envelope protocol version. */
    public static final byte PROTOCOL_VERSION = 1;
    /** Message type: periodic status heartbeat. */
    public static final byte TYPE_HEARTBEAT = 1;
    /** Message type: crash report sent when an uncaught exception kills the bot. */
    public static final byte TYPE_CRASH = 2;

    /**
     * Fixed AES-128 encryption key (16 bytes). Intentionally hard-coded so no
     * secret ever touches the config file; receivers must use the same value.
     */
    static final byte[] ENCRYPTION_KEY = "xinbot-telemetry".getBytes(StandardCharsets.US_ASCII);

    public static final long HEARTBEAT_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(5);
    public static final String DEFAULT_MODE = "udp";
    public static final String DEFAULT_IP = "127.0.0.1";
    public static final int DEFAULT_PORT = 9000;
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int HEADER_LENGTH = 4 + 1 + 1 + IV_LENGTH;
    /** UDP payload ceiling (65535 minus UDP/IP headers), also caps HTTP bodies. */
    private static final int MAX_PAYLOAD = 65507;
    /** Stack trace length cap so a crash report cannot exceed the UDP payload ceiling. */
    private static final int MAX_STACK_TRACE_CHARS = 12000;

    private static final Logger log = LoggerFactory.getLogger(TelemetryManager.class.getSimpleName());
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();

    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private volatile String mode = DEFAULT_MODE;
    private volatile String ip = DEFAULT_IP;
    private volatile int port = DEFAULT_PORT;
    private volatile ScheduledExecutorService heartbeatExecutor;
    private volatile Thread.UncaughtExceptionHandler previousHandler;
    private volatile boolean handlerInstalled = false;

    /**
     * Applies a telemetry configuration, starting or stopping the heartbeat and
     * installing/keeping the crash handler accordingly. Idempotent: no-op when
     * the effective settings did not change.
     */
    public synchronized void configure(BotConfigData.Telemetry telemetry) {
        if (telemetry == null || !telemetry.isEnable()) {
            shutdown();
            return;
        }

        String nextMode = normalizeMode(telemetry.getMode());
        String nextIp = telemetry.getIp() == null || telemetry.getIp().isBlank()
            ? DEFAULT_IP : telemetry.getIp().trim();
        int nextPort = telemetry.getPort() > 0 ? telemetry.getPort() : DEFAULT_PORT;

        if (enabled.get() && mode.equals(nextMode) && ip.equals(nextIp) && port == nextPort) {
            return;
        }

        shutdown();
        this.mode = nextMode;
        this.ip = nextIp;
        this.port = nextPort;
        enabled.set(true);
        installCrashHandler();
        startHeartbeat();
        log.info(LangManager.get("xinbot.telemetry.enabled", mode, ip, port));
    }

    /** Stops heartbeat reporting. The crash handler stays installed. */
    public synchronized void shutdown() {
        enabled.set(false);
        ScheduledExecutorService executor = heartbeatExecutor;
        heartbeatExecutor = null;
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * Registers this manager as the JVM-wide uncaught exception handler (once),
     * chaining any handler that was installed before us.
     */
    private void installCrashHandler() {
        if (handlerInstalled) {
            return;
        }
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        handlerInstalled = true;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            if (enabled.get()) {
                log.info(LangManager.get("xinbot.telemetry.crash.detected", thread.getName()));
                sendCrashReport(throwable, thread);
            }
        } catch (Throwable ignored) {
            // A crash handler must never throw or re-enter the uncaught path.
        }

        Thread.UncaughtExceptionHandler handler = previousHandler;
        if (handler != null) {
            handler.uncaughtException(thread, throwable);
        } else if (!(throwable instanceof ThreadDeath)) {
            throwable.printStackTrace();
        }

        // When the bot main thread dies while non-daemon threads (e.g. Netty) are
        // still alive the JVM would otherwise hang forever. The crash report above
        // was sent synchronously, so exiting here is safe.
        if (thread == Bot.INSTANCE.getMainThread()) {
            System.exit(1);
        }
    }

    private void startHeartbeat() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Telemetry-Heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        heartbeatExecutor = executor;
        // Send once immediately so the link is verified early, then every 5 minutes.
        executor.scheduleWithFixedDelay(
            this::sendHeartbeatSafely,
            0,
            HEARTBEAT_INTERVAL_MILLIS,
            TimeUnit.MILLISECONDS
        );
    }

    private void sendHeartbeatSafely() {
        if (!enabled.get()) {
            return;
        }
        try {
            transmit(buildEnvelope(TYPE_HEARTBEAT, toJsonBytes(heartbeatPayload())));
        } catch (Throwable e) {
            log.warn(LangManager.get(
                "xinbot.telemetry.send.failed", "heartbeat", ip, port, String.valueOf(e)
            ));
        }
    }

    private void sendCrashReport(Throwable throwable, Thread thread) {
        try {
            transmit(buildEnvelope(TYPE_CRASH, toJsonBytes(crashPayload(throwable, thread))));
        } catch (Throwable e) {
            log.warn(LangManager.get(
                "xinbot.telemetry.send.failed", "crash", ip, port, String.valueOf(e)
            ));
        }
    }

    private void transmit(byte[] envelope) throws IOException, InterruptedException {
        if (envelope.length > MAX_PAYLOAD) {
            throw new IOException("Payload too large: " + envelope.length + " bytes");
        }
        if ("http".equals(mode)) {
            sendHttp(envelope);
        } else {
            sendUdp(envelope);
        }
    }

    private void sendUdp(byte[] envelope) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.send(new DatagramPacket(
                envelope, envelope.length, InetAddress.getByName(ip), port
            ));
        }
    }

    private void sendHttp(byte[] envelope) throws IOException, InterruptedException {
        String host = ip.contains(":") && !ip.startsWith("[") ? "[" + ip + "]" : ip;
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://" + host + ":" + port + "/telemetry"))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/octet-stream")
            .POST(HttpRequest.BodyPublishers.ofByteArray(envelope))
            .build();
        HttpResponse<Void> response = HTTP_CLIENT.send(
            request, HttpResponse.BodyHandlers.discarding()
        );
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP status " + status);
        }
    }

    /**
     * Builds an encrypted envelope: magic + version + type + random IV + ciphertext.
     */
    static byte[] buildEnvelope(byte type, byte[] plaintext) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(iv);
        byte[] ciphertext = encrypt(plaintext, iv);
        return ByteBuffer.allocate(HEADER_LENGTH + ciphertext.length)
            .put(MAGIC)
            .put(PROTOCOL_VERSION)
            .put(type)
            .put(iv)
            .put(ciphertext)
            .array();
    }

    /**
     * Decrypts an envelope and returns the plain JSON bytes. Exposed for tests
     * and as the reference implementation for telemetry receivers.
     */
    static byte[] decryptEnvelope(byte[] envelope) throws Exception {
        if (envelope.length < HEADER_LENGTH) {
            throw new IllegalArgumentException("Envelope too short: " + envelope.length);
        }
        if (envelope[0] != MAGIC[0] || envelope[1] != MAGIC[1]
            || envelope[2] != MAGIC[2] || envelope[3] != MAGIC[3]) {
            throw new IllegalArgumentException("Bad magic");
        }
        if (envelope[4] != PROTOCOL_VERSION) {
            throw new IllegalArgumentException("Unsupported protocol version: " + envelope[4]);
        }
        byte[] iv = Arrays.copyOfRange(envelope, 6, 6 + IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(envelope, 6 + IV_LENGTH, envelope.length);
        return decrypt(ciphertext, iv);
    }

    private static byte[] encrypt(byte[] plaintext, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(ENCRYPTION_KEY, "AES"),
            new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher.doFinal(plaintext);
    }

    private static byte[] decrypt(byte[] ciphertext, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(ENCRYPTION_KEY, "AES"),
            new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher.doFinal(ciphertext);
    }

    private static byte[] toJsonBytes(Map<String, Object> payload) throws Exception {
        return MAPPER.writeValueAsBytes(payload);
    }

    private static Map<String, Object> basePayload(String type) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("timestamp_ms", System.currentTimeMillis());
        payload.put("version", Xinbot.VERSION.toString());
        payload.put("bot", botName());
        payload.put("online", isOnline());
        payload.put("state", serverState());
        payload.put("server", serverAddress());
        payload.put("players", playerCount());
        payload.put("uptime_ms", ManagementFactory.getRuntimeMXBean().getUptime());
        return payload;
    }

    private static Map<String, Object> heartbeatPayload() {
        Map<String, Object> payload = basePayload("heartbeat");
        Runtime runtime = Runtime.getRuntime();
        payload.put("heap_used_bytes", runtime.totalMemory() - runtime.freeMemory());
        payload.put("heap_max_bytes", runtime.maxMemory());
        payload.put("os_name", System.getProperty("os.name", "unknown"));
        payload.put("os_arch", System.getProperty("os.arch", "unknown"));
        payload.put("java_version", System.getProperty("java.version", "unknown"));
        return payload;
    }

    private static Map<String, Object> crashPayload(Throwable throwable, Thread thread) {
        Map<String, Object> payload = basePayload("crash");
        payload.put("thread_name", thread.getName());
        String exception = throwable.getClass().getName();
        if (throwable.getMessage() != null) {
            exception += ": " + throwable.getMessage();
        }
        payload.put("exception", exception);
        payload.put("stack_trace", stackTrace(throwable));
        return payload;
    }

    private static String botName() {
        try {
            var protocol = Bot.INSTANCE.getProtocol();
            if (protocol != null && protocol.getProfile() != null) {
                return protocol.getProfile().getName();
            }
        } catch (Exception ignored) {
        }
        try {
            var account = Bot.INSTANCE.getConfig().getConfigData().getAccount();
            if (account != null && account.getName() != null) {
                return account.getName();
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    private static boolean isOnline() {
        try {
            var session = Bot.INSTANCE.getSession();
            return session != null && session.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private static String serverState() {
        try {
            var server = Bot.INSTANCE.getServer();
            return server == null ? null : server.name();
        } catch (Exception e) {
            return null;
        }
    }

    private static String serverAddress() {
        try {
            var meta = Bot.INSTANCE.getPluginManager().getMetaPlugin();
            if (meta == null) {
                return null;
            }
            SocketAddress address = meta.getServerSocketAddress();
            if (address instanceof InetSocketAddress inet) {
                return inet.getHostString() + ":" + inet.getPort();
            }
            return address == null ? null : address.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static int playerCount() {
        try {
            return Bot.INSTANCE.players.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        String stack = writer.toString();
        if (stack.length() > MAX_STACK_TRACE_CHARS) {
            stack = stack.substring(0, MAX_STACK_TRACE_CHARS) + "\n... (truncated)";
        }
        return stack;
    }

    private static String normalizeMode(String mode) {
        if (mode != null && "http".equalsIgnoreCase(mode.trim())) {
            return "http";
        }
        return DEFAULT_MODE;
    }
}
