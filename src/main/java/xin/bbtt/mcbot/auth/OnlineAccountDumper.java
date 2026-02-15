
/*
 *   Copyright (C) 2026 huangdihd
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import xin.bbtt.mcbot.config.BotConfigData;

public class OnlineAccountDumper {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static BotConfigData.Account DumpAccount(StepFullJavaSession.FullJavaSession session) throws JsonProcessingException {
        String sessionJson = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(session).toString();
        JsonNode serializedSession = objectMapper.readTree(sessionJson);
        BotConfigData.Account account = new BotConfigData.Account();
        account.setOnlineMode(true);
        account.setName(session.getMcProfile().getName());
        account.setPassword("");
        account.setFullSession(serializedSession);
        return account;
    }
}
