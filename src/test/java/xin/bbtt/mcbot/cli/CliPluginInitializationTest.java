/*
 * Copyright (C) 2026 huangdihd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package xin.bbtt.mcbot.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.LangManager;
import xin.bbtt.mcbot.plugin.DummyPlugin;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CliPluginInitializationTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        Field running = Bot.class.getDeclaredField("running");
        running.setAccessible(true);
        running.set(Bot.INSTANCE, false);

        Field session = Bot.class.getDeclaredField("session");
        session.setAccessible(true);
        session.set(Bot.INSTANCE, null);

        Bot.INSTANCE.getPluginManager().unloadPlugins();
        DummyPlugin.reset();
        LangManager.clear();
        LangManager.init();
        LangManager.initLang(Cli.class.getClassLoader());
    }

    @Test
    void loadsAndUnloadsPluginsWithoutEnablingOrStartingTheBot() throws Exception {
        Path pluginDirectory = Files.createDirectories(tempDir.resolve("plugin"));
        createPluginJar(pluginDirectory.resolve("dummy-plugin.jar"));
        Path config = tempDir.resolve("server.conf");
        Files.writeString(config, "plugin.directory = \"" + pluginDirectory + "\"\n");

        boolean success = Cli.create().dispatch(new String[]{"--init-plugins", config.toString()});

        assertThat(success).isTrue();
        assertThat(DummyPlugin.loadCount).isEqualTo(1);
        assertThat(DummyPlugin.unloadCount).isEqualTo(1);
        assertThat(DummyPlugin.enableCount).isZero();
        assertThat(DummyPlugin.disableCount).isZero();
        assertThat(Bot.INSTANCE.isRunning()).isFalse();
        assertThat(Bot.INSTANCE.getSession()).isNull();
        assertThat(Bot.INSTANCE.getPluginManager().getPlugins()).isEmpty();
    }

    @Test
    void acceptsZeroOrOneConfigArgumentsOnly() {
        Subcommand command = new Subcommand(List.of("--init-plugins"), 0, 1, "usage", args -> true);

        assertThat(command.acceptsArgumentCount(0)).isTrue();
        assertThat(command.acceptsArgumentCount(1)).isTrue();
        assertThat(command.acceptsArgumentCount(2)).isFalse();
        assertThatThrownBy(() -> new Subcommand(List.of("--bad"), 2, 1, "usage", args -> true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void createPluginJar(Path jarPath) throws Exception {
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jar.putNextEntry(new JarEntry("plugin.yml"));
            jar.write(("name: DummyPlugin\n"
                    + "main: xin.bbtt.mcbot.plugin.DummyPlugin\n"
                    + "version: 1.0.0\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            jar.closeEntry();

            String classResource = DummyPlugin.class.getName().replace('.', '/') + ".class";
            jar.putNextEntry(new JarEntry(classResource));
            try (InputStream input = DummyPlugin.class.getClassLoader().getResourceAsStream(classResource)) {
                if (input == null) throw new IllegalStateException("DummyPlugin class resource not found");
                input.transferTo(jar);
            }
            jar.closeEntry();
        }
    }
}
