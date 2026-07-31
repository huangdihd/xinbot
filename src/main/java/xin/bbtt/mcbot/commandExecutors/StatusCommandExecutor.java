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

import java.lang.management.ManagementFactory;

@SuppressWarnings("unused")
public class StatusCommandExecutor extends CommandExecutor {
    private static final Logger log = LoggerFactory.getLogger(StatusCommandExecutor.class.getSimpleName());

    @Override
    public void onCommand(Command command, String label, String[] args) {
        // Uptime
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long days = uptimeMs / (1000 * 60 * 60 * 24);
        long hours = (uptimeMs / (1000 * 60 * 60)) % 24;
        long minutes = (uptimeMs / (1000 * 60)) % 60;
        long seconds = (uptimeMs / 1000) % 60;
        String uptimeStr = String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);

        // Connection
        boolean isConnected = Bot.INSTANCE.getSession() != null && Bot.INSTANCE.getSession().isConnected();
        String connectionStr = isConnected ? LangManager.get("xinbot.command.status.connected") : LangManager.get("xinbot.command.status.disconnected");

        // Memory
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        String memoryStr = usedMemory + " MB / " + maxMemory + " MB";

        // Print Status
        log.info(LangManager.get("xinbot.command.status.header"));
        log.info(LangManager.get("xinbot.command.status.uptime", uptimeStr));
        log.info(LangManager.get("xinbot.command.status.connection", connectionStr));
        log.info(LangManager.get("xinbot.command.status.memory", memoryStr));
        if (isConnected) {
            log.info(LangManager.get("xinbot.command.status.players", Bot.INSTANCE.players.size()));
        }
    }
}
