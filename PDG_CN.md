# Xinbot 插件开发指南

[English](PDG.md) / 简体中文

## 1. 概述
本指南帮助开发者为 Xinbot 创建插件，以扩展其功能。插件通过实现 `Plugin` 接口和事件监听器与机器人交互，从而为游戏内事件添加自定义逻辑。

## 2. 环境搭建
- **Java**：JDK 17 或更高版本（与 Xinbot 运行环境一致）。
- **依赖**：引入 Xinbot 核心类库，以及 `mcprotocollib`（Minecraft 协议处理）和 `slf4j`（日志）。
- **IDE**：推荐使用带 Maven 支持的 IntelliJ IDEA 或 Eclipse。

## 3. 插件基础
### 3.1 核心接口：`Plugin`
所有插件必须实现 `xin.bbtt.mcbot.Plugin` 接口，该接口定义了插件生命周期方法：
```java
public interface Plugin {
    default String getName() { return this.getClass().getSimpleName(); }
    void onLoad();   // 插件加载时调用（初始化配置）
    void onEnable(); // 插件启用时调用（注册监听器）
    void onDisable();// 插件禁用时调用（释放资源）
}
```

## 4. 插件生命周期

由 `PluginManager` 管理：

1. **加载阶段**：`onLoad()` 在 Xinbot 扫描 `plugins` 目录时调用（目录位置由 `config.conf` 中的 `plugin.directory` 配置）。适合进行轻量级初始化（如加载配置文件）。

2. **启用阶段**：`onEnable()` 在所有插件加载完成后调用。在此阶段注册事件监听器或启动主要逻辑。

3. **禁用阶段**：`onDisable()` 在机器人关闭时调用。应在此释放资源（如关闭文件、移除监听器等）。


## 5. 核心开发步骤

### 5.1 创建插件类

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
        log.info("{} loaded", getName());
    }

    @Override
    public void onEnable() {
        log.info("{} enabled", getName());
        Bot.Instance.addListener(new MyChatListener());
    }

    @Override
    public void onDisable() {
        log.info("{} disabled", getName());
    }
}
```

### 5.2 事件处理

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
                Bot.Instance.sendChatMessage("Hi there!"); // 发送回复
            }
        }
    }
}
```

### 5.3 配置文件加载

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

### 5.4 插件注册

1. 在项目的 `resources` 目录下创建 `META-INF/services` 文件夹。

2. 在该文件夹中新建文件 `xin.bbtt.mcbot.Plugin`。

3. 在文件中写入插件类的完整路径（每行一个）：


```
com.example.myplugin.MyPlugin
```

## 6. 部署

1. 将插件打包成 `.jar`（确保 `META-INF/services` 和配置文件被包含）。

2. 将 `.jar` 放入 Xinbot 的插件目录。

3. 启动 Xinbot，插件会自动加载。


## 7. 注意事项

* 不要重复打包 Xinbot 已有的依赖（如 `mcprotocollib`），以免冲突。

* 使用 `slf4j` 进行日志记录，不要用 `System.out`。

* 在 `onDisable()` 中释放资源，避免内存泄漏。

* 使用 `Utils` 工具类进行文本处理。
    