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
        if (!configFile.isFile()) {
            throw new FileNotFoundException("Config file not found: " + configPath);
        }
        this.configPath = configPath;
        Config config = ConfigFactory.parseFile(configFile).resolve();
        ObjectMapper mapper = new ObjectMapper(new HoconFactory());
        configData = mapper.readValue(
                config.root().render(),
                BotConfigData.class
        );
    }

    public void saveToFile() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> configMap = mapper.convertValue(configData, new TypeReference<>() {
        });
        Config hocon = ConfigFactory.parseMap(configMap);
        String text = hocon.root().render(
                ConfigRenderOptions.defaults()
                        .setJson(true)
                        .setFormatted(true)
                        .setComments(false)
                        .setOriginComments(false)
        );
        Files.writeString(
                Path.of(configPath),
                text,
                java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
        );
    }
}
