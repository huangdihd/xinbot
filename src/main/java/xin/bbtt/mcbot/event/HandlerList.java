
package xin.bbtt.mcbot.event;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
public class HandlerList {
    private final EnumMap<EventPriority, CopyOnWriteArrayList<RegisteredListener>> handlers =
        new EnumMap<>(EventPriority.class);
    public HandlerList() {
        for (EventPriority p : EventPriority.values()) {
            handlers.put(p, new CopyOnWriteArrayList<>());
        }
    }
    void register(RegisteredListener rl) {
        handlers.get(rl.priority()).add(rl);
    }
    void unregister(RegisteredListener rl) {
        handlers.get(rl.priority()).remove(rl);
    }
    public java.util.List<RegisteredListener> getRegisteredListenersInOrder() {
        java.util.List<RegisteredListener> all = new java.util.ArrayList<>();
        for (EventPriority p : EventPriority.values()) {
            all.addAll(handlers.get(p));
        }
        return java.util.Collections.unmodifiableList(all);
    }
}
