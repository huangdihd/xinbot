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

package xin.bbtt.mcbot.listeners;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetTitleTextPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.Server;
import xin.bbtt.mcbot.events.LoginSuccessEvent;
import xin.bbtt.mcbot.events.SendLoginCommandEvent;
import xin.bbtt.mcbot.events.SendRegisterCommandEvent;
import xin.bbtt.mcbot.events.UseJoinItemEvent;

import java.time.Instant;

public class AutoLoginListener extends SessionAdapter {
    public static Long last_action_time = System.currentTimeMillis();

    private static final Logger log = LoggerFactory.getLogger(AutoLoginListener.class.getSimpleName());

    public static int join_button_slot = 2;

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundSetTitleTextPacket titlePacket) login(titlePacket);
        if (Bot.Instance.login) join();
    }

    private void login(ClientboundSetTitleTextPacket titlePacket) {
        if (titlePacket.toString().contains("登陆成功")) {
            LoginSuccessEvent loginSuccessEvent = new LoginSuccessEvent();
            Bot.Instance.getPluginManager().events().callEvent(loginSuccessEvent);
            log.info("Login successful");
            Bot.Instance.login = true;
        }
        else if (titlePacket.toString().contains("注册")) {
            String registerCommand = "reg " + Bot.Instance.getConfig().getAccount().getPassword() + " " + Bot.Instance.getConfig().getAccount().getPassword();
            SendRegisterCommandEvent registerCommandEvent = new SendRegisterCommandEvent(registerCommand);
            Bot.Instance.getPluginManager().events().callEvent(registerCommandEvent);
            if (!registerCommandEvent.isDefaultActionCancelled())
                Bot.Instance.sendCommand(registerCommandEvent.getCommand());
        }
        else {
            String loginCommand = "l " + Bot.Instance.getConfig().getAccount().getPassword();
            SendLoginCommandEvent loginCommandEvent = new SendLoginCommandEvent(loginCommand);
            if (!loginCommandEvent.isDefaultActionCancelled())
                Bot.Instance.sendCommand(loginCommandEvent.getCommand());
        }
    }

    private void join() {
        if (last_action_time > System.currentTimeMillis() - 2000) return;
        if (Bot.Instance.getServer() != Server.Login) return;
        UseJoinItemEvent useJoinItemEvent = new UseJoinItemEvent();
        Bot.Instance.getPluginManager().events().callEvent(useJoinItemEvent);
        last_action_time = System.currentTimeMillis();
        if (useJoinItemEvent.isDefaultActionCancelled()) return;
        Bot.Instance.getSession().send(new ServerboundSetCarriedItemPacket(join_button_slot));
        Bot.Instance.getSession().send(
                new ServerboundUseItemPacket(
                        Hand.MAIN_HAND,
                        (int) Instant.now().toEpochMilli(),
                        0,
                        0
                )
        );
    }
}
