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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

public class PluginClassLoader extends URLClassLoader {
    private final java.util.List<PluginClassLoader> extraDependencies = new java.util.ArrayList<>();

    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void addDependency(PluginClassLoader loader) {
        if (loader != null && !extraDependencies.contains(loader)) {
            extraDependencies.add(loader);
        }
    }

    public void addURLFile(URL url) {
        super.addURL(url);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass != null) {
                if (resolve) resolveClass(loadedClass);
                return loadedClass;
            }

            try {
                loadedClass = getParent().loadClass(name);
                if (loadedClass != null) {
                    if (resolve) resolveClass(loadedClass);
                    return loadedClass;
                }
            } catch (ClassNotFoundException ignored) {
            }

            try {
                loadedClass = findClass(name);
                if (loadedClass != null) {
                    if (resolve) resolveClass(loadedClass);
                    return loadedClass;
                }
            } catch (ClassNotFoundException ignored) {
            }

            for (PluginClassLoader depLoader : extraDependencies) {
                try {
                    loadedClass = depLoader.loadClass(name, resolve);
                    if (loadedClass != null) return loadedClass;
                } catch (ClassNotFoundException ignored) {
                }
            }

            throw new ClassNotFoundException(name);
        }
    }

    @Override
    public URL getResource(String name) {
        return findResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);
        try {
            return url != null ? url.openStream() : null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return findResources(name);
    }
}
