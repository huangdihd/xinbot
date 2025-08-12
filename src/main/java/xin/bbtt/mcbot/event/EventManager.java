
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import xin.bbtt.mcbot.plugin.Plugin;
import java.lang.reflect.Method;
import java.util.*;

public class EventManager {

    private static final Logger log = LoggerFactory.getLogger(EventManager.class.getSimpleName());

    Marker eventErrorMarker = MarkerFactory.getMarker("[EventError]");

    private static HandlerList getHandlerListForEventClass(Class<?> eventClass) {
        try {
            Method method = eventClass.getMethod("getHandlerList");
            return (HandlerList) method.invoke(null);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Event class must provide public static HandlerList getHandlerList(): " + eventClass.getName(), e);
        }
    }

    private final Map<Plugin, List<RegisteredListener>> byPlugin = new HashMap<>();

    public void registerEvents(Listener listener, Plugin plugin) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            EventHandler annotation = method.getAnnotation(EventHandler.class);
            if (annotation == null) continue;

            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1 || !Event.class.isAssignableFrom(params[0])) {
                throw new IllegalArgumentException(
                        "@EventHandler method must have exactly one parameter, and it must be a subclass of Event: " + method);
            }
            Class<?> eventParamType = params[0];

            HandlerList list = getHandlerListForEventClass(eventParamType);
            EventExecutor executor = new ReflectiveEventExecutor(method, eventParamType);
            RegisteredListener registeredListener = new RegisteredListener(
                    plugin, listener, annotation.priority(), executor, list);

            list.register(registeredListener);
            byPlugin.computeIfAbsent(plugin, k -> new ArrayList<>()).add(registeredListener);
        }
    }

    /** Unregister all listeners owned by the given plugin/owner. */
    public void unregisterAll(Plugin plugin) {
        List<RegisteredListener> listenerList = byPlugin.remove(plugin);
        if (listenerList == null) return;
        for (RegisteredListener rl : listenerList) {
            rl.handlerList().unregister(rl);
        }
    }

    public void callEvent(Event event) {
        HandlerList list = event.getHandlers();
        for (RegisteredListener registeredListener : list.getRegisteredListenersInOrder()) {
            try {
                registeredListener.callEvent(event);
            } catch (Throwable throwable) {
                log.error(eventErrorMarker,
                        "Error while executing event {}",
                        throwable.getClass().getSimpleName(),
                        throwable
                );
            }
        }
    }
}
