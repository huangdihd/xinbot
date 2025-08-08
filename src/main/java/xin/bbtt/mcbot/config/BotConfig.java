package xin.bbtt.mcbot.config;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import lombok.Data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Data
public class  BotConfig {
    private Account account;
    private String owner;
    private Plugin plugin;
    private Advances advances;

    public static BotConfig loadFromFile(String configPath) throws FileNotFoundException, JsonProcessingException {
        File configFile = new File(configPath);
        if (!configFile.isFile()) {
            throw new FileNotFoundException("Config file not found: " + configPath);
        }
        Config config = ConfigFactory.parseFile(configFile).resolve();
        ObjectMapper mapper = new ObjectMapper(new HoconFactory());
        return mapper.readValue(
                config.root().render(),
                BotConfig.class
        );
    }

    public static void saveToFile(String configPath, BotConfig botConfig) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> configMap = mapper.convertValue(botConfig, new TypeReference<>() {
        });
        Config hocon = ConfigFactory.parseMap(configMap);
        String text = hocon.root().render(
                ConfigRenderOptions.defaults()
                        .setJson(false)
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

    @Data
    public static class Account {
        private boolean onlineMode;
        private String name;
        private String password;
        private String fullSession;
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

