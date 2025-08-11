package xin.bbtt.mcbot.events;

import lombok.Getter;
import lombok.Setter;
import xin.bbtt.mcbot.event.Event;
import xin.bbtt.mcbot.event.HandlerList;
import xin.bbtt.mcbot.event.HasDefaultAction;

public class AnswerQuestionEvent extends Event implements HasDefaultAction {
    private static final HandlerList HANDLERS = new HandlerList();
    @Getter
    private final String question;
    @Getter
    @Setter
    private String answer;


    private boolean cancelDefault;

    public AnswerQuestionEvent(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    @Override public boolean isDefaultActionCancelled() { return cancelDefault; }
    @Override public void setDefaultActionCancelled(boolean c) { this.cancelDefault = c; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
