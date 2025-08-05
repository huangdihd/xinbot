package xin.bbtt.mcbot;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LangManager {
    private static Map<String, String> currentLang = Map.of();
    private static final Logger log = LoggerFactory.getLogger(LangManager.class.getSimpleName());

    private LangManager() {}

    public static void loadLanguage(@Nullable String langCode) {
        if (langCode == null || langCode.isBlank()) {
            langCode = Locale.getDefault()
                    .toLanguageTag()
                    .toLowerCase()
                    .replace("-", "_"); // e.g. zh-CN → zh_cn
        }

        try (InputStream is = LangManager.class.getClassLoader().getResourceAsStream("lang.json")) {
            if (is == null) {
                currentLang = Map.of();
                return;
            }

            JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();

            JsonObject langObj = root.getAsJsonObject(langCode);
            if (langObj == null) {
                langObj = root.getAsJsonObject("en_us");
                if (langObj == null) {
                    currentLang = Map.of();
                    return;
                }
            }

            Type type = new TypeToken<Map<String, String>>() {}.getType();
            currentLang = new Gson().fromJson(langObj, type);

            log.info("Loaded language {}", langCode);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            currentLang = Map.of();
        }
    }

    public static void unloadLanguage() {
        currentLang = Map.of();
        log.info("Language unloaded");
        System.gc();
    }

    public static String get(String key) {
        return currentLang.getOrDefault(key, key);
    }
}
