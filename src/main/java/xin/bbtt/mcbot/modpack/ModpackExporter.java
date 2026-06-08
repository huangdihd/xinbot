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
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Packs the current environment (plugin jars + external language overrides)
 * into a distributable modpack {@code .zip}.
 */
public class ModpackExporter {
    private static final Logger log = LoggerFactory.getLogger(ModpackExporter.class.getSimpleName());

    /** Exports a modpack of plugins and language files (without a bundled config). */
    public static void export(Path outZip, Path pluginDir, Path langDir, ModpackManifest manifest) throws IOException {
        export(outZip, pluginDir, langDir, manifest, null);
    }

    /**
     * Exports a modpack archive to {@code outZip}.
     *
     * @param outZip      destination archive path
     * @param pluginDir   directory whose {@code *.jar} files are bundled under {@code plugins/}
     * @param langDir     directory whose {@code *.lang} files are bundled under {@code lang/}
     * @param manifest    the manifest written as {@code modpack.yml}; its {@code plugins}
     *                    list is regenerated from the bundled jars when empty
     * @param configHocon a sanitized {@code config.conf} body to bundle under {@code config/},
     *                    or {@code null} to omit it. Callers must sanitize before passing.
     * @throws IOException if the archive cannot be written
     */
    public static void export(Path outZip, Path pluginDir, Path langDir, ModpackManifest manifest,
                              String configHocon) throws IOException {
        List<String> jarNames = listFiles(pluginDir, ".jar");
        List<String> langNames = listFiles(langDir, ".lang");
        ModpackManifest effective = withDerivedPlugins(manifest, jarNames);

        if (outZip.getParent() != null) Files.createDirectories(outZip.getParent());

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outZip))) {
            writeEntry(zos, ModpackManifest.FILE_NAME, effective.toYaml().getBytes(StandardCharsets.UTF_8));
            for (String jar : jarNames) copyEntry(zos, "plugins/" + jar, pluginDir.resolve(jar));
            for (String lang : langNames) copyEntry(zos, "lang/" + lang, langDir.resolve(lang));
            if (configHocon != null) writeEntry(zos, ModpackConfig.ENTRY, configHocon.getBytes(StandardCharsets.UTF_8));
        }

        log.info(LangManager.get("xinbot.modpack.export.done", outZip, jarNames.size(), langNames.size()));
        if (configHocon != null) log.info(LangManager.get("xinbot.modpack.export.config"));
    }

    /**
     * Returns a manifest whose {@code plugins} list is derived from the bundled jar
     * names when the supplied manifest does not already list them.
     */
    private static ModpackManifest withDerivedPlugins(ModpackManifest manifest, List<String> jarNames) {
        if (!manifest.getPlugins().isEmpty() || jarNames.isEmpty()) return manifest;
        List<String> derived = new ArrayList<>();
        for (String jar : jarNames) derived.add(jar.substring(0, jar.length() - ".jar".length()));
        return new ModpackManifest(manifest.getName(), manifest.getVersion(), manifest.getAuthor(),
                manifest.getDescription(), manifest.getXinbotVersion(), derived);
    }

    private static List<String> listFiles(Path dir, String suffix) throws IOException {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(dir)) return names;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (Files.isRegularFile(p) && p.getFileName().toString().endsWith(suffix)) {
                    names.add(p.getFileName().toString());
                }
            }
        }
        return names;
    }

    private static void writeEntry(ZipOutputStream zos, String name, byte[] data) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(data);
        zos.closeEntry();
    }

    private static void copyEntry(ZipOutputStream zos, String name, Path source) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        Files.copy(source, zos);
        zos.closeEntry();
    }

    private ModpackExporter() {}
}
