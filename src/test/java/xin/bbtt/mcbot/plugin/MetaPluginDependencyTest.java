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

import org.junit.jupiter.api.AfterEach;
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

/**
 * Tests that a MetaPlugin can depend on regular plugins, and that those
 * dependencies inherit the MetaPlugin's lifecycle restriction: they cannot be
 * unloaded during runtime, because that would tear down the MetaPlugin's
 * classloader chain.
 */
class MetaPluginDependencyTest {

    private PluginManager pluginManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        DummyMetaPlugin.reset();
        pluginManager = new PluginManager();
        BotTestState.clearSession();
        BotTestState.setRunning(false);
    }

    @AfterEach
    void tearDown() throws Exception {
        BotTestState.setRunning(false);
    }

    private void createPluginJar(String fileName, String yml, Class<?> mainClass) throws Exception {
        File file = tempDir.resolve(fileName).toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(file))) {
            jos.putNextEntry(new JarEntry("plugin.yml"));
            jos.write(yml.getBytes());
            jos.closeEntry();

            String className = mainClass.getName().replace('.', '/') + ".class";
            jos.putNextEntry(new JarEntry(className));
            try (InputStream is = mainClass.getClassLoader().getResourceAsStream(className)) {
                assertThat(is).isNotNull();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    jos.write(buffer, 0, len);
                }
            }
            jos.closeEntry();
        }
    }

    private void loadMetaWithDependency() throws Exception {
        createPluginJar("lib-plugin.jar",
            """
                name: LibPlugin
                main: xin.bbtt.mcbot.plugin.DummyLibPlugin
                version: 1.0.0
                """,
                DummyLibPlugin.class);
        createPluginJar("meta-plugin.jar",
            """
                name: TestMeta
                main: xin.bbtt.mcbot.plugin.DummyMetaPlugin
                version: 1.0.0
                type: META_PLUGIN
                depend: [LibPlugin]
                """,
                DummyMetaPlugin.class);
        pluginManager.loadPlugins(tempDir.toString());
    }

    @Test
    void metaPluginCanDependOnAnotherPlugin() throws Exception {
        loadMetaWithDependency();

        RegisteredPlugin meta = pluginManager.getPlugin("TestMeta");
        assertThat(meta).isInstanceOf(RegisteredMetaPlugin.class);
        assertThat(pluginManager.getPlugin("LibPlugin")).isNotNull();
        assertThat(pluginManager.getPluginDependencies().get("TestMeta")).contains("LibPlugin");

        // The dependency must be loaded first and be part of the meta plugin's
        // classloader chain.
        assertThat(DummyMetaPlugin.events.indexOf("lib-load"))
                .isLessThan(DummyMetaPlugin.events.indexOf("meta-load"));
        assertThat(pluginManager.getPluginLoader("TestMeta").getParent())
                .isSameAs(pluginManager.getPluginLoader("LibPlugin"));
    }

    @Test
    void enableAllEnablesMetaDependenciesFirst() throws Exception {
        loadMetaWithDependency();

        pluginManager.enableAll();

        assertThat(pluginManager.isPluginEnabled("LibPlugin")).isTrue();
        assertThat(pluginManager.isPluginEnabled("TestMeta")).isTrue();
        assertThat(DummyMetaPlugin.events.indexOf("lib-enable"))
                .isLessThan(DummyMetaPlugin.events.indexOf("meta-enable"));
    }

    @Test
    void metaPluginDependencyCannotBeUnloadedDuringRuntime() throws Exception {
        loadMetaWithDependency();
        BotTestState.setRunning(true);

        pluginManager.unloadPlugin(pluginManager.getPlugin("LibPlugin"));

        assertThat(pluginManager.isPluginLoaded("LibPlugin")).isTrue();
        assertThat(pluginManager.isPluginLoaded("TestMeta")).isTrue();
        assertThat(DummyMetaPlugin.events).doesNotContain("lib-unload", "meta-unload");
    }

    @Test
    void metaPluginDependencyUnloadsWhenNotRunning() throws Exception {
        loadMetaWithDependency();

        pluginManager.unloadPlugin(pluginManager.getPlugin("LibPlugin"));

        // The meta plugin depends on the lib, so it is unloaded first as a dependent.
        assertThat(pluginManager.isPluginLoaded("LibPlugin")).isFalse();
        assertThat(pluginManager.isPluginLoaded("TestMeta")).isFalse();
        assertThat(DummyMetaPlugin.events.indexOf("meta-unload"))
                .isLessThan(DummyMetaPlugin.events.indexOf("lib-unload"));
    }
}
