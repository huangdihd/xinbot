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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LangManager {
    // Use HashMap to support key-value merging and overriding across multiple loads
    private static final Map<String, String> currentLang = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(LangManager.class.getSimpleName());
    private static String currentLanguageCode = "en_us";

    // Supported languages for .lang files
    private static final List<String> SUPPORTED_LANG_FILES = Arrays.asList(
        "en_us",    // English
        "zh_cn",    // Simplified Chinese
        "zh_tw",    // Traditional Chinese
        "ru_ru",    // Russian
        "ja_jp",    // Japanese
        "de_de",    // German
        "fr_fr"     // French
    );

    public static void clear() {
        currentLang.clear();
    }

    public static void initMinecraftLang() {
        String targetLangCode = getSystemLangCode();
        loadFromJson(targetLangCode);
        System.gc();
    }

    public static void initXinbotLang() {
        String targetLangCode = getSystemLangCode();
        loadFromJson(targetLangCode);
        loadFromLangFile(targetLangCode);
    }

    /**
     * Gets the system's default language code (e.g., zh_cn, en_us)
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
     * Loads the aggregated lang.json file.
     * Validates dynamically by checking if the JSON contains the target language key.
     */
    public static void loadFromJson(@Nullable String langCode) {
        if (langCode == null || langCode.isBlank()) langCode = "en_us";

        try (InputStream is = LangManager.class.getClassLoader().getResourceAsStream("lang.json")) {
            if (is == null) {
                log.debug("lang.json not found, skipping JSON load.");
                return;
            }

            JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();

            // Dynamic validation: Check if the language exists in the JSON root
            if (!root.has(langCode)) {
                log.warn("Language {} not supported in lang.json, falling back to en_us", langCode);
                langCode = "en_us";
            }

            JsonObject langObj = root.getAsJsonObject(langCode);
            if (langObj != null) {
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> jsonMap = new Gson().fromJson(langObj, type);
                if (jsonMap != null) {
                    currentLang.putAll(jsonMap);
                    currentLanguageCode = langCode;
                    log.info("Loaded language {} from lang.json", langCode);
                }
            }

        } catch (Exception e) {
            log.error("Error loading lang.json: {}", e.getMessage(), e);
        }
    }

    /**
     * Loads standalone .lang files (Minecraft format).
     * Validates against a predefined list of supported languages.
     */
    public static void loadFromLangFile(@Nullable String langCode) {
        if (langCode == null || langCode.isBlank()) langCode = "en_us";

        // Explicit validation: Check against the supported .lang files list
        if (!SUPPORTED_LANG_FILES.contains(langCode)) {
            log.warn("Language {} not supported for .lang files, falling back to en_us", langCode);
            langCode = "en_us";
        }

        String langFileName = langCode + ".lang";
        try (InputStream is = LangManager.class.getClassLoader().getResourceAsStream(langFileName)) {
            if (is == null) {
                if (!"en_us".equals(langCode)) {
                    log.debug("{} not found, trying en_us.lang fallback", langFileName);
                    tryFallbackLangFile();
                }
                return;
            }

            currentLang.putAll(parseLangStream(is));
            currentLanguageCode = langCode;
            log.info("Loaded language {} from {}", langCode, langFileName);

        } catch (Exception e) {
            log.error("Error loading language file {}: {}", langFileName, e.getMessage(), e);
        }
    }

    /**
     * Attempts to load en_us.lang as a fallback
     */
    private static void tryFallbackLangFile() {
        try (InputStream is = LangManager.class.getClassLoader().getResourceAsStream("en_us.lang")) {
            if (is != null) {
                currentLang.putAll(parseLangStream(is));
                // Only override currentLanguageCode if it wasn't already set properly by JSON
                if (!currentLanguageCode.equals(getSystemLangCode())) {
                    currentLanguageCode = "en_us";
                }
                log.info("Loaded fallback language en_us from en_us.lang");
            }
        } catch (Exception e) {
            log.error("Error loading fallback en_us.lang: {}", e.getMessage());
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
