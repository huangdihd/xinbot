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
