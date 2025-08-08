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
