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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Sends encrypted heartbeat and crash-report payloads to a telemetry server.
 *
 * <p>Wire format (identical for both transports):
 * <pre>
 *   [0..3]   magic "XBTL"
 *   [4]      protocol version (1)
 *   [5]      message type (1 = heartbeat, 2 = crash report)
 *   [6..17]  AES-GCM 12-byte IV
 *   [18..]   AES-GCM ciphertext (plain JSON payload + 16-byte auth tag)
 * </pre>
 * The first six header bytes (magic + version + type) are bound to the ciphertext
 * as AES-GCM AAD, so flipping the type byte alone invalidates the tag. The key is
 * deployment-specific: the {@code telemetry.key} configured here (Base64 of 32
 * random bytes) must match on the receiver. Leaving telemetry.key empty switches
 * to the weakened mode: the key is fetched from the server at startup through a
 * plaintext exchange over the configured transport (UDP type 3/4 control packets,
 * or HTTP GET /telemetry/key). This is convenient but anyone able to eavesdrop on
 * that exchange learns the key, so it only makes sense on trusted networks.
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
    /** Message type: plaintext key request (header only, no body). */
    public static final byte TYPE_KEY_REQUEST = 3;
    /** Message type: plaintext key response carrying the Base64 deployment key. */
    public static final byte TYPE_KEY_RESPONSE = 4;

    /** AES-256 key size: the configured secret is Base64 of exactly 32 random bytes. */
    private static final int KEY_LENGTH = 32;

    public static final long HEARTBEAT_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(5);
    public static final String DEFAULT_MODE = "udp";
    public static final String DEFAULT_IP = "127.0.0.1";
    public static final int DEFAULT_PORT = 9000;
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int HEADER_LENGTH = 4 + 1 + 1 + IV_LENGTH;
    /** Plaintext control-packet header length (magic + version + type). */
    private static final int CONTROL_HEADER_LENGTH = 6;
    /** UDP key-exchange round trip must finish before telemetry gives up and stays off. */
    private static final int KEY_EXCHANGE_TIMEOUT_MILLIS = 4000;
    /** UDP payload ceiling (65535 minus UDP/IP headers), also caps HTTP bodies. */
    private static final int MAX_PAYLOAD = 65507;
    /** Stack trace length cap so a crash report cannot exceed the UDP payload ceiling. */
    private static final int MAX_STACK_TRACE_CHARS = 12000;

    /** Matches any run of 4+ digits, with an optional leading minus (player coordinates
     * and other location data are commonly 4+ digit numbers, negative included). */
    private static final Pattern SENSITIVE_NUMBER = Pattern.compile("-?\\d{4,}");
    /** Replacement for every sensitive number; fixed width leaks no digit count. */
    private static final String REDACTED = "****";
    /** Replacement for redacted passwords; six stars leak no length. */
    private static final String REDACTION_MASK = "******";

    private static final Logger log = LoggerFactory.getLogger(TelemetryManager.class.getSimpleName());
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();

    /**
     * Snapshot of the per-field reporting switches taken from {@code telemetry.send*}.
     * Each boolean mirrors one switch in the user config; the default is everything on.
     */
    record PayloadOptions(boolean bot, boolean server, boolean state, boolean players,
                          boolean uptime, boolean system) {
        static PayloadOptions all() {
            return new PayloadOptions(true, true, true, true, true, true);
        }

        static PayloadOptions of(BotConfigData.Telemetry telemetry) {
            return new PayloadOptions(telemetry.isSendBot(), telemetry.isSendServer(),
                telemetry.isSendState(), telemetry.isSendPlayers(), telemetry.isSendUptime(),
                telemetry.isSendSystem());
        }
    }

    /** Validated settings the manager runs with; produced by {@link #resolveSettings}. */
    private record ResolvedSettings(String mode, String ip, int port, byte[] key,
                                    PayloadOptions options) {
    }

    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private volatile String mode = DEFAULT_MODE;
    private volatile String ip = DEFAULT_IP;
    private volatile int port = DEFAULT_PORT;
    /** Deployment-specific AES-256 key, decoded from {@code telemetry.key} on configure(). */
    private volatile byte[] key;
    /** Which optional payload fields may be sent; refreshed on each configure(). */
    private volatile PayloadOptions payloadOptions = PayloadOptions.all();
    /** Passwords copied from the config; crash text is scanned for them before sending. */
    private volatile Collection<String> secrets = List.of();
    private volatile ScheduledExecutorService heartbeatExecutor;
    private volatile Thread.UncaughtExceptionHandler previousHandler;
    private volatile boolean handlerInstalled = false;

    /**
     * Applies a telemetry configuration, starting or stopping the heartbeat and
     * installing/keeping the crash handler accordingly. Idempotent: no-op when
     * the effective settings did not change.
     */
    public synchronized void configure(BotConfigData.Telemetry telemetry) {
        ResolvedSettings settings = resolveSettings(telemetry);
        if (settings == null) {
            // Disabled or unusable (e.g. no deployment key): keep telemetry off.
            shutdown();
            return;
        }

        if (enabled.get() && mode.equals(settings.mode()) && ip.equals(settings.ip())
                && port == settings.port() && Arrays.equals(key, settings.key())
                && payloadOptions.equals(settings.options())) {
            return;
        }

        shutdown();
        this.mode = settings.mode();
        this.ip = settings.ip();
        this.port = settings.port();
        this.key = settings.key();
        this.payloadOptions = settings.options();
        enabled.set(true);
        installCrashHandler();
        startHeartbeat();
        log.info(LangManager.get("xinbot.telemetry.enabled", mode, ip, port));
    }

    /**
     * Validates and normalizes a telemetry configuration into the settings the
     * manager would run with. {@code null} means telemetry must stay off: the
     * section is disabled or missing, or no deployment key could be resolved.
     * An explicitly configured key is parsed as-is (an invalid one fails closed);
     * an empty key triggers the weakened auto-fetch over the configured transport,
     * which fails closed too when the server cannot be reached.
     */
    private ResolvedSettings resolveSettings(BotConfigData.Telemetry telemetry) {
        if (telemetry == null || !telemetry.isEnable()) {
            return null;
        }
        String mode = normalizeMode(telemetry.getMode());
        String host = normalizeIp(telemetry.getIp());
        int targetPort = telemetry.getPort() > 0 ? telemetry.getPort() : DEFAULT_PORT;
        String configuredKey = telemetry.getKey();
        final byte[] nextKey;
        if (configuredKey == null || configuredKey.isBlank()) {
            nextKey = fetchKeyOrNull(mode, host, targetPort);
            if (nextKey == null) {
                return null; // fetchKeyOrNull already logged the reason
            }
            log.info(LangManager.get("xinbot.telemetry.key.auto", host, targetPort));
        } else {
            try {
                nextKey = parseKey(configuredKey);
            } catch (IllegalArgumentException e) {
                log.error(LangManager.get("xinbot.telemetry.key.invalid", e.getMessage()));
                return null;
            }
        }
        return new ResolvedSettings(mode, host, targetPort, nextKey,
                PayloadOptions.of(telemetry));
    }

    /**
     * Asks the server to disclose its deployment key over the transport the manager
     * runs on; {@code null} (and an error log) when the exchange fails. The request
     * itself travels in clear, so this weakened mode only makes sense on trusted
     * networks; an explicit telemetry.key never goes through this path.
     */
    private byte[] fetchKeyOrNull(String mode, String ip, int port) {
        try {
            return "http".equals(mode) ? fetchKeyHttp(ip, port) : fetchKeyUdp(ip, port);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error(LangManager.get(
                "xinbot.telemetry.key.fetch.failed", ip, port, String.valueOf(e)));
            return null;
        }
    }

    /** UDP key exchange: send the plaintext request, wait for the key response. */
    private byte[] fetchKeyUdp(String ip, int port) throws IOException {
        byte[] request = buildKeyRequest();
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(KEY_EXCHANGE_TIMEOUT_MILLIS);
            socket.send(new DatagramPacket(
                request, request.length, InetAddress.getByName(ip), port));
            byte[] buffer = new byte[MAX_PAYLOAD];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            return parseKeyResponse(Arrays.copyOf(buffer, response.getLength()));
        }
    }

    /** HTTP key exchange: GET the plaintext key from {@code /telemetry/key}. */
    private byte[] fetchKeyHttp(String ip, int port) throws IOException, InterruptedException {
        String host = ip.contains(":") && !ip.startsWith("[") ? "[" + ip + "]" : ip;
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://" + host + ":" + port + "/telemetry/key"))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();
        HttpResponse<byte[]> response = HTTP_CLIENT.send(
            request, HttpResponse.BodyHandlers.ofByteArray());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP status " + status);
        }
        return parseKey(new String(response.body(), StandardCharsets.UTF_8).trim());
    }

    /** Plaintext key request: magic + version + type = KEY_REQUEST (no body). */
    static byte[] buildKeyRequest() {
        return new byte[]{MAGIC[0], MAGIC[1], MAGIC[2], MAGIC[3],
            PROTOCOL_VERSION, TYPE_KEY_REQUEST};
    }

    /**
     * Validates a plaintext key response (magic + version + type = KEY_RESPONSE)
     * and decodes the deployment key carried in its body.
     */
    static byte[] parseKeyResponse(byte[] data) {
        if (data == null || data.length <= CONTROL_HEADER_LENGTH
                || data[0] != MAGIC[0] || data[1] != MAGIC[1]
                || data[2] != MAGIC[2] || data[3] != MAGIC[3]
                || data[4] != PROTOCOL_VERSION
                || data[5] != TYPE_KEY_RESPONSE) {
            throw new IllegalArgumentException("Malformed key response");
        }
        return parseKey(new String(data, CONTROL_HEADER_LENGTH,
            data.length - CONTROL_HEADER_LENGTH, StandardCharsets.UTF_8));
    }

    /**
     * Refreshes the passwords redacted from crash reports from the current config.
     * Called together with {@link #configure} whenever the config is (re)loaded, so
     * the redaction always matches the passwords actually in use.
     */
    public synchronized void updateSecrets(BotConfigData configData) {
        Collection<String> next = new ArrayList<>(4);
        if (configData != null && configData.getAccount() != null) {
            addSecret(next, configData.getAccount().getPassword());
        }
        if (configData != null && configData.getProxy() != null
                && configData.getProxy().getInfo() != null) {
            addSecret(next, configData.getProxy().getInfo().getPassword());
        }
        secrets = next;
    }

    private static void addSecret(Collection<String> target, String password) {
        if (password != null && !password.isBlank()) {
            target.add(password);
        }
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
            byte[] plaintext = toJsonBytes(filterPayload(heartbeatPayload(), payloadOptions));
            transmit(buildEnvelope(TYPE_HEARTBEAT, plaintext, key));
        } catch (Throwable e) {
            log.warn(LangManager.get(
                "xinbot.telemetry.send.failed", "heartbeat", ip, port, String.valueOf(e)
            ));
        }
    }

    private void sendCrashReport(Throwable throwable, Thread thread) {
        try {
            byte[] plaintext = toJsonBytes(filterPayload(crashPayload(throwable, thread), payloadOptions));
            transmit(buildEnvelope(TYPE_CRASH, plaintext, key));
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
     * The six header bytes are bound to the ciphertext as GCM AAD.
     *
     * @param key 32-byte AES-256 key shared with the receiver
     */
    static byte[] buildEnvelope(byte type, byte[] plaintext, byte[] key) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(iv);
        byte[] ciphertext = encrypt(plaintext, iv, headerAad(type), key);
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
    static byte[] decryptEnvelope(byte[] envelope, byte[] key) throws Exception {
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
        byte type = envelope[5];
        byte[] iv = Arrays.copyOfRange(envelope, 6, 6 + IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(envelope, 6 + IV_LENGTH, envelope.length);
        return decrypt(ciphertext, iv, headerAad(type), key);
    }

    /** AAD for AES-GCM: the header bytes before the IV, i.e. magic + version + type. */
    private static byte[] headerAad(byte type) {
        return new byte[]{MAGIC[0], MAGIC[1], MAGIC[2], MAGIC[3], PROTOCOL_VERSION, type};
    }

    /**
     * Decodes the configured deployment secret: Base64 of exactly 32 random bytes
     * (AES-256). Throws {@link IllegalArgumentException} when blank, not Base64,
     * or of the wrong decoded length.
     */
    static byte[] parseKey(String configured) {
        String text = configured == null ? "" : configured.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("empty");
        }
        final byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("not valid Base64: " + e.getMessage());
        }
        if (decoded.length != KEY_LENGTH) {
            throw new IllegalArgumentException(
                "expected 32 bytes after Base64 decoding, got " + decoded.length);
        }
        return decoded;
    }

    private static byte[] encrypt(byte[] plaintext, byte[] iv, byte[] aad, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
            new GCMParameterSpec(GCM_TAG_BITS, iv));
        cipher.updateAAD(aad);
        return cipher.doFinal(plaintext);
    }

    private static byte[] decrypt(byte[] ciphertext, byte[] iv, byte[] aad, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
            new GCMParameterSpec(GCM_TAG_BITS, iv));
        cipher.updateAAD(aad);
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

    /**
     * Removes every payload field disabled by the per-field privacy switches, returning
     * a new map (the input is left untouched). Protocol fields ({@code type},
     * {@code timestamp_ms}, {@code version}) and crash details ({@code thread_name},
     * {@code exception}, {@code stack_trace}) are never filtered.
     */
    static Map<String, Object> filterPayload(Map<String, Object> payload, PayloadOptions options) {
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            boolean allowed = switch (entry.getKey()) {
                case "bot" -> options.bot();
                case "server" -> options.server();
                case "online", "state" -> options.state();
                case "players" -> options.players();
                case "uptime_ms" -> options.uptime();
                case "heap_used_bytes", "heap_max_bytes", "os_name", "os_arch", "java_version"
                    -> options.system();
                default -> true;
            };
            if (allowed) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
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

    private Map<String, Object> crashPayload(Throwable throwable, Thread thread) {
        Map<String, Object> payload = basePayload("crash");
        payload.put("thread_name", thread.getName());
        String exception = throwable.getClass().getName();
        if (throwable.getMessage() != null) {
            exception += ": " + throwable.getMessage();
        }
        // Secrets first (a password may itself contain 4+ digits), then 4+ digit
        // numbers (e.g. player coordinates): the report leaves the client already
        // redacted, so a compromised receiver cannot harvest either from the text.
        payload.put("exception", redactCrashText(exception, secrets));
        payload.put("stack_trace", redactCrashText(stackTrace(throwable), secrets));
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

    /**
     * Replaces every run of 4+ digits (an optional leading minus included) with
     * {@code ****}. Numbers of 3 digits or fewer are left untouched. Exposed as a
     * static helper so the redaction rule is unit-testable.
     */
    static String redactNumbers(String text) {
        if (text == null) {
            return null;
        }
        return SENSITIVE_NUMBER.matcher(text).replaceAll(REDACTED);
    }

    /**
     * Replaces every configured password (exact substring) with {@code ******}.
     * Exposed as a static helper so the rule is unit-testable.
     */
    static String redactSecrets(String text, Collection<String> secrets) {
        if (text == null) {
            return null;
        }
        String redacted = text;
        for (String secret : secrets) {
            if (secret != null && !secret.isEmpty()) {
                redacted = redacted.replace(secret, REDACTION_MASK);
            }
        }
        return redacted;
    }

    /**
     * Combined crash-text redaction: passwords first, then 4+ digit numbers.
     * The order matters: if a password holds 4+ digits (e.g. "pass1234") it must
     * be masked as a whole before the digit rule would fragment it.
     */
    static String redactCrashText(String text, Collection<String> secrets) {
        return redactNumbers(redactSecrets(text, secrets));
    }

    private static String normalizeIp(String ip) {
        return ip == null || ip.isBlank() ? DEFAULT_IP : ip.trim();
    }

    private static String normalizeMode(String mode) {
        if (mode != null && "http".equalsIgnoreCase(mode.trim())) {
            return "http";
        }
        return DEFAULT_MODE;
    }
}
