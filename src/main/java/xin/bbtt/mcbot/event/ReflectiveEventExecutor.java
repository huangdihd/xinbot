
package xin.bbtt.mcbot.event;
import java.lang.reflect.Method;
public final class ReflectiveEventExecutor implements EventExecutor {
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
        method.invoke(listener, event);
    }
}
