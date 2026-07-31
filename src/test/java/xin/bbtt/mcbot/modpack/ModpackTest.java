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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package xin.bbtt.mcbot.modpack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the modpack subsystem: manifest parsing, installation
 * (including zip-slip protection and entry whitelisting) and export round-trips.
 */
class ModpackTest {

    @TempDir
    Path tempDir;

    // ---- manifest --------------------------------------------------------

    @Test
    void parsesValidManifest() {
        ModpackManifest m = ModpackManifest.parse(stream("""
                name: My Pack
                version: 2.0.0
                author: someone
                description: a pack
                plugins: [Foo, Bar]
                """));
        assertThat(m.getName()).isEqualTo("My Pack");
        assertThat(m.getVersion()).isEqualTo("2.0.0");
        assertThat(m.getAuthor()).isEqualTo("someone");
        assertThat(m.getPlugins()).containsExactly("Foo", "Bar");
    }

    @Test
    void rejectsManifestMissingRequiredFields() {
        assertThatThrownBy(() -> ModpackManifest.parse(stream("author: nobody\n")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ModpackManifest.parse(stream("name: only name\n")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void manifestYamlRoundTrips() {
        ModpackManifest original = new ModpackManifest("Pack", "1.2.3", "me", "desc", "2.2.0",
                java.util.List.of("A", "B"));
        ModpackManifest parsed = ModpackManifest.parse(stream(original.toYaml()));
        assertThat(parsed.getName()).isEqualTo("Pack");
        assertThat(parsed.getVersion()).isEqualTo("1.2.3");
        assertThat(parsed.getAuthor()).isEqualTo("me");
        assertThat(parsed.getXinbotVersion()).isEqualTo("2.2.0");
        assertThat(parsed.getPlugins()).containsExactly("A", "B");
    }

    // ---- install ---------------------------------------------------------

    @Test
    void installsPluginsAndLangFiles() throws Exception {
        Path zip = tempDir.resolve("pack.zip");
        writeZip(zip, Map.of(
                "modpack.yml", "name: Pack\nversion: 1.0.0\n",
                "plugins/Foo.jar", "foo-bytes",
                "plugins/Bar.jar", "bar-bytes",
                "lang/zh_cn.lang", "k=v"
        ));

        Path pluginDir = tempDir.resolve("plugin");
        Path langDir = tempDir.resolve("lang");
        ModpackInstaller.install(zip, pluginDir, langDir);

        assertThat(pluginDir.resolve("Foo.jar")).exists();
        assertThat(pluginDir.resolve("Bar.jar")).exists();
        assertThat(langDir.resolve("zh_cn.lang")).hasContent("k=v");
    }

    @Test
    void installCreatesMissingDirectories() throws Exception {
        Path zip = tempDir.resolve("pack.zip");
        writeZip(zip, Map.of(
                "modpack.yml", "name: Pack\nversion: 1.0.0\n",
                "plugins/Foo.jar", "foo"
        ));

        Path pluginDir = tempDir.resolve("nested/plugin");
        Path langDir = tempDir.resolve("nested/lang");
        ModpackInstaller.install(zip, pluginDir, langDir);

        assertThat(pluginDir.resolve("Foo.jar")).exists();
    }

    @Test
    void installOverwritesExistingFiles() throws Exception {
        Path pluginDir = tempDir.resolve("plugin");
        Files.createDirectories(pluginDir);
        Files.writeString(pluginDir.resolve("Foo.jar"), "old");

        Path zip = tempDir.resolve("pack.zip");
        writeZip(zip, Map.of(
                "modpack.yml", "name: Pack\nversion: 1.0.0\n",
                "plugins/Foo.jar", "new"
        ));

        ModpackInstaller.install(zip, pluginDir, tempDir.resolve("lang"));
        assertThat(pluginDir.resolve("Foo.jar")).hasContent("new");
    }

    @Test
    void installIgnoresNonWhitelistedEntries() throws Exception {
        Path zip = tempDir.resolve("pack.zip");
        writeZip(zip, Map.of(
                "modpack.yml", "name: Pack\nversion: 1.0.0\n",
                "plugins/readme.txt", "not a jar",
                "config/secret.conf", "password=123",
                "lang/zh_cn.lang", "k=v"
        ));

        Path pluginDir = tempDir.resolve("plugin");
        Path langDir = tempDir.resolve("lang");
        ModpackInstaller.install(zip, pluginDir, langDir);

        assertThat(pluginDir.resolve("readme.txt")).doesNotExist();
        assertThat(langDir.resolve("zh_cn.lang")).exists();
        // No stray files leaked outside the whitelisted targets.
        assertThat(tempDir.resolve("config")).doesNotExist();
        assertThat(tempDir.resolve("secret.conf")).doesNotExist();
    }

    @Test
    void installRejectsZipSlipEntries() throws Exception {
        Path zip = tempDir.resolve("evil.zip");
        writeZip(zip, Map.of(
                "modpack.yml", "name: Evil\nversion: 1.0.0\n",
                "plugins/../../escape.jar", "pwned"
        ));

        Path pluginDir = tempDir.resolve("install/plugin");
        ModpackInstaller.install(zip, pluginDir, tempDir.resolve("install/lang"));

        // The traversal entry must not be written anywhere.
        assertThat(pluginDir.resolve("escape.jar")).doesNotExist();
        assertThat(tempDir.resolve("escape.jar")).doesNotExist();
        assertThat(tempDir.getParent().resolve("escape.jar")).doesNotExist();
    }

    @Test
    void installFailsOnMissingManifest() throws Exception {
        Path zip = tempDir.resolve("nomanifest.zip");
        writeZip(zip, Map.of("plugins/Foo.jar", "foo"));

        assertThatThrownBy(() ->
                ModpackInstaller.install(zip, tempDir.resolve("plugin"), tempDir.resolve("lang")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- export ----------------------------------------------------------

    @Test
    void exportThenInstallReproducesFiles() throws Exception {
        // Arrange a source environment with two plugins and a lang file.
        Path srcPlugins = tempDir.resolve("src/plugin");
        Path srcLang = tempDir.resolve("src/lang");
        Files.createDirectories(srcPlugins);
        Files.createDirectories(srcLang);
        Files.writeString(srcPlugins.resolve("Foo.jar"), "foo");
        Files.writeString(srcPlugins.resolve("Bar.jar"), "bar");
        Files.writeString(srcLang.resolve("zh_cn.lang"), "k=v");
        Files.writeString(srcPlugins.resolve("notes.txt"), "ignored"); // non-jar ignored

        // Export.
        Path zip = tempDir.resolve("out/pack.zip");
        ModpackExporter.export(zip, srcPlugins, srcLang,
                new ModpackManifest("Pack", "1.0.0", null, null, "2.2.0", null));
        assertThat(zip).exists();

        // The manifest's plugins list is derived from the bundled jars.
        ModpackManifest manifest = ModpackManifest.readFromZip(zip);
        assertThat(manifest.getPlugins()).containsExactlyInAnyOrder("Foo", "Bar");

        // Install into a fresh location and confirm the round-trip.
        Path destPlugins = tempDir.resolve("dest/plugin");
        Path destLang = tempDir.resolve("dest/lang");
        ModpackInstaller.install(zip, destPlugins, destLang);

        assertThat(destPlugins.resolve("Foo.jar")).hasContent("foo");
        assertThat(destPlugins.resolve("Bar.jar")).hasContent("bar");
        assertThat(destPlugins.resolve("notes.txt")).doesNotExist();
        assertThat(destLang.resolve("zh_cn.lang")).hasContent("k=v");
    }

    // ---- helpers ---------------------------------------------------------

    private static java.io.InputStream stream(String content) {
        return new java.io.ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    /** Writes a zip with the given entry name -> text content mapping. */
    private static void writeZip(Path zip, Map<String, String> entries) throws Exception {
        Files.createDirectories(zip.getParent());
        // Preserve insertion order for deterministic archives.
        Map<String, String> ordered = new LinkedHashMap<>(entries);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (Map.Entry<String, String> e : ordered.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
    }
}
