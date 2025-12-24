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
