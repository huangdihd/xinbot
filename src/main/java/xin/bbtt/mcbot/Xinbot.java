/*
 * # Copyright (C) 2025 huangdihd
 * #
 * # This program is free software: you can redistribute it and/or modify
 * # it under the terms of the GNU General Public License as published by
 * # the Free Software Foundation, either version 3 of the License, or
 * # (at your option) any later version.
 * #
 * # This program is distributed in the hope that it will be useful,
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * # GNU General Public License for more details.
 * #
 * # You should have received a copy of the GNU General Public License
 * # along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package xin.bbtt.mcbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.JLine.CLI;
import xin.bbtt.mcbot.auth.AccountLoader;
import xin.bbtt.mcbot.auth.OnlineAccountDumper;
import xin.bbtt.mcbot.config.BotConfig;

import java.io.File;

public class Xinbot {
    private static final Logger log = LoggerFactory.getLogger(Xinbot.class.getSimpleName());

    public static final String version = Xinbot.class.getPackage().getImplementationVersion();

    public static String configPath = "config.conf";

    public static void main(String[] args){
        BotConfig config = null;
        try {
            if (args.length == 1) {
                if (args[0].equals("--version") || args[0].equals("-v")) {
                    log.info("version: {}", version);
                    System.exit(0);
                }
                configPath = args[0];
            }
            log.info("Loading config file: {}", configPath);
            config = BotConfig.loadFromFile(configPath);
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
            System.exit(1);
        }

        if (config.getAdvances().isEnableJLine()) CLI.init();
        if (config.getAdvances().isEnableTranslation()) LangManager.Init();
        log.info("version: {}", version);
        File pluginDir = new File(config.getPlugin().getDirectory());
        if (!pluginDir.exists() || !pluginDir.isDirectory()) {
            if (pluginDir.mkdir()) {
                log.info("Created plugins directory: {}", pluginDir.isDirectory());
            }
            else {
                log.error("Failed to create plugins directory: {}", pluginDir.isDirectory());
                return;
            }
        }
        try {
            AccountLoader.init(config.getAccount());
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
            System.exit(1);
        }
        if (config.getAccount().isOnlineMode())
            config.setAccount(OnlineAccountDumper.DumpAccount(AccountLoader.getJavaSession()));
        try {
            BotConfig.saveToFile(configPath, config);
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        Bot.Instance.init(config);
        Bot.Instance.start();

    }
}
