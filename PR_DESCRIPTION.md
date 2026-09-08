# Xinbot PR 草稿(标题与描述)

> 使用说明:下方标题与描述可直接复制到 GitHub PR 创建页
> (https://github.com/huangdihd/xinbot/compare/master...2698269088:telemetry-reporting?expand=1)。
> 本文件仅为草稿,复制完成后请删除或不要提交,避免混入 PR 内容。

---

## PR 标题(英文,与上游提交风格一致,推荐)

```
Add encrypted telemetry reporting and fix startup console mojibake
```

## PR 标题(中文备选)

```
新增加密遥测上报功能并修复启动控制台中文乱码
```

---

## PR 描述

```
## Summary

This PR adds optional encrypted telemetry reporting to the bot and fixes two
Windows-console startup issues:

- New TelemetryManager sends AES-256-GCM encrypted heartbeats (every 5 min)
  and crash reports from the client, over UDP or HTTP, driven by the new
  `telemetry` section in config.conf. Telemetry is opt-in (default off) and
  fails closed without the deployment-specific `telemetry.key` (Base64 of 32
  random bytes) shared with the telemetry server. The envelope format is a
  fixed "XBTL" frame (magic + version + type + IV + ciphertext); the header
  bytes are bound as GCM AAD, so tampering with the type byte is rejected and
  the payload JSON type must match the envelope type.
- CJK log output was garbled on Windows GBK consoles before JLine was
  ready (logback fell back to System.out and wrote raw UTF-8 bytes).
  In interactive mode JLine is now initialized before anything can log, so
  all messages go through the console-safe lineReader.printAbove() path;
  sub-commands and CI runs never build a terminal eagerly.
- A missing or corrupted lang.json (e.g. an un-downloaded Git LFS
  placeholder when building from a source ZIP) no longer aborts startup;
  it degrades to a warning and translation stays unavailable.

Version bumped from 2.4.2-RELEASE to 2.5.0-RELEASE.

## Changes

- New: src/main/java/xin/bbtt/mcbot/telemetry/TelemetryManager.java
  (XBTL envelope, AES-256-GCM, header-as-AAD, UDP/HTTP transmit modes)
- New: scripts/telemetry-grabber.py (UDP packet sniffer helper for debugging)
- New: src/test/java/xin/bbtt/mcbot/telemetry/TelemetryTest.java
  (round-trip, wrong-key, type-byte mutation and tamper detection)
- New: src/test/java/xin/bbtt/mcbot/telemetry/InteropVectorTest.java
  (writes real envelopes for the telemetry server's ClientInteropTest)
- Config: telemetry { enable, mode, ip, port, key } in BotConfigData / config.conf
- Fix: Xinbot.java initializes CLI (JLine) before any logging in interactive mode
- Fix: LangManager.loadFromJson() catches broken JSON and continues
- i18n: new keys in en_us/zh_cn/zh_tw .lang files
- .gitignore: exclude local XinBotTelemetry project folder

## Compatibility

- Fully optional: telemetry only transmits when config.conf sets
  telemetry.enable = true (default false).
- No protocol or config breakage for existing users.

## Testing

- mvn test (TelemetryTest covers envelope round-trip and tamper rejection)
- Manual: run with telemetry.enable=true, mode=udp / mode=http, observe
  heartbeats on the server; verify console logs show correct CJK text
  from the very first line on a Windows GBK console.

## Notes

- The telemetry server is a separate repository and is intentionally not
  part of this PR.
- lang.json is tracked via Git LFS (~71 MB); when building from a source
  ZIP, run `git lfs pull` (or disable enableTranslation) so the real file
  is present.
```

---

## PR 描述(中文版,可选)

```
## 摘要

本 PR 为 BOT 新增可选加密遥测上报,并修复两个 Windows 控制台启动问题:

- 新增 TelemetryManager:通过 UDP 或 HTTP 发送 AES-256-GCM 加密心跳(每 5 分钟)
  与崩溃报告;由 config.conf 新增的 telemetry 段控制。遥测为 opt-in(默认关闭),
  未配置与服务端共享的部署密钥 telemetry.key(Base64 编码 32 字节)时 fail-closed。
  信封格式为固定 "XBTL" 帧(magic + 版本 + 类型 + IV + 密文),头部字节作为 GCM
  AAD 绑定,篡改类型字节会被拒绝,负载 JSON 的 type 也必须与信封类型一致。
- 修复 Windows GBK 控制台上 JLine 就绪前的中文乱码(logback 回退到
  System.out 直接输出 UTF-8 字节)。现在 JLine 在任何日志输出前初始化,
  全部消息经控制台安全的 lineReader.printAbove() 通道输出。
- lang.json 缺失或损坏(例如从源码 ZIP 构建时未下载的 Git LFS 占位文件)
  不再导致启动崩溃,降级为警告并跳过翻译加载。

版本号由 2.4.2-RELEASE 升级为 2.5.0-RELEASE。

## 变更内容

- 新增:src/main/java/xin/bbtt/mcbot/telemetry/TelemetryManager.java
  (XBTL 信封、AES-256-GCM、头部 AAD、UDP/HTTP 双传输模式)
- 新增:scripts/telemetry-grabber.py(UDP 抓包辅助调试脚本,支持 --key)
- 新增:src/test/java/xin/bbtt/mcbot/telemetry/TelemetryTest.java
  (往返、错误密钥、类型字节篡改与密文篡改检测)
- 新增:src/test/java/xin/bbtt/mcbot/telemetry/InteropVectorTest.java
  (为遥测服务端 ClientInteropTest 刷新真实信封向量)
- 配置:BotConfigData / config.conf 增加 telemetry { enable, mode, ip, port, key }
- 修复:Xinbot.java 在交互模式下于任何日志输出前初始化 CLI(JLine)
- 修复:LangManager.loadFromJson() 捕获损坏 JSON 后继续运行
- i18n:en_us/zh_cn/zh_tw .lang 新增若干 key
- .gitignore:排除本地独立项目 XinBotTelemetry

## 兼容性

- 完全可选:仅当 config.conf 中 telemetry.enable = true 时才会发送(默认 false)。
- 对现有用户无协议或配置破坏。

## 测试

- mvn test(TelemetryTest 覆盖信封往返与篡改拒绝)
- 手动:telemetry.enable=true、mode=udp / mode=http 验证服务端收到心跳;
  在 Windows GBK 控制台确认日志从第一行起中文即正常。

## 备注

- 遥测服务端为独立仓库,有意不包含在本 PR 中。
- lang.json 由 Git LFS 跟踪(约 71 MB);从源码 ZIP 构建时需 `git lfs pull`
  (或关闭 enableTranslation)以确保真实文件存在。
```
