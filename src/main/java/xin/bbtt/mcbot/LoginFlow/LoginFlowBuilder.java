/*
 * Copyright (C) 2024-2026 huangdihd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package xin.bbtt.mcbot.LoginFlow;

import org.geysermc.mcprotocollib.network.packet.Packet;
import xin.bbtt.mcbot.event.EventManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builder for constructing a {@link LoginFlow}.
 * <p>
 * Obtained via {@link LoginFlow#builder(Consumer)}.
 */
public class LoginFlowBuilder {
    final List<LoginFlowStep<?, ?>> steps = new ArrayList<>();
    long cooldownMs = 2000;
    long stepTimeoutMs = 0;
    final Consumer<String> commandSender;
    Function<String, String> templateExpander = null;
    Consumer<LoginFlow.LoginFlowContext> stateChangeListener = null;
    EventManager eventManager = null;

    LoginFlowBuilder(Consumer<String> commandSender) {
        this.commandSender = commandSender;
    }

    /**
     * Adds a new step to the flow.
     *
     * @param packetClass the packet class to listen for
     * @param <T>        the packet type
     * @return a {@link LoginFlowStepBuilder} for configuring the step
     */
    public <T extends Packet> LoginFlowStepBuilder<T> step(Class<T> packetClass) {
        return new LoginFlowStepBuilder<>(this, packetClass);
    }

    /**
     * Sets the cooldown between consecutive commands in milliseconds.
     * Default: 2000ms.
     */
    public LoginFlowBuilder cooldown(long ms) {
        this.cooldownMs = ms;
        return this;
    }

    /**
     * Sets the timeout for each step in milliseconds.
     * If a step doesn't complete within this time, the flow transitions to {@link LoginFlow.FlowState#FAILED}.
     * Default: 0 (no timeout).
     */
    public LoginFlowBuilder stepTimeout(long ms) {
        this.stepTimeoutMs = ms;
        return this;
    }

    /**
     * Sets a template expander for command templates.
     * For example, expanding {@code {password}} to the actual password.
     */
    public LoginFlowBuilder templateExpander(Function<String, String> expander) {
        this.templateExpander = expander;
        return this;
    }

    /**
     * Sets a listener that is called on every state change.
     */
    public LoginFlowBuilder onStateChange(Consumer<LoginFlow.LoginFlowContext> listener) {
        this.stateChangeListener = listener;
        return this;
    }

    /**
     * Sets an EventManager to fire {@link xin.bbtt.mcbot.events.LoginFlowEvent} on state transitions.
     */
    public LoginFlowBuilder eventManager(EventManager eventManager) {
        this.eventManager = eventManager;
        return this;
    }

    void addStep(LoginFlowStep<?, ?> step) {
        steps.add(step);
    }

    /**
     * Builds the {@link LoginFlow}.
     *
     * @throws IllegalStateException if no steps have been added
     */
    public LoginFlow build() {
        if (steps.isEmpty()) {
            throw new IllegalStateException("LoginFlow must have at least one step");
        }
        return new LoginFlow(this);
    }
}
