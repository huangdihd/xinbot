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

import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.event.EventHandler;
import xin.bbtt.mcbot.event.Listener;
import xin.bbtt.mcbot.events.OverlayUpdateEvent;
import xin.bbtt.mcbot.events.PositionInQueueUpdateEvent;

public class PositonInQueueOverlayListener implements Listener {
    @EventHandler
    public void onOverlay(OverlayUpdateEvent event) {
        String text = event.getText();
        if(!text.startsWith("§0§lPosition in queue: §6§l")) return;
        String positionInQueue = text.replace("§0§lPosition in queue: §6§l", "");
        PositionInQueueUpdateEvent positionInQueueUpdateEvent = new PositionInQueueUpdateEvent(Integer.parseInt(positionInQueue));
        Bot.Instance.getPluginManager().events().callEvent(positionInQueueUpdateEvent);
    }
}
