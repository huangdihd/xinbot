/*
 * # Copyright (C) 2025 huangdihd
 * #
 * # This program is free software: you can redistribute it and/or modify
 * # it under the terms of the GNU General Public License as published by
 * # the Free Software Foundation, either version 3 of the License, or
 * # (at your option) any later version.
 * #
 * # This program is distributed in the hope that it will be useful,
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * # GNU General Public License for more details.
 * #
 * # You should have received a copy of the GNU General Public License
 * # along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package xin.bbtt.mcbot.listeners;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.Server;

public class JoinButtonRecorder extends SessionAdapter {

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(packet instanceof ClientboundContainerSetSlotPacket containerSetSlotPacket)) return;
        if (Bot.Instance.getServer().equals(Server.Xin)) return;
        if (!containerSetSlotPacket.toString().contains("加入游戏")) return;
        AutoLoginListener.join_button_slot = containerSetSlotPacket.getSlot() % 9;
    }
}
