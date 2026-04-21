# Xinbot
![logo](logo.jpeg)

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

1) 下载
   获取最新版 [Xinbot Core](https://github.com/huangdihd/xinbot/releases) 及适配您目标服务器的 [元插件](https://github.com/huangdihd/xinMetaPlugin)。

2) 安装
   将元插件放入 plugin 目录。需要 Java 17 或更高版本。

3) 配置
   创建 config.conf，示例配置如下：

    ```hocon
       {
        "account" : {
            "fullSession" : null,
            "name" : "[Bot name]",
            "onlineMode" : false,
            "password" : ""                 // 机器人登录服务器的密码
        },
        "enableTranslation" : true,
        "reconnectTimeout" : 5000,
        "reconnectDelay" : 3000,
        "owner" : "[Owner name]",
        "plugin" : {
            "directory" : "plugin"
        },
        "proxy" : {
            "enable" : false,
            "info" : {
                "address" : "",
                "type" : "SOCKS5",
                "password" : "",
                "username" : ""
            }
        }
    }
    ```

4) 运行
    ```bash
    java -jar xinbot-[版本号].jar
    ```

---

## 许可证
GPL-3.0-or-later，详见 LICENSE。

如果你喜欢 Xinbot，欢迎点亮一个 Star！

Made with ❤️ by huangdihd
