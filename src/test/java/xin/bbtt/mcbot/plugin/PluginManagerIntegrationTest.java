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

package xin.bbtt.mcbot.plugin;

import org.geysermc.mcprotocollib.network.ClientSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.event.EventHandler;
import xin.bbtt.mcbot.event.HandlerList;
import xin.bbtt.mcbot.event.Listener;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class PluginManagerIntegrationTest {

    private PluginManager pluginManager;
    private Bot bot;

    @BeforeEach
    void setUp() throws Exception {
        pluginManager = new PluginManager();
        bot = Bot.INSTANCE;
        
        // Reset Bot state using reflection if necessary, but let's try setting fields first
        Field runningField = Bot.class.getDeclaredField("running");
        runningField.setAccessible(true);
        runningField.set(bot, false);
        
        Field sessionField = Bot.class.getDeclaredField("session");
        sessionField.setAccessible(true);
        sessionField.set(bot, null);
    }

    public static class MockPlugin implements Plugin {
        public boolean loaded = false;
        public boolean enabled = false;
        public boolean disabled = false;
        public boolean unloaded = false;

        @Override
        public void onLoad() { loaded = true; }
        @Override
        public void onEnable() { enabled = true; }
        @Override
        public void onDisable() { disabled = true; }
        @Override
        public void onUnload() { unloaded = true; }
    }

    @Test
    void testPluginLifecycle() throws Exception {
        MockPlugin plugin = new MockPlugin();
        pluginManager.loadPlugin(plugin);
        
        assertThat(plugin.loaded).isTrue();
        assertThat(pluginManager.getPlugin("MockPlugin")).isNotNull();
        
        // Simulate bot having a session to allow enabling
        ClientSession mockSession = mock(ClientSession.class);
        Field sessionField = Bot.class.getDeclaredField("session");
        sessionField.setAccessible(true);
        sessionField.set(bot, mockSession);

        RegisteredPlugin rp = pluginManager.getPlugin("MockPlugin");
        pluginManager.enablePlugin(rp);

        assertThat(plugin.enabled).isTrue();
        assertThat(pluginManager.isPluginEnabled("MockPlugin")).isTrue();

        pluginManager.disablePlugin(rp);
        assertThat(plugin.disabled).isTrue();
        assertThat(pluginManager.isPluginEnabled("MockPlugin")).isFalse();

    }

    public static class EventTestEvent extends xin.bbtt.mcbot.event.Event {
        private static final HandlerList handlers = new HandlerList();
        @SuppressWarnings("unused")
        public static HandlerList getHandlerList() { return handlers; }
        @Override
        public HandlerList getHandlers() { return handlers; }
    }

    public static class EventListener implements Listener {
        public AtomicBoolean called = new AtomicBoolean(false);
        @EventHandler
        @SuppressWarnings("unused")
        public void onEvent(EventTestEvent event) {
            called.set(true);
        }
    }

    @Test
    void testEventRegistrationViaPluginManager() throws Exception {
        MockPlugin plugin = new MockPlugin();
        pluginManager.loadPlugin(plugin);
        
        // Must enable plugin for disablePlugin to work
        ClientSession mockSession = mock(ClientSession.class);
        Field sessionField = Bot.class.getDeclaredField("session");
        sessionField.setAccessible(true);
        sessionField.set(bot, mockSession);
        
        RegisteredPlugin rp = pluginManager.getPlugin("MockPlugin");
        pluginManager.enablePlugin(rp);
        
        EventListener listener = new EventListener();
        pluginManager.registerEvents(listener, plugin);
        
        pluginManager.events().callEvent(new EventTestEvent());
        
        assertThat(listener.called.get()).isTrue();
        
        // Test unregister on disable
        listener.called.set(false);
        pluginManager.disablePlugin(rp);
        
        pluginManager.events().callEvent(new EventTestEvent());
        assertThat(listener.called.get()).isFalse();
    }
}
