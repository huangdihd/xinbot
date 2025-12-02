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

package xin.bbtt.mcbot.config;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.net.InetSocketAddress;

@Data
public class  BotConfigData {
    private Account account;
    private String owner;
    private Plugin plugin;
    private Advances advances;
    private Proxy proxy;

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
    public static class Advances {
        private boolean enableJLine;
        private boolean enableTranslation;
        private boolean enableHighStability;
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
                } else {
                    this.type = org.geysermc.mcprotocollib.network.ProxyInfo.Type.valueOf(type);
                }
            }

            public org.geysermc.mcprotocollib.network.ProxyInfo toMcProtocolLibProxyInfo() {
                return new org.geysermc.mcprotocollib.network.ProxyInfo(type, address, username, password);
            }
        }
    }
}
