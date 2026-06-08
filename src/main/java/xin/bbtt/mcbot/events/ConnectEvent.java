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

package xin.bbtt.mcbot.events;

import xin.bbtt.mcbot.event.Event;
import xin.bbtt.mcbot.event.HandlerList;

/**
 * Called each time the bot establishes a connection to the server, fired after
 * plugins are enabled and right before the underlying session connects. This
 * covers both the initial connection and every automatic reconnect; listeners
 * that need to tell them apart can simply count occurrences (the first is the
 * initial connection, the rest are reconnects).
 */
public class ConnectEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
