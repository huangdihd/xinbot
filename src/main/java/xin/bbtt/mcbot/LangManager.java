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

    /**
     * Initializes the language manager by detecting the system language.
     */
    public static void init() {
        currentLanguageCode = getSystemLangCode();
    }

    /**
     * Loads Minecraft protocol translations from internal lang.json.
     */
    public static void loadMinecraft() {
        String targetLangCode = getCurrentLanguage();
        // Load en_us as base fallback
        loadFromJson("en_us");
        if (!"en_us".equals(targetLangCode)) {
            loadFromJson(targetLangCode);
        }
        System.gc();
    }

    /**
     * Loads external overrides from the ./lang/ directory.
     */
    public static void loadExternal() {
        String targetLangCode = getCurrentLanguage();
        // 1. Load en_us as base fallback
        loadFromExternalLangFile("en_us");
        // 2. Override with target language
        if (!"en_us".equals(targetLangCode)) {
            loadFromExternalLangFile(targetLangCode);
        }
    }

    /**
     * Initializes translations for a component (core or plugin) using its ClassLoader.
     * @param classLoader The ClassLoader to load resources from
     */
    public static void initLang(ClassLoader classLoader) {
        if (classLoader == null) return;
        String targetLangCode = getCurrentLanguage();

        // 1. Load en_us as base fallback from resources
        loadFromClassLoader(classLoader, "en_us");

        // 2. Override with target language if not en_us
        if (!"en_us".equals(targetLangCode)) {
            loadFromClassLoader(classLoader, targetLangCode);
        }
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
                return;
            }

            JsonObject langObj = root.getAsJsonObject(langCode);
            if (langObj == null) return;

            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> jsonMap = new Gson().fromJson(langObj, type);
            if (jsonMap != null) {
                currentLang.putAll(jsonMap);
                log.debug("Loaded language {} from lang.json", langCode);
            }

        } catch (Exception e) {
            log.error("Error loading lang.json for {}: {}", langCode, e.getMessage(), e);
        }
    }

    /**
     * Loads translations from an external directory ./lang/
     */
    private static void loadFromExternalLangFile(String langCode) {
        Path langDir = Paths.get("lang");
        if (!Files.isDirectory(langDir)) return;
        
        Path langFile = langDir.resolve(langCode + ".lang");
        if (!Files.isRegularFile(langFile)) return;

        try (InputStream is = Files.newInputStream(langFile)) {
            currentLang.putAll(parseLangStream(is));
            log.info("Loaded custom language {} from external {}", langCode, langFile);
        } catch (Exception e) {
            log.error("Error loading external language file {}: {}", langFile, e.getMessage(), e);
        }
    }

    /**
     * Adds a map of translations to the current language.
     * @param translations Map of key-value pairs to add
     */
    public static void addTranslations(Map<String, String> translations) {
        if (translations == null) return;
        currentLang.putAll(translations);
    }

    /**
     * Loads translations from a .lang format input stream.
     * @param is Input stream to parse
     * @throws IOException If reading fails
     */
    public static void loadFromStream(InputStream is) throws IOException {
        if (is == null) return;
        currentLang.putAll(parseLangStream(is));
    }

    /**
     * Loads translations from a .lang file.
     * @param file File to load
     * @throws IOException If reading fails
     */
    public static void loadFromFile(File file) throws IOException {
        if (file == null || !file.exists()) return;
        try (InputStream is = Files.newInputStream(file.toPath())) {
            loadFromStream(is);
        }
    }

    /**
     * Loads translations from a ClassLoader's resources.
     */
    public static void loadFromClassLoader(ClassLoader classLoader, String langCode) {
        if (classLoader == null || langCode == null || langCode.isBlank()) return;
        String fileName = langCode + ".lang";
        try (InputStream is = classLoader.getResourceAsStream(fileName)) {
            if (is == null) return;
            loadFromStream(is);
        } catch (Exception e) {
            log.error("Error loading language {} from classloader: {}", langCode, e.getMessage());
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
                    String value = line.substring(equalsIndex + 1).trim();
                    if (!key.isEmpty()) {
                        // Allow basic escaping for newlines and tabs
                        value = value.replace("\\n", "\n").replace("\\t", "\t");
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
