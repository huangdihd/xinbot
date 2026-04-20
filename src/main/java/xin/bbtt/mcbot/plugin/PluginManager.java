/*
 *   Copyright (C) 2024-2026 huangdihd
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xin.bbtt.mcbot.plugin;

import lombok.Getter;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.LangManager;
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.CommandExecutor;
import xin.bbtt.mcbot.command.CommandManager;
import xin.bbtt.mcbot.event.EventManager;
import xin.bbtt.mcbot.event.Listener;
import xin.bbtt.mcbot.events.DisablePluginEvent;

import java.util.*;
import java.io.*;
import java.net.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

// Plugin Manager
public class PluginManager {
    private final Map<String, RegisteredPlugin> plugins = new HashMap<>();
    private final Map<String, RegisteredPlugin> enabledPlugins = new HashMap<>();
    private final Map<String, List<SessionListener>> sessionListeners = new HashMap<>();
    @Getter
    private final Map<String, List<String>> pluginDependencies = new HashMap<>();
    private final Map<String, PluginClassLoader> pluginLoaders = new HashMap<>();

    private static final Logger log = LoggerFactory.getLogger(PluginManager.class.getSimpleName());

    // Event manager
    private final EventManager eventManager = new EventManager();
    // Command manager
    private final CommandManager commandManager = new CommandManager();

    public EventManager events() { return eventManager; }

    public CommandManager commands() { return commandManager; }

    public void registerEvents(Listener listener, Plugin plugin) {
        eventManager.registerEvents(listener, plugin);
    }
    public void registerCommand(Command command, CommandExecutor executor, Plugin plugin) {
        commandManager.registerCommand(command, executor, plugin);
    }

    public PluginClassLoader getPluginLoader(String name) {
        return pluginLoaders.get(name);
    }

    public long countMetaPlugins() {
        return plugins.values().stream().filter(rp -> rp instanceof RegisteredMetaPlugin).count();
    }

    public RegisteredMetaPlugin getMetaPlugin() {
        return (RegisteredMetaPlugin) plugins.values().stream()
                .filter(rp -> rp instanceof RegisteredMetaPlugin)
                .findFirst()
                .orElse(null);
    }

    public String getPluginName(Plugin plugin) {
        if (plugin == null) return "Core";
        for (RegisteredPlugin rp : plugins.values()) {
            if (rp.getPlugin() == plugin) {
                return rp.getName();
            }
        }
        return plugin.getClass().getSimpleName();
    }

    public void loadPlugin(Plugin plugin) {
        String name = plugin.getClass().getSimpleName();
        String version = plugin.getClass().getPackage().getImplementationVersion();
        if (version == null) version = "1.0.0";
        
        RegisteredPlugin rp;
        if (plugin instanceof MetaPlugin) {
            rp = new RegisteredMetaPlugin(name, version, plugin.getClass().getName(), new ArrayList<>(), null, null, (MetaPlugin) plugin);
        } else {
            rp = new RegisteredPlugin(name, version, plugin.getClass().getName(), new ArrayList<>(), null, null, plugin, PluginType.PLUGIN);
        }
        loadPlugin(rp);
    }

    public void loadPlugin(RegisteredPlugin rp) {
        if (rp instanceof RegisteredMetaPlugin && Bot.Instance.isRunning()) {
            log.error(LangManager.get("xinbot.metaplugin.error.load_runtime"));
            return;
        }
        plugins.put(rp.getName(), rp);
        rp.getPlugin().onLoad();
        if (Bot.Instance.getSession() != null) {
            enablePlugin(rp);
        }
        String key = rp instanceof RegisteredMetaPlugin ? "xinbot.metaplugin.loaded" : "xinbot.plugin.loaded";
        log.info(LangManager.get(key, rp.getName()));
    }

    static class PluginInfo {
        File file;
        String name;
        String mainClass;
        String version;
        PluginType type;
        List<String> depends = new ArrayList<>();
        URL url;
    }

    public void loadPlugin(File pluginFile) throws Exception {
        URL url = pluginFile.toURI().toURL();
        PluginInfo info = loadPluginYaml(pluginFile);
        if (info == null) {
            throw new Exception("Missing or invalid plugin.yml in " + pluginFile.getName());
        }
        
        if (plugins.containsKey(info.name)) return;

        PluginClassLoader pluginClassLoader = new PluginClassLoader(new URL[]{url}, PluginManager.class.getClassLoader());
        pluginLoaders.put(info.name, pluginClassLoader);
        pluginDependencies.put(info.name, info.depends);
        
        Class<?> clazz = Class.forName(info.mainClass, true, pluginClassLoader);
        Plugin plugin = (Plugin) clazz.getDeclaredConstructor().newInstance();
        
        RegisteredPlugin rp;
        if (info.type == PluginType.META_PLUGIN && plugin instanceof MetaPlugin) {
            rp = new RegisteredMetaPlugin(info.name, info.version, info.mainClass, info.depends, info.file, url, (MetaPlugin) plugin);
        } else {
            rp = new RegisteredPlugin(info.name, info.version, info.mainClass, info.depends, info.file, url, plugin, PluginType.PLUGIN);
        }
        loadPlugin(rp);
    }

    public void loadPlugins(String pluginsDirectory) {
        File pluginsDir = new File(pluginsDirectory);
        if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
            log.error(LangManager.get("xinbot.plugin.dir.invalid", pluginsDirectory));
            return;
        }

        File[] files = pluginsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null || files.length == 0) {
            log.info(LangManager.get("xinbot.plugin.not.found"));
            return;
        }

        Map<String, PluginInfo> infoMap = new HashMap<>();

        for (File file : files) {
            try {
                PluginInfo info = loadPluginYaml(file);
                if (info == null) {
                    log.error("Failed to load plugin from {}: Missing or invalid plugin.yml", file.getName());
                    continue;
                }
                info.file = file;
                info.url = file.toURI().toURL();
                infoMap.put(info.name, info);
            } catch (Exception e) {
                log.error(LangManager.get("xinbot.plugin.load.failed", file.getName()), e);
            }
        }

        List<PluginInfo> sortedInfos = sortPluginInfosTopologically(infoMap);

        for (PluginInfo info : sortedInfos) {
            log.info(LangManager.get("xinbot.plugin.loading", info.name));
            try {
                instantiateAndLoad(info);
            } catch (Exception e) {
                log.error(LangManager.get("xinbot.plugin.load.smoothly.failed", info.name), e);
            }
        }
    }

    private void instantiateAndLoad(PluginInfo info) throws Exception {
        ClassLoader parent = PluginManager.class.getClassLoader();
        if (!info.depends.isEmpty()) {
            PluginClassLoader firstDepLoader = pluginLoaders.get(info.depends.get(0));
            if (firstDepLoader != null) {
                parent = firstDepLoader;
            }
        }

        PluginClassLoader pluginClassLoader = new PluginClassLoader(new URL[]{info.url}, parent);
        
        for (int i = 1; i < info.depends.size(); i++) {
            PluginClassLoader depLoader = pluginLoaders.get(info.depends.get(i));
            if (depLoader != null) {
                pluginClassLoader.addDependency(depLoader);
            }
        }

        pluginLoaders.put(info.name, pluginClassLoader);
        this.pluginDependencies.put(info.name, info.depends);
        
        Class<?> clazz = Class.forName(info.mainClass, true, pluginClassLoader);
        Plugin plugin = (Plugin) clazz.getDeclaredConstructor().newInstance();
        
        if (plugins.containsKey(info.name)) {
            pluginClassLoader.close();
            return;
        }
        
        RegisteredPlugin rp;
        if (info.type == PluginType.META_PLUGIN && plugin instanceof MetaPlugin) {
            rp = new RegisteredMetaPlugin(info.name, info.version, info.mainClass, info.depends, info.file, info.url, (MetaPlugin) plugin);
        } else {
            rp = new RegisteredPlugin(info.name, info.version, info.mainClass, info.depends, info.file, info.url, plugin, PluginType.PLUGIN);
        }
        loadPlugin(rp);
    }

    private PluginInfo loadPluginYaml(File file) throws IOException {
        try (JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("plugin.yml");
            if (entry == null) return null;
            
            try (InputStream is = jar.getInputStream(entry)) {
                Map<String, Object> map = new Yaml().load(is);
                if (map == null || !map.containsKey("name") || !map.containsKey("main")) return null;
                
                PluginInfo info = new PluginInfo();
                info.name = String.valueOf(map.get("name"));
                info.mainClass = String.valueOf(map.get("main"));
                info.version = String.valueOf(map.getOrDefault("version", "1.0.0"));
                info.type = PluginType.valueOf(String.valueOf(map.getOrDefault("type", "PLUGIN")).toUpperCase());
                
                parseDepends(map, info.depends);
                return info;
            }
        }
    }

    private void parseDepends(Map<String, Object> map, List<String> target) {
        Object dependObj = map.getOrDefault("depend", map.get("depends"));
        if (dependObj instanceof List) {
            for (Object dep : (List<?>) dependObj) target.add(String.valueOf(dep));
        } else if (dependObj instanceof String) {
            target.add((String) dependObj);
        }
    }

    private List<PluginInfo> sortPluginInfosTopologically(Map<String, PluginInfo> infoMap) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();

        for (String name : infoMap.keySet()) {
            inDegree.put(name, 0);
            dependents.put(name, new ArrayList<>());
        }

        for (PluginInfo info : infoMap.values()) {
            for (String dep : info.depends) {
                if (!infoMap.containsKey(dep) && !plugins.containsKey(dep)) {
                    log.error(LangManager.get("xinbot.plugin.dependency.missing", dep, info.name));
                    inDegree.merge(info.name, 1, Integer::sum);
                } else if (infoMap.containsKey(dep)) {
                    dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(info.name);
                    inDegree.merge(info.name, 1, Integer::sum);
                }
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<PluginInfo> sortedList = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            sortedList.add(infoMap.get(current));

            for (String dependent : dependents.getOrDefault(current, Collections.emptyList())) {
                int newInDegree = inDegree.get(dependent) - 1;
                inDegree.put(dependent, newInDegree);
                if (newInDegree == 0) {
                    queue.offer(dependent);
                }
            }
        }

        if (sortedList.size() != infoMap.size()) {
            log.error(LangManager.get("xinbot.plugin.load.all.failed"));
            for (String name : infoMap.keySet()) {
                if (inDegree.get(name) > 0) {
                    log.error(LangManager.get("xinbot.plugin.not.loaded", name));
                }
            }
        }

        return sortedList;
    }

    public void enablePlugin(RegisteredPlugin rp) {
        if (rp instanceof RegisteredMetaPlugin && Bot.Instance.isRunning()) {
            log.error(LangManager.get("xinbot.metaplugin.error.enable_runtime"));
            return;
        }
        try {
            try (InputStream is = rp.getPlugin().getClass().getClassLoader().getResourceAsStream("commands.yml")) {
                if (is != null) {
                    commandManager.registerCommands(is, rp.getPlugin());
                }
            } catch (Exception e) {
                log.error("Failed to load commands.yml for plugin " + rp.getName(), e);
            }
            sessionListeners.put(rp.getName(), new ArrayList<>());
            rp.getPlugin().onEnable();
            enabledPlugins.put(rp.getName(), rp);
            String key = rp instanceof RegisteredMetaPlugin ? "xinbot.metaplugin.enabled" : "xinbot.plugin.enabled";
            log.info(LangManager.get(key, rp.getName()));
        } catch (Exception e) {
            log.error(LangManager.get("xinbot.plugin.enable.failed", rp.getName()), e);
        }
    }

    public void disablePlugin(RegisteredPlugin rp) {
        if (rp instanceof RegisteredMetaPlugin && Bot.Instance.isRunning()) {
            log.error(LangManager.get("xinbot.metaplugin.error.disable_runtime"));
            return;
        }
        if (!enabledPlugins.containsKey(rp.getName())) {
            log.error(LangManager.get("xinbot.plugin.not.enabled", rp.getName()));
            return;
        }
        eventManager.unregisterAll(rp.getPlugin());
        commandManager.unregisterAll(rp.getPlugin());
        for (SessionListener sessionListener : sessionListeners.getOrDefault(rp.getName(), Collections.emptyList())) {
            Bot.Instance.getSession().removeListener(sessionListener);
        }
        sessionListeners.remove(rp.getName());
        try {
            rp.getPlugin().onDisable();
        }
        catch (Exception e) {
            log.error(LangManager.get("xinbot.plugin.disable.failed", rp.getName()), e);
        }
        finally {
            enabledPlugins.remove(rp.getName());
            DisablePluginEvent disablePluginEvent = new DisablePluginEvent(rp.getPlugin());
            eventManager.callEvent(disablePluginEvent);
            String key = rp instanceof RegisteredMetaPlugin ? "xinbot.metaplugin.disabled" : "xinbot.plugin.disabled";
            log.info(LangManager.get(key, rp.getName()));
        }
    }

    public void unloadPlugin(RegisteredPlugin rp) {
        if (rp instanceof RegisteredMetaPlugin && Bot.Instance.isRunning()) {
            log.error(LangManager.get("xinbot.metaplugin.error.unload_runtime"));
            return;
        }
        if (!plugins.containsKey(rp.getName())) return;

        String pluginName = rp.getName();

        List<RegisteredPlugin> dependents = new ArrayList<>();
        for (RegisteredPlugin p : plugins.values()) {
            if (p.getName().equals(pluginName)) continue;
            List<String> deps = pluginDependencies.get(p.getName());
            if (deps != null && deps.contains(pluginName)) {
                dependents.add(p);
            }
        }

        for (RegisteredPlugin dependent : dependents) {
            if (plugins.containsKey(dependent.getName())) {
                log.info(LangManager.get("xinbot.plugin.unload.dependent", dependent.getName(), pluginName));
                unloadPlugin(dependent);
            }
        }

        try {
            if (enabledPlugins.containsKey(pluginName)) {
                disablePlugin(rp);
            }
            rp.getPlugin().onUnload();
        } catch (Exception e) {
            log.error(LangManager.get("xinbot.plugin.unload.failed", pluginName), e);
        }
        finally {
            plugins.remove(pluginName);
            pluginDependencies.remove(pluginName);
            PluginClassLoader loader = pluginLoaders.remove(pluginName);
            if (loader != null) {
                try {
                    loader.close();
                } catch (IOException ignored) {}
            }
            String key = rp instanceof RegisteredMetaPlugin ? "xinbot.metaplugin.unloaded" : "xinbot.plugin.unloaded";
            log.info(LangManager.get(key, rp.getPlugin().getClass().getName()));
        }
    }

    public void unloadPlugins() {
        List<RegisteredPlugin> pluginsList = new ArrayList<>(this.plugins.values());
        for (RegisteredPlugin rp : pluginsList) {
            try {
                unloadPlugin(rp);
            } catch (Exception e) {
                log.error(LangManager.get("xinbot.plugin.unload.failed", rp.getName()), e);
            }
        }
    }

    public void enableAll() {
        for (RegisteredPlugin rp : plugins.values()) {
            try {
                if (rp.getType() == PluginType.PLUGIN) enablePlugin(rp);
            } catch (Exception e) {
                log.error(LangManager.get("xinbot.plugin.enable.failed", rp.getName()), e);
            }
        }

        RegisteredMetaPlugin meta = getMetaPlugin();
        if (meta != null) {
            sessionListeners.put(meta.getName(), new ArrayList<>());
            meta.getPlugin().onEnable();
            enabledPlugins.put(meta.getName(), meta);
            log.info(LangManager.get("xinbot.metaplugin.enabled", meta.getName()));
        }
    }

    public void disableAll() {
        for (RegisteredPlugin rp : plugins.values()) {
            try {
                disablePlugin(rp);
            } catch (Exception e) {
                log.error(LangManager.get("xinbot.plugin.disable.failed", rp.getName()), e);
            }
        }
    }

    public RegisteredPlugin getPlugin(String name) {
        return plugins.get(name);
    }

    @SuppressWarnings("unused")
    public boolean isPluginLoaded(String name) {
        return plugins.containsKey(name);
    }
    @SuppressWarnings("unused")
    public boolean isPluginEnabled(String name) {
        return enabledPlugins.containsKey(name);
    }

    public Collection<RegisteredPlugin> getPlugins() {
        return plugins.values();
    }

    public void addListener(SessionListener sessionListener, Plugin plugin) {
        sessionListeners.get(getPluginName(plugin)).add(sessionListener);
        Bot.Instance.getSession().addListener(sessionListener);
    }

    public void removeListener(SessionListener sessionListener, Plugin plugin) {
        sessionListeners.get(getPluginName(plugin)).remove(sessionListener);
        Bot.Instance.getSession().removeListener(sessionListener);
    }
}
