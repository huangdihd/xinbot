package xin.bbtt.mcbot.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.config.BotConfig;

public class AccountLoader {
    private static final Logger log = LoggerFactory.getLogger(AccountLoader.class.getSimpleName());
    @Getter
    private static StepFullJavaSession.FullJavaSession javaSession;
    @Getter
    private static MinecraftProtocol protocol;
    public static void init(@NotNull BotConfig.Account account) throws Exception {
        if (!account.isOnlineMode()) {
            protocol = new MinecraftProtocol(account.getName());
            return;
        }
        if (account.getFullSession() == null || account.getFullSession().isEmpty()) {
            log.warn("No session found for the online account");
            log.info("Starting device code login...");
            HttpClient httpClient = MinecraftAuth.createHttpClient();
            StepMsaDeviceCode.MsaDeviceCodeCallback callback = new StepMsaDeviceCode.MsaDeviceCodeCallback(
                    msaDeviceCode ->
                            log.info("Go to {} to login your minecraft account", msaDeviceCode.getDirectVerificationUri())
            );
            javaSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(httpClient, callback);
        }
        else {
            JsonObject jsonObject = JsonParser.parseString(account.getFullSession()).getAsJsonObject();
            javaSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.fromJson(jsonObject);
        }
        GameProfile gameProfile = new GameProfile(javaSession.getMcProfile().getId(), javaSession.getMcProfile().getName());
        String accessToken = javaSession.getMcProfile().getMcToken().getAccessToken();

        protocol = new MinecraftProtocol(gameProfile, accessToken);
    }

}
