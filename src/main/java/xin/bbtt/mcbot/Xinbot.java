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
            log.error("插件目录不是一个目录！");
            return false;
        }

        log.info("插件目录不存在，正在尝试创建。");

        if (!pluginDir.mkdir()) {
            log.error("无法创建插件目录: {}", pluginDir.isDirectory());
            return false;
        }

        log.info("已创建插件目录: {}", pluginDir.isDirectory());
        return true;
    }

    // Copy the default config file to the specified path
    private static void copyDefaultConfig(String configPath) {
        try (InputStream is = Xinbot.class.getClassLoader().getResourceAsStream("config.conf")) {
            if (is == null) {
                log.error("在资源中找不到默认配置文件！");
                return;
            }

            Path configFilePath = Paths.get(configPath);
            if (configFilePath.getParent() != null) {
                Files.createDirectories(configFilePath.getParent());
            }
            Files.copy(is, configFilePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("默认配置文件已复制到: {}", configPath);
        } catch (IOException e) {
            log.error("无法复制默认配置文件: {}", e.getMessage(), e);
        }
    }


    public static void main(String[] args){
        BotConfig config = null;

        // Handle arguments
        if (args.length > 1) {
            log.error("只能使用一个参数运行此程序！");
            return;
        }

        // If didn't specify a configuration file path then use default path
        if (args.length == 0) {
            args = new String[] { defaultConfigPath };
        }

        // The version and The license sub command
        if (args[0].equals("--version") || args[0].equals("-v")) {
            log.info("版本: {}", version);
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
            log.info("未找到配置文件，正在复制默认配置...");
            copyDefaultConfig(configPath);
        }
        log.info("正在加载配置文件: {}", configPath);
        try {
            config = new BotConfig(configPath);
        }
        catch (Exception e) {
            log.error("无法加载配置文件: {}", configPath, e);
            System.exit(1);
        }

        // Initialize JLine
        if (config.getConfigData().getAdvances().isEnableJLine()) CLI.init();

        // Initialize the language manager
        if (config.getConfigData().getAdvances().isEnableTranslation()) LangManager.Init();

        log.info("版本: {}", version);

        // Initialize the plugin directory
        File pluginDir = new File(config.getConfigData().getPlugin().getDirectory());
        if (!initializePluginDirectory(pluginDir)) System.exit(1);

        // Initialize the account
        try {
            config.getConfigData().setAccount(AccountLoader.init(config.getConfigData().getAccount()));
        }
        catch (Exception e) {
            log.error("无法加载您的账户。", e);
            System.exit(1);
        }

        // Save changes back to the configuration file
        try {
            config.saveToFile();
        }
        catch (Exception e) {
            log.error("无法保存配置文件。", e);
        }

        // Initialize the bot
        Bot.Instance.init(config);

        // Start the bot
        Bot.Instance.start();

        // After the bot stopped
        log.info("机器人已停止。");
        log.info("再见！");
        System.exit(0);
    }
}
