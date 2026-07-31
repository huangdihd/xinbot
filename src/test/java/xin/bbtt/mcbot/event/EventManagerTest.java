/*
 * Copyright (C) 2026 huangdihd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package xin.bbtt.mcbot.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xin.bbtt.mcbot.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class EventManagerTest {

    private EventManager eventManager;
    private Plugin mockPlugin;

    @BeforeEach
    void setUp() {
        eventManager = new EventManager();
        mockPlugin = mock(Plugin.class);
    }

    public static class TestEvent extends Event {
        private static final HandlerList handlers = new HandlerList();

        @SuppressWarnings("unused")
        public static HandlerList getHandlerList() {
            return handlers;
        }
        
        @Override
        public HandlerList getHandlers() {
            return handlers;
        }
    }

    public static class TestListener implements Listener {
        public List<String> callOrder = new ArrayList<>();

        @EventHandler(priority = EventPriority.NORMAL)
        @SuppressWarnings("unused")
        public void onNormal(TestEvent event) {
            callOrder.add("NORMAL");
        }

        @EventHandler(priority = EventPriority.LOWEST)
        @SuppressWarnings("unused")
        public void onLowest(TestEvent event) {
            callOrder.add("LOWEST");
        }

        @EventHandler(priority = EventPriority.MONITOR)
        @SuppressWarnings("unused")
        public void onMonitor(TestEvent event) {
            callOrder.add("MONITOR");
        }
        
        @EventHandler(priority = EventPriority.HIGHEST)
        @SuppressWarnings("unused")
        public void onHighest(TestEvent event) {
            callOrder.add("HIGHEST");
        }
    }

    @Test
    void testEventPriorityOrder() {
        TestListener listener = new TestListener();
        eventManager.registerEvents(listener, mockPlugin);

        eventManager.callEvent(new TestEvent());

        // Order should be LOWEST -> LOW -> NORMAL -> HIGH -> HIGHEST -> MONITOR
        assertThat(listener.callOrder).containsExactly("LOWEST", "NORMAL", "HIGHEST", "MONITOR");
    }

    @Test
    void testUnregisterAll() {
        TestListener listener = new TestListener();
        eventManager.registerEvents(listener, mockPlugin);

        eventManager.unregisterAll(mockPlugin);

        eventManager.callEvent(new TestEvent());

        // Since it was unregistered, it should not have been called
        assertThat(listener.callOrder).isEmpty();
    }
}
