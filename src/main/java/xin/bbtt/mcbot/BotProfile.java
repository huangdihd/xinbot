package xin.bbtt.mcbot;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class BotProfile {
    @Parameter(names = {"--plugins_directory", "-pd"}, description = "Plugins directory.")
    private String pluginsDirectory = "plugin";
    @Parameter(names = {"--owner", "-o"}, description = "Owner.")
    private String owner;
    @Parameter(names = {"--username", "-u"}, description = "Username.")
    private String username;
    @Parameter(names = {"--password", "-p"}, description = "Password.")
    private String password;

    @Override
    public String toString() {
        return "BotProfile{" +
                "pluginsDirectory='" + pluginsDirectory + '\'' +
                ", owner='" + owner + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

    public String getPassword() {
        return password;
    }

    public void load(String[] args){
        JCommander.newBuilder()
                .addObject(this)
                .build()
                .parse(args);
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
}
