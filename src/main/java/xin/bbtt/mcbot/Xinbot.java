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

package xin.bbtt.mcbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.jLine.CLI;
import xin.bbtt.mcbot.auth.AccountLoader;
import xin.bbtt.mcbot.config.BotConfig;
import xin.bbtt.mcbot.config.BotConfigData;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;


public class Xinbot {
    private static final Logger log = LoggerFactory.getLogger(Xinbot.class.getSimpleName());

    public static final String version = Xinbot.class.getPackage().getImplementationVersion();
    public static final String license = """
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

    private static boolean isDefaultConfig(BotConfigData configData) {
        if (configData == null) return true;
        BotConfigData.Account account = configData.getAccount();
        if (account == null) return true;
        String name = account.getName();
        return name == null || name.isEmpty() || "[Bot name]".equals(name);
    }

    private static void interactiveConfigSetup(BotConfig config) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        log.info(LangManager.get("xinbot.config.interactive.welcome"));
        log.info(LangManager.get("xinbot.config.interactive.offline.prompt"));

        String playerName = reader.readLine();
        BotConfigData.Account account = config.getConfigData().getAccount();
        if (account == null) {
            account = new BotConfigData.Account();
            config.getConfigData().setAccount(account);
        }

        if (playerName == null || playerName.trim().isEmpty()) {
            account.setOnlineMode(true);
            log.info(LangManager.get("xinbot.config.interactive.online.selected"));
            // Perform online account login immediately
            config.getConfigData().setAccount(AccountLoader.init(account));
            config.saveToFile();
            log.info(LangManager.get("xinbot.config.interactive.saved"));
        } else {
            account.setOnlineMode(false);
            account.setName(playerName.trim());
            log.info(LangManager.get("xinbot.config.interactive.offline.selected", playerName.trim()));

            log.info(LangManager.get("xinbot.config.interactive.password.prompt"));
            String password = reader.readLine();
            if (password != null) {
                account.setPassword(password.trim());
            }

            config.saveToFile();
            log.info(LangManager.get("xinbot.config.interactive.saved"));
        }

        log.info(LangManager.get("xinbot.config.interactive.other.settings"));
    }


    public static void main(String[] args){
        BotConfig config = null;

        // Handle arguments
        if (args.length > 1) {
            log.error(LangManager.get("xinbot.args.invalid"));
            return;
        }

        // If didn't specify a configuration file path then use default path
        if (args.length == 0) {
            args = new String[] { defaultConfigPath };
        }

        // The version and The license sub command
        if (args[0].equals("--version") || args[0].equals("-v")) {
            log.info(LangManager.get("xinbot.version", version));
            return;
        }
        if (args[0].equals("--license") || args[0].equals("-l")) {
            Arrays.stream(license.split("\n")).forEach(log::info);
            return;
        }

        // Init xinbot language
        LangManager.init();
        LangManager.initLang(Xinbot.class.getClassLoader());
        LangManager.loadExternal();

        // Initialize JLine first to ensure proper console encoding
        CLI.init();

        // Load the configuration file
        configPath = args[0];
        // Check if config file exists, if not copy from resources
        Path configFilePath = Paths.get(configPath);
        boolean isNewConfig = false;
        if (!Files.exists(configFilePath)) {
            log.info(LangManager.get("xinbot.config.loading", configPath));
            copyDefaultConfig(configPath);
            isNewConfig = true;
        }
        log.info(LangManager.get("xinbot.config.loading", configPath));
        try {
            config = new BotConfig(configPath);
        }
        catch (Exception e) {
            log.error(LangManager.get("xinbot.config.error", configPath), e);
            System.exit(1);
        }

        // Check if config is still default and prompt user for setup
        if (isNewConfig || isDefaultConfig(config.getConfigData())) {
            try {
                interactiveConfigSetup(config);
            } catch (Exception e) {
                log.error(LangManager.get("xinbot.config.interactive.error"), e);
                System.exit(1);
            }
        }

        // Initialize minecraft language
        if (config.getConfigData().isEnableTranslation()) LangManager.loadMinecraft();

        log.info(LangManager.get("xinbot.version", version));

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
