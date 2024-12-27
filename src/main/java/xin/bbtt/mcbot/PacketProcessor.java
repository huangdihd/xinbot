package xin.bbtt.mcbot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.*;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoRemovePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetTitleTextPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import static xin.bbtt.mcbot.Utils.parseColors;

class AutoLoginProcessor extends SessionAdapter {
    private static final Logger log = LoggerFactory.getLogger("AutoLoginProcessor");
    private Long wait_time = System.currentTimeMillis();

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundSetTitleTextPacket titlePacket) login(titlePacket);
        if (Bot.Instance.login) join();
    }

    private void login(ClientboundSetTitleTextPacket titlePacket) {
        if (titlePacket.toString().contains("/L")) Bot.Instance.sendCommand("l " + Bot.Instance.getBotProfile().getPassword());
        if (titlePacket.toString().contains("登陆成功")) Bot.Instance.login = true;
    }

    private void join() {
        if (wait_time > System.currentTimeMillis() - 1000) return;
        Bot.Instance.setCarriedItem(2);
        Bot.Instance.useItemWithMainHand(0, 0);
        wait_time = System.currentTimeMillis();
    }
}

class ChatMessagePrinter extends SessionAdapter {
    private static final Logger log = LoggerFactory.getLogger("[ChatMessage]");
    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(packet instanceof ClientboundSystemChatPacket systemChatPacket)) return;
        log.info(parseColors(Utils.toString(systemChatPacket.getContent())));
    }
}

class ServerRecorder extends SessionAdapter {
    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(packet instanceof ClientboundLoginPacket loginPacket)) return;
        if (loginPacket.toString().contains(", gameMode=ADVENTURE")) Bot.Instance.server = Server.Login;
        if (loginPacket.toString().contains(", gameMode=SURVIVAL")) Bot.Instance.server = Server.Xin;
    }
}

class ServerMembersChangedMessagePrinter extends SessionAdapter {

    private static final Logger log = LoggerFactory.getLogger("[ServerMemberChangesMessage]");

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
        if (playerInfoUpdatePacket.getEntries()[0].getProfile().getName().equals(Bot.Instance.getBotProfile().getUsername())) return;
        log.info(parseColors("§8[§2+§8]§7{}"), playerInfoUpdatePacket.getEntries()[0].getProfile().getName());
    }

    private void playerInfoRemovePacketProcessor(ClientboundPlayerInfoRemovePacket playerInfoRemovePacket) {
        if (playerInfoRemovePacket.getProfileIds().size() != 1) return;
        if (Bot.Instance.players.get(playerInfoRemovePacket.getProfileIds().get(0)) == null) return;
        String name = Bot.Instance.players.get(playerInfoRemovePacket.getProfileIds().get(0)).getName();
        if (!name.equals(Bot.Instance.getBotProfile().getUsername())) log.info(parseColors("§8[§c-§8]§7{}"), name);
        Bot.Instance.players.remove(playerInfoRemovePacket.getProfileIds().get(0));
    }
}

class QueueProcessor extends SessionAdapter {
    private final JsonObject problem = JsonParser.parseString(new BufferedReader(new InputStreamReader(Objects.requireNonNull(QueueProcessor.class.getClassLoader().getResourceAsStream("problems.json")), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"))).getAsJsonObject();

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(packet instanceof ClientboundSystemChatPacket systemChatPacket)) return;
        if (problem.has(Utils.toString(systemChatPacket.getContent()))) {
            Bot.Instance.sendChatMessage(problem.get(Utils.toString(systemChatPacket.getContent())).getAsString());
        }
    }
}

class DisconnectReasonPointer extends SessionAdapter {
    private static final Logger log = LoggerFactory.getLogger("[DisconnectReason]");

    @Override
    public void disconnected(DisconnectedEvent event) {
        log.info(parseColors(Utils.toString(event.getReason())));
    }
}
