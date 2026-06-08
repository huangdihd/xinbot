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

    /**
     * Exports a modpack archive to {@code outZip}.
     *
     * @param outZip    destination archive path
     * @param pluginDir directory whose {@code *.jar} files are bundled under {@code plugins/}
     * @param langDir   directory whose {@code *.lang} files are bundled under {@code lang/}
     * @param manifest  the manifest written as {@code modpack.yml}; its {@code plugins}
     *                  list is regenerated from the bundled jars when empty
     * @throws IOException if the archive cannot be written
     */
    public static void export(Path outZip, Path pluginDir, Path langDir, ModpackManifest manifest) throws IOException {
        List<String> jarNames = listFiles(pluginDir, ".jar");
        List<String> langNames = listFiles(langDir, ".lang");

        ModpackManifest effective = manifest;
        if (manifest.getPlugins().isEmpty() && !jarNames.isEmpty()) {
            List<String> derived = new ArrayList<>();
            for (String jar : jarNames) derived.add(jar.substring(0, jar.length() - ".jar".length()));
            effective = new ModpackManifest(manifest.getName(), manifest.getVersion(), manifest.getAuthor(),
                    manifest.getDescription(), manifest.getXinbotVersion(), derived);
        }

        if (outZip.getParent() != null) Files.createDirectories(outZip.getParent());

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outZip))) {
            writeEntry(zos, ModpackManifest.FILE_NAME, effective.toYaml().getBytes(StandardCharsets.UTF_8));
            for (String jar : jarNames) copyEntry(zos, "plugins/" + jar, pluginDir.resolve(jar));
            for (String lang : langNames) copyEntry(zos, "lang/" + lang, langDir.resolve(lang));
        }

        log.info(LangManager.get("xinbot.modpack.export.done", outZip, jarNames.size(), langNames.size()));
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
