/*
 * Copyright (C) 2026 huangdihd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package xin.bbtt.mcbot.listeners;

import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the recorded root command list is rebuilt from each commands
 * packet instead of accumulating duplicates across reconnects.
 */
class CommandsRecorderTest {

    private static CommandNode root(int[] childIndices) {
        return new CommandNode(CommandType.ROOT, false, false, childIndices,
                OptionalInt.empty(), null, null, null, null);
    }

    private static CommandNode literal(String name) {
        return new CommandNode(CommandType.LITERAL, true, false, new int[0],
                OptionalInt.empty(), name, null, null, null);
    }

    private static ClientboundCommandsPacket commandsPacket() {
        CommandNode[] nodes = new CommandNode[new String[]{"tp", "gamemode"}.length + 1];
        int[] childIndices = new int[new String[]{"tp", "gamemode"}.length];
        for (int i = 0; i < new String[]{"tp", "gamemode"}.length; i++) {
            nodes[i + 1] = literal(new String[]{"tp", "gamemode"}[i]);
            childIndices[i] = i + 1;
        }
        nodes[0] = root(childIndices);
        return new ClientboundCommandsPacket(nodes, 0);
    }

    @Test
    void rebuildsRootCommandsWithoutAccumulatingOnReconnect() {
        CommandsRecorder recorder = new CommandsRecorder();
        ClientboundCommandsPacket packet = commandsPacket();

        // First commands packet from the server.
        recorder.packetReceived(null, packet);
        assertThat(CommandsRecorder.rootCommands).containsExactly("tp", "gamemode");

        // Reconnect: the same packet arrives again and must not accumulate duplicates.
        recorder.packetReceived(null, packet);
        assertThat(CommandsRecorder.rootCommands).containsExactly("tp", "gamemode");
    }
}
