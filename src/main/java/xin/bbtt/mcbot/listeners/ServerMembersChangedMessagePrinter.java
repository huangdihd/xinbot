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

package xin.bbtt.mcbot.listeners;

import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoRemovePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.events.PlayerJoinEvent;
import xin.bbtt.mcbot.events.PlayerLeaveEvent;

import java.util.Arrays;

import static xin.bbtt.mcbot.Utils.parseColors;

public class ServerMembersChangedMessagePrinter extends SessionAdapter {

    private static final Logger log = LoggerFactory.getLogger(ServerMembersChangedMessagePrinter.class.getSimpleName());

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundLoginPacket) Bot.Instance.players.clear();
        if (packet instanceof ClientboundPlayerInfoUpdatePacket playerInfoUpdatePacket)
            playerInfoUpdatePacketProcessor(playerInfoUpdatePacket);
        if (packet instanceof ClientboundPlayerInfoRemovePacket playerInfoRemovePacket)
            playerInfoRemovePacketProcessor(playerInfoRemovePacket);

    }

    private void playerInfoUpdatePacketProcessor(ClientboundPlayerInfoUpdatePacket playerInfoUpdatePacket) {
        Arrays.stream(playerInfoUpdatePacket.getEntries()).forEach((playerEntry) -> {
            if (playerEntry.getProfile() == null) return;
            Bot.Instance.players.put(playerEntry.getProfileId(), playerEntry.getProfile());
        });
        if (playerInfoUpdatePacket.getEntries().length != 1) return;
        if (playerInfoUpdatePacket.getEntries()[0].getProfile() == null) return;
        if (playerInfoUpdatePacket.getEntries()[0].getProfile().getName().equals(Bot.Instance.getConfig().getAccount().getName())) return;
        PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(playerInfoUpdatePacket.getEntries()[0].getProfile());
        Bot.Instance.getPluginManager().events().callEvent(playerJoinEvent);
        log.info(parseColors("§8[§2+§8]§7{}"), playerInfoUpdatePacket.getEntries()[0].getProfile().getName());
    }

    private void playerInfoRemovePacketProcessor(ClientboundPlayerInfoRemovePacket playerInfoRemovePacket) {
        if (playerInfoRemovePacket.getProfileIds().size() != 1) return;
        if (Bot.Instance.players.get(playerInfoRemovePacket.getProfileIds().get(0)) == null) return;
        GameProfile gameProfile = Bot.Instance.players.get(playerInfoRemovePacket.getProfileIds().get(0));
        Bot.Instance.players.remove(playerInfoRemovePacket.getProfileIds().get(0));
        if (gameProfile.getName().equals(Bot.Instance.getConfig().getAccount().getName())) return;
        PlayerLeaveEvent playerLeaveEvent = new PlayerLeaveEvent(gameProfile);
        Bot.Instance.getPluginManager().events().callEvent(playerLeaveEvent);
        log.info(parseColors("§8[§c-§8]§7{}"), gameProfile.getName());

    }
}
