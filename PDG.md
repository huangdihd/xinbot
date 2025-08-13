# Plugin Development Guide (PDG)

English / [简体中文](PDG_CN.md)  
This guide will help you develop plugins for Xinbot to extend its functionality. Plugins can add custom packet listeners, event handlers, and commands.

## 1. Add Xinbot Dependency

Xinbot is available via JitPack. Add the following to your build configuration:

### Maven

Add JitPack repository to your `pom.xml`:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Add Xinbot dependency:
```xml
<dependencies>
    <dependency>
        <groupId>com.github.huangdihd</groupId>
        <artifactId>xinbot</artifactId>
        <version>VERSION</version> <!-- Replace with latest version -->
    </dependency>
</dependencies>
```

### Gradle

Add JitPack repository to your `build.gradle`:
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

Add Xinbot dependency:
```groovy
dependencies {
    implementation 'com.github.huangdihd:xinbot:VERSION' // Replace with latest version
}
```

## 2. Basic Plugin Structure

Create a main plugin class implementing the `Plugin` interface:

```java
package com.yourpackage;

import xin.bbtt.mcbot.plugin.Plugin;

public class MyPlugin implements Plugin {

    @Override
    public void onLoad() {
        // Called when plugin is loaded
    }

    @Override
    public void onEnable() {
        // Called when plugin is enabled
        // Register listeners and commands here
    }

    @Override
    public void onDisable() {
        // Called when plugin is disabled
    }
}
```

## 3. Registering Packet Listeners

Packet listeners handle incoming and outgoing network packets:

```java
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import xin.bbtt.mcbot.Bot;

public class MyPacketListener extends SessionAdapter {
    @Override
    public void packetReceived(Session session, Packet packet) {
        // Handle incoming packets
        System.out.println("Received packet: " + packet.getClass().getSimpleName());
        }
    }
    
// Register in your plugin's onEnable()
@Override
    public void onEnable() {
    Bot.Instance.addPacketListener(new MyPacketListener());
}
```

## 4. Registering Event Listeners

Event listeners respond to internal bot events. Use actual event classes from the `xin.bbtt.mcbot.events` package.

### 4.1 Create an Event Listener Class

Implement the `Listener` interface and use the `@EventHandler` annotation for event-handling methods:

```java
import xin.bbtt.mcbot.event.Listener;
import xin.bbtt.mcbot.event.EventHandler;
import xin.bbtt.mcbot.event.EventPriority;
import xin.bbtt.mcbot.events.SystemChatMessageEvent;
import xin.bbtt.mcbot.events.LoginSuccessEvent;
import xin.bbtt.mcbot.events.AnswerQuestionEvent;

public class MyEventListener implements Listener {

    // Handle system chat messages with normal priority
    @EventHandler(priority = EventPriority.NORMAL)
    public void onSystemChatMessage(SystemChatMessageEvent event) {
        System.out.println("Received system message: " + event.getText());
        // Access event data via getter methods
        boolean isOverlay = event.isOverlay();
    }
    
    // Handle login success events
    @EventHandler
    public void onLoginSuccess(LoginSuccessEvent event) {
        System.out.println("Bot has successfully logged in!");
    }
    
    // Handle question answering events with high priority
    @EventHandler(priority = EventPriority.HIGH)
    public void onAnswerQuestion(AnswerQuestionEvent event) {
        System.out.println("Question received: " + event.getQuestion());
        // Modify the answer if needed
        event.setAnswer("Custom answer for: " + event.getQuestion());
        // Cancel default action if necessary
        // event.setDefaultActionCancelled(true);
    }
}
```

### 4.2 Register the Listener

Register your event listener in the plugin's `onEnable()` method:

```java
@Override
public void onEnable() {
    Bot.Instance.getPluginManager().registerEvents(new MyEventListener(), this);
}
```

### 4.3 Event Handling Notes

- Event methods must:
    - Be annotated with `@EventHandler`
    - Have exactly one parameter (a subclass of `Event`)
    - Be public (implicit in the example due to interface implementation)

- Available events include:
    - `SystemChatMessageEvent`: Triggered when a system chat message is received
    - `LoginSuccessEvent`: Triggered when the bot successfully logs in
    - `AnswerQuestionEvent`: Triggered when a question needs to be answered
    - `ClickJoinItemEvent`, `UseJoinItemEvent`: Related to joining actions
    - `SendCommandEvent`: Triggered when a command is sent

- Events implementing `HasDefaultAction` can have their default behavior cancelled using `setDefaultActionCancelled(true)`

## 5. Creating Commands

### 5.1 Basic Command (without tab completion)

Create a command class:
```java
import xin.bbtt.mcbot.command.Command;

public class MyCommand extends Command {
@Override
public String getName() {
return "mycommand";
}

    @Override
    public String[] getAliases() {
        return new String[]{"mc"};
    }

    @Override
    public String getDescription() {
        return "My custom command";
    }

    @Override
    public String getUsage() {
        return "mycommand <arg>";
    }
}
```

Create a command executor:
```java
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.CommandExecutor;

public class MyCommandExecutor implements CommandExecutor {
    @Override
    public void onCommand(Command command, String label, String[] args) {
        if (args.length > 0) {
            System.out.println("Received argument: " + args[0]);
        } else {
            System.out.println("No arguments provided");
        }
    }
}
```

Register the command in your plugin's `onEnable()`:
```java
@Override
public void onEnable() {
    Bot.Instance.getPluginManager().registerCommand(
        new MyCommand(),
        new MyCommandExecutor(),
        this
    );
}
```

### 5.2 Command with Tab Completion

Create a command executor with tab completion by implementing `TabExecutor`:

```java
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.TabExecutor;
import java.util.List;
import java.util.Arrays;

public class MyTabbedCommandExecutor implements TabExecutor {
@Override
public void onCommand(Command command, String label, String[] args) {
// Command logic here
}

    @Override
    public List<String> onTabComplete(Command command, String label, String[] args) {
        // Provide tab completion suggestions
        if (args.length == 1) {
            return Arrays.asList("option1", "option2", "option3");
        }
        return List.of();
    }
}
```

Register it the same way as a basic command:
```java
@Override
public void onEnable() {
    Bot.Instance.getPluginManager().registerCommand(
        new MyCommand(),
        new MyTabbedCommandExecutor(),
        this
    );
}
```

## 6. Packaging and Deployment

1. Create resource directory structure: Under `src/main/resources`, create `META-INF/services` directory;
2. In `META-INF/services` directory, create a file named `xin.bbtt.mcbot.plugin.Plugin` (the fully qualified name of the Plugin interface);
3. In this file, add the fully qualified name of your plugin implementation class (e.g., `com.yourpackage.MyPlugin`);
4. Package your plugin as a JAR file;
5. Place the JAR file in the `plugin` directory (configurable in `config.conf`);
6. Start Xinbot - your plugin will be loaded automatically.

### 6.1 META-INF/services Example

Assuming your plugin main class is `com.example.MyPlugin`:
- Create file: `src/main/resources/META-INF/services/xin.bbtt.mcbot.plugin.Plugin`
- Add this line to the file: `com.example.MyPlugin`

## 7. Notes

- Refer to the `xin.bbtt.mcbot.events` package for all available event classes
- Event priority (LOWEST, LOW, NORMAL, HIGH, HIGHEST, MONITOR) determines execution order
- The plugin directory is specified in `config.conf` under `plugin.directory`
- When a plugin links to Xinbot (GPL-3.0) and is distributed as a derivative work, the plugin must be released under a GPL-3.0-compatible license. If a plugin communicates with Xinbot as a separate process via a general-purpose protocol, it is typically not subject to this requirement. The project may specify which plugin license types it accepts

For more advanced usage, examine the existing plugin infrastructure in the Xinbot source code.