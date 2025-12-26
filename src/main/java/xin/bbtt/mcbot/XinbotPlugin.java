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

package xin.bbtt.mcbot;

import xin.bbtt.mcbot.commands.command.*;
import xin.bbtt.mcbot.commands.executor.*;
import xin.bbtt.mcbot.eventListeners.PositonInQueueOverlayListener;
import xin.bbtt.mcbot.eventListeners.PrivateChatMessageListener;
import xin.bbtt.mcbot.eventListeners.PublicChatMessageListener;
import xin.bbtt.mcbot.plugin.Plugin;
import xin.bbtt.mcbot.listeners.*;

public class XinbotPlugin implements Plugin {

    @Override
    public void onLoad() {
    }

    @Override
    public void onUnload() {
    }

    @Override
    public void onEnable() {
        // Packet listeners
        Bot.Instance.addPacketListener(new MessageSender(), this);
        Bot.Instance.addPacketListener(new AutoLoginListener(), this);
        Bot.Instance.addPacketListener(new AutoJoinListener(), this);
        Bot.Instance.addPacketListener(new ServerRecorder(), this);
        Bot.Instance.addPacketListener(new ChatMessagePrinter(), this);
        Bot.Instance.addPacketListener(new CaptchaListener(), this);
        Bot.Instance.addPacketListener(new AnswerQuestionListener(), this);
        Bot.Instance.addPacketListener(new PositionInQueueListener(), this);
        Bot.Instance.addPacketListener(new ServerMembersChangedMessagePrinter(), this);
        Bot.Instance.addPacketListener(new DisconnectReasonPrinter(), this);
        Bot.Instance.addPacketListener(new JoinButtonRecorder(), this);
        Bot.Instance.addPacketListener(new CommandsRecorder(), this);
        Bot.Instance.addPacketListener(new PacketListener(), this);

        // Commands
        Bot.Instance.getPluginManager().registerCommand(new SayCommand(), new SayCommandExecutor(), this);
        Bot.Instance.getPluginManager().registerCommand(new CommandCommand(), new CommandCommandExecutor(), this);
        Bot.Instance.getPluginManager().registerCommand(new StopCommand(), new StopCommandExecutor(), this);
        Bot.Instance.getPluginManager().registerCommand(new HelpCommand(), new HelpCommandExecutor(), this);
        Bot.Instance.getPluginManager().registerCommand(new DisconnectCommand(), new DisconnectExecutor(), this);
        Bot.Instance.getPluginManager().registerCommand(new ListCommand(), new ListCommandExecutor(), this);
        Bot.Instance.getPluginManager().registerCommand(new PluginManagerCommand(), new PluginManagerCommandExecutor(), this);
        Bot.Instance.getPluginManager().registerCommand(new PluginsCommand(), new PluginsCommandExecutor(), this);
        Bot.Instance.getPluginManager().registerCommand(new LicenseCommand(), new LicenseExecutor(), this);

        // Event listeners
        Bot.Instance.getPluginManager().registerEvents(new PositonInQueueOverlayListener(), this);
        Bot.Instance.getPluginManager().registerEvents(new PrivateChatMessageListener(), this);
        Bot.Instance.getPluginManager().registerEvents(new PublicChatMessageListener(), this);
    }

    @Override
    public void onDisable() {
    }
}
