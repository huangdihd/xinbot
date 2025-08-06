package xin.bbtt.mcbot;

public class DefaultPlugin implements Plugin {

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
        Bot.Instance.addListener(new MessageSender());
        Bot.Instance.addListener(new AutoLoginProcessor());
        Bot.Instance.addListener(new AutoJoinProcessor());
        Bot.Instance.addListener(new ServerRecorder());
        Bot.Instance.addListener(new ChatMessagePrinter());
        Bot.Instance.addListener(new CaptchaProcessor());
        Bot.Instance.addListener(new QueueProcessor());
        Bot.Instance.addListener(new ServerMembersChangedMessagePrinter());
        Bot.Instance.addListener(new DisconnectReasonPrinter());
        Bot.Instance.addListener(new JoinButtonRecorder());
    }

    @Override
    public void onDisable() {
    }
}
