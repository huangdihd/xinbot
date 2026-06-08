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

    /** Outcome of an install: how much was extracted, and the config written (if any). */
    public record InstallResult(int plugins, int langs, Path configFile) {
        /** @return true if a bundled config was extracted and needs completing. */
        public boolean hasConfig() {
            return configFile != null;
        }
    }

    /** Installs without extracting any bundled config. */
    public static InstallResult install(Path zipFile, Path pluginDir, Path langDir) throws IOException {
        return install(zipFile, pluginDir, langDir, null);
    }

    /**
     * Installs the modpack at {@code zipFile}.
     *
     * @param zipFile    path to the modpack archive
     * @param pluginDir  destination directory for plugin jars
     * @param langDir    destination directory for {@code .lang} overrides (usually {@code ./lang})
     * @param configFile destination for a bundled {@code config.conf}, or {@code null} to skip it;
     *                   an existing config file is never overwritten
     * @return what was installed
     * @throws IOException              if the archive cannot be read or files cannot be written
     * @throws IllegalArgumentException if the manifest is missing or invalid
     */
    public static InstallResult install(Path zipFile, Path pluginDir, Path langDir, Path configFile) throws IOException {
        if (!Files.isRegularFile(zipFile)) {
            throw new IOException(LangManager.get("xinbot.modpack.file.not_found", zipFile));
        }

        ModpackManifest manifest = ModpackManifest.readFromZip(zipFile);
        log.info(LangManager.get("xinbot.modpack.install.start", manifest.getName(), manifest.getVersion()));

        Path pluginRoot = pluginDir.toAbsolutePath().normalize();
        Path langRoot = langDir.toAbsolutePath().normalize();
        Files.createDirectories(pluginRoot);
        Files.createDirectories(langRoot);

        int[] counts = new int[2];
        Path writtenConfig;
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                installEntry(zip, entries.nextElement(), pluginRoot, langRoot, counts);
            }
            writtenConfig = extractConfig(zip, configFile);
        }

        log.info(LangManager.get("xinbot.modpack.install.done", manifest.getName(), counts[0], counts[1]));
        return new InstallResult(counts[0], counts[1], writtenConfig);
    }

    /**
     * Extracts a bundled {@code config.conf} to {@code configFile}, unless one is
     * already present (an existing config — possibly with real credentials — is
     * never clobbered).
     *
     * @return the written config path, or {@code null} if nothing was extracted
     */
    private static Path extractConfig(ZipFile zip, Path configFile) throws IOException {
        if (configFile == null) return null;
        ZipEntry entry = zip.getEntry(ModpackConfig.ENTRY);
        if (entry == null) return null;

        Path target = configFile.toAbsolutePath().normalize();
        if (Files.exists(target)) {
            log.warn(LangManager.get("xinbot.modpack.install.config.exists", target.getFileName().toString()));
            return null;
        }
        if (target.getParent() != null) Files.createDirectories(target.getParent());

        try (InputStream is = zip.getInputStream(entry)) {
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info(LangManager.get("xinbot.modpack.install.config.written", target.getFileName().toString()));
        return target;
    }

    /**
     * Handles a single archive entry: extracts whitelisted plugin/lang files into
     * their target directory, skipping directories and unsafe paths. Everything
     * outside {@code plugins/*.jar} and {@code lang/*.lang} (including
     * {@code modpack.yml}) is ignored. {@code counts[0]} / {@code counts[1]}
     * accumulate the number of installed plugins / language files.
     */
    private static void installEntry(ZipFile zip, ZipEntry entry, Path pluginRoot, Path langRoot, int[] counts)
            throws IOException {
        if (entry.isDirectory()) return;
        String name = entry.getName();
        if (hasParentTraversal(name)) {
            log.warn(LangManager.get("xinbot.modpack.zip.unsafe_entry", name));
            return;
        }
        if (matches(name, PLUGINS_PREFIX, ".jar") && extract(zip, entry, pluginRoot, fileName(name))) {
            counts[0]++;
        } else if (matches(name, LANG_PREFIX, ".lang") && extract(zip, entry, langRoot, fileName(name))) {
            counts[1]++;
        }
    }

    /** True if the entry name has the given directory prefix and file suffix. */
    private static boolean matches(String name, String prefix, String suffix) {
        return name.startsWith(prefix) && name.endsWith(suffix);
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
