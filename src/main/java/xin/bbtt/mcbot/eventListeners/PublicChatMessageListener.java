package xin.bbtt.mcbot.eventListeners;

import org.geysermc.mcprotocollib.auth.GameProfile;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.Utils;
import xin.bbtt.mcbot.event.EventHandler;
import xin.bbtt.mcbot.event.Listener;
import xin.bbtt.mcbot.events.PublicChatEvent;
import xin.bbtt.mcbot.events.SystemChatMessageEvent;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PublicChatMessageListener implements Listener {
    @EventHandler
    public void onChatMessage(SystemChatMessageEvent event) {
        ArrayList<String> strings = Utils.toStrings(event.getContent());
        if (strings.size() != 3) return;
        Pattern pattern = Pattern.compile("<(?:§a)?([^§>]+)(?:§f)?>");
        Matcher matcher = pattern.matcher(strings.get(1));
        if (!matcher.find()) return;
        String playerName = matcher.group(1);
        String message = strings.get(2);
        if (message.startsWith("§a")) {
            message = message.substring(2);
        }
        for (GameProfile profile : Bot.Instance.players.values()) {
            if (profile.getName().equals(playerName)) {
                PublicChatEvent publicChatEvent = new PublicChatEvent(profile, message);
                Bot.Instance.getPluginManager().events().callEvent(publicChatEvent);
                break;
            }
        }
    }
}
