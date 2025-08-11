package xin.bbtt.mcbot.listeners;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CaptchaListener extends SessionAdapter {
    private static final Logger log = LoggerFactory.getLogger(CaptchaListener.class.getSimpleName());
    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(packet instanceof ClientboundSystemChatPacket systemChatPacket)) return;

        String message = Utils.toString(systemChatPacket.getContent());
        Pattern pattern = Pattern.compile("请先输入：(.*) 完成人机验证！");
        Matcher matcher = pattern.matcher(message);

        if (!matcher.find()) return;

        String captchaMessage = matcher.group(1);

        Bot.Instance.sendChatMessage(captchaMessage);

        log.debug(captchaMessage);
    }
}
