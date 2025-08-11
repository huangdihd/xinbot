package xin.bbtt.mcbot.events;

import xin.bbtt.mcbot.event.Event;
import xin.bbtt.mcbot.event.HandlerList;

public class LoginSuccessEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}