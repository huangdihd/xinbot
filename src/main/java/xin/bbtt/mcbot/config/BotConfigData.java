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

package xin.bbtt.mcbot.config;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.net.InetSocketAddress;

@Data
public class  BotConfigData {
    private Account account;
    private String owner;
    private Boolean checkForUpdates;
    private Plugin plugin;
    private Proxy proxy;
    private Telemetry telemetry = new Telemetry();
    private boolean enableTranslation;
    private int reconnectTimeout = 5000;
    private int reconnectDelay = 3000;

    @Data
    public static class Account {
        private boolean onlineMode;
        private String name;
        private String password;
        private JsonNode fullSession;
    }

    @Data
    public static class Plugin {
        private String directory;
    }

    @Data
    public static class Telemetry {
        // Opt-in: telemetry stays off unless explicitly enabled in the user config.
        private boolean enable = false;
        private String mode = "udp";        // Transport mode: "udp" (default) or "http"
        private String ip = "127.0.0.1";    // Telemetry server IP address
        private int port = 9000;            // Telemetry server port
        // Deployment-specific secret shared with the telemetry server: Base64 of 32 random
        // bytes (generate with `openssl rand -base64 32`). Empty means telemetry cannot start.
        private String key = "";

        // Per-field privacy switches (default true = report everything). Set a switch to
        // false to stop sending that piece of data; the server then shows a placeholder
        // for the omitted field. Only applies to heartbeat & crash base fields; the crash
        // details (thread_name / exception / stack_trace) are always reported.
        private boolean sendBot = true;       // report "bot" (BOT name)
        private boolean sendServer = true;    // report "server" (server address)
        private boolean sendState = true;     // report "online" + "state" (login status & main-server stage)
        private boolean sendPlayers = true;   // report "players" (player count)
        private boolean sendUptime = true;    // report "uptime_ms"
        private boolean sendSystem = true;    // report JVM heap / OS / Java version (heartbeat only)
    }

    @Data
    public static class Proxy {
        private boolean enable;
        private ProxyInfo info;
        @Data
        public static class ProxyInfo {
            org.geysermc.mcprotocollib.network.ProxyInfo.Type type;
            InetSocketAddress address;
            String username;
            String password;

            @JsonSetter("type")
            public void setType(String type) {
                if (type == null || type.isEmpty()) {
                    this.type = null;
                    return;
                }
                try {
                    this.type = org.geysermc.mcprotocollib.network.ProxyInfo.Type.valueOf(type.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid proxy type '" + type + "', valid values: "
                            + java.util.Arrays.toString(org.geysermc.mcprotocollib.network.ProxyInfo.Type.values()));
                }
            }

            public org.geysermc.mcprotocollib.network.ProxyInfo toMcProtocolLibProxyInfo() {
                return new org.geysermc.mcprotocollib.network.ProxyInfo(type, address, username, password);
            }
        }
    }
}
