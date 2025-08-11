# **Xinbot**
<!-- Badges -->
<p >
  <a href="https://github.com/huangdihd/xinbot/releases">
    <img src="https://img.shields.io/github/v/release/huangdihd/xinbot?style=for-the-badge&label=Release&color=brightgreen" alt="Latest Release">
  </a>
  <a href="https://github.com/huangdihd/xinbot/issues">
    <img src="https://img.shields.io/github/issues/huangdihd/xinbot?style=for-the-badge&label=Issues&color=yellow" alt="Issues">
  </a>
  <a href="https://github.com/huangdihd/xinbot/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/huangdihd/xinbot?style=for-the-badge&label=License&color=blue" alt="License">
  </a>
  <a href="https://github.com/huangdihd/xinbot/stargazers">
    <img src="https://img.shields.io/github/stars/huangdihd/xinbot?style=for-the-badge&label=Stars&color=ff69b4" alt="Stars">
  </a>
</p>

---

### *一个适用于 2b2t.xin 的 Minecraft 机器人客户端*

[English](README.md) / 简体中文

## **功能特点**

### 1. **彩色日志渲染**
强大的消息解析器，可以像官方 Minecraft 客户端一样渲染出色彩鲜艳的日志信息。

### 2. **支持正版账号**
支持使用正版 Minecraft 账号登录，提升安全性。

### 3. **可扩展插件系统**
内置插件框架，采用 Bukkit 风格的事件系统，方便开发插件。

---

## **快速开始**

### 1. **下载**
获取最新版：  
`xinbot-[版本号].jar`

### 2. **安装 Java**
需要 Java 17 或更高版本。

### 3. **创建配置文件**
示例 `config.conf`：
```hocon
account {
    fullSession=""         // 由 Xinbot 自动生成；保持为空
    name="[机器人名称]"      // 机器人用户名
    onlineMode=false       // true = 使用正版账号登录
    password=""            // 2b2t.xin 密码
}
advances {
    enableHighStability=false  // 高稳定模式（更高 CPU 占用）
    enableJLine=true           // 使用 JLine 输入处理（更高内存占用）
    enableTranslation=true     // 加载语言文件（更高内存占用）
}
owner="[主人名称]"       // 机器人的主人名称
plugin {
    directory=plugin        // 插件目录
}
```

### 4. **运行 Xinbot**

```bash
java -jar xinbot-[版本号].jar [配置文件路径，默认: config.conf]
```

### 5. **正版账号登录**

如果 `onlineMode=true` 且 `fullSession` 为空，Xinbot 会启动正版登录流程。  
根据控制台提示，打开登录链接并完成账号登录。

### 6. **插件开发**

通过开发插件为机器人添加自定义功能。  
详见 [插件开发指南](PDG.md)。