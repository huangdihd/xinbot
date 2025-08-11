
/*
 * # Copyright (C) 2025 huangdihd
 * #
 * # This program is free software: you can redistribute it and/or modify
 * # it under the terms of the GNU General Public License as published by
 * # the Free Software Foundation, either version 3 of the License, or
 * # (at your option) any later version.
 * #
 * # This program is distributed in the hope that it will be useful,
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * # GNU General Public License for more details.
 * #
 * # You should have received a copy of the GNU General Public License
 * # along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
