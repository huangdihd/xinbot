package xin.bbtt.mcbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Xinbot {
    private static final Logger log = LoggerFactory.getLogger(Xinbot.class.getSimpleName());

    public static final String version = Xinbot.class.getPackage().getImplementationVersion();

    public static void main(String[] args){
        if (Bot.Instance.getBotProfile().load(args)) return;
        CLI.init();
        log.info("version: {}", version);
        File dir = new File(Bot.Instance.getBotProfile().getPluginsDirectory());
        if (!dir.exists() || !dir.isDirectory()) {
            if (dir.mkdir()) {
                log.info("Created plugins directory: {}", Bot.Instance.getBotProfile().getPluginsDirectory());
            }
            else {
                log.error("Failed to create plugins directory: {}", Bot.Instance.getBotProfile().getPluginsDirectory());
                return;
            }
        }
        Bot.Instance.init();
        Bot.Instance.start();

    }
}
