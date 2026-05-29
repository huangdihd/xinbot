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

package xin.bbtt.mcbot.LoginFlow;

import lombok.Getter;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.event.EventManager;
import xin.bbtt.mcbot.events.LoginFlowEvent;
import xin.bbtt.mcbot.events.LoginSuccessEvent;
import xin.bbtt.mcbot.events.RegisterSuccessEvent;
import xin.bbtt.mcbot.events.SendCommandEvent;
import xin.bbtt.mcbot.events.SendLoginCommandEvent;
import xin.bbtt.mcbot.events.SendRegisterCommandEvent;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A declarative, builder-based login flow state machine for MetaPlugins.
 * <p>
 * Instead of writing scattered {@code SessionAdapter} listeners with static boolean flags,
 * MetaPlugin authors can declare the entire login sequence as a chain of steps:
 * <pre>{@code
 * LoginFlow flow = LoginFlow.builder(Bot.INSTANCE::sendChatMessage)
 *     .templateExpander(t -> t.replace("{password}", config.getPassword()))
 *     .step(ClientboundSetTitleTextPacket.class)
 *         .match(p -> p.toString().contains("注册"))
 *         .then("reg {password} {password}")
 *     .step(ClientboundSetTitleTextPacket.class)
 *         .match(p -> p.toString().contains("登陆成功"))
 *         .onSuccess(p -> Bot.INSTANCE.getPluginManager().events().callEvent(new LoginSuccessEvent()))
 *     .cooldown(2000)
 *     .build();
 *
 * Bot.INSTANCE.addPacketListener(flow, this);
 * }</pre>
 *
 * <h3>Key features</h3>
 * <ul>
 *   <li>Declarative step chain — each step declares "what to match" and "what to do"</li>
 *   <li>Built-in cooldown — prevents duplicate commands within a configurable window</li>
 *   <li>Template expansion — command templates with {@code {key}} placeholders</li>
 *   <li>Step timeout — marks flow as FAILED if a step takes too long</li>
 *   <li>Fires {@link LoginFlowEvent} on state transitions (if EventManager is provided)</li>
 *   <li>Decoupled from {@code Bot.INSTANCE} — accepts a command sender callback</li>
 * </ul>
 */
public class LoginFlow extends SessionAdapter {

    private static final Logger log = LoggerFactory.getLogger(LoginFlow.class.getSimpleName());

    private final List<LoginFlowStep<?, ?>> steps;
    private final long cooldownMs;
    private final long stepTimeoutMs;
    private final Consumer<String> commandSender;
    private final Function<String, String> templateExpander;
    private final Consumer<LoginFlowContext> stateChangeListener;
    private final EventManager eventManager;

    /**
     * -- GETTER --
     *  Returns the current step index (0-based).
     */
    @Getter
    private int currentStepIndex = 0;
    private long lastCommandTime = 0;
    private volatile long stepStartTime = 0;
    @Getter
    private volatile FlowState state = FlowState.WAITING;

    LoginFlow(LoginFlowBuilder builder) {
        this.steps = Collections.unmodifiableList(builder.steps);
        this.cooldownMs = builder.cooldownMs;
        this.stepTimeoutMs = builder.stepTimeoutMs;
        this.commandSender = builder.commandSender;
        this.templateExpander = builder.templateExpander;
        this.stateChangeListener = builder.stateChangeListener;
        this.eventManager = builder.eventManager;
    }

    /**
     * Creates a new builder with a command sender.
     *
     * @param commandSender callback to send commands, e.g. {@code Bot.INSTANCE::sendChatMessage}
     */
    public static LoginFlowBuilder builder(Consumer<String> commandSender) {
        return new LoginFlowBuilder(Objects.requireNonNull(commandSender, "commandSender"));
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (state == FlowState.COMPLETED || state == FlowState.FAILED) return;
        if (currentStepIndex >= steps.size()) return;

        if (stepStartTime == 0) {
            stepStartTime = System.currentTimeMillis();
        }

        checkStepTimeout();
        if (state == FlowState.FAILED) return;

        processPacket(packet, System.currentTimeMillis());
    }

    private void processPacket(Packet packet, long now) {
        LoginFlowStep<?, ?> currentStep = steps.get(currentStepIndex);

        boolean isTriggerPacket = currentStep.packetClass.isInstance(packet);
        boolean isSuccessPacket = currentStep.successPacketClass != null
                && currentStep.successPacketClass.isInstance(packet);

        if (!isTriggerPacket && !isSuccessPacket) return;

        if (isTriggerPacket && currentStep.shouldSkip(packet)) {
            log.debug("LoginFlow step {} skipped by skipWhen predicate", currentStepIndex);
            advanceStep(packet);
            processPacket(packet, now);
            return;
        }

        if (currentStep.successPredicate == null) {
            handleAutoAdvanceStep(currentStep, packet, isTriggerPacket, now);
            return;
        }

        handleConditionalStep(currentStep, packet, isTriggerPacket, isSuccessPacket, now);
    }

    private void handleAutoAdvanceStep(LoginFlowStep<?, ?> step, Packet packet,
                                       boolean isTriggerPacket, long now) {
        if (!isTriggerPacket || !step.matches(packet)) return;
        sendCommandIfReady(step, now);
        advanceStep(packet);
    }

    private void handleConditionalStep(LoginFlowStep<?, ?> step, Packet packet,
                                       boolean isTriggerPacket, boolean isSuccessPacket, long now) {
        if (isTriggerPacket && step.matches(packet)) {
            sendCommandIfReady(step, now);
        }
        if (isSuccessPacket && step.isSuccess(packet)) {
            advanceStep(packet);
        }
    }

    private void checkStepTimeout() {
        if (stepTimeoutMs <= 0) return;
        if (stepStartTime <= 0) return;
        if (System.currentTimeMillis() - stepStartTime > stepTimeoutMs) {
            log.warn("LoginFlow step {} timed out after {}ms, marking as FAILED",
                    currentStepIndex, stepTimeoutMs);
            state = FlowState.FAILED;
            fireStateChange();
        }
    }

    private void sendCommandIfReady(LoginFlowStep<?, ?> step, long now) {
        if (step.commandTemplate != null && now - lastCommandTime >= cooldownMs) {
            String command = expandTemplate(step.commandTemplate);
            log.debug("LoginFlow step {}: sending '{}'", currentStepIndex, command);

            if (eventManager != null && step.commandType != LoginFlowStep.CommandType.GENERIC) {
                SendCommandEvent event = createCommandEvent(step.commandType, command);
                eventManager.callEvent(event);

                if (event.isDefaultActionCancelled()) {
                    log.debug("LoginFlow step {}: command cancelled by event", currentStepIndex);
                    return;
                }

                command = event.getCommand();
            }

            commandSender.accept(command);
            lastCommandTime = now;
        }
    }

    private SendCommandEvent createCommandEvent(LoginFlowStep.CommandType commandType, String command) {
        return switch (commandType) {
            case LOGIN -> new SendLoginCommandEvent(command);
            case REGISTER -> new SendRegisterCommandEvent(command);
            default -> new SendCommandEvent(command);
        };
    }

    @SuppressWarnings("unchecked")
    private void advanceStep(Packet packet) {
        LoginFlowStep<?, ?> step = steps.get(currentStepIndex);
        if (step.onSuccess != null) {
            ((Consumer<Packet>) step.onSuccess).accept(packet);
        }

        if (eventManager != null) {
            switch (step.commandType) {
                case LOGIN -> eventManager.callEvent(new LoginSuccessEvent());
                case REGISTER -> eventManager.callEvent(new RegisterSuccessEvent());
                default -> {}
            }
        }

        currentStepIndex++;
        state = currentStepIndex >= steps.size() ? FlowState.COMPLETED : FlowState.WAITING;
        stepStartTime = state == FlowState.WAITING ? System.currentTimeMillis() : 0;
        log.debug("LoginFlow advanced to step {}/{} ({})", currentStepIndex, steps.size(), state);
        fireStateChange();
    }

    private void fireStateChange() {
        if (stateChangeListener != null) {
            stateChangeListener.accept(new LoginFlowContext(currentStepIndex, state));
        }
        if (eventManager != null) {
            eventManager.callEvent(new LoginFlowEvent(currentStepIndex, state));
        }
    }

    private String expandTemplate(String template) {
        return templateExpander != null ? templateExpander.apply(template) : template;
    }

    /**
     * Resets the flow to the initial state.
     */
    public void reset() {
        currentStepIndex = 0;
        lastCommandTime = 0;
        stepStartTime = 0;
        state = FlowState.WAITING;
        log.debug("LoginFlow reset");
        fireStateChange();
    }

    /**
     * Returns the total number of steps.
     */
    public int getTotalSteps() {
        return steps.size();
    }

    public enum FlowState {
        WAITING,
        COMPLETED,
        FAILED
    }

    /**
     * Immutable snapshot of the login flow state at a point in time.
     */
    public record LoginFlowContext(int stepIndex, FlowState state) {}
}
