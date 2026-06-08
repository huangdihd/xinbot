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

package xin.bbtt.mcbot.modpack;

import lombok.Getter;
import org.yaml.snakeyaml.Yaml;

import xin.bbtt.mcbot.LangManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Data model for a modpack manifest ({@code modpack.yml}).
 *
 * <p>The manifest describes a distributable bundle of plugins and language
 * resources. Only {@code name} and {@code version} are required; the
 * {@code plugins} list is informational (the actual install is driven by the
 * {@code plugins/} directory inside the archive).
 */
@Getter
public class ModpackManifest {
    /** The file name of the manifest inside a modpack archive. */
    public static final String FILE_NAME = "modpack.yml";

    private final String name;
    private final String version;
    private final String author;
    private final String description;
    private final String xinbotVersion;
    private final List<String> plugins;

    public ModpackManifest(String name, String version, String author, String description,
                           String xinbotVersion, List<String> plugins) {
        this.name = name;
        this.version = version;
        this.author = author;
        this.description = description;
        this.xinbotVersion = xinbotVersion;
        this.plugins = plugins == null ? new ArrayList<>() : plugins;
    }

    /**
     * Parses a manifest from a {@code modpack.yml} input stream.
     *
     * @param is the input stream (not closed by this method)
     * @return the parsed manifest
     * @throws IllegalArgumentException if the manifest is missing required fields
     */
    public static ModpackManifest parse(InputStream is) {
        Map<String, Object> map = new Yaml().load(is);
        if (map == null || map.get("name") == null || map.get("version") == null) {
            throw new IllegalArgumentException(LangManager.get("xinbot.modpack.manifest.invalid"));
        }

        String name = String.valueOf(map.get("name"));
        String version = String.valueOf(map.get("version"));
        String author = map.get("author") == null ? null : String.valueOf(map.get("author"));
        String description = map.get("description") == null ? null : String.valueOf(map.get("description"));
        String xinbotVersion = map.get("xinbotVersion") == null ? null : String.valueOf(map.get("xinbotVersion"));

        List<String> plugins = new ArrayList<>();
        Object pluginsObj = map.get("plugins");
        if (pluginsObj instanceof List<?> list) {
            for (Object o : list) plugins.add(String.valueOf(o));
        }

        return new ModpackManifest(name, version, author, description, xinbotVersion, plugins);
    }

    /**
     * Reads the manifest from a modpack {@code .zip} archive without extracting anything.
     *
     * @param zipFile path to the modpack archive
     * @return the parsed manifest
     * @throws IOException              if the archive cannot be read
     * @throws IllegalArgumentException if the manifest is missing or invalid
     */
    public static ModpackManifest readFromZip(Path zipFile) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            ZipEntry entry = zip.getEntry(FILE_NAME);
            if (entry == null) {
                throw new IllegalArgumentException(LangManager.get("xinbot.modpack.manifest.missing", FILE_NAME));
            }
            try (InputStream is = zip.getInputStream(entry)) {
                return parse(is);
            }
        }
    }

    /**
     * Serializes this manifest into {@code modpack.yml} YAML text.
     */
    public String toYaml() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("version", version);
        if (author != null) map.put("author", author);
        if (description != null) map.put("description", description);
        if (xinbotVersion != null) map.put("xinbotVersion", xinbotVersion);
        if (plugins != null && !plugins.isEmpty()) map.put("plugins", plugins);
        return new Yaml().dump(map);
    }
}
