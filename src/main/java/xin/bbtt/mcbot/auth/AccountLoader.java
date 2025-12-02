/*
 *   Copyright (C) 2024-2025 huangdihd
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

package xin.bbtt.mcbot.auth;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
import xin.bbtt.mcbot.config.BotConfigData;

public class AccountLoader {
    private static final Logger log = LoggerFactory.getLogger(AccountLoader.class.getSimpleName());
    @Getter
    private static StepFullJavaSession.FullJavaSession javaSession;
    @Getter
    private static MinecraftProtocol protocol;
    private static final Gson gson = new Gson();
    public static BotConfigData.Account init(@NotNull BotConfigData.Account account) throws Exception {
        if (!account.isOnlineMode()) {
            protocol = new MinecraftProtocol(account.getName());
            return account;
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
            String sessionJson = account.getFullSession().toString();
            JsonObject gsonObject = gson.fromJson(sessionJson, JsonObject.class);
            javaSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.fromJson(gsonObject);
        }
        GameProfile gameProfile = new GameProfile(javaSession.getMcProfile().getId(), javaSession.getMcProfile().getName());
        String accessToken = javaSession.getMcProfile().getMcToken().getAccessToken();

        protocol = new MinecraftProtocol(gameProfile, accessToken);

        return OnlineAccountDumper.DumpAccount(AccountLoader.getJavaSession());
    }

}
