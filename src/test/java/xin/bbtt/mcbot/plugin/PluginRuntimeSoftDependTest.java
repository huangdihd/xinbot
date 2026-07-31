/*
 * Copyright (C) 2026 huangdihd
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

package xin.bbtt.mcbot.plugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xin.bbtt.mcbot.BotTestState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression test for runtime plugin loading (e.g. the {@code pm load} / {@code pm reload}
 * commands, which call {@link PluginManager#loadPlugin(File)} one JAR at a time).
 *
 * <p>The startup batch loader wires every plugin's classloader to the loaders of its
 * (soft)dependencies, but the single-file runtime path used to skip that wiring entirely,
 * so a plugin loaded at runtime could not see its (soft)dependency's classes. This test
 * pins down that the runtime path now builds the same classloader chain.
 */
class PluginRuntimeSoftDependTest {

    private PluginManager pluginManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        DummyPlugin.reset();
        pluginManager = new PluginManager();

        // Ensure no session/running state leaks in from other tests so loading does not
        // try to auto-enable the plugins (we only care about the classloader wiring here).
        BotTestState.clearSession();
        BotTestState.setRunning(false);
    }

    private File createPluginJar(String fileName, String yml) throws Exception {
        File file = tempDir.resolve(fileName).toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(file))) {
            jos.putNextEntry(new JarEntry("plugin.yml"));
            jos.write(yml.getBytes());
            jos.closeEntry();

            String className = DummyPlugin.class.getName().replace('.', '/') + ".class";
            jos.putNextEntry(new JarEntry(className));
            try (InputStream is = DummyPlugin.class.getClassLoader().getResourceAsStream(className)) {
                assertThat(is).isNotNull();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    jos.write(buffer, 0, len);
                }
            }
            jos.closeEntry();
        }
        return file;
    }

    @Test
    void runtimeLoadWiresPresentSoftDependency() throws Exception {
        File libJar = createPluginJar("lib-plugin.jar",
            """
                name: LibPlugin
                main: xin.bbtt.mcbot.plugin.DummyPlugin
                version: 1.0.0
                """);
        File appJar = createPluginJar("app-plugin.jar",
            """
                name: AppPlugin
                main: xin.bbtt.mcbot.plugin.DummyPlugin
                version: 1.0.0
                softdepend: [LibPlugin]
                """);

        // Load the soft dependency first, then load the consumer at runtime.
        pluginManager.loadPlugin(libJar);
        pluginManager.loadPlugin(appJar);

        PluginClassLoader libLoader = pluginManager.getPluginLoader("LibPlugin");
        PluginClassLoader appLoader = pluginManager.getPluginLoader("AppPlugin");
        assertThat(libLoader).isNotNull();
        assertThat(appLoader).isNotNull();

        // The present soft dependency must become part of the consumer's classloader chain
        // (the single soft dep is wired in as the parent) and be recorded as an effective
        // dependency so unload ordering still works.
        assertThat(appLoader.getParent()).isSameAs(libLoader);
        assertThat(pluginManager.getPluginDependencies().get("AppPlugin")).contains("LibPlugin");
    }

    @Test
    void runtimeLoadIgnoresAbsentSoftDependency() throws Exception {
        File appJar = createPluginJar("app-plugin.jar",
            """
                name: AppPlugin
                main: xin.bbtt.mcbot.plugin.DummyPlugin
                version: 1.0.0
                softdepend: [MissingPlugin]
                """);

        // A soft dependency that is not present must not block the load and must not be
        // recorded as an effective dependency.
        pluginManager.loadPlugin(appJar);

        assertThat(pluginManager.getPlugin("AppPlugin")).isNotNull();
        assertThat(pluginManager.getPluginDependencies().get("AppPlugin")).doesNotContain("MissingPlugin");
    }

    @Test
    void runtimeLoadRejectsMissingHardDependency() throws Exception {
        File appJar = createPluginJar("app-plugin.jar",
            """
                name: AppPlugin
                main: xin.bbtt.mcbot.plugin.DummyPlugin
                version: 1.0.0
                depend: [MissingPlugin]
                """);

        // The startup batch loader refuses plugins with missing hard dependencies,
        // so the runtime path must refuse them too instead of silently loading
        // without the dependency wired in.
        assertThatThrownBy(() -> pluginManager.loadPlugin(appJar))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(pluginManager.getPlugin("AppPlugin")).isNull();
        assertThat(pluginManager.getPluginLoader("AppPlugin")).isNull();
    }
}
