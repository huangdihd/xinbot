/*
 *   Copyright (C) 2024-2026 huangdihd
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

package xin.bbtt.mcbot;

import xin.bbtt.mcbot.eventListeners.DisconnectListener;
import xin.bbtt.mcbot.eventListeners.PositonInQueueOverlayListener;
import xin.bbtt.mcbot.eventListeners.PrivateChatMessageListener;
import xin.bbtt.mcbot.eventListeners.PublicChatMessageListener;
import xin.bbtt.mcbot.plugin.MetaPlugin;
import xin.bbtt.mcbot.listeners.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class XinbotPlugin implements MetaPlugin {


    @Override
    public void onLoad() {
    }

    @Override
    public void onUnload() {
    }

    @Override
    public void onEnable() {
        // Packet listeners
        Bot.Instance.addPacketListener(new AutoLoginListener(), this);
        Bot.Instance.addPacketListener(new AutoJoinListener(), this);
        Bot.Instance.addPacketListener(new CaptchaListener(), this);
        Bot.Instance.addPacketListener(new AnswerQuestionListener(), this);
        Bot.Instance.addPacketListener(new PositionInQueueListener(), this);
        Bot.Instance.addPacketListener(new ServerMembersChangedMessagePrinter(), this);
        Bot.Instance.addPacketListener(new JoinButtonRecorder(), this);
        Bot.Instance.addPacketListener(new CommandsRecorder(), this);

        // Event listeners
        Bot.Instance.getPluginManager().registerEvents(new PositonInQueueOverlayListener(), this);
        Bot.Instance.getPluginManager().registerEvents(new PrivateChatMessageListener(), this);
        Bot.Instance.getPluginManager().registerEvents(new PublicChatMessageListener(), this);
        Bot.Instance.getPluginManager().registerEvents(new DisconnectListener(), this);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public SocketAddress getServerSocketAddress() {
        return  new InetSocketAddress("2b2t.xin", 25565);
    }
}
