/*
 *   Copyright (C) 2024-2025 huangdihd
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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


public class Xinbot {
    private static final Logger log = LoggerFactory.getLogger(Xinbot.class.getSimpleName());

    public static final String version = Xinbot.class.getPackage().getImplementationVersion();
    public static final String license = """
            Copyright (C) 2024-2025 huangdihd
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
            log.error("Plugin directory is not a directory!");
            return false;
        }

        log.info("Plugin directory is not exists, trying to create it.");

        if (!pluginDir.mkdir()) {
            log.error("Failed to create plugins directory: {}", pluginDir.isDirectory());
            return false;
        }

        log.info("Created plugins directory: {}", pluginDir.isDirectory());
        return true;
    }

    // Copy the default config file to the specified path
    private static void copyDefaultConfig(String configPath) {
        try (InputStream is = Xinbot.class.getClassLoader().getResourceAsStream("config.conf")) {
            if (is == null) {
                log.error("Default config file not found in resources!");
                return;
            }

            Path configFilePath = Paths.get(configPath);
            if (configFilePath.getParent() != null) {
                Files.createDirectories(configFilePath.getParent());
            }
            Files.copy(is, configFilePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Default config file copied to: {}", configPath);
        } catch (IOException e) {
            log.error("Failed to copy default config file: {}", e.getMessage(), e);
        }
    }


    public static void main(String[] args){
        BotConfig config = null;

        // Handle arguments
        if (args.length > 1) {
            log.error("You can only run this program with one argument!");
            return;
        }

        // If didn't specify a configuration file path then use default path
        if (args.length == 0) {
            args = new String[] { defaultConfigPath };
        }

        // The version and The license sub command
        if (args[0].equals("--version") || args[0].equals("-v")) {
            log.info("version: {}", version);
            return;
        }
        if (args[0].equals("--license") || args[0].equals("-l")) {
            Arrays.stream(license.split("\n")).forEach(log::info);
            return;
        }

        // Load the configuration file
        configPath = args[0];
        // Check if config file exists, if not copy from resources
        Path configFilePath = Paths.get(configPath);
        if (!Files.exists(configFilePath)) {
            log.info("Config file not found, copying default config...");
            copyDefaultConfig(configPath);
            log.info("Please modify the config file: {}", configPath);
            System.exit(1);
        }
        log.info("Loading config file: {}", configPath);
        try {
            config = new BotConfig(configPath);
        }
        catch (Exception e) {
            log.error("Failed to load configuration file: {}", configPath, e);
            System.exit(1);
        }

        // Initialize JLine
        if (config.getConfigData().getAdvances().isEnableJLine()) CLI.init();

        // Initialize the language manager
        if (config.getConfigData().getAdvances().isEnableTranslation()) LangManager.Init();

        log.info("version: {}", version);

        // Initialize the plugin directory
        File pluginDir = new File(config.getConfigData().getPlugin().getDirectory());
        if (!initializePluginDirectory(pluginDir)) System.exit(1);

        // Initialize the account
        try {
            config.getConfigData().setAccount(AccountLoader.init(config.getConfigData().getAccount()));
        }
        catch (Exception e) {
            log.error("Failed to load your account.", e);
            System.exit(1);
        }

        // Save changes back to the configuration file
        try {
            config.saveToFile();
        }
        catch (Exception e) {
            log.error("Failed to save the configuration file.", e);
        }

        // Initialize the bot
        Bot.Instance.init(config);

        // Start the bot
        Bot.Instance.start();

        // After the bot stopped
        log.info("Bot stopped.");
        log.info("Bye!");
    }
}
