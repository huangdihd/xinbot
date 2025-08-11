package xin.bbtt.mcbot.events;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import xin.bbtt.mcbot.Utils;
import xin.bbtt.mcbot.event.HandlerList;
import xin.bbtt.mcbot.event.Event;

@Getter
public class SystemChatMessageEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Component message;
    private final String text;
    private final boolean overlay;

    public SystemChatMessageEvent(Component message, boolean overlay) {
        this.message = message;
        this.text = Utils.toString(message);
        this.overlay = overlay;
    }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
