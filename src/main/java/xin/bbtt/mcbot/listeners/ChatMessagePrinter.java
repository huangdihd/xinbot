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

package xin.bbtt.mcbot.listeners;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.Utils;
import xin.bbtt.mcbot.events.OverlayUpdateEvent;
import xin.bbtt.mcbot.events.SystemChatMessageEvent;

import java.util.Arrays;

import static xin.bbtt.mcbot.Utils.parseColors;
import static xin.bbtt.mcbot.Utils.toStrings;

public class ChatMessagePrinter extends SessionAdapter {
    private static final Logger log = LoggerFactory.getLogger(ChatMessagePrinter.class.getSimpleName());
    private static final Marker chatMessageMarker = MarkerFactory.getMarker("[SysChatMessage]");
    private static final Marker overlayMessageMarker = MarkerFactory.getMarker("[OverlayMessage]");
    private static String overlayMessage;
    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(packet instanceof ClientboundSystemChatPacket systemChatPacket)) return;
        boolean overlay = systemChatPacket.isOverlay();
        SystemChatMessageEvent event = new SystemChatMessageEvent(systemChatPacket.getContent(), systemChatPacket.isOverlay());
        Bot.Instance.getPluginManager().events().callEvent(event);
        Marker marker = overlay ? overlayMessageMarker : chatMessageMarker;
        if (overlay) {
            if (Utils.toString(systemChatPacket.getContent()).equals(overlayMessage)) return;
            overlayMessage = Utils.toString(systemChatPacket.getContent());
            OverlayUpdateEvent overlayUpdateEvent = new OverlayUpdateEvent(systemChatPacket.getContent());
            Bot.Instance.getPluginManager().events().callEvent(overlayUpdateEvent);
        }
        Arrays.stream(event.getText().split("\n"))
                .forEach((line) -> log.info(marker, parseColors(line)));
        log.debug(marker, "Received message: {}", toStrings(systemChatPacket.getContent()));
    }
}
