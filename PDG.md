# Xinbot Plugin Development Guide

English / [简体中文](PDG_CN.md)

## 1. Overview
This guide helps developers create plugins for Xinbot to extend its functionality. Plugins interact with the bot through the `Plugin` interface and event listeners, enabling custom logic for in-game events.


## 2. Environment Setup
- **Java**: JDK 17 or higher (matches Xinbot's runtime).
- **Dependencies**: Include Xinbot core classes and libraries like `mcprotocollib` (for Minecraft protocol handling) and `slf4j` (logging).
- **IDE**: Recommended IntelliJ IDEA or Eclipse with Maven support.


## 3. Plugin Basics
### 3.1 Core Interface: `Plugin`
All plugins must implement `xin.bbtt.mcbot.Plugin`, which defines lifecycle methods:
```java
public interface Plugin {
default String getName() { return this.getClass().getSimpleName(); }
void onLoad();   // Called when the plugin is loaded (init configs)
void onEnable(); // Called when the plugin is enabled (register listeners)
void onDisable();// Called when the plugin is disabled (cleanup resources)
}
```


## 4. Plugin Lifecycle
Managed by `PluginManager`:
1. **Load Phase**: `onLoad()` is called when Xinbot scans the `plugins` directory (configured via `plugin.directory` in `config.conf`). Use this for lightweight setup (e.g., loading configs).
2. **Enable Phase**: `onEnable()` runs after all plugins load. Register event listeners or start core logic here.
3. **Disable Phase**: `onDisable()` triggers on bot shutdown. Clean up resources (e.g., close files, remove listeners).


## 5. Key Development Steps
### 5.1 Create a Plugin Class
Implement `Plugin` and override lifecycle methods:
```java
package com.example.myplugin;

import xin.bbtt.mcbot.Plugin;
import xin.bbtt.mcbot.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyPlugin implements Plugin {
private static final Logger log = LoggerFactory.getLogger(MyPlugin.class);

    @Override
    public void onLoad() {
        log.info("\{} loaded", getName());
    }

    @Override
    public void onEnable() {
        log.info("\{} enabled", getName());
        Bot.Instance.addListener(new MyChatListener()); // Register listener
    }

    @Override
    public void onDisable() {
        log.info("\{} disabled", getName());
    }
}
```


### 5.2 Event Handling
Use `SessionListener` to respond to in-game events (e.g., chat, player joins). Register listeners in `onEnable()` via `Bot.Instance.addListener()`.

Example: Listen for chat messages
```java
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;

public class MyChatListener implements SessionListener {
@Override
public void packetReceived(Session session, Packet packet) {
if (packet instanceof ClientboundSystemChatPacket chatPacket) {
String message = xin.bbtt.mcbot.Utils.toString(chatPacket.getContent());
if (message.contains("hello")) {
Bot.Instance.sendChatMessage("Hi there!"); // Send response
}
}
}
}
```


### 5.3 Configuration
Load custom configs (e.g., JSON) in `onLoad()`. The plugin directory is specified in `config.conf` under `plugin.directory`:
```java
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileReader;

@Override
public void onLoad() {
try (FileReader reader = new FileReader("plugins/myplugin/config.json")) {
JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
String apiKey = config.get("api_key").getAsString();
} catch (Exception e) {
log.error("Failed to load config", e);
}
}
```


### 5.4 Plugin Registration
To make your plugin discoverable by `PluginManager` (which uses `ServiceLoader`):
1. Create `META-INF/services` in your project's `resources` directory.
2. Add a file named `xin.bbtt.mcbot.Plugin` in this folder.
3. List your plugin class's full path (one per line):
   ```
   com.example.myplugin.MyPlugin
   ```


## 6. Deployment
1. Package the plugin as a `.jar` (ensure `META-INF/services` and configs are included).
2. Place the `.jar` in Xinbot's `plugins` directory (configured via `plugin.directory` in `config.conf`).
3. Start Xinbot with `java -jar xinbot-[version].jar`—the plugin will load automatically.


## 7. Notes
- Avoid including dependencies already used by Xinbot (e.g., `mcprotocollib`) to prevent conflicts.
- Use `slf4j` for logging instead of `System.out` for consistency.
- Clean up resources in `onDisable()` to avoid memory leaks.
- Utilize `Utils` class for text processing (e.g., `Utils.toString(Component)` for chat message conversion).

This guide covers the essentials to build and integrate plugins, expanding Xinbot's capabilities seamlessly.