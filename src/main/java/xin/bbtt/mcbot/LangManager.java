/*
 * # Copyright (C) 2025 huangdihd
 * #
 * # This program is free software: you can redistribute it and/or modify
 * # it under the terms of the GNU General Public License as published by
 * # the Free Software Foundation, either version 3 of the License, or
 * # (at your option) any later version.
 * #
 * # This program is distributed in the hope that it will be useful,
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * # GNU General Public License for more details.
 * #
 * # You should have received a copy of the GNU General Public License
 * # along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

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

    public static void Init() {
        loadLanguage(null);
    }

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
                langCode = "en_us";
                langObj = root.getAsJsonObject("en_us");
                if (langObj == null) {
                    currentLang = Map.of();
                    return;
                }
            }
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            currentLang = new Gson().fromJson(langObj, type);

            System.gc();

            log.info("Loaded language {}", langCode);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            currentLang = Map.of();
        }
    }

    public static String get(String key) {
        return currentLang.getOrDefault(key, key);
    }
}
