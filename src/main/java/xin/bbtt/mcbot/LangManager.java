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

package xin.bbtt.mcbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LangManager {
    private static Map<String, String> currentLang = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(LangManager.class.getSimpleName());
    private static String currentLanguageCode = "en_us";

    /**
     * Initialize the language system
     * @param langCode Language code: "default" (auto-detect), "en_us", "zh_cn", etc.
     */
    public static void Init(@Nullable String langCode) {
        loadLanguage(langCode);
    }

    /**
     * Load specified language
     * @param langCode Language code: "default" (auto-detect), null or invalid value defaults to en_us
     */
    public static void loadLanguage(@Nullable String langCode) {
        // Handle default option: auto-detect based on JVM locale
        if (langCode == null || langCode.isBlank() || "default".equalsIgnoreCase(langCode)) {
            Locale locale = Locale.getDefault();
            String language = locale.getLanguage().toLowerCase();
            String country = locale.getCountry().toLowerCase();

            // Use zh_cn for Simplified Chinese locale, otherwise use en_us
            if ("zh".equals(language) && ("cn".equals(country) || "sg".equals(country))) {
                langCode = "zh_cn";
            } else {
                langCode = "en_us";
            }
        } else {
            langCode = langCode.toLowerCase();
        }

        // Validate language code, support multiple languages
        List<String> supportedLanguages = Arrays.asList(
            "en_us",    // English
            "zh_cn",    // 简体中文
            "zh_tw",    // 繁體中文
            "ru_ru",    // Русский
            "ja_jp",    // 日本語
            "de_de",    // Deutsch
            "fr_fr"     // Français
        );
        
        if (!supportedLanguages.contains(langCode)) {
            log.warn("Unsupported language code: {}, falling back to en_us", langCode);
            langCode = "en_us";
        }

        // Load language file
        String langFileName = langCode + ".lang";
        try (InputStream is = LangManager.class.getClassLoader().getResourceAsStream(langFileName)) {
            if (is == null) {
                log.error("Language file not found: {}", langFileName);
                currentLang = new HashMap<>();
                currentLanguageCode = "en_us";
                return;
            }

            Map<String, String> langMap = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    line = line.trim();

                    // Skip empty lines and comments
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
            
            currentLang = langMap;
            currentLanguageCode = langCode;
            log.info("Loaded language {}", langCode);

        } catch (Exception e) {
            log.error("Error loading language file {}: {}", langFileName, e.getMessage(), e);
            currentLang = new HashMap<>();
            currentLanguageCode = "en_us";
        }
    }

    /**
     * Get translated text
     * @param key Language key
     * @return Translated text, returns the key itself if not found
     */
    public static String get(String key) {
        return currentLang.getOrDefault(key, key);
    }

    /**
     * Get translated text with formatting
     * @param key Language key
     * @param args Format arguments
     * @return Formatted translated text
     */
    public static String get(String key, Object... args) {
        String template = currentLang.getOrDefault(key, key);
        try {
            return String.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }

    /**
     * Get current language code
     * @return Currently used language code
     */
    public static String getCurrentLanguage() {
        return currentLanguageCode;
    }
}
