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
import java.util.Optional;

public class LangManager {
    public static final String DEFAULT_LANGUAGE = "en_us";
    
    // Use HashMap to support key-value merging and overriding across multiple loads
    private static final Map<String, String> currentLang = new HashMap<>();

    // Bootstrap translations for LangManager itself to use if loading fails
    private static final Map<String, Map<String, String>> bootstrap = Map.of(
            "zh", Map.of(
                    "xinbot.langmanager.json.not_found", "未找到 lang/lang.json，跳过 JSON 加载。",
                    "xinbot.langmanager.json.error", "加载语言 %s 的 lang.json 时出错：%s",
                    "xinbot.langmanager.classloader.error", "通过 ClassLoader 加载语言 %s 时出错：%s",
                    "xinbot.config.error", "加载配置文件时出错"
            ),
            "en", Map.of(
                    "xinbot.langmanager.json.not_found", "lang/lang.json not found, skipping JSON load.",
                    "xinbot.langmanager.json.error", "Error loading lang.json for %s: %s",
                    "xinbot.langmanager.classloader.error", "Error loading language %s from classloader: %s",
                    "xinbot.config.error", "Error loading config file"
            ),
            "de", Map.of(
                    "xinbot.langmanager.json.not_found", "lang/lang.json nicht gefunden, JSON-Ladevorgang wird übersprungen.",
                    "xinbot.langmanager.json.error", "Fehler beim Laden von lang.json für %s: %s",
                    "xinbot.langmanager.classloader.error", "Fehler beim Laden der Sprache %s über ClassLoader: %s",
                    "xinbot.config.error", "Fehler beim Laden der Konfigurationsdatei"
            ),
            "fr", Map.of(
                    "xinbot.langmanager.json.not_found", "lang/lang.json non trouvé, saut du chargement JSON.",
                    "xinbot.langmanager.json.error", "Erreur lors du chargement de lang.json pour %s : %s",
                    "xinbot.langmanager.classloader.error", "Erreur lors du chargement de la langue %s depuis le classloader : %s",
                    "xinbot.config.error", "Erreur lors du chargement du fichier de configuration"
            ),
            "ja", Map.of(
                    "xinbot.langmanager.json.not_found", "lang/lang.json が见つかりません。JSON の読み込みをスキップします。",
                    "xinbot.langmanager.json.error", "%s の lang.json 読み込み中にエラーが発生しました: %s",
                    "xinbot.langmanager.classloader.error", "ClassLoader から言语 %s を読み込み中にエラーが発生しました: %s",
                    "xinbot.config.error", "設定ファイルの読み込み中にエラーが発生しました"
            ),
            "ru", Map.of(
                    "xinbot.langmanager.json.not_found", "lang/lang.json не найден, пропуск загрузки JSON.",
                    "xinbot.langmanager.json.error", "Ошибка при загрузке lang.json для %s: %s",
                    "xinbot.langmanager.classloader.error", "Ошибка при загрузке языка %s из classloader: %s",
                    "xinbot.config.error", "Ошибка загрузки файла конфигурации"
            )
    );

    private static final Logger log = LoggerFactory.getLogger(LangManager.class.getSimpleName());
    private static String currentLanguageCode = DEFAULT_LANGUAGE;

    public static void clear() {
        currentLang.clear();
    }

    /**
     * Initializes the language manager by detecting the system language.
     */
    public static void init() {
        currentLanguageCode = getSystemLangCode();
        // Load bootstrap for detected language
        String shortLang = currentLanguageCode.split("_")[0];
        currentLang.putAll(bootstrap.getOrDefault(shortLang, bootstrap.get("en")));
    }

    /**
     * Loads Minecraft protocol translations from internal lang.json.
     */
    public static void loadMinecraft() {
        String targetLangCode = getCurrentLanguage();
        // Load default as base fallback
        loadFromJson(DEFAULT_LANGUAGE);
        if (!DEFAULT_LANGUAGE.equals(targetLangCode)) {
            loadFromJson(targetLangCode);
        }
        System.gc();
    }

    /**
     * Loads external overrides from the ./lang/ directory.
     */
    public static void loadExternal() {
        String targetLangCode = getCurrentLanguage();
        // 1. Load default as base fallback
        loadFromExternalLangFile(DEFAULT_LANGUAGE);
        // 2. Override with target language
        if (!DEFAULT_LANGUAGE.equals(targetLangCode)) {
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

        // 1. Load default as base fallback from resources
        loadFromClassLoader(classLoader, DEFAULT_LANGUAGE);

        // 2. Override with target language if not default
        if (!DEFAULT_LANGUAGE.equals(targetLangCode)) {
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
        return tag.isBlank() ? DEFAULT_LANGUAGE : tag;
    }

    /**
     * Loads the aggregated lang.json file from internal resources.
     */
    public static void loadFromJson(@Nullable String langCode) {
        final String targetLang = Optional.ofNullable(langCode)
            .filter(s -> !s.isBlank())
            .orElse(DEFAULT_LANGUAGE);

        try (InputStream is = LangManager.class.getClassLoader().getResourceAsStream("lang/lang.json")) {
            if (is == null) {
                log.debug(get("xinbot.langmanager.json.not_found"));
                return;
            }

            JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();

            if (!root.has(targetLang)) {
                log.debug(get("xinbot.langmanager.json.lang_not_found", targetLang));
                return;
            }

            JsonObject langObj = root.getAsJsonObject(targetLang);
            if (langObj == null) return;

            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> jsonMap = new Gson().fromJson(langObj, type);
            if (jsonMap != null) {
                currentLang.putAll(jsonMap);
                log.debug(get("xinbot.langmanager.json.loaded", targetLang));
            }

        } catch (Exception e) {
            log.error(get("xinbot.langmanager.json.error", targetLang, e.getMessage()), e);
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
            log.info(get("xinbot.langmanager.external.loaded", langCode, langFile));
        } catch (Exception e) {
            log.error(get("xinbot.config.error") + " " + langFile + ": " + e.getMessage(), e);
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
     * The stream is closed after reading.
     * @param is Input stream to parse
     * @throws IOException If reading fails
     */
    public static void loadFromStream(InputStream is) throws IOException {
        if (is == null) return;
        try (is) {
            currentLang.putAll(parseLangStream(is));
        }
    }

    /**
     * Loads translations from a .lang file.
     * @param file File to load
     * @throws IOException If reading fails
     */
    public static void loadFromFile(File file) throws IOException {
        if (file == null || !file.exists()) return;
        loadFromStream(Files.newInputStream(file.toPath()));
    }

    /**
     * Loads translations from a ClassLoader's resources.
     */
    public static void loadFromClassLoader(ClassLoader classLoader, String langCode) {
        if (classLoader == null || langCode == null || langCode.isBlank()) return;
        String fileName = "lang/" + langCode + ".lang";
        try (InputStream is = classLoader.getResourceAsStream(fileName)) {
            if (is == null) return;
            loadFromStream(is);
        } catch (Exception e) {
            log.error(get("xinbot.langmanager.classloader.error", langCode, e.getMessage()));
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
                if (equalsIndex <= 0) continue;

                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();
                if (!key.isEmpty()) {
                    // Allow basic escaping for newlines and tabs
                    value = value.replace("\\n", "\n").replace("\\t", "\t");
                    langMap.put(key, value);
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
            log.warn(get("xinbot.langmanager.format.error", key, e.getMessage()));
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
