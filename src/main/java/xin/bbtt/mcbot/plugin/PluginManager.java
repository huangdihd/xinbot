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
    private final Map<String, Plugin> plugins = new HashMap<>();
    private final Map<String, Plugin> enabledPlugins = new HashMap<>();
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

    public void loadPlugin(Plugin plugin) {
        plugins.put(plugin.getName(), plugin);
        plugin.onLoad();
        if (Bot.Instance.getSession() != null) {
            enablePlugin(plugin);
        }
        log.info(LangManager.get("xinbot.plugin.loaded", plugin.getName()));
    }

    static class PluginInfo {
        File file;
        String name;
        String mainClass;
        String version;
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
        loadPlugin(plugin);
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
                if (info != null) {
                    info.file = file;
                    info.url = file.toURI().toURL();
                    infoMap.put(info.name, info);
                } else {
                    log.error("Failed to load plugin from {}: Missing or invalid plugin.yml", file.getName());
                }
            } catch (Exception e) {
                log.error(LangManager.get("xinbot.plugin.load.failed", file.getName()), e);
            }
        }

        List<PluginInfo> sortedInfos = sortPluginInfosTopologically(infoMap);

        for (PluginInfo info : sortedInfos) {
            log.info(LangManager.get("xinbot.plugin.loading", info.name));
            try {
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
                
                if (plugins.containsKey(plugin.getName())) {
                    pluginClassLoader.close();
                    continue;
                }
                
                loadPlugin(plugin);
            } catch (Exception e) {
                log.error(LangManager.get("xinbot.plugin.load.smoothly.failed", info.name), e);
            }
        }
    }

    private PluginInfo loadPluginYaml(File file) throws IOException {
        try (JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("plugin.yml");
            if (entry == null) return null;
            
            try (InputStream is = jar.getInputStream(entry)) {
                Yaml yaml = new Yaml();
                Map<String, Object> map = yaml.load(is);
                if (map == null || !map.containsKey("name") || !map.containsKey("main")) {
                    return null;
                }
                
                PluginInfo info = new PluginInfo();
                info.name = String.valueOf(map.get("name"));
                info.mainClass = String.valueOf(map.get("main"));
                info.version = map.containsKey("version") ? String.valueOf(map.get("version")) : "1.0.0";
                
                if (map.containsKey("depend")) {
                    Object dependObj = map.get("depend");
                    if (dependObj instanceof List) {
                        for (Object dep : (List<?>) dependObj) {
                            info.depends.add(String.valueOf(dep));
                        }
                    } else if (dependObj instanceof String) {
                        info.depends.add((String) dependObj);
                    }
                }
                if (map.containsKey("depends")) {
                    Object dependObj = map.get("depends");
                    if (dependObj instanceof List) {
                        for (Object dep : (List<?>) dependObj) {
                            info.depends.add(String.valueOf(dep));
                        }
                    } else if (dependObj instanceof String) {
                        info.depends.add((String) dependObj);
                    }
                }
                return info;
            }
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

    public void enablePlugin(Plugin plugin) {
        try {
            sessionListeners.put(plugin.getName(), new ArrayList<>());
            plugin.onEnable();
            enabledPlugins.put(plugin.getName(), plugin);
            log.info(LangManager.get("xinbot.plugin.enabled", plugin.getName()));
        } catch (Exception e) {
            log.error(LangManager.get("xinbot.plugin.enable.failed", plugin.getName()), e);
        }
    }

    public void disablePlugin(Plugin plugin) {
        if (!enabledPlugins.containsKey(plugin.getName())) {
            log.error(LangManager.get("xinbot.plugin.not.enabled", plugin.getName()));
            return;
        }
        eventManager.unregisterAll(plugin);
        commandManager.unregisterAll(plugin);
        for (SessionListener sessionListener : sessionListeners.getOrDefault(plugin.getName(), Collections.emptyList())) {
            Bot.Instance.getSession().removeListener(sessionListener);
        }
        sessionListeners.remove(plugin.getName());
        try {
            plugin.onDisable();
        }
        catch (Exception e) {
            log.error(LangManager.get("xinbot.plugin.disable.failed", plugin.getName()), e);
        }
        finally {
            enabledPlugins.remove(plugin.getName());
            DisablePluginEvent disablePluginEvent = new DisablePluginEvent(plugin);
            eventManager.callEvent(disablePluginEvent);
            log.info(LangManager.get("xinbot.plugin.disabled", plugin.getName()));
        }
    }

    public void unloadPlugin(Plugin plugin) {
        if (!plugins.containsKey(plugin.getName())) return;

        String pluginName = plugin.getName();

        List<Plugin> dependents = new ArrayList<>();
        for (Plugin p : plugins.values()) {
            if (p.getName().equals(pluginName)) continue;
            List<String> deps = pluginDependencies.get(p.getName());
            if (deps != null && deps.contains(pluginName)) {
                dependents.add(p);
            }
        }

        for (Plugin dependent : dependents) {
            if (plugins.containsKey(dependent.getName())) {
                log.info(LangManager.get("xinbot.plugin.unload.dependent", dependent.getName(), pluginName));
                unloadPlugin(dependent);
            }
        }

        try {
            if (enabledPlugins.containsKey(pluginName)) {
                disablePlugin(plugin);
            }
            plugin.onUnload();
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
            log.info(LangManager.get("xinbot.plugin.unloaded", plugin.getClass().getName()));
        }
    }

    public void unloadPlugins() {
        List<Plugin> plugins = new ArrayList<>(this.plugins.values());
        for (Plugin plugin : plugins) {
            try {
                unloadPlugin(plugin);
            } catch (Exception e) {
                log.error(LangManager.get("xinbot.plugin.unload.failed", plugin.getName()), e);
            }
        }
    }

    public void enableAll() {
        for (Plugin plugin : plugins.values()) {
            try {
                enablePlugin(plugin);
            } catch (Exception e) {
                log.error(LangManager.get("xinbot.plugin.enable.failed", plugin.getName()), e);
            }
        }
    }

    public void disableAll() {
        for (Plugin plugin : plugins.values()) {
            try {
                disablePlugin(plugin);
            } catch (Exception e) {
                log.error(LangManager.get("xinbot.plugin.disable.failed", plugin.getName()), e);
            }
        }
    }

    public Plugin getPlugin(String name) {
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

    public Collection<Plugin> getPlugins() {
        return plugins.values();
    }

    public void addListener(SessionListener sessionListener, Plugin plugin) {
        sessionListeners.get(plugin.getName()).add(sessionListener);
        Bot.Instance.getSession().addListener(sessionListener);
    }

    public void removeListener(SessionListener sessionListener, Plugin plugin) {
        sessionListeners.get(plugin.getName()).remove(sessionListener);
        Bot.Instance.getSession().removeListener(sessionListener);
    }
}
