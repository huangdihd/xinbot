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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xin.bbtt.mcbot;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LangManager {
    // Use HashMap to support key-value merging and overriding across multiple loads
    private static final Map<String, String> currentLang = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(LangManager.class.getSimpleName());
    private static String currentLanguageCode = "en_us";

    public static void clear() {
        currentLang.clear();
    }

    public static void initMinecraftLang() {
        String targetLangCode = getSystemLangCode();
        // Load en_us as base fallback
        loadFromJson("en_us");
        if (!"en_us".equals(targetLangCode)) {
            // Override with target language
            loadFromJson(targetLangCode);
        }
        currentLanguageCode = targetLangCode;
        System.gc();
    }

    public static void initXinbotLang() {
        String targetLangCode = getSystemLangCode();
        
        // 1. Load en_us as base fallback from JSON and internal .lang
        loadFromJson("en_us");
        loadFromLangFile("en_us", true);
        
        // 2. Override with target language from internal resources
        if (!"en_us".equals(targetLangCode)) {
            loadFromJson(targetLangCode);
            loadFromLangFile(targetLangCode, true);
        }
        
        // 3. Override with external files (allows users to customize without recompiling)
        loadFromExternalLangFile("en_us");
        if (!"en_us".equals(targetLangCode)) {
            loadFromExternalLangFile(targetLangCode);
        }
        
        currentLanguageCode = targetLangCode;
    }

    /**
     * Gets the system's default language code (e.g., zh_cn, en_us)
     * This respects standard JVM parameters like -Duser.language=zh -Duser.country=CN
     */
    private static String getSystemLangCode() {
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage().toLowerCase();
        String country = locale.getCountry().toLowerCase();

        // Special handling for Simplified Chinese
        if ("zh".equals(language) && ("cn".equals(country) || "sg".equals(country))) {
            return "zh_cn";
        }

        String tag = locale.toLanguageTag().toLowerCase().replace("-", "_");
        return tag.isBlank() ? "en_us" : tag;
    }

    /**
     * Loads the aggregated lang.json file from internal resources.
     */
    public static void loadFromJson(@Nullable String langCode) {
        if (langCode == null || langCode.isBlank()) langCode = "en_us";

        try (InputStream is = LangManager.class.getClassLoader().getResourceAsStream("lang.json")) {
            if (is == null) {
                log.debug("lang.json not found, skipping JSON load.");
                return;
            }

            JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();

            if (!root.has(langCode)) {
                log.debug("Language {} not found in lang.json", langCode);
                return; // Fallback is already handled by loading en_us first
            }

            JsonObject langObj = root.getAsJsonObject(langCode);
            if (langObj != null) {
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> jsonMap = new Gson().fromJson(langObj, type);
                if (jsonMap != null) {
                    currentLang.putAll(jsonMap);
                    log.debug("Loaded language {} from lang.json", langCode);
                }
            }

        } catch (Exception e) {
            log.error("Error loading lang.json for {}: {}", langCode, e.getMessage(), e);
        }
    }

    /**
     * Loads standalone .lang files (Minecraft format) from internal resources.
     */
    public static void loadFromLangFile(@Nullable String langCode, boolean internal) {
        if (langCode == null || langCode.isBlank()) return;

        String langFileName = langCode + ".lang";
        try (InputStream is = LangManager.class.getClassLoader().getResourceAsStream(langFileName)) {
            if (is == null) {
                log.debug("Internal language file {} not found", langFileName);
                return;
            }

            currentLang.putAll(parseLangStream(is));
            log.debug("Loaded language {} from internal {}", langCode, langFileName);

        } catch (Exception e) {
            log.error("Error loading internal language file {}: {}", langFileName, e.getMessage(), e);
        }
    }
    
    /**
     * Loads translations from an external directory ./lang/
     * This allows server admins to customize bot messages.
     */
    private static void loadFromExternalLangFile(String langCode) {
        Path langDir = Paths.get("lang");
        if (!Files.isDirectory(langDir)) {
            return;
        }
        
        Path langFile = langDir.resolve(langCode + ".lang");
        if (Files.isRegularFile(langFile)) {
            try (InputStream is = Files.newInputStream(langFile)) {
                currentLang.putAll(parseLangStream(is));
                log.info("Loaded custom language {} from external {}", langCode, langFile);
            } catch (Exception e) {
                log.error("Error loading external language file {}: {}", langFile, e.getMessage(), e);
            }
        }
    }

    /**
     * Parses the input stream of a .lang file
     */
    private static @NonNull Map<String, String> parseLangStream(InputStream is) throws IOException {
        Map<String, String> langMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Ignore empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Parse key=value format
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    // Supports empty values, though uncommon
                    String value = line.substring(equalsIndex + 1).trim();
                    if (!key.isEmpty()) {
                        // Allow basic escaping for newlines if users want multi-line messages
                        value = value.replace("\\n", "\n");
                        langMap.put(key, value);
                    }
                }
            }
        }
        return langMap;
    }

    /**
     * Gets the translated text
     * @param key Language key
     * @return Translated text, or the key itself if not found
     */
    public static String get(String key) {
        return currentLang.getOrDefault(key, key);
    }

    /**
     * Gets the translated text with formatting parameters
     * @param key Language key
     * @param args Formatting arguments
     * @return Formatted translated text
     */
    public static String get(String key, Object... args) {
        String template = currentLang.getOrDefault(key, key);
        try {
            return String.format(template, args);
        } catch (Exception e) {
            log.warn("Error formatting lang key {}: {}", key, e.getMessage());
            return template;
        }
    }

    /**
     * Gets the currently loaded language code
     * @return Currently used language code
     */
    public static String getCurrentLanguage() {
        return currentLanguageCode;
    }
}
