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

package xin.bbtt.mcbot.eventListeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.Utils;
import xin.bbtt.mcbot.auth.AccountLoader;
import xin.bbtt.mcbot.config.BotConfig;
import xin.bbtt.mcbot.config.BotConfigData;
import xin.bbtt.mcbot.event.EventHandler;
import xin.bbtt.mcbot.event.Listener;
import xin.bbtt.mcbot.events.DisconnectEvent;

public class DisconnectListener implements Listener {
    private static final Logger log = LoggerFactory.getLogger(DisconnectListener.class.getSimpleName());
    @EventHandler
    public void onDisconnect(DisconnectEvent event) {
        if ("§c微软认证失败".equals(Utils.toString(event.getReason()))) {
            BotConfig config = Bot.Instance.getConfig();
            BotConfigData configData = config.getConfigData();

            boolean shouldStopBot = false;

            try {
                configData.setAccount(AccountLoader.refresh());
            } catch (Exception e) {
                log.error("Microsoft authentication failed.", e);
                configData.getAccount().setFullSession(null);
                shouldStopBot = true;
            } finally {
                try {
                    config.saveToFile();
                } catch (Exception e) {
                    log.error("Failed to save the configuration file.", e);
                }
            }

            if (shouldStopBot) {
                Bot.Instance.stop();
            }
        }
    }
}
