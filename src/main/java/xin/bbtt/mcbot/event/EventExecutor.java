
package xin.bbtt.mcbot.event;
public interface EventExecutor {
    void execute(Listener listener, Event event) throws Exception;
}
