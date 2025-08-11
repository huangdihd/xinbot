package xin.bbtt.mcbot.events;

import lombok.Getter;
import lombok.Setter;
import xin.bbtt.mcbot.event.Event;
import xin.bbtt.mcbot.event.HandlerList;
import xin.bbtt.mcbot.event.HasDefaultAction;

public class SendChatMessageEvent extends Event implements HasDefaultAction {
    private static final HandlerList HANDLERS = new HandlerList();
    @Getter
    @Setter
    private String message;


    private boolean cancelDefault;

    public SendChatMessageEvent(String message) {
        this.message = message;
    }

    @Override public boolean isDefaultActionCancelled() { return cancelDefault; }
    @Override public void setDefaultActionCancelled(boolean c) { this.cancelDefault = c; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
