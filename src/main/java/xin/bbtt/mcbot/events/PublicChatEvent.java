package xin.bbtt.mcbot.events;

import lombok.Getter;
import org.geysermc.mcprotocollib.auth.GameProfile;
import xin.bbtt.mcbot.event.Event;
import xin.bbtt.mcbot.event.HandlerList;

public class PublicChatEvent extends Event {
    private final static HandlerList HANDLERS = new HandlerList();
    @Getter
    private final GameProfile sender;
    @Getter
    private final String message;

    public PublicChatEvent(GameProfile sender, String message) {
        this.sender = sender;
        this.message = message;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
