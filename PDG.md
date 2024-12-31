# Plugin Development Guide

This guide is intended to help developers understand how to create plugins for **Xinbot** and integrate them with the `Bot`.

## 1. Basic Structure of a Plugin

Each plugin needs to implement the `Plugin` interface, which requires three methods: `onLoad()`, `onEnable()`, and `onDisable()`.

```java
package xin.bbtt.mcbot;

public interface Plugin {
    default String getName(){
        return this.getClass().getSimpleName();
    }

    void onLoad();      // Called when the plugin is loaded
    void onEnable();    // Called when the plugin is enabled
    void onDisable();   // Called when the plugin is disabled
}
```
## 2. Plugin Lifecycle

The plugin lifecycle is managed by the `PluginManager`. The sequence of plugin loading, enabling, and disabling is as follows:

1. **Plugin Loading**:
   - The `.jar` file of the plugin is placed in the `plugins` directory. When the `Bot` starts, it automatically loads all plugins from this directory.

2. **Plugin Enabling**:
   - After the plugin is loaded, the `onEnable()` method is called. This is where the plugin should register event listeners and initialize resources.

3. **Plugin Disabling**:
   - When the `Bot` stops or the plugin is manually disabled, the `onDisable()` method is called. The plugin should clean up resources and remove event listeners here.

## 3. Steps to Write a Plugin

### 3.1 Creating the Plugin Class

To develop a plugin, you need to create a class that implements the `Plugin` interface. For example, let's create a plugin named `MyPlugin`:

```java
package xin.bbtt.mcbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyPlugin implements Plugin {
    private static final Logger log = LoggerFactory.getLogger(MyPlugin.class);

    @Override
    public void onLoad() {
        log.info("Plugin Loaded: {}", getName());
        // Initialization tasks such as loading configuration files
    }

    @Override
    public void onEnable() {
        log.info("Plugin Enabled: {}", getName());
        // Register event listeners or start services here
        Bot.Instance.addListener(new MyCustomListener());
    }

    @Override
    public void onDisable() {
        log.info("Plugin Disabled: {}", getName());
        // Clean up resources such as removing event listeners
        Bot.Instance.removeListener(new MyCustomListener());
    }
}
```
## 3. Steps to Write a Plugin

### 3.1 Creating the Plugin Class

To develop a plugin, you need to create a class that implements the `Plugin` interface. For example, let's create a plugin named `MyPlugin`:

```java
package xin.bbtt.mcbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyPlugin implements Plugin {
    private static final Logger log = LoggerFactory.getLogger(MyPlugin.class);

    @Override
    public void onLoad() {
        log.info("Plugin Loaded: {}", getName());
        // Initialization tasks such as loading configuration files
    }

    @Override
    public void onEnable() {
        log.info("Plugin Enabled: {}", getName());
        // Register event listeners or start services here
        Bot.Instance.addListener(new MyCustomListener());
    }

    @Override
    public void onDisable() {
        log.info("Plugin Disabled: {}", getName());
        // Clean up resources such as removing event listeners
        Bot.Instance.removeListener(new MyCustomListener());
    }
}
```
### 3.3 Plugin Configuration

If your plugin requires configuration files, you can load them in the `onLoad()` method. For example, you can read configuration from a JSON or YAML file when the plugin starts:

```java
import java.io.FileReader;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MyPlugin implements Plugin {

    @Override
    public void onLoad() {
        try (FileReader reader = new FileReader("plugins/config.json")) {
            JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
            String welcomeMessage = config.get("welcome_message").getAsString();
            log.info("Welcome Message: {}", welcomeMessage);
        } catch (Exception e) {
            log.error("Failed to load config file", e);
        }
    }

    @Override
    public void onEnable() {
        // Enable plugin tasks
    }

    @Override
    public void onDisable() {
        // Clean up tasks
    }
}
```
## 4. Managing Plugins

The `PluginManager` class is responsible for loading, enabling, and disabling plugins. The `.jar` files of plugins should be placed in the `plugins` directory, and the `Bot` will automatically scan and load them on startup.

```java
public class PluginManager {
    private final Map<String, Plugin> plugins = new HashMap<>();

    public void loadPlugin(Plugin plugin) {
        plugins.put(plugin.getName(), plugin);
        plugin.onLoad();
    }

    public void enableAll() {
        for (Plugin plugin : plugins.values()) {
            plugin.onEnable();
        }
    }

    public void disableAll() {
        for (Plugin plugin : plugins.values()) {
            plugin.onDisable();
        }
    }
}
```
## 5. Plugin and Event Listeners

`Bot` uses `SessionListener` to handle events in the game. Plugins can respond to these events by registering listeners. For example, you might want to listen to player info changes, chat messages, player joins, and player leaves.

```java
public class MyPlugin implements Plugin {
    @Override
    public void onEnable() {
        Bot.Instance.addListener(new PlayerJoinListener());
    }
}

public class PlayerJoinListener implements SessionListener {
    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundPlayerInfoUpdatePacket playerInfoUpdatePacket) {
            // Handle player join event here
        }
    }
}
```

## 6. Add Your Class in the Services Folder

The `PluginManager` uses `ServiceLoader` to import plugins. To ensure your plugin is properly recognized, follow these steps:

1. **Create the Services Folder**:  
   Navigate to the `resources` folder of your project. Inside it, create a directory named `META-INF/services`.

2. **Create the Descriptor File**:  
   In the `META-INF/services` folder, create a file named `xin.bbtt.mcbot.Plugin`.

3. **Add Plugin Class Full Paths**:  
   Open the `xin.bbtt.mcbot.Plugin` file and list the full paths of your plugin classes, one per line. For example:
   ```plaintext
   com.example.myplugin.MyFirstPlugin
   com.example.myplugin.MySecondPlugin
   ```
By completing these steps, your plugins will be discoverable by the `PluginManager` during runtime.
## 7. Conclusion

By implementing the `Plugin` interface, developers can easily create and manage plugins. The plugin lifecycle is controlled by the `PluginManager`, and plugins can register event listeners to handle events in the game. Developers can initialize configurations in the `onLoad()` method, register event listeners in the `onEnable()` method, and clean up resources in the `onDisable()` method.

This guide provides a comprehensive overview to quickly get started with plugin development and seamlessly integrate plugins into `Xinbot`, expanding the bot’s capabilities.
