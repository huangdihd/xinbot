/*
 * # Copyright (C) 2025 huangdihd
 * #
 * # This program is free software: you can redistribute it and/or modify
 * # it under the terms of the GNU General Public License as published by
 * # the Free Software Foundation, either version 3 of the License, or
 * # (at your option) any later version.
 * #
 * # This program is distributed in the hope that it will be useful,
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * # GNU General Public License for more details.
 * #
 * # You should have received a copy of the GNU General Public License
 * # along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package xin.bbtt.mcbot;

import xin.bbtt.mcbot.plugin.Plugin;
import xin.bbtt.mcbot.listeners.*;

public class DefaultPlugin implements Plugin {

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
        Bot.Instance.addPacketListener(new MessageSender());
        Bot.Instance.addPacketListener(new AutoLoginListener());
        Bot.Instance.addPacketListener(new AutoJoinListener());
        Bot.Instance.addPacketListener(new ServerRecorder());
        Bot.Instance.addPacketListener(new ChatMessagePrinter());
        Bot.Instance.addPacketListener(new CaptchaListener());
        Bot.Instance.addPacketListener(new AnswerQuestionListener());
        Bot.Instance.addPacketListener(new PositionInQueueListener());
        Bot.Instance.addPacketListener(new ServerMembersChangedMessagePrinter());
        Bot.Instance.addPacketListener(new DisconnectReasonPrinter());
        Bot.Instance.addPacketListener(new JoinButtonRecorder());
    }

    @Override
    public void onDisable() {
    }
}
