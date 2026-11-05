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

package xin.bbtt.mcbot.commandExecutors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.LangManager;
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.CommandExecutor;
import xin.bbtt.mcbot.command.SubCommandExecutor;

@SuppressWarnings("unused")
public class JfrCommandExecutor extends SubCommandExecutor {
    private static final Logger log = LoggerFactory.getLogger(JfrCommandExecutor.class.getSimpleName());

    public JfrCommandExecutor() {
        registerSubCommand("start", new StartCommand());
        registerSubCommand("stop", new StopCommand());
        registerSubCommand("status", new StatusCommand());
    }

    @Override
    protected void onNoSubCommand(Command command, String label) {
        printStatus();
    }

    private static void printStatus() {
        var manager = Bot.INSTANCE.getJfrManager();
        if (manager.isRunning()) {
            log.info(LangManager.get("xinbot.jfr.status.running", manager.getCurrentFile()));
        } else if (manager.getLastFile() != null) {
            log.info(LangManager.get("xinbot.jfr.status.stopped.last", manager.getLastFile()));
        } else {
            log.info(LangManager.get("xinbot.jfr.status.stopped"));
        }
    }

    private static class StartCommand extends CommandExecutor {
        @Override
        public void onCommand(Command command, String label, String[] args) {
            try {
                var output = Bot.INSTANCE.getJfrManager().start();
                log.info(LangManager.get("xinbot.jfr.started", output));
            } catch (IllegalStateException e) {
                log.warn(LangManager.get("xinbot.jfr.already.running"));
            } catch (Exception e) {
                log.error(LangManager.get("xinbot.jfr.start.failed", e.getMessage()), e);
            }
        }
    }

    private static class StopCommand extends CommandExecutor {
        @Override
        public void onCommand(Command command, String label, String[] args) {
            try {
                var output = Bot.INSTANCE.getJfrManager().stop();
                log.info(LangManager.get("xinbot.jfr.stopped", output));
            } catch (IllegalStateException e) {
                log.warn(LangManager.get("xinbot.jfr.not.running"));
            } catch (Exception e) {
                log.error(LangManager.get("xinbot.jfr.stop.failed", e.getMessage()), e);
            }
        }
    }

    private static class StatusCommand extends CommandExecutor {
        @Override
        public void onCommand(Command command, String label, String[] args) {
            printStatus();
        }
    }
}
