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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import xin.bbtt.mcbot.config.BotConfig;
import xin.bbtt.mcbot.config.BotConfigData;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for bundling a sanitized {@code config.conf} in a modpack and filling
 * it back in at install time.
 */
class ModpackConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void sanitizeClearsSecretsButKeepsSettings() {
        BotConfigData data = sampleConfig();
        ModpackConfig.sanitize(data);

        // Secrets cleared
        assertThat(data.getOwner()).isEmpty();
        assertThat(data.getAccount().getName()).isEmpty();
        assertThat(data.getAccount().getPassword()).isEmpty();
        assertThat(data.getAccount().getFullSession()).isNull();
        assertThat(data.getProxy().getInfo().getUsername()).isEmpty();
        assertThat(data.getProxy().getInfo().getPassword()).isEmpty();

        // Non-sensitive settings kept
        assertThat(data.getAccount().isOnlineMode()).isFalse();
        assertThat(data.getReconnectTimeout()).isEqualTo(5000);
        assertThat(data.getPlugin().getDirectory()).isEqualTo("plugin");
        assertThat(data.getProxy().isEnable()).isTrue();
    }

    @Test
    void exportWithConfigThenInstallRoundTrip() throws Exception {
        BotConfigData data = sampleConfig();
        ModpackConfig.sanitize(data);
        String hocon = BotConfig.render(data);

        Path zip = tempDir.resolve("pack.zip");
        ModpackExporter.export(zip, tempDir.resolve("noplugins"), tempDir.resolve("nolang"),
                new ModpackManifest("Pack", "1.0.0", null, null, null, null), hocon);

        assertThat(ModpackConfig.isPresentIn(zip)).isTrue();

        Path destConfig = tempDir.resolve("dest/config.conf");
        ModpackInstaller.InstallResult result = ModpackInstaller.install(
                zip, tempDir.resolve("dest/plugin"), tempDir.resolve("dest/lang"), destConfig);

        assertThat(result.hasConfig()).isTrue();
        assertThat(result.configFile()).exists();

        BotConfigData reloaded = new BotConfig(destConfig.toString()).getConfigData();
        assertThat(reloaded.getAccount().getName()).isEmpty();
        assertThat(reloaded.getAccount().getPassword()).isEmpty();
        assertThat(reloaded.getPlugin().getDirectory()).isEqualTo("plugin");
    }

    @Test
    void installDoesNotOverwriteExistingConfig() throws Exception {
        BotConfigData data = sampleConfig();
        ModpackConfig.sanitize(data);
        Path zip = tempDir.resolve("pack.zip");
        ModpackExporter.export(zip, tempDir.resolve("noplugins"), tempDir.resolve("nolang"),
                new ModpackManifest("Pack", "1.0.0", null, null, null, null), BotConfig.render(data));

        Path existing = tempDir.resolve("config.conf");
        Files.writeString(existing, "{ owner = \"keep-me\" }", StandardCharsets.UTF_8);

        ModpackInstaller.InstallResult result = ModpackInstaller.install(
                zip, tempDir.resolve("plugin"), tempDir.resolve("lang"), existing);

        assertThat(result.hasConfig()).isFalse();
        assertThat(existing).content(StandardCharsets.UTF_8).contains("keep-me");
    }

    @Test
    void plainInstallDoesNotExtractConfig() throws Exception {
        BotConfigData data = sampleConfig();
        ModpackConfig.sanitize(data);
        Path zip = tempDir.resolve("pack.zip");
        ModpackExporter.export(zip, tempDir.resolve("noplugins"), tempDir.resolve("nolang"),
                new ModpackManifest("Pack", "1.0.0", null, null, null, null), BotConfig.render(data));

        // The 3-arg overload must never touch config, even if the modpack bundles one.
        ModpackInstaller.InstallResult result = ModpackInstaller.install(
                zip, tempDir.resolve("plugin"), tempDir.resolve("lang"));

        assertThat(result.hasConfig()).isFalse();
    }

    @Test
    void setupFillsBlankedFields() throws Exception {
        BotConfigData data = sampleConfig();
        ModpackConfig.sanitize(data);
        Path configFile = tempDir.resolve("config.conf");
        Files.writeString(configFile, BotConfig.render(data), StandardCharsets.UTF_8);

        Map<String, String> answers = Map.of(
                "xinbot.modpack.setup.owner", "Steve",
                "xinbot.modpack.setup.account_name", "MyBot",
                "xinbot.modpack.setup.account_password", "pw",
                "xinbot.modpack.setup.proxy_username", "pu",
                "xinbot.modpack.setup.proxy_password", "pp");
        ModpackConfigSetup.Input input = (key, secret) -> answers.getOrDefault(key, "");

        ModpackConfigSetup.complete(configFile, input);

        BotConfigData filled = new BotConfig(configFile.toString()).getConfigData();
        assertThat(filled.getOwner()).isEqualTo("Steve");
        assertThat(filled.getAccount().getName()).isEqualTo("MyBot");
        assertThat(filled.getAccount().getPassword()).isEqualTo("pw");
        assertThat(filled.getProxy().getInfo().getUsername()).isEqualTo("pu");
        assertThat(filled.getProxy().getInfo().getPassword()).isEqualTo("pp");
    }

    /** A config with both account and proxy credentials populated. */
    private static BotConfigData sampleConfig() {
        BotConfigData data = new BotConfigData();
        data.setOwner("realOwner");
        data.setEnableTranslation(true);
        data.setReconnectTimeout(5000);
        data.setReconnectDelay(3000);

        BotConfigData.Plugin plugin = new BotConfigData.Plugin();
        plugin.setDirectory("plugin");
        data.setPlugin(plugin);

        BotConfigData.Account account = new BotConfigData.Account();
        account.setName("SecretBot");
        account.setPassword("hunter2");
        account.setOnlineMode(false);
        data.setAccount(account);

        BotConfigData.Proxy proxy = new BotConfigData.Proxy();
        proxy.setEnable(true);
        BotConfigData.Proxy.ProxyInfo info = new BotConfigData.Proxy.ProxyInfo();
        info.setUsername("proxyUser");
        info.setPassword("proxyPass");
        proxy.setInfo(info);
        data.setProxy(proxy);

        return data;
    }
}
