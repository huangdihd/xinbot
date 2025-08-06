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
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetTitleTextPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static xin.bbtt.mcbot.Utils.parseColors;
import static xin.bbtt.mcbot.Utils.toStrings;

class MessageSender extends SessionAdapter {
    private static Long last_send_time = System.currentTimeMillis();

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (System.currentTimeMillis() - last_send_time < 3000 && Bot.Instance.server == Server.Xin) return;
        if (Bot.Instance.protocol.getOutboundState() != ProtocolState.GAME) return;
        if (Bot.Instance.protocol.getInboundState() != ProtocolState.GAME) return;
        if (Bot.Instance.to_be_sent_messages.isEmpty()) return;
        if (Bot.Instance.to_be_sent_messages.get(0).startsWith("/")) {
            String command = Bot.Instance.to_be_sent_messages.get(0).replaceFirst("/", "");
            session.send(new ServerboundChatCommandPacket(command));
        }
        else {
            String message = Bot.Instance.to_be_sent_messages.get(0);
            session.send(
                    new ServerboundChatPacket(
                            message,
                            Instant.now().toEpochMilli(),
                            0L,
                            null,
                            0,
                            new BitSet()
                    )
            );
        }
        last_send_time = System.currentTimeMillis();
        Bot.Instance.to_be_sent_messages.remove(0);
    }
}

class AutoLoginProcessor extends SessionAdapter {
    private Long wait_time = System.currentTimeMillis();

    private static final Logger log = LoggerFactory.getLogger(AutoLoginProcessor.class.getSimpleName());

    public static int join_button_slot;

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundSetTitleTextPacket titlePacket) login(titlePacket);
        if (Bot.Instance.login) join();
    }

    private void login(ClientboundSetTitleTextPacket titlePacket) {
        if (titlePacket.toString().contains("登陆成功")) {
            log.info("Login successful");
            Bot.Instance.login = true;
            return;
        }
        Bot.Instance.sendCommand("l " + Bot.Instance.getBotProfile().getPassword());

    }

    private void join() {
        if (wait_time > System.currentTimeMillis() - 1000) return;
        Bot.Instance.setCarriedItem(join_button_slot);
        Bot.Instance.useItemWithMainHand(0, 0);
        wait_time = System.currentTimeMillis();
    }
}

class ChatMessagePrinter extends SessionAdapter {
    private static final Logger log = LoggerFactory.getLogger(ChatMessagePrinter.class.getSimpleName());
    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(packet instanceof ClientboundSystemChatPacket systemChatPacket)) return;
        Arrays.stream(Utils.toString(systemChatPacket.getContent()).split("\n")).forEach((line) -> log.info(parseColors(line)));
        log.debug(toStrings(systemChatPacket.getContent()).toString());
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
    private final JsonObject questions = JsonParser.parseString(new BufferedReader(new InputStreamReader(Objects.requireNonNull(QueueProcessor.class.getClassLoader().getResourceAsStream("questions.json")), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"))).getAsJsonObject();

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(packet instanceof ClientboundSystemChatPacket systemChatPacket)) return;
        String fullQuestion = Utils.toString(systemChatPacket.getContent());
        if (!fullQuestion.contains("丨")) return;
        String[] parts = fullQuestion.split("丨");
        if (parts.length != 2) return;
        String question = parts[0];
        String options = parts[1];
        if (!questions.has(question)) return;
        Pattern pattern = Pattern.compile(questions.get(question).getAsString());
        Matcher matcher = pattern.matcher(options);

        if (!matcher.find()) return;

        String answer = matcher.group(1);
        Bot.Instance.sendChatMessage(answer);
    }
}

class DisconnectReasonPrinter extends SessionAdapter {
    private static final Logger log = LoggerFactory.getLogger(DisconnectReasonPrinter.class.getSimpleName());

    @Override
    public void disconnected(DisconnectedEvent event) {
        log.info(parseColors(Utils.toString(event.getReason())));
        log.error(event.getCause().getMessage(), event.getCause());
    }
}


class JoinButtonRecorder extends SessionAdapter {

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(packet instanceof ClientboundContainerSetSlotPacket containerSetSlotPacket)) return;
        if (Bot.Instance.server.equals(Server.Xin)) return;
        if (!containerSetSlotPacket.toString().contains("加入游戏")) return;
        AutoLoginProcessor.join_button_slot = containerSetSlotPacket.getSlot() % 9;
    }
}
