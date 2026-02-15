/*
 *   Copyright (C) 2024-2026 huangdihd
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xin.bbtt.mcbot.event;

import xin.bbtt.mcbot.events.ReceivePacketEvent;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ReflectiveEventExecutor implements EventExecutor {

    private final Method method;
    private final Class<?> eventParamType;

    public ReflectiveEventExecutor(Method method, Class<?> eventParamType) {
        this.method = method;
        this.eventParamType = eventParamType;
        this.method.setAccessible(true);
    }

    @Override
    public void execute(Listener listener, Event event) throws Exception {
        if (!eventParamType.isInstance(event)) return;
        if (event instanceof ReceivePacketEvent<?> receiveEvent) {
            Type genericType = method.getGenericParameterTypes()[0];
            if (genericType instanceof ParameterizedType pt) {
                Type[] args = pt.getActualTypeArguments();
                if (args.length == 1 && args[0] instanceof Class<?> clazz) {
                    if (!clazz.isAssignableFrom(receiveEvent.getPacketClass())) {
                        return;
                    }
                }
            }
        }
        method.invoke(listener, event);
    }
}
