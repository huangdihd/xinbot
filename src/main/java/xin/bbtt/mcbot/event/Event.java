
package xin.bbtt.mcbot.event;
public abstract class Event {
    private final boolean async;
    protected Event() { this(false); }
    protected Event(boolean isAsync) { this.async = isAsync; }
    public boolean isAsynchronous() { return async; }
    public abstract HandlerList getHandlers();
}
