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

import org.geysermc.mcprotocollib.network.ClientSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xin.bbtt.mcbot.Bot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for the full plugin lifecycle using actual JAR files.
 * Tests: Load, Unload, Reload, Enable, Disable, and Re-enable.
 */
public class PluginFileLifecycleTest {

    private PluginManager pluginManager;
    private File jarFile;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // Reset static counters in the dummy plugin
        DummyPlugin.reset();
        pluginManager = new PluginManager();
        Bot bot = Bot.INSTANCE;

        // Mock a connected session to allow automatic enabling upon loading
        ClientSession mockSession = mock(ClientSession.class);
        when(mockSession.isConnected()).thenReturn(true);
        
        Field sessionField = Bot.class.getDeclaredField("session");
        sessionField.setAccessible(true);
        sessionField.set(bot, mockSession);
        
        Field runningField = Bot.class.getDeclaredField("running");
        runningField.setAccessible(true);
        runningField.set(bot, true);

        // Create a temporary plugin JAR file for testing
        jarFile = tempDir.resolve("dummy-plugin.jar").toFile();
        createPluginJar(jarFile);
    }

    /**
     * Helper to create a valid plugin JAR with plugin.yml and DummyPlugin class.
     */
    private void createPluginJar(File file) throws Exception {
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(file))) {
            // Write plugin.yml
            jos.putNextEntry(new JarEntry("plugin.yml"));
            String yml = "name: DummyPlugin\nmain: xin.bbtt.mcbot.plugin.DummyPlugin\nversion: 1.0.0\n";
            jos.write(yml.getBytes());
            jos.closeEntry();

            // Write the class file
            String className = DummyPlugin.class.getName().replace('.', '/') + ".class";
            jos.putNextEntry(new JarEntry(className));
            try (InputStream is = DummyPlugin.class.getClassLoader().getResourceAsStream(className)) {
                byte[] buffer = new byte[1024];
                int len;
                while (true) {
                    Assertions.assertNotNull(is);
                    if (!((len = is.read(buffer)) > 0)) break;
                    jos.write(buffer, 0, len);
                }
            }
            jos.closeEntry();
        }
    }

    @Test
    void testFullLifecycle() throws Exception {
        // 1. Load: The plugin is loaded and automatically enabled because a session exists
        pluginManager.loadPlugin(jarFile);
        assertThat(DummyPlugin.loadCount).isEqualTo(1);
        assertThat(DummyPlugin.enableCount).isEqualTo(1);
        
        RegisteredPlugin rp = pluginManager.getPlugin("DummyPlugin");
        assertThat(rp).isNotNull();
        assertThat(pluginManager.isPluginEnabled("DummyPlugin")).isTrue();

        // 2. Enable (Manual): Should do nothing as it's already enabled
        pluginManager.enablePlugin(rp);
        assertThat(DummyPlugin.enableCount).isEqualTo(1); 

        // 3. Disable: Should trigger onDisable
        pluginManager.disablePlugin(rp);
        assertThat(DummyPlugin.disableCount).isEqualTo(1);
        assertThat(pluginManager.isPluginEnabled("DummyPlugin")).isFalse();

        // 4. Re-enable: Should trigger onEnable again
        pluginManager.enablePlugin(rp);
        assertThat(DummyPlugin.enableCount).isEqualTo(2);
        assertThat(pluginManager.isPluginEnabled("DummyPlugin")).isTrue();

        // 5. Unload: Should automatically disable and then unload
        pluginManager.unloadPlugin(rp);
        assertThat(DummyPlugin.disableCount).isEqualTo(2); // Automatically disabled
        assertThat(DummyPlugin.unloadCount).isEqualTo(1);
        assertThat(pluginManager.getPlugin("DummyPlugin")).isNull();

        // 6. Reload (Load again): Should trigger onLoad and automatic onEnable again
        pluginManager.loadPlugin(jarFile);
        assertThat(DummyPlugin.loadCount).isEqualTo(2);
        assertThat(DummyPlugin.enableCount).isEqualTo(3);
        assertThat(pluginManager.getPlugin("DummyPlugin")).isNotNull();
        assertThat(pluginManager.isPluginEnabled("DummyPlugin")).isTrue();
    }
}
