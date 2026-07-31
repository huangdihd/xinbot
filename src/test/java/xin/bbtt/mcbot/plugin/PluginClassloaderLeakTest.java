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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that a plugin load which fails during class instantiation does not
 * leave the plugin's classloader registered (which would leak the file handle).
 */
class PluginClassloaderLeakTest {

    @TempDir
    Path tempDir;

    private File createJar() throws Exception {
        File file = tempDir.resolve("bad-plugin.jar").toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(file))) {
            jos.putNextEntry(new JarEntry("plugin.yml"));
            jos.write("name: LeakTest\nmain: xin.bbtt.mcbot.plugin.NoSuchPluginClass\nversion: 1.0.0\n".getBytes());
            jos.closeEntry();
        }
        return file;
    }

    @Test
    void failedLoadDoesNotLeakClassloader() throws Exception {
        PluginManager pluginManager = new PluginManager();
        // Valid plugin.yml, but 'main' points to a class that does not exist,
        // so Class.forName fails after the classloader has been registered.
        File jar = createJar();

        assertThatThrownBy(() -> pluginManager.loadPlugin(jar)).isInstanceOf(Exception.class);

        // The classloader and dependency entry must not linger after a failed load.
        assertThat(pluginManager.getPluginLoader("LeakTest")).isNull();
        assertThat(pluginManager.getPluginDependencies().get("LeakTest")).isNull();
    }
}
