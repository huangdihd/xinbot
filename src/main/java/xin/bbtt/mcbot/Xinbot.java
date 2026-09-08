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

package xin.bbtt.mcbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.jLine.CLI;
import xin.bbtt.mcbot.auth.AccountLoader;
import xin.bbtt.mcbot.cli.Cli;
import xin.bbtt.mcbot.config.BotConfig;
import xin.bbtt.mcbot.versions.UpdateChecker;
import xin.bbtt.mcbot.versions.Version;
import xin.bbtt.mcbot.versions.VersionInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


public class Xinbot {
    private static final Logger log = LoggerFactory.getLogger(Xinbot.class.getSimpleName());

    public static final Version VERSION = Version.from(Optional.ofNullable(Xinbot.class.getPackage().getImplementationVersion()).orElse("0.0.0-DEV"));
    public static final String LICENSE = """
            Copyright (C) 2024-2026 huangdihd
            This program is free software: you can redistribute it and/or modify
            it under the terms of the GNU General Public License as published by
            the Free Software Foundation, either version 3 of the License, or
            (at your option) any later version.
            This program is distributed in the hope that it will be useful,
            but WITHOUT ANY WARRANTY; without even the implied warranty of
            MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
            GNU General Public License for more details.
            You should have received a copy of the GNU General Public License
            along with this program.  If not, see <https://www.gnu.org/licenses/>.""";

    public static String configPath;
    public static final String defaultConfigPath = "config.conf";

    private static boolean initializePluginDirectory(File pluginDir) {
        if (pluginDir.isDirectory())
            return true;

        if (pluginDir.exists()) {
            log.error(LangManager.get("xinbot.plugin.dir.not.dir"));
            return false;
        }

        log.info(LangManager.get("xinbot.plugin.dir.not.exists"));

        if (!pluginDir.mkdir()) {
            log.error(LangManager.get("xinbot.plugin.dir.create.failed", pluginDir.isDirectory()));
            return false;
        }

        log.info(LangManager.get("xinbot.plugin.dir.created", pluginDir.isDirectory()));
        return true;
    }

    // Copy the default config file to the specified path
    private static void copyDefaultConfig(String configPath) {
        try (InputStream is = Xinbot.class.getClassLoader().getResourceAsStream("config.conf")) {
            if (is == null) {
                log.error(LangManager.get("xinbot.config.default.not.found"));
                return;
            }

            Path configFilePath = Paths.get(configPath);
            if (configFilePath.getParent() != null) {
                Files.createDirectories(configFilePath.getParent());
            }
            Files.copy(is, configFilePath, StandardCopyOption.REPLACE_EXISTING);
            log.info(LangManager.get("xinbot.config.default.copied", configPath));
        } catch (IOException e) {
            log.error(LangManager.get("xinbot.config.default.copy.failed", e.getMessage()), e);
        }
    }

    /**
     * First-run guidance: asks once whether to opt in to telemetry (helps improve
     * Xinbot) and writes the answer into the freshly copied config file so the
     * question never comes up again. Anything but an explicit "y"/"yes" keeps
     * telemetry off; a failed write only warns and never blocks startup.
     */
    private static void promptTelemetryOptIn(Path configPath) {
        if (!readYesNo(LangManager.get("xinbot.telemetry.optin.prompt"))) {
            log.info(LangManager.get("xinbot.telemetry.optin.declined"));
            return;
        }
        try {
            if (BotConfig.setTelemetryEnabled(configPath, true)) {
                log.info(LangManager.get("xinbot.telemetry.optin.enabled", configPath));
            } else {
                log.warn(LangManager.get("xinbot.telemetry.optin.apply_failed", configPath));
            }
        } catch (IOException e) {
            log.warn(LangManager.get("xinbot.telemetry.optin.apply_failed", configPath), e);
        }
    }

    /** Reads one line from the interactive console; falls back to plain stdin when
     * no JLine reader exists. EOF, Ctrl+C/Ctrl+D or any read failure counts as "no"
     * so telemetry can never be enabled by accident. */
    private static boolean readYesNo(String prompt) {
        String answer;
        if (CLI.getLineReader() != null) {
            try {
                answer = CLI.getLineReader().readLine(prompt);
            } catch (Exception e) {
                answer = null; // interrupted or EOF: keep telemetry off
            }
        } else {
            System.out.print(prompt);
            System.out.flush();
            try {
                answer = new BufferedReader(new InputStreamReader(
                        System.in, java.nio.charset.StandardCharsets.UTF_8)).readLine();
            } catch (IOException e) {
                answer = null;
            }
        }
        if (answer == null) {
            return false;
        }
        String trimmed = answer.trim();
        return trimmed.equalsIgnoreCase("y") || trimmed.equalsIgnoreCase("yes");
    }

    private static void checkForUpdates() {
        log.info(LangManager.get("xinbot.update.checking"));
        VersionInfo latestVersionInfo = UpdateChecker.fetchLatestVersionInfo();
        if (latestVersionInfo.latestVersion().isNewerThan(VERSION)) {
            log.info(LangManager.get("xinbot.update.newversion", latestVersionInfo.latestVersion(), latestVersionInfo.releaseUrl()));
        }
    }


    public static void main(String[] args){
        BotConfig config = null;

        // Init xinbot language early so all CLI output is localized
        LangManager.init();

        Cli cli = Cli.create();

        // Interactive mode only: make the output layer ready before anything can
        // log. Until the LineReader exists, JLineConsoleAppender prints UTF-8 bytes
        // raw to System.out, which garbles CJK text on Windows GBK consoles.
        // Sub-commands (e.g. --version, CI runs) never build a terminal eagerly and
        // keep logging on plain stdout instead.
        if (!cli.isSubcommand(args)) {
            CLI.init();
        }

        // Loading translations can emit info/error logs; the output layer above is
        // already safe by this point in interactive mode.
        LangManager.initLang(Xinbot.class.getClassLoader());
        LangManager.loadExternal();

        // Handle sub-commands (anything starting with '-'), e.g. --version, --install
        if (cli.isSubcommand(args)) {
            System.exit(cli.dispatch(args) ? 0 : 1);
        }

        if (args.length > 1) {
            log.error(LangManager.get("xinbot.args.invalid"));
            return;
        }

        // Load the configuration file (use the default path when none is given)
        configPath = args.length == 0 ? defaultConfigPath : args[0];
        // Check if config file exists, if not copy from resources
        Path configFilePath = Paths.get(configPath);
        if (!Files.exists(configFilePath)) {
            log.info(LangManager.get("xinbot.config.loading", configPath));
            copyDefaultConfig(configPath);
            // First run: ask once whether to enable telemetry, and persist the
            // answer into the config that was just created (see promptTelemetryOptIn).
            promptTelemetryOptIn(configFilePath);
            log.info(LangManager.get("xinbot.config.modify.prompt", configPath));
            System.exit(1);
        }
        log.info(LangManager.get("xinbot.config.loading", configPath));
        try {
            config = new BotConfig(configPath);
        }
        catch (Exception e) {
            log.error(LangManager.get("xinbot.config.error", configPath), e);
            System.exit(1);
        }

        // Initialize minecraft language
        if (config.getConfigData().isEnableTranslation()) LangManager.loadMinecraft();

        log.info(LangManager.get("xinbot.version", VERSION));

        // Initialize the plugin directory
        File pluginDir = new File(config.getConfigData().getPlugin().getDirectory());
        if (!initializePluginDirectory(pluginDir)) System.exit(1);

        // Initialize the account
        try {
            config.getConfigData().setAccount(AccountLoader.init(config.getConfigData().getAccount()));
        }
        catch (Exception e) {
            log.error(LangManager.get("xinbot.account.load.failed"), e);
            System.exit(1);
        }

        // Save changes back to the configuration file
        try {
            config.saveToFile();
        }
        catch (Exception e) {
            log.error(LangManager.get("xinbot.config.save.failed"), e);
        }

        // Check for updates
        try {
            if (config.getConfigData().getCheckForUpdates()) checkForUpdates();
        }
        catch (Exception e) {
            log.error(LangManager.get("xinbot.update.checking.failed"), e);
        }

        // Initialize the bot
        Bot.INSTANCE.init(config);

        // Start the bot
        Bot.INSTANCE.start();

        // After the bot stopped
        log.info(LangManager.get("xinbot.bot.stopped"));
        log.info(LangManager.get("xinbot.bot.bye"));
        System.exit(0);
    }
}
