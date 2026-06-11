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

## 社区

- **QQ 群**: `434173700` — 用户与玩家交流主群
- **Telegram**: [t.me/xinbot_develop](https://t.me/xinbot_develop)
- **问题反馈**: 欢迎在 [GitHub Issues](https://github.com/huangdihd/xinbot/issues) 提交 bug 与新功能建议

---

## 许可证
GPL-3.0-or-later，详见 LICENSE。

如果你喜欢 Xinbot，欢迎点亮一个 Star！

Made with ❤️ by huangdihd
