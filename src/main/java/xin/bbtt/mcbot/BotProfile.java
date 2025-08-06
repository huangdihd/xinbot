package xin.bbtt.mcbot;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class BotProfile {
    @Parameter(names = {"--help", "-h"}, description = "Show help.")
    private boolean help;
    @Parameter(names = {"--version", "-v"}, description = "Show version.")
    private boolean version;
    @Parameter(names = {"--plugins_directory", "-pd"}, description = "Plugins directory.")
    private String pluginsDirectory = "plugin";
    @Parameter(names = {"--owner", "-o"}, description = "Owner.")
    private String owner;
    @Parameter(names = {"--username", "-u"}, description = "Username.")
    private String username;
    @Parameter(names = {"--password", "-p"}, description = "Password.")
    private String password;
    @Parameter(names = {"--high-stability", "-hs"}, description = "Use old version reconnect system to improve stability(May lead to high cpu usage).")
    private boolean highStability;
    @Parameter(names = {"--disable-language-file", "-dlf"}, description = "Do not load language files (reduce memory usage).")
    private boolean disableLanguageFile;

    @Override
    public String toString() {
        return "BotProfile{" +
                "pluginsDirectory='" + pluginsDirectory + '\'' +
                ", owner='" + owner + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", highStability=" + highStability +
                '}';
    }

    public String getPassword() {
        return password;
    }

    public boolean load(String[] args){
        JCommander jcommander = JCommander.newBuilder()
                .addObject(this)
                .build();
        jcommander.parse(args);
        if (help) {
            jcommander.usage();
            return true;
        }
        if (version) {
            System.out.println(Xinbot.version);
            return true;
        }
        return false;
    }

    public void setPassword(String password) {
        if (Bot.Instance.isRunning()) throw new RuntimeException("Cannot change password of bot when it is running");
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (Bot.Instance.isRunning()) throw new RuntimeException("Cannot change username of bot when it is running");
        this.username = username;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        if (Bot.Instance.isRunning()) throw new RuntimeException("Cannot change owner of bot when it is running");
        this.owner = owner;
    }

    public String getPluginsDirectory() {
        return pluginsDirectory;
    }

    public void setPluginsDirectory(String pluginsDirectory) {
        if (Bot.Instance.isRunning()) throw new RuntimeException("Cannot change plugins directory of bot when it is running");
        this.pluginsDirectory = pluginsDirectory;
    }

    public boolean getHighStability() {
        return this.highStability;
    }

    public void setHighStability(boolean highStability) {
        this.highStability = highStability;
    }

    public boolean getDisableLanguageFile() {
        return this.disableLanguageFile;
    }
}
