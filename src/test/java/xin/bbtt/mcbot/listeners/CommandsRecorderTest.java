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

    private static ClientboundCommandsPacket commandsPacket(String... names) {
        CommandNode[] nodes = new CommandNode[names.length + 1];
        int[] childIndices = new int[names.length];
        for (int i = 0; i < names.length; i++) {
            nodes[i + 1] = literal(names[i]);
            childIndices[i] = i + 1;
        }
        nodes[0] = root(childIndices);
        return new ClientboundCommandsPacket(nodes, 0);
    }

    @Test
    void rebuildsRootCommandsWithoutAccumulatingOnReconnect() {
        CommandsRecorder recorder = new CommandsRecorder();
        ClientboundCommandsPacket packet = commandsPacket("tp", "gamemode");

        // First commands packet from the server.
        recorder.packetReceived(null, packet);
        assertThat(CommandsRecorder.rootCommands).containsExactly("tp", "gamemode");

        // Reconnect: the same packet arrives again and must not accumulate duplicates.
        recorder.packetReceived(null, packet);
        assertThat(CommandsRecorder.rootCommands).containsExactly("tp", "gamemode");
    }
}
