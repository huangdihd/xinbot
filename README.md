# Xinbot

![logo](logo.png)

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

## Demonstration
[![asciicast](https://asciinema.org/a/BEV8M98rQ9oAko3d.svg)](https://asciinema.org/a/BEV8M98rQ9oAko3d)

## ⚠️ Important Note
Starting from 2.0.0, Xinbot must have a MetaPlugin installed to start and interact with the server. The purpose of a MetaPlugin is to handle server-specific interaction logic (such as login handshakes, auto-reconnect, etc.), allowing the core framework to remain generic.

You can find the official MetaPlugin implementation for 2b2t.xin here: [xinMetaPlugin](https://github.com/huangdihd/xinMetaPlugin).

## Features
- Vibrant logs: parse and render messages just like the official client.
- Secure login: use your legit Minecraft account with confidence.
- MetaPlugin Architecture: core interaction logic is decoupled, enabling support for various server types via MetaPlugins.
- Plugin architecture: extend behavior with a familiar Bukkit-style event system.
- Internationalization: support for multiple languages and bootstrap error reporting.

---

## Quick Start

Please refer to the [documentation site](https://xinbot.shouldbe.top/guide/getting-started.html).

---

## Modpacks

A **modpack** bundles a set of plugins and language files into a single `.zip` so a
ready-to-use setup can be shared and installed in one step. A modpack can optionally
also carry a bot config, but only after every secret has been stripped — see below.

Archive layout:

```
example-modpack.zip
├── modpack.yml          # manifest (name & version required)
├── plugins/             # plugin jars -> installed into the plugin directory
│   └── *.jar
├── lang/                # optional .lang overrides -> installed into ./lang/
│   └── *.lang
└── config/              # optional sanitized config (--export-config only)
    └── config.conf
```

`modpack.yml`:

```yaml
name: "2b2t.xin Survival Pack"   # required
version: "1.0.0"                  # required
author: "huangdihd"              # optional
description: "..."               # optional
xinbotVersion: ">=2.2.0"         # optional, informational
plugins: [PluginA, PluginB]       # optional, informational
```

CLI sub-commands (run instead of starting the bot):

```bash
java -jar xinbot.jar --install <file.zip>       # install a modpack into ./plugin and ./lang
java -jar xinbot.jar --export <out.zip>         # pack current plugins + lang files into a modpack
java -jar xinbot.jar --export-config <out.zip>  # ...also bundle the current config, with secrets stripped
java -jar xinbot.jar --modpack-info <file.zip>  # print a modpack's manifest
java -jar xinbot.jar --help                     # list all sub-commands
```

Installing overwrites existing files of the same name (with a warning) and ignores any
archive entry outside `plugins/`, `lang/` and `config/`. The plugin directory is read
from `config.conf` when present, otherwise the default `plugin/` is used.

### Bundling a config

`--export-config` packs the current `config.conf` into the modpack **after clearing
every secret**: the account name/password/session, the `owner`, and proxy
credentials are all blanked. Non-sensitive settings (reconnect timings, plugin
directory, translation, online mode, proxy address/type) are kept.

When such a modpack is installed and the working directory has **no** existing
`config.conf`, the bundled config is written and you are prompted to fill the blanked
fields back in (owner, bot account name/password, and proxy credentials if a proxy is
enabled). An existing `config.conf` is never overwritten.

---

## License
GPL-3.0-or-later, see LICENSE for the full text.

If you like Xinbot, a star goes a long way!

Made with ❤️ by huangdihd
