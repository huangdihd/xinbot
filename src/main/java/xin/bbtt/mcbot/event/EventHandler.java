
package xin.bbtt.mcbot.event;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {
    EventPriority priority() default EventPriority.NORMAL;
}