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

package xin.bbtt.mcbot.eventListeners;

import org.geysermc.mcprotocollib.auth.GameProfile;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.event.EventHandler;
import xin.bbtt.mcbot.event.Listener;
import xin.bbtt.mcbot.events.PrivateChatEvent;
import xin.bbtt.mcbot.events.SystemChatMessageEvent;

public class PrivateChatMessageListener implements Listener {
    @EventHandler
    public void onChatMessage(SystemChatMessageEvent event) {
        if (event.isOverlay()) return;
        String text = event.getText();
        if (!text.startsWith("§d来自 ")) return;
        text = text.replaceFirst("§d来自 ", "");
        String playerName = text.split(": ")[0];
        String message = text.replaceFirst(playerName + ": §d", "");
        for (GameProfile profile : Bot.Instance.players.values()) {
            if (profile.getName().equals(playerName)) {
                PrivateChatEvent privateChatEvent = new PrivateChatEvent(profile, message);
                Bot.Instance.getPluginManager().events().callEvent(privateChatEvent);
                break;
            }
        }
    }
}
