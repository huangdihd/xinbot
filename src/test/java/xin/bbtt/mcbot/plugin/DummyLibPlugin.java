package xin.bbtt.mcbot.plugin;

public class DummyLibPlugin implements Plugin {

    @Override
    public void onLoad() {
        DummyMetaPlugin.events.add("lib-load");
    }

    @Override
    public void onEnable() {
        DummyMetaPlugin.events.add("lib-enable");
    }

    @Override
    public void onDisable() {
        DummyMetaPlugin.events.add("lib-disable");
    }

    @Override
    public void onUnload() {
        DummyMetaPlugin.events.add("lib-unload");
    }
}
