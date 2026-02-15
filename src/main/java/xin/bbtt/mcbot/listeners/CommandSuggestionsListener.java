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
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandSuggestionsPacket;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CommandSuggestionsListener extends SessionAdapter {
    private final CompletableFuture<List<String>> future;
    private final int transactionId;

    public CommandSuggestionsListener(CompletableFuture<List<String>> future, int transactionId) {
        super();
        this.future = future;
        this.transactionId = transactionId;
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(packet instanceof ClientboundCommandSuggestionsPacket commandSuggestionsPacket)) return;
        if (commandSuggestionsPacket.getTransactionId() != transactionId) return;
        List<String> result = List.of(commandSuggestionsPacket.getMatches());
        if (!future.isDone()) {
            future.complete(result);
        }
        session.removeListener(this);
    }
}
