package xin.bbtt.mcbot.commands.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.CommandExecutor;
import xin.bbtt.mcbot.plugin.Plugin;

import java.util.stream.Collectors;


public class PluginsCommandExecutor extends CommandExecutor {
    private final static Logger log = LoggerFactory.getLogger(ListCommandExecutor.class.getSimpleName());
    @Override
    public void onCommand(Command command, String label, String[] args) {
        log.info("There are {} plugins loaded: {}", Bot.Instance.getPluginManager().getPlugins().size(), Bot.Instance.getPluginManager().getPlugins().parallelStream().map(Plugin::getName).collect(Collectors.joining(", ")));
    }
}
