package xin.bbtt.mcbot.plugin;

import org.junit.jupiter.api.AfterEach;
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
        setBotField("session", null);
        setBotField("running", false);
    }

    @AfterEach
    void tearDown() throws Exception {
        setBotField("running", false);
    }

    private void setBotField(String name, Object value) throws Exception {
        Field field = Bot.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(Bot.INSTANCE, value);
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
                "name: LibPlugin\nmain: xin.bbtt.mcbot.plugin.DummyLibPlugin\nversion: 1.0.0\n",
                DummyLibPlugin.class);
        createPluginJar("meta-plugin.jar",
                "name: TestMeta\nmain: xin.bbtt.mcbot.plugin.DummyMetaPlugin\nversion: 1.0.0\n"
                        + "type: META_PLUGIN\ndepend: [LibPlugin]\n",
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
        setBotField("running", true);

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
