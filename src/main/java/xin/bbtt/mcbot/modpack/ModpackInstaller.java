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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xin.bbtt.mcbot.LangManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Installs a modpack {@code .zip} into the working environment:
 * {@code plugins/*.jar} are extracted to the plugin directory and
 * {@code lang/*.lang} files to the external language directory ({@code ./lang/}).
 *
 * <p>The installer is non-interactive: existing files are overwritten with a
 * warning so that re-installing a modpack is repeatable.
 */
public class ModpackInstaller {
    private static final Logger log = LoggerFactory.getLogger(ModpackInstaller.class.getSimpleName());

    private static final String PLUGINS_PREFIX = "plugins/";
    private static final String LANG_PREFIX = "lang/";

    /**
     * Installs the modpack at {@code zipFile}.
     *
     * @param zipFile   path to the modpack archive
     * @param pluginDir destination directory for plugin jars
     * @param langDir   destination directory for {@code .lang} overrides (usually {@code ./lang})
     * @throws IOException              if the archive cannot be read or files cannot be written
     * @throws IllegalArgumentException if the manifest is missing or invalid
     */
    public static void install(Path zipFile, Path pluginDir, Path langDir) throws IOException {
        if (!Files.isRegularFile(zipFile)) {
            throw new IOException(LangManager.get("xinbot.modpack.file.not_found", zipFile));
        }

        ModpackManifest manifest = ModpackManifest.readFromZip(zipFile);
        log.info(LangManager.get("xinbot.modpack.install.start", manifest.getName(), manifest.getVersion()));

        Path pluginRoot = pluginDir.toAbsolutePath().normalize();
        Path langRoot = langDir.toAbsolutePath().normalize();
        Files.createDirectories(pluginRoot);
        Files.createDirectories(langRoot);

        int plugins = 0;
        int langs = 0;

        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String name = entry.getName();

                if (hasParentTraversal(name)) {
                    log.warn(LangManager.get("xinbot.modpack.zip.unsafe_entry", name));
                    continue;
                }

                if (name.startsWith(PLUGINS_PREFIX) && name.endsWith(".jar")) {
                    if (extract(zip, entry, pluginRoot, fileName(name))) plugins++;
                } else if (name.startsWith(LANG_PREFIX) && name.endsWith(".lang")) {
                    if (extract(zip, entry, langRoot, fileName(name))) langs++;
                }
                // Everything else (including modpack.yml and unknown paths) is ignored.
            }
        }

        log.info(LangManager.get("xinbot.modpack.install.done", manifest.getName(), plugins, langs));
    }

    /**
     * Safely extracts a single entry into {@code targetDir} under {@code fileName}.
     * Guards against zip-slip by ensuring the resolved path stays inside the target.
     *
     * @return {@code true} if the file was written, {@code false} if it was skipped
     */
    private static boolean extract(ZipFile zip, ZipEntry entry, Path targetDir, String fileName) throws IOException {
        if (fileName.isEmpty()) return false;

        Path target = targetDir.resolve(fileName).normalize();
        if (!target.startsWith(targetDir)) {
            log.warn(LangManager.get("xinbot.modpack.zip.unsafe_entry", entry.getName()));
            return false;
        }

        if (Files.exists(target)) {
            log.warn(LangManager.get("xinbot.modpack.install.overwrite", target.getFileName().toString()));
        }

        try (InputStream is = zip.getInputStream(entry)) {
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info(LangManager.get("xinbot.modpack.install.file", target.getFileName().toString()));
        return true;
    }

    /** Returns the last path segment of a zip entry name. */
    private static String fileName(String entryName) {
        int slash = entryName.lastIndexOf('/');
        return slash < 0 ? entryName : entryName.substring(slash + 1);
    }

    /** True if any path segment of the entry name is a parent-directory traversal ({@code ..}). */
    private static boolean hasParentTraversal(String entryName) {
        for (String segment : entryName.split("/")) {
            if (segment.equals("..")) return true;
        }
        return false;
    }

    private ModpackInstaller() {}
}
