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

package xin.bbtt.mcbot.events;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import xin.bbtt.mcbot.Utils;
import xin.bbtt.mcbot.event.HandlerList;
import xin.bbtt.mcbot.event.Event;

@Getter
public class SystemChatMessageEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Component content;
    private final String text;
    private final boolean overlay;

    public SystemChatMessageEvent(Component content, boolean overlay) {
        this.content = content;
        this.text = Utils.toString(content);
        this.overlay = overlay;
    }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
