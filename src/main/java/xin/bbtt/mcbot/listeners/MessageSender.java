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

package xin.bbtt.mcbot.listeners;

import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.events.SendChatMessageEvent;
import xin.bbtt.mcbot.events.SendCommandEvent;

import java.time.Instant;
import java.util.BitSet;

public class MessageSender extends SessionAdapter {
    /** Minimum interval between two outbound messages; also used as the scheduler period. */
    public static final long SEND_INTERVAL_MS = 3000;

    /**
     * Sends at most one queued message. Driven by the Bot scheduler at a fixed rate so the queue
     * drains even when the server sends no inbound packets, instead of piggybacking on packetReceived.
     */
    public void trySend() {
        ClientSession session = Bot.INSTANCE.getSession();
        if (session == null || !session.isConnected()) return;
        if (Bot.INSTANCE.getProtocol().getOutboundState() != ProtocolState.GAME) return;
        if (Bot.INSTANCE.getProtocol().getInboundState() != ProtocolState.GAME) return;

        String message = Bot.INSTANCE.getToBeSentMessages().poll();
        if (message == null) return;

        if (message.startsWith("/")) {
            String command = message.replaceFirst("/", "");
            SendCommandEvent sendCommandEvent = new SendCommandEvent(command);
            Bot.INSTANCE.getPluginManager().events().callEvent(sendCommandEvent);
            if (!sendCommandEvent.isDefaultActionCancelled())
                session.send(new ServerboundChatCommandPacket(sendCommandEvent.getCommand()));
        }
        else {
            if (message.startsWith("\\/")) {
                message = message.substring(1);
            }
            SendChatMessageEvent sendChatMessageEvent = new SendChatMessageEvent(message);
            Bot.INSTANCE.getPluginManager().events().callEvent(sendChatMessageEvent);
            if (!sendChatMessageEvent.isDefaultActionCancelled())
                session.send(
                        new ServerboundChatPacket(
                                sendChatMessageEvent.getMessage(),
                                Instant.now().toEpochMilli(),
                                0L,
                                null,
                                0,
                                new BitSet(),
                                0
                        )
                );
        }
    }
}
