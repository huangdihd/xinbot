package xin.bbtt.mcbot.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Verifies plugin.yml hardening: blank name/main is rejected, and an invalid
 * plugin type falls back to PLUGIN instead of crashing the load.
 */
class PluginYmlValidationTest {

    @TempDir
    Path tempDir;

    /** Writes a jar containing the given plugin.yml, optionally with the DummyPlugin class. */
    private File jar(String yml, boolean withClass) throws Exception {
        File file = tempDir.resolve("p.jar").toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(file))) {
            jos.putNextEntry(new JarEntry("plugin.yml"));
            jos.write(yml.getBytes());
            jos.closeEntry();
            if (withClass) {
                String className = DummyPlugin.class.getName().replace('.', '/') + ".class";
                jos.putNextEntry(new JarEntry(className));
                try (InputStream is = DummyPlugin.class.getClassLoader().getResourceAsStream(className)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while (is != null && (len = is.read(buf)) > 0) jos.write(buf, 0, len);
                }
                jos.closeEntry();
            }
        }
        return file;
    }

    @Test
    void blankNameIsRejected() throws Exception {
        PluginManager pm = new PluginManager();
        File file = jar("name: \"\"\nmain: xin.bbtt.mcbot.plugin.DummyPlugin\nversion: 1.0.0\n", false);
        assertThatThrownBy(() -> pm.loadPlugin(file)).isInstanceOf(Exception.class);
    }

    @Test
    void invalidTypeFallsBackToPluginAndStillLoads() throws Exception {
        DummyPlugin.reset();
        PluginManager pm = new PluginManager();
        File file = jar("name: DummyPlugin\nmain: xin.bbtt.mcbot.plugin.DummyPlugin\nversion: 1.0.0\ntype: BOGUS\n", true);
        assertDoesNotThrow(() -> pm.loadPlugin(file));
        assertThat(pm.getPlugin("DummyPlugin")).isNotNull();
    }
}
