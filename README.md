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
[![asciicast](https://asciinema.org/a/AerrrnFkYGqLboa0.svg)](https://asciinema.org/a/AerrrnFkYGqLboa0)

## ⚠️ Important Note
Starting from 2.0.0, Xinbot must have a MetaPlugin installed to start and interact with the server. The purpose of a MetaPlugin is to handle server-specific interaction logic (such as login handshakes, auto-reconnect, etc.), allowing the core framework to remain generic.

You can find the official MetaPlugin implementation for 2b2t.xin here: [xinMetaPlugin](https://github.com/huangdihd/xinMetaPlugin).

## Features
- Vibrant logs: parse and render messages just like the official client.
- Secure login: use your legit Minecraft account with confidence.
- MetaPlugin Architecture: core interaction logic is decoupled, enabling support for various server types via MetaPlugins.
- Plugin architecture: extend behavior with a familiar Bukkit-style event system.
- Internationalization: support for multiple languages and bootstrap error reporting.
- Telemetry: optional AES-256-GCM encrypted heartbeat & crash reporting with privacy redaction (opt-in, off by default).

---

## Quick Start

Please refer to the [documentation site](https://xinbot.shouldbe.top/guide/getting-started.html).

---

## Modpacks

A **modpack** bundles a set of plugins and language files into a single `.zip` so a
ready-to-use setup can be shared and installed in one step. A modpack never contains
`config.conf`, so account credentials and sessions are never shipped.

Archive layout:

```
example-modpack.zip
├── modpack.yml          # manifest (name & version required)
├── plugins/             # plugin jars -> installed into the plugin directory
│   └── *.jar
└── lang/                # optional .lang overrides -> installed into ./lang/
    └── *.lang
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
java -jar xinbot.jar --modpack-info <file.zip>  # print a modpack's manifest
java -jar xinbot.jar --help                     # list all sub-commands
```

Installing overwrites existing files of the same name (with a warning) and ignores any
archive entry outside `plugins/` and `lang/`. The plugin directory is read from
`config.conf` when present, otherwise the default `plugin/` is used.

---

## Telemetry Reporting

Since 2.5.0, Xinbot can report encrypted heartbeats (every 5 minutes) and crash
reports to your own telemetry server. Telemetry is **fully optional and opt-in**:
the config default is off, and on first run the bot asks once (Y/N) before any
data is transmitted. Enable it in `config.conf`:

```hocon
"telemetry" : {
    "enable" : true,      // Opt-in: enable telemetry (heartbeat & crash reports)
    "mode" : "udp",       // Transport mode: "udp" (default) or "http"
    "ip" : "127.0.0.1",   // Your telemetry server
    "port" : 9000,
    "key" : ""            // Deployment secret: Base64 of 32 random bytes,
                           // e.g. `openssl rand -base64 32`. Must match the server.
    // Per-field privacy switches: sendBot, sendServer, sendState,
    // sendPlayers, sendUptime, sendSystem (all default true)
}
```

**Encryption** — every packet is an "XBTL" envelope (magic + version + type +
IV + ciphertext) encrypted with AES-256-GCM under the deployment key above,
which must match the key on your server. The 6-byte header is bound to the
ciphertext as GCM AAD and the JSON payload `type` must match the envelope type,
so tampered or wrong-key packets are rejected. Without a configured key nothing
is sent (fails closed). UDP and HTTP transports carry the same envelope.

**Privacy** — per-field switches decide which payload groups are reported:
`sendBot` (bot name), `sendServer` (server address), `sendState` (online
status & stage), `sendPlayers` (player count), `sendUptime` (uptime), and
`sendSystem` (JVM heap / OS / Java version). Crash report text (exception
message and stack trace) is redacted before it leaves the client: any run of
4+ digits, positive or negative (e.g. player coordinates), becomes `****`, and
passwords from the config (`account.password`, proxy password) become `******`.

---

## Community

- **QQ Group**: `434173700` — main community for users and players
- **Telegram**: [t.me/xinbot_develop](https://t.me/xinbot_develop)
- **Issues**: bug reports and feature requests are welcome on [GitHub Issues](https://github.com/huangdihd/xinbot/issues)

---

## License
GPL-3.0-or-later, see LICENSE for the full text.

If you like Xinbot, a star goes a long way!

Made with ❤️ by huangdihd
