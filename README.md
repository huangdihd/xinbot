# Xinbot
![logo](logo.jpeg)

## 📖 Official Documentation: [xinbot.shouldbe.top](https://xinbot.shouldbe.top/)

<!-- Badges -->
<p>
  <a href="https://github.com/huangdihd/xinbot/releases" target="_blank">
    <img src="https://img.shields.io/github/v/release/huangdihd/xinbot?style=for-the-badge&label=Release&color=brightgreen" alt="Latest Release">
  </a>
  <a href="https://github.com/huangdihd/xinbot/issues" target="_blank">
    <img src="https://img.shields.io/github/issues/huangdihd/xinbot?style=for-the-badge&label=Issues&color=yellow" alt="Issues">
  </a>
  <a href="https://github.com/huangdihd/xinbot/blob/master/LICENSE" target="_blank">
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

> A lightweight, highly modular Minecraft bot framework.

English / [简体中文](README_CN.md)

## ⚠️ Important Note
Starting from 2.0.0, Xinbot must have a MetaPlugin installed to start and interact with the server. The purpose of a MetaPlugin is to handle server-specific interaction logic (such as login handshakes, auto-reconnect, etc.), allowing the core framework to remain generic.

You can find the official MetaPlugin implementation here: [xinMetaPlugin](https://github.com/huangdihd/xinMetaPlugin).

## Features
- Vibrant logs: parse and render messages just like the official client.
- Secure login: use your legit Minecraft account with confidence.
- MetaPlugin Architecture: core interaction logic is decoupled, enabling support for various server types via MetaPlugins.
- Plugin architecture: extend behavior with a familiar Bukkit-style event system.
- Internationalization: support for multiple languages and bootstrap error reporting.

---

## Quick Start

1) Download
   Get the latest [Xinbot Core](https://github.com/huangdihd/xinbot/releases) and a [MetaPlugin](https://github.com/huangdihd/xinMetaPlugin) for your target server.

2) Install
   Put the MetaPlugin into the plugin directory. Java 17 or newer is required.

3) Configure
   Create config.conf, example below:

    ```hocon
    {
        "account" : {
            "fullSession" : null,
            "name" : "[Bot name]",
            "onlineMode" : false,
            "password" : ""                 // Bot's login password for the server
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

4) Run
    ```bash
    java -jar xinbot-[version].jar
    ```

---

## License
GPL-3.0-or-later, see LICENSE for the full text.

If you like Xinbot, a star goes a long way!

Made with ❤️ by huangdihd
