package xin.bbtt.mcbot.plugin;

import java.net.URL;
import java.net.URLClassLoader;

public class PluginClassLoader extends URLClassLoader {
    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void addURLFile(URL url) {
        super.addURL(url);
    }
}
