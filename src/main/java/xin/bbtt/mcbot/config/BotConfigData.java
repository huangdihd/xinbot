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
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class  BotConfigData {
    private Account account;
    private String owner;
    private Plugin plugin;
    private Advances advances;

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
}
