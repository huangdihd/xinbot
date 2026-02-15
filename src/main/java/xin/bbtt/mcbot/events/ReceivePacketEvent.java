/*
 *   Copyright (C) 2026 huangdihd
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
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import xin.bbtt.mcbot.event.Event;
import xin.bbtt.mcbot.event.HandlerList;

public class ReceivePacketEvent<T extends MinecraftPacket> extends Event {
    @Getter
    private final T packet;
    @Getter
    private final Class<? extends T> packetClass;
    private final static HandlerList HANDLERS = new HandlerList();

    public ReceivePacketEvent(T packet, Class<? extends T> packetClass) {
        this.packet = packet;
        this.packetClass = packetClass;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
