# Xinbot Usage Guide

English / [简体中文](USAGE_CN.md)

This guide provides an overview of Xinbot's core concepts and a complete reference for its built-in commands.

---

## 1. Core Concepts

### Bot
The central client that connects to the Minecraft server. It handles the network session, authentication (including Microsoft official accounts), and coordinates all other systems.

### Plugin
Extensible modules that add custom logic to the bot. Xinbot uses a plugin-first architecture, allowing you to load, unload, or reload features without restarting the entire client.

### Event
Internal triggers that occur within the bot (e.g., receiving a chat message, login success). Plugins can "listen" to these events to respond automatically.

### Packet
The low-level network data units exchanged between the bot and the server. Advanced plugins can intercept raw packets for fine-grained control.

### Command
Console-based instructions used to control the bot's behavior manually. Commands support tab-completion and real-time syntax highlighting via the JLine console.

---

## 2. Command Reference

Xinbot commands can be executed directly in the console. Commands and arguments are case-insensitive.

| Command | Aliases | Usage | Description |
| :--- | :--- | :--- | :--- |
| `help` | - | `help [command]` | Shows all available commands or help for a specific one. |
| `say` | `chat` | `say <message>` | Sends a message to the server's public chat. |
| `command` | `cmd` | `cmd <command>` | Sends a command to the server (e.g., `cmd home`). |
| `pm` | `PluginManager`| `pm <sub-command>` | Manages plugins (list, load, unload, reload, etc.). |
| `plugins` | - | `plugins` | Lists all currently loaded plugins and their versions. |
| `list` | - | `list [uuid]` | Lists online players. Use `uuid` to see their IDs. |
| `disconnect` | - | `disconnect` | Disconnects the bot from the current server. |
| `stop` | - | `stop` | Stops the bot and closes the application gracefully. |
| `license` | - | `license` | Displays the GPL-3.0 license information. |

### PluginManager (pm) Sub-commands
- `pm list`: Lists all plugins.
- `pm load <file>`: Loads a plugin JAR from the configured plugin directory.
- `pm unload <name>`: Unloads a plugin.
- `pm reload <name>`: Reloads a plugin.
- `pm enable/disable <name>`: Toggles a plugin's enabled state.
- `pm re-enable <name>`: Disables and then immediately enables a plugin.

---

## 3. Advanced Features

### Tab Completion
Xinbot features a powerful JLine-based console. Pressing `Tab` will suggest command names, sub-commands, plugin names, and even server-side commands when using the `cmd` prefix.

### Syntax Highlighting
Commands in the console are highlighted in real-time. Valid commands/arguments appear in specific colors (e.g., Cyan, Blue), while unrecognized ones appear in Red, helping you avoid typos.

### Server Commands
To send a command to the Minecraft server (e.g., `/w`), do **not** type it directly into the console. Instead, use the `cmd` (or `command`) prefix:
> `cmd w <username> <message>`

---

## 4. Quick Tips

- **Owner Configuration**: Ensure the `owner` field in `config.conf` matches your Minecraft username so plugins can identify you as the administrator.
- **Server Commands**: The console automatically handles the `/` prefix for server commands when using `cmd`.
- **Exiting**: Use the `stop` command to ensure all plugins are unloaded properly before the application closes.
