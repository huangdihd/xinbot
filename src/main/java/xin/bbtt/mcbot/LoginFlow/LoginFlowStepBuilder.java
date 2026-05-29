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

import org.geysermc.mcprotocollib.network.packet.Packet;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Builder for a single step within a {@link LoginFlow}.
 * <p>
 * Obtained via {@link LoginFlowBuilder#step(Class)}.
 *
 * @param <T> the packet type this step listens for
 */
public class LoginFlowStepBuilder<T extends Packet> {
    private final LoginFlowBuilder parent;
    private final Class<T> packetClass;
    private Predicate<T> predicate = p -> true;
    private String commandTemplate;
    private Predicate<?> successPredicate;
    private Class<?> successPacketClass;
    private Consumer<?> onSuccess;
    private String description;
    private LoginFlowStep.CommandType commandType;
    private Predicate<?> skipPredicate;

    LoginFlowStepBuilder(LoginFlowBuilder parent, Class<T> packetClass) {
        this.parent = parent;
        this.packetClass = packetClass;
    }

    /**
     * Sets the predicate that determines whether this step's trigger condition is met.
     */
    public LoginFlowStepBuilder<T> match(Predicate<T> predicate) {
        this.predicate = predicate;
        return this;
    }

    /**
     * Sets the command template to send when this step triggers.
     * Use {@code {key}} placeholders for dynamic values (requires {@link LoginFlowBuilder#templateExpander}).
     */
    public LoginFlowStepBuilder<T> then(String commandTemplate) {
        this.commandTemplate = commandTemplate;
        return this;
    }

    /**
     * Sets a predicate that determines when this step is considered successful.
     * If not set, the step completes immediately after the match condition is met.
     *
     * @param successPacketClass the packet class to check for success
     * @param predicate          the success condition
     * @param <S>                the success packet type
     */
    public <S extends Packet> LoginFlowStepBuilder<T> successWhen(Class<S> successPacketClass, Predicate<S> predicate) {
        this.successPacketClass = successPacketClass;
        this.successPredicate = predicate;
        return this;
    }

    /**
     * Sets a success predicate that checks the same packet type as the trigger.
     */
    public LoginFlowStepBuilder<T> successWhen(Predicate<T> predicate) {
        this.successPacketClass = this.packetClass;
        this.successPredicate = predicate;
        return this;
    }

    /**
     * Sets a callback to execute when this step succeeds.
     */
    public LoginFlowStepBuilder<T> onSuccess(Consumer<T> callback) {
        this.onSuccess = callback;
        return this;
    }

    /**
     * Sets a human-readable description for this step.
     */
    public LoginFlowStepBuilder<T> describe(String description) {
        this.description = description;
        return this;
    }

    /**
     * Marks this step as a login command step.
     * When the command is sent, a {@link xin.bbtt.mcbot.events.SendLoginCommandEvent} will be fired.
     */
    public LoginFlowStepBuilder<T> login() {
        this.commandType = LoginFlowStep.CommandType.LOGIN;
        return this;
    }

    /**
     * Marks this step as a register command step.
     * When the command is sent, a {@link xin.bbtt.mcbot.events.SendRegisterCommandEvent} will be fired.
     */
    public LoginFlowStepBuilder<T> register() {
        this.commandType = LoginFlowStep.CommandType.REGISTER;
        return this;
    }

    /**
     * Sets a predicate that determines when this step should be skipped.
     * If the predicate returns true for a packet, the step is immediately skipped
     * and the flow advances to the next step without sending any command.
     *
     * @param predicate the skip condition
     */
    public LoginFlowStepBuilder<T> skipWhen(Predicate<T> predicate) {
        this.skipPredicate = predicate;
        return this;
    }

    /**
     * Adds this step and returns the parent builder for chaining.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public LoginFlowBuilder add() {
        parent.addStep(new LoginFlowStep<>(
            packetClass, predicate, commandTemplate,
            (Class) successPacketClass, (Predicate) successPredicate,
            onSuccess, description, commandType,
            (Predicate) skipPredicate
        ));
        return parent;
    }

    /**
     * Shortcut: adds this step and immediately starts another step.
     */
    public <U extends Packet> LoginFlowStepBuilder<U> step(Class<U> packetClass) {
        add();
        return parent.step(packetClass);
    }
}
