# 插件开发指南（PDG）

[English](PDG.md) / 简体中文  
本指南将帮助开发者为 Xinbot 开发插件，以扩展其功能。插件可以添加自定义的数据包监听器、事件处理器以及命令。

# 1. 添加 Xinbot 依赖

Xinbot 通过 JitPack 提供。将以下配置加入构建文件。

## Maven

在 `pom.xml` 中添加 JitPack 仓库：
```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```

添加 Xinbot 依赖：
```xml
<dependencies>
  <dependency>
    <groupId>com.github.huangdihd</groupId>
    <artifactId>xinbot</artifactId>
    <version>VERSION</version> <!-- 替换为最新版本 -->
  </dependency>
</dependencies>
```

## Gradle

在 `build.gradle` 中添加 JitPack 仓库：
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

添加 Xinbot 依赖：
```groovy
dependencies {
    implementation 'com.github.huangdihd:xinbot:VERSION' // 替换为最新版本
}
```

# 2. 基础插件结构

创建一个实现 `Plugin` 接口的主插件类：

```java
package com.yourpackage;

import xin.bbtt.mcbot.plugin.Plugin;

public class MyPlugin implements Plugin {
    
    @Override
    public String getName() {
        return "MyPlugin";
        // 返回插件名称
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
        // 返回插件版本
    }

    @Override
    public void onLoad() {
        // 插件被加载时调用
    }
    
    @Override
    public void onUnload() {
        // 插件被卸载时调用
    }

    @Override
    public void onEnable() {
        // 插件被启用时调用
        // 在这里注册监听器与命令
    }

    @Override
    public void onDisable() {
        // 插件被禁用时调用
    }
}
```

# 3. 注册数据包监听器（Packet Listener）

数据包监听器用于处理收发的网络数据包：

```java
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import xin.bbtt.mcbot.Bot;

public class MyPacketListener extends SessionAdapter {
    @Override
    public void packetReceived(Session session, Packet packet) {
        // 处理收到的数据包
        System.out.println("Received packet: " + packet.getClass().getSimpleName());
    }
}

// 在插件的 onEnable() 中注册
@Override
public void onEnable() {
    Bot.Instance.addPacketListener(new MyPacketListener(), this);
}
```

# 4. 注册事件监听器

事件监听器用于响应内部 Bot 事件。请使用 `xin.bbtt.mcbot.events` 包中的实际事件类。

## 4.1 创建事件监听器类

实现 `Listener` 接口，并在事件处理方法上使用 `@EventHandler` 注解：

```java
import xin.bbtt.mcbot.event.Listener;
import xin.bbtt.mcbot.event.EventHandler;
import xin.bbtt.mcbot.event.EventPriority;
import xin.bbtt.mcbot.events.SystemChatMessageEvent;
import xin.bbtt.mcbot.events.LoginSuccessEvent;
import xin.bbtt.mcbot.events.AnswerQuestionEvent;

public class MyEventListener implements Listener {

    // 以 NORMAL 优先级处理系统聊天消息
    @EventHandler(priority = EventPriority.NORMAL)
    public void onSystemChatMessage(SystemChatMessageEvent event) {
        System.out.println("Received system message: " + event.getText());
        // 通过 getter 访问事件数据
        boolean isOverlay = event.isOverlay();
    }
    
    // 处理登录成功事件
    @EventHandler
    public void onLoginSuccess(LoginSuccessEvent event) {
        System.out.println("Bot has successfully logged in!");
    }
    
    // 以 HIGH 优先级处理问答事件
    @EventHandler(priority = EventPriority.HIGH)
    public void onAnswerQuestion(AnswerQuestionEvent event) {
        System.out.println("Question received: " + event.getQuestion());
        // 如有需要可修改答案
        event.setAnswer("Custom answer for: " + event.getQuestion());
        // 必要时可取消默认行为
        // event.setDefaultActionCancelled(true);
    }
}
```

## 4.2 注册事件监听器

在插件的 `onEnable()` 方法中注册事件监听器：

```java
@Override
public void onEnable() {
    Bot.Instance.getPluginManager().registerEvents(new MyEventListener(), this);
}
```

## 4.3 事件处理注意事项

- 事件处理方法必须：
    - 使用 `@EventHandler` 注解；
    - 恰好接收一个参数（`Event` 的子类）；
    - 为 `public`（示例中因接口实现已隐含）。

- 可用事件示例：
    - `SystemChatMessageEvent`：收到系统聊天消息时触发
    - `LoginSuccessEvent`：Bot 成功登录时触发
    - `AnswerQuestionEvent`：需要回答问题时触发
    - `ClickJoinItemEvent`、`UseJoinItemEvent`：与加入操作相关
    - `SendCommandEvent`：发送命令时触发

- 实现了 `HasDefaultAction` 的事件可通过 `setDefaultActionCancelled(true)` 取消默认行为。

# 5. 创建命令

## 5.1 基础命令（无 Tab 补全）

创建命令类：
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

创建命令执行器：
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

在 `onEnable()` 中注册命令：
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

## 5.2 带 Tab 补全的命令

实现 `TabExecutor`，为命令提供补全建议：

```java
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.TabExecutor;
import java.util.List;
import java.util.Arrays;

public class MyTabbedCommandExecutor implements TabExecutor {
    @Override
    public void onCommand(Command command, String label, String[] args) {
        // 在这里编写命令逻辑
    }

    @Override
    public List<String> onTabComplete(Command command, String label, String[] args) {
        // 提供 Tab 补全项
        if (args.length == 1) {
            return Arrays.asList("option1", "option2", "option3");
        }
        return List.of();
    }
}
```

注册方式与基础命令相同：
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

# 6. 打包与部署

1. 创建资源目录结构：在 `src/main/resources` 下创建 `META-INF/services` 目录；
2. 在 `META-INF/services` 目录中创建文件，文件名为 `xin.bbtt.mcbot.plugin.Plugin`（即 Plugin 接口的全限定名）；
3. 在该文件中添加插件实现类的全限定名（例如 `com.yourpackage.MyPlugin`）；
4. 将插件打包为 JAR 文件；
5. 把 JAR 放到 `plugin` 目录（可在 `config.conf` 中配置）；
6. 启动 Xinbot —— 插件会被自动加载。

### 6.1 META-INF/services 示例

假设你的插件主类为 `com.example.MyPlugin`，则需要：
- 创建文件：`src/main/resources/META-INF/services/xin.bbtt.mcbot.plugin.Plugin`
- 在文件中添加一行：`com.example.MyPlugin`

# 7. 备注

- 可在 `xin.bbtt.mcbot.events` 包中查看所有可用事件类；
- 事件优先级（`LOWEST, LOW, NORMAL, HIGH, HIGHEST, MONITOR`）决定执行顺序；
- 插件目录通过 `config.conf` 中的 `plugin.directory` 指定；
- 当插件与 Xinbot（GPL-3.0）进行链接并作为其派生作品分发时，插件需要在 GPL-3.0 兼容许可下发布。若插件以独立进程方式通过通用协议与 Xinbot 通信，通常不受此限制。项目可自行规定接受的插件许可类型。

如需更深入的高级用法，建议阅读 Xinbot 源码中的现有插件基础设施。
