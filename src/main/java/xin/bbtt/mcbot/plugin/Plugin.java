package xin.bbtt.mcbot.plugin;

public interface Plugin {
    default String getName(){
        return this.getClass().getSimpleName();
    }
    void onLoad();
    void onEnable();
    void onDisable();
}
