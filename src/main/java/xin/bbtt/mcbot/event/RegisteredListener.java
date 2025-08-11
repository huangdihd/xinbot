
package xin.bbtt.mcbot.event;

import xin.bbtt.mcbot.plugin.Plugin;

public record RegisteredListener(Plugin plugin, Listener listener, EventPriority priority,
                          EventExecutor executor, HandlerList handlerList) {
    void callEvent(Event event) throws Exception {
        executor.execute(listener, event);
    }
}
