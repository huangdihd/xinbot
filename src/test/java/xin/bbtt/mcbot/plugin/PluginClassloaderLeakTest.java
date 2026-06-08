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

    private File createJar(String ymlContent) throws Exception {
        File file = tempDir.resolve("bad-plugin.jar").toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(file))) {
            jos.putNextEntry(new JarEntry("plugin.yml"));
            jos.write(ymlContent.getBytes());
            jos.closeEntry();
        }
        return file;
    }

    @Test
    void failedLoadDoesNotLeakClassloader() throws Exception {
        PluginManager pluginManager = new PluginManager();
        // Valid plugin.yml, but 'main' points to a class that does not exist,
        // so Class.forName fails after the classloader has been registered.
        File jar = createJar("name: LeakTest\nmain: xin.bbtt.mcbot.plugin.NoSuchPluginClass\nversion: 1.0.0\n");

        assertThatThrownBy(() -> pluginManager.loadPlugin(jar)).isInstanceOf(Exception.class);

        // The classloader and dependency entry must not linger after a failed load.
        assertThat(pluginManager.getPluginLoader("LeakTest")).isNull();
        assertThat(pluginManager.getPluginDependencies().get("LeakTest")).isNull();
    }
}
