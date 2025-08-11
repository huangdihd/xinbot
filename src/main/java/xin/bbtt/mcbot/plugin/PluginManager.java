
package xin.bbtt.mcbot.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xin.bbtt.mcbot.event.EventManager;
import xin.bbtt.mcbot.event.Listener;

import java.util.*;
import java.io.*;
import java.net.*;
import java.util.ServiceLoader;

// Plugin Manager
public class PluginManager {
    private final Map<String, Plugin> plugins = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(PluginManager.class.getSimpleName());

    // Event manager
    private final EventManager eventManager = new EventManager();

    /** Expose event system to core code (callEvent/registration). */
    public EventManager events() { return eventManager; }

    public void registerEvents(Listener listener, Plugin plugin) {
        eventManager.registerEvents(listener, plugin);
    }

    public void loadPlugin(Plugin plugin) {
        plugins.put(plugin.getName(), plugin);
        plugin.onLoad();
        log.info("Loaded plugin: {}", plugin.getClass().getName());
    }

    public void loadPlugins(String pluginsDirectory) {
        File dir = new File(pluginsDirectory);
        if (!dir.exists() || !dir.isDirectory()) {
            log.error("The plugins directory does not exist or is invalid: {}", pluginsDirectory);
            return;
        }

        File[] files = dir.listFiles((dir1, name) -> name.endsWith(".jar"));
        if (files == null || files.length == 0) {
            log.error("No plugins found.");
            return;
        }

        for (File file : files) {
            try {
                log.info("Trying to load plugin: {}", file.getName());
                URL[] urls = { file.toURI().toURL() };
                URLClassLoader classLoader = new URLClassLoader(urls);
                ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class, classLoader);
                for (Plugin plugin : serviceLoader) {
                    loadPlugin(plugin);
                }
            } catch (Exception e) {
                log.error("Failed to load plugin: {}", file.getName(), e);
            }
        }
    }

    public void enableAll() {
        for (Plugin plugin : plugins.values()) {
            try {
                plugin.onEnable();
            } catch (Exception e) {
                log.error("Failed to enable plugin: {}", plugin.getName(), e);
            }
        }
    }

    public void disableAll() {
        for (Plugin plugin : plugins.values()) {
            try {
                eventManager.unregisterAll(plugin);
            } catch (Exception ex) {
                log.warn("Error while unregistering listeners for plugin {}", plugin.getName(), ex);
            }
            try {
                plugin.onDisable();
            } catch (Exception e) {
                log.error("Failed to disable plugin: {}", plugin.getName(), e);
            }
        }
    }

    public Plugin getPlugin(String name) {
        return plugins.get(name);
    }
}
