package xin.bbtt.mcbot.events;

import lombok.Getter;
import org.geysermc.mcprotocollib.auth.GameProfile;
import xin.bbtt.mcbot.event.Event;
import xin.bbtt.mcbot.event.HandlerList;

@Getter
public class PlayerJoinEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final GameProfile playerProfile;

    public PlayerJoinEvent(GameProfile playerProfile) {
        this.playerProfile = playerProfile;
    }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
