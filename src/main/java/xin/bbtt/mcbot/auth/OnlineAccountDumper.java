package xin.bbtt.mcbot.auth;

import com.google.gson.JsonObject;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import xin.bbtt.mcbot.config.BotConfig;

public class OnlineAccountDumper {
    public static BotConfig.Account DumpAccount(StepFullJavaSession.FullJavaSession session) {
        JsonObject serializedSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(session);
        BotConfig.Account account = new BotConfig.Account();
        account.setOnlineMode(true);
        account.setName(session.getMcProfile().getName());
        account.setPassword("");
        account.setFullSession(serializedSession.toString());
        return account;
    }
}
