# Xinbot
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
</p>

---

> 一个为 2b2t.xin 打造的轻量、可扩展的 Minecraft 机器人客户端——稳定、可读、易扩展。

[English](README.md) / 简体中文

## 为什么选择 Xinbot？
- 高可读日志：像官方客户端一样渲染颜色与格式。
- 正版登录更安心：可选的正版账号登录流程。
- 插件优先的架构：类 Bukkit 事件系统，快速扩展能力。
- 上手简单：单 JAR、清晰配置、开箱即用。

## 功能特性
- 彩色日志渲染 —— 丰富的颜色与样式，轻松看懂服务器消息。
- 支持正版账号 —— 可选在线模式，控制台引导登录。
- 可扩展插件系统 —— 内置插件生命周期与事件总线。
- 稳定性可调 —— 可在稳定性与资源占用间自由取舍。
- 多语言支持 —— 可选加载语言文件，提升使用体验。

---

## 快速开始

1) 下载
   前往 Releases 获取最新版：
   xinbot-[版本号].jar

2) 安装 Java
   需要 Java 17 或更高版本。

3) 配置
   在 JAR 同目录创建 config.conf（示例）：
    ```hocon
       {
        "account" : {
            "fullSession" : null,           // 由 Xinbot 自动生成；保持为空
            "name" : "[Bot name]",          // 机器人用户名
            "onlineMode" : false,           // true = 使用正版账号登录
            "password" : ""                 // 2b2t.xin 密码
        },
        "advances" : {
            "enableHighStability" : false,  // 高稳定模式（更高 CPU 占用）
            "enableJLine" : true,           // 使用 JLine 输入处理（更高内存占用）
            "enableTranslation" : true      // 加载语言文件（更高内存占用）
        },
        "owner" : "[Owner name]",           // 机器人的主人名称
        "plugin" : {
            "directory" : "plugin"          // 插件目录
        },
        "proxy" : {
            "enable" : false,               // 是否启用代理链接服务器
            "info" : {
                "address" : "",             // 代理服务器的地址
                "type" : "",                // 代理的类型(HTTP, SOCKS4, SOCKS5)
                "password" : "",            // 代理的密码
                "username" : ""             // 代理的用户名
            }
        }
    }
    ```

4) 运行
    ```bash
    # 默认配置路径：./config.conf
    java -jar xinbot-[版本号].jar [配置文件路径]
    ```

5) 正版登录（可选）
   当 onlineMode=true 且 fullSession 为空时，控制台会提示打开登录链接完成授权。

6) 开发插件
   通过插件扩展功能，详见插件开发指南（PDG_CN.md）。

---

## 常见问题
- 一定需要正版账号吗？  
  非必须；开启 onlineMode 可使用正版账号以提升兼容性与可信度。

- 我能写自己的插件吗？  
  可以。Xinbot 提供类 Bukkit 的事件系统，方便扩展。详细参考[插件开发指南](PDG_CN.md)。

- 如何管理多个机器人？  
  Xinbot 支持使用 [xinManager](https://github.com/huangdihd/xinManager) 管理多个机器人。

- 如何管理移动和地形?
  我们开发了一个叫做 [MovementSync](https://github.com/huangdihd/movementsync) 的插件来管理移动和地形。

- 如何反馈问题或提建议？  
  请在 GitHub Issues 提交，附上复现步骤更佳。亦可加入官方插件开发qq交流群:434173700

---

## 许可证
GPL-3.0-or-later，详见 LICENSE。
- 允许使用、修改与分发。
- 若分发修改版，需开源完整源码并沿用同一许可证。
- 分发时需保留版权与许可证声明。

如果你喜欢 Xinbot，欢迎点亮一个 Star！

Made with ❤️ by huangdihd
