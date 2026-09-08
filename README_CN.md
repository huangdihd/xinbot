# Xinbot

![logo](logo.png)

## 📖 官方文档: [xinbot.shouldbe.top](https://xinbot.shouldbe.top/)

<!-- Badges -->
<p>
  <a href="https://github.com/huangdihd/xinbot/releases" target="_blank">
    <img src="https://img.shields.io/github/v/release/huangdihd/xinbot?style=for-the-badge&label=Release&color=brightgreen" alt="Latest Release">
  </a>
  <a href="https://github.com/huangdihd/xinbot/issues" target="_blank">
    <img src="https://img.shields.io/github/issues/huangdihd/xinbot?style=for-the-badge&label=Issues&color=yellow" alt="Issues">
  </a>
  <a href="https://github.com/huangdihd/xinbot/blob/main/LICENSE" target="_blank">
    <img src="https://img.shields.io/github/license/huangdihd/xinbot?style=for-the-badge&label=License&color=blue" alt="License">
  </a>
  <a href="https://github.com/huangdihd/xinbot/stargazers" target="_blank">
    <img src="https://img.shields.io/github/stars/huangdihd/xinbot?style=for-the-badge&label=Stars&color=ff69b4" alt="Stars">
  </a>
  <a href="https://jitpack.io/#huangdihd/xinbot" target="_blank">
    <img src="https://img.shields.io/jitpack/version/com.github.huangdihd/xinbot?style=for-the-badge&label=JitPack&color=b22222" alt="jitpack">
  </a>
  <a href="https://github.com/huangdihd/xinbot/commits/master/">
    <img src="https://img.shields.io/github/commit-activity/w/huangdihd/xinbot?style=for-the-badge&color=purple" alt="commit activity"/>
  </a>
</p>

---

> 一个轻量、高度模块化的 Minecraft 机器人框架。

[English](README.md) / 简体中文

## 演示
[![asciicast](https://asciinema.org/a/BEV8M98rQ9oAko3d.svg)](https://asciinema.org/a/BEV8M98rQ9oAko3d)

## ⚠️ 重要提示
自 2.0.0 起，Xinbot 必须安装元插件才能启动并与服务器交互。元插件的作用是处理与特定服务器相关的交互逻辑（如登录握手、自动重连等），使核心框架保持通用性。

您可以从这里获取官方提供的2b2t.xin元插件示例：[xinMetaPlugin](https://github.com/huangdihd/xinMetaPlugin)。

## 功能特性
- 高可读日志：像官方客户端一样渲染颜色与格式。
- 正版登录：可选的正版账号登录流程。
- 元插件架构：核心交互逻辑完全剥离，通过元插件适配不同的服务器需求。
- 插件架构：类 Bukkit 事件系统，支持快速功能扩展。
- 完善的国际化：支持多国语言动态切换及引导报错。
- 遥测上报：可选的心跳与崩溃报告加密上报，内置隐私脱敏（默认关闭，首次运行询问开启）。

---

## 快速开始

请参考[文档站](https://xinbot.shouldbe.top/zh/guide/getting-started.html)

## 整合包（Modpack）

**整合包**把一组插件和语言文件打包成单个 `.zip`，便于把一套开箱即用的配置分享给他人并一键安装。整合包**不包含** `config.conf`，因此不会泄露账号密码与登录会话。

压缩包结构：

```
example-modpack.zip
├── modpack.yml          # 清单（name 与 version 必填）
├── plugins/             # 插件 jar —— 安装到插件目录
│   └── *.jar
└── lang/                # 可选的 .lang 覆盖 —— 安装到 ./lang/
    └── *.lang
```

`modpack.yml`：

```yaml
name: "2b2t.xin 生存整合包"      # 必填
version: "1.0.0"                  # 必填
author: "huangdihd"              # 可选
description: "..."               # 可选
xinbotVersion: ">=2.2.0"         # 可选，仅作展示
plugins: [PluginA, PluginB]       # 可选，仅作展示
```

命令行子命令（执行后不会启动机器人）：

```bash
java -jar xinbot.jar --install <file.zip>       # 将整合包安装到 ./plugin 与 ./lang
java -jar xinbot.jar --export <out.zip>         # 把当前插件与语言文件打包成整合包
java -jar xinbot.jar --modpack-info <file.zip>  # 打印整合包清单信息
java -jar xinbot.jar --help                     # 列出全部子命令
```

安装时会覆盖同名文件（并给出警告），且会忽略 `plugins/` 与 `lang/` 以外的所有条目。插件目录在存在 `config.conf` 时从中读取，否则使用默认的 `plugin/`。

## 遥测上报

自 2.5.0 起，Xinbot 可以将加密的心跳（每 5 分钟一次）与崩溃报告上报到你自建的遥测服务器。遥测**完全可选且默认关闭**：首次运行时机器人会询问一次（Y/N）是否开启，之后才会上报数据。在 `config.conf` 中启用：

```hocon
"telemetry" : {
    "enable" : true,      // Opt-in：开启遥测（心跳与崩溃报告）
    "mode" : "udp",       // 传输方式："udp"（默认）或 "http"
    "ip" : "127.0.0.1",   // 你的遥测服务器
    "port" : 9000,
    "key" : ""            // 部署密钥：Base64 编码的 32 字节随机数，
                           // 例如 `openssl rand -base64 32`。留空则启动时
                           // 自动向服务器获取（见下方说明）
    // 字段级隐私开关：sendBot、sendServer、sendState、
    // sendPlayers、sendUptime、sendSystem（默认均开启）
}
```

**加密** —— 每个数据包都是 "XBTL" 信封（magic + 版本 + 类型 + IV + 密文），以 AES-256-GCM 加密。6 字节头部作为 GCM AAD 与密文绑定，且 JSON 载荷的 `type` 必须与信封类型一致，因此篡改或使用错误密钥的数据包会被拒收。UDP 与 HTTP 两种传输使用相同的信封格式。

**密钥处理** —— 部署密钥必须与服务器端一致：可在 `telemetry.key` 中显式配置，也可留空让机器人在启动时按所选传输方式自动向服务器获取（UDP 密钥请求/应答报文，或 HTTP `GET /telemetry/key`）。该获取过程为明文交换，能监听到它的人即可获得密钥，因此自动获取仅建议在可信网络使用（例如机器人与服务器位于同一局域网）。无论无法获取密钥还是显式配置的密钥无效，都不会发送任何数据（fail-closed）。

**隐私脱敏** —— 字段级开关决定上报哪些数据组：`sendBot`（机器人名）、`sendServer`（服务器地址）、`sendState`（在线状态与阶段）、`sendPlayers`（玩家数）、`sendUptime`（运行时长）、`sendSystem`（JVM 堆 / 系统 / Java 版本）。崩溃报告的文本（异常消息与堆栈）在离开客户端之前即完成脱敏：连续 4 位及以上的数字（含负数，例如玩家坐标）会被替换为 `****`；配置文件中的密码（`account.password`、代理密码）会被整段替换为 `******`。

## 社区

- **QQ 群**: `434173700` — 用户与玩家交流主群
- **Telegram**: [t.me/xinbot_develop](https://t.me/xinbot_develop)
- **问题反馈**: 欢迎在 [GitHub Issues](https://github.com/huangdihd/xinbot/issues) 提交 bug 与新功能建议

---

## 许可证
GPL-3.0-or-later，详见 LICENSE。

如果你喜欢 Xinbot，欢迎点亮一个 Star！

Made with ❤️ by huangdihd
