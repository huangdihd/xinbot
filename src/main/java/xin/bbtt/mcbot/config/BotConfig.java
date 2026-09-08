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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import lombok.Getter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class BotConfig {
    public BotConfig(String configPath) throws FileNotFoundException, JsonProcessingException {
        this.loadFromFile(configPath);
    }
    private String configPath;
    @Getter
    private BotConfigData configData;

    public void loadFromFile(String configPath) throws FileNotFoundException, JsonProcessingException {
        File configFile = new File(configPath);
        if (!configFile.exists()) {
            throw new FileNotFoundException(xin.bbtt.mcbot.LangManager.get("xinbot.config.file.not_found", configPath));
        }

        this.configPath = configPath;
        Config userConfig = ConfigFactory.parseFile(configFile);

        Config defaultConfig = ConfigFactory.parseResources(
            BotConfig.class.getClassLoader(),
            "config.conf"
        );

        Config config = userConfig
            .withFallback(defaultConfig)
            .resolve();
        ObjectMapper mapper = new ObjectMapper(new HoconFactory());
        configData = mapper.readValue(
                config.root().render(),
                BotConfigData.class
        );
    }

    /**
     * Renders a {@link BotConfigData} into formatted HOCON/JSON text, the same
     * format written to {@code config.conf}.
     */
    public static String render(BotConfigData data) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> configMap = mapper.convertValue(data, new TypeReference<>() {
        });
        Config hocon = ConfigFactory.parseMap(configMap);
        return hocon.root().render(
                ConfigRenderOptions.defaults()
                        .setJson(true)
                        .setFormatted(true)
                        .setComments(false)
                        .setOriginComments(false)
        );
    }

    public void saveToFile() throws IOException {
        Files.writeString(
                Path.of(configPath),
                render(configData),
                java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    /**
     * Rewrites the {@code telemetry.enable} value inside config text (e.g. the
     * freshly copied default config) so the first-run telemetry opt-in choice
     * survives restarts, keeping every comment and the rest of the file intact.
     * Only the value inside the {@code telemetry} block is touched: it must
     * already be a boolean literal, otherwise nothing is changed and
     * {@code null} is returned. Never touches e.g. {@code proxy.enable}.
     */
    static String replaceTelemetryEnabled(String configText, boolean enabled) {
        int block = configText.indexOf("\"telemetry\"");
        if (block < 0) {
            return null;
        }
        int key = configText.indexOf("\"enable\"", block);
        if (key < 0) {
            return null;
        }
        int colon = configText.indexOf(':', key);
        if (colon < 0) {
            return null;
        }
        int valueStart = colon + 1;
        while (valueStart < configText.length()
                && Character.isWhitespace(configText.charAt(valueStart))) {
            valueStart++;
        }
        int valueEnd = valueStart;
        while (valueEnd < configText.length()
                && configText.charAt(valueEnd) != ','
                && configText.charAt(valueEnd) != '\n'
                && configText.charAt(valueEnd) != '\r') {
            valueEnd++;
        }
        while (valueEnd > valueStart
                && Character.isWhitespace(configText.charAt(valueEnd - 1))) {
            valueEnd--;
        }
        String current = configText.substring(valueStart, valueEnd);
        if (!current.equals("true") && !current.equals("false")) {
            return null;
        }
        return configText.substring(0, valueStart) + enabled + configText.substring(valueEnd);
    }

    /**
     * Applies the first-run telemetry opt-in choice to the config file on disk.
     * Returns {@code false} (leaving the file untouched) when the file does not
     * contain a boolean {@code telemetry.enable} to rewrite.
     */
    public static boolean setTelemetryEnabled(Path configPath, boolean enabled) throws IOException {
        String text = Files.readString(configPath, java.nio.charset.StandardCharsets.UTF_8);
        String updated = replaceTelemetryEnabled(text, enabled);
        if (updated == null) {
            return false;
        }
        Files.writeString(configPath, updated, java.nio.charset.StandardCharsets.UTF_8);
        return true;
    }
}
