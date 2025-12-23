package xin.bbtt.mcbot.events;

import lombok.Getter;
import xin.bbtt.mcbot.event.Event;
import xin.bbtt.mcbot.event.HandlerList;
import xin.bbtt.mcbot.plugin.Plugin;

public class DisablePluginEvent extends Event {
    private final static HandlerList HANDLERS = new HandlerList();
    @Getter
    private final Plugin plugin;

    public DisablePluginEvent(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
