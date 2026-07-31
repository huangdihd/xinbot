/*
 * Copyright (C) 2026 huangdihd
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

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import xin.bbtt.mcbot.event.EventManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LoginFlowTest {

    private Session mockSession;
    private List<String> sentCommands;
    private LoginFlowBuilder baseBuilder;

    static class FakePacketA implements Packet {
        private final String data;
        FakePacketA(String data) { this.data = data; }
        String getData() { return data; }
    }

    static class FakePacketB implements Packet {
        private final String data;
        FakePacketB(String data) { this.data = data; }
        String getData() { return data; }
    }

    @BeforeEach
    void setUp() {
        mockSession = mock(Session.class);
        sentCommands = new ArrayList<>();
        baseBuilder = LoginFlow.builder(sentCommands::add);
    }

    @Nested
    class BuilderValidation {
        @Test
        void buildWithoutStepsThrows() {
            assertThatThrownBy(() -> baseBuilder.build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least one step");
        }

        @Test
        void buildWithNullCommandSenderThrows() {
            assertThatThrownBy(() -> LoginFlow.builder(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class SingleStep {
        @Test
        void matchAndSendCommand() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> p.getData().contains("login")).then("l mypassword")
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketA("please login"));
            assertThat(sentCommands).containsExactly("l mypassword");
        }

        @Test
        void doesNotSendWhenPredicateFails() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> p.getData().contains("login")).then("l mypassword")
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketA("something else"));
            assertThat(sentCommands).isEmpty();
        }

        @Test
        void stepWithoutCommandJustAdvances() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> p.getData().contains("ready"))
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketA("ready"));
            assertThat(sentCommands).isEmpty();
            assertThat(flow.getState()).isEqualTo(LoginFlow.FlowState.COMPLETED);
        }

        @Test
        void ignoresDifferentPacketType() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> p.getData().contains("trigger")).then("cmd")
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketB("trigger"));
            assertThat(sentCommands).isEmpty();
        }
    }

    @Nested
    class MultiStep {
        @Test
        void stepsExecuteInOrder() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> p.getData().contains("step1")).then("cmd1")
                .step(FakePacketA.class).match(p -> p.getData().contains("step2")).then("cmd2")
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketA("step1"));
            assertThat(sentCommands).containsExactly("cmd1");
            assertThat(flow.getCurrentStepIndex()).isEqualTo(1);

            flow.packetReceived(mockSession, new FakePacketA("step2"));
            assertThat(sentCommands).containsExactly("cmd1", "cmd2");
            assertThat(flow.getState()).isEqualTo(LoginFlow.FlowState.COMPLETED);
        }

        @Test
        void ignoresIrrelevantPacketsBetweenSteps() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> p.getData().contains("trigger")).then("cmd")
                .step(FakePacketA.class).match(p -> p.getData().contains("done")).then("final")
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketA("trigger"));
            flow.packetReceived(mockSession, new FakePacketA("noise"));
            flow.packetReceived(mockSession, new FakePacketA("done"));
            assertThat(sentCommands).containsExactly("cmd", "final");
        }

        @Test
        void differentPacketTypesInDifferentSteps() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> p.getData().contains("a")).then("cmdA")
                .step(FakePacketB.class).match(p -> p.getData().contains("b")).then("cmdB")
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketA("a"));
            assertThat(sentCommands).containsExactly("cmdA");

            flow.packetReceived(mockSession, new FakePacketA("b"));
            assertThat(sentCommands).containsExactly("cmdA");

            flow.packetReceived(mockSession, new FakePacketB("b"));
            assertThat(sentCommands).containsExactly("cmdA", "cmdB");
        }

        @Test
        void threeStepFlow() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> p.getData().contains("1")).then("cmd1")
                .step(FakePacketA.class).match(p -> p.getData().contains("2")).then("cmd2")
                .step(FakePacketA.class).match(p -> p.getData().contains("3")).then("cmd3")
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketA("1"));
            flow.packetReceived(mockSession, new FakePacketA("2"));
            flow.packetReceived(mockSession, new FakePacketA("3"));

            assertThat(sentCommands).containsExactly("cmd1", "cmd2", "cmd3");
            assertThat(flow.getState()).isEqualTo(LoginFlow.FlowState.COMPLETED);
        }
    }

    @Nested
    class Cooldown {
        @Test
        void cooldownPreventsRapidCommands() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> true).then("cmd")
                .add().cooldown(10000).build();

            flow.packetReceived(mockSession, new FakePacketA("x"));
            flow.packetReceived(mockSession, new FakePacketA("x"));
            assertThat(sentCommands).hasSize(1);
        }

        @Test
        void cooldownOnlyAffectsCurrentStep() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> p.getData().contains("1")).then("cmd1")
                .step(FakePacketA.class).match(p -> p.getData().contains("2")).then("cmd2")
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketA("1"));
            flow.packetReceived(mockSession, new FakePacketA("2"));
            assertThat(sentCommands).containsExactly("cmd1", "cmd2");
        }
    }

    @Nested
    class TemplateExpansion {
        @Test
        void templateExpanderModifiesCommand() {
            LoginFlow flow = baseBuilder
                .templateExpander(t -> t.replace("{password}", "secret123"))
                .step(FakePacketA.class).match(p -> true).then("l {password}")
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketA("x"));
            assertThat(sentCommands).containsExactly("l secret123");
        }

        @Test
        void multiplePlaceholders() {
            LoginFlow flow = baseBuilder
                .templateExpander(t -> t
                    .replace("{password}", "mypass")
                    .replace("{username}", "myuser"))
                .step(FakePacketA.class).match(p -> true).then("login {username} {password}")
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketA("x"));
            assertThat(sentCommands).containsExactly("login myuser mypass");
        }

        @Test
        void noExpanderUsesRawTemplate() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> true).then("l {password}")
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketA("x"));
            assertThat(sentCommands).containsExactly("l {password}");
        }
    }

    @Nested
    class SuccessPredicate {
        @Test
        void stepWaitsForSuccessCondition() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class)
                    .match(p -> p.getData().contains("trigger")).then("cmd")
                    .successWhen(p -> p.getData().contains("success"))
                .step(FakePacketA.class).match(p -> true).then("cmd2")
                .add().cooldown(10000).build();

            flow.packetReceived(mockSession, new FakePacketA("trigger"));
            assertThat(flow.getCurrentStepIndex()).isEqualTo(0);

            flow.packetReceived(mockSession, new FakePacketA("success"));
            assertThat(flow.getCurrentStepIndex()).isEqualTo(1);
        }

        @Test
        void successWithDifferentPacketType() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class)
                    .match(p -> p.getData().contains("trigger")).then("cmd")
                    .successWhen(FakePacketB.class, p -> p.getData().contains("done"))
                .step(FakePacketA.class).match(p -> true).then("cmd2")
                .add().cooldown(10000).build();

            flow.packetReceived(mockSession, new FakePacketA("trigger"));
            assertThat(flow.getCurrentStepIndex()).isEqualTo(0);

            flow.packetReceived(mockSession, new FakePacketB("nope"));
            assertThat(flow.getCurrentStepIndex()).isEqualTo(0);

            flow.packetReceived(mockSession, new FakePacketB("done"));
            assertThat(flow.getCurrentStepIndex()).isEqualTo(1);
        }

        @Test
        void onSuccessCallbackIsCalled() {
            AtomicReference<String> captured = new AtomicReference<>();
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> true).then("cmd")
                    .onSuccess(p -> captured.set(p.getData()))
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketA("done"));
            assertThat(captured.get()).isEqualTo("done");
        }

        @Test
        void onSuccessCalledOnlyWhenMatched() {
            AtomicReference<String> captured = new AtomicReference<>();
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class)
                    .match(p -> p.getData().contains("trigger")).then("cmd")
                    .onSuccess(p -> captured.set(p.getData()))
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketA("nope"));
            assertThat(captured.get()).isNull();
        }
    }

    @Nested
    class Reset {
        @Test
        void resetReturnsToInitialState() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> true).then("cmd1")
                .step(FakePacketA.class).match(p -> true).then("cmd2")
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketA("x"));
            assertThat(flow.getCurrentStepIndex()).isEqualTo(1);

            flow.reset();
            assertThat(flow.getCurrentStepIndex()).isEqualTo(0);
            assertThat(flow.getState()).isEqualTo(LoginFlow.FlowState.WAITING);
        }

        @Test
        void resetAfterCompletedAllowsReuse() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> true).then("cmd")
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketA("x"));
            assertThat(flow.getState()).isEqualTo(LoginFlow.FlowState.COMPLETED);
            assertThat(sentCommands).hasSize(1);

            flow.reset();
            flow.packetReceived(mockSession, new FakePacketA("x"));
            assertThat(sentCommands).hasSize(2);
        }

        @Test
        void resetNotifiesStateChangeListener() {
            List<LoginFlow.LoginFlowContext> changes = new ArrayList<>();
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> true).then("cmd")
                .add().cooldown(0).onStateChange(changes::add).build();

            flow.packetReceived(mockSession, new FakePacketA("x"));
            assertThat(changes).hasSize(1);
            assertThat(changes.get(0).state()).isEqualTo(LoginFlow.FlowState.COMPLETED);

            flow.reset();
            assertThat(changes).hasSize(2);
            assertThat(changes.get(1).state()).isEqualTo(LoginFlow.FlowState.WAITING);
        }
    }

    @Nested
    class CompletedState {
        @Test
        void completedFlowIgnoresFurtherPackets() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> true).then("cmd")
                .add().cooldown(0).build();

            flow.packetReceived(mockSession, new FakePacketA("x"));
            assertThat(flow.getState()).isEqualTo(LoginFlow.FlowState.COMPLETED);

            flow.packetReceived(mockSession, new FakePacketA("x"));
            flow.packetReceived(mockSession, new FakePacketA("x"));
            assertThat(sentCommands).hasSize(1);
        }
    }

    @Nested
    class FailedState {
        @Test
        void failedFlowIgnoresFurtherPackets() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> true).then("cmd")
                    .successWhen(p -> p.getData().contains("never"))
                .add().cooldown(0).stepTimeout(1).build();

            flow.packetReceived(mockSession, new FakePacketA("trigger"));
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}

            flow.packetReceived(mockSession, new FakePacketA("anything"));
            assertThat(flow.getState()).isEqualTo(LoginFlow.FlowState.FAILED);
            assertThat(sentCommands).hasSize(1);
        }

        @Test
        void resetFromFailedAllowsReuse() {
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> true).then("cmd")
                    .successWhen(p -> p.getData().contains("never"))
                .add().cooldown(0).stepTimeout(1).build();

            flow.packetReceived(mockSession, new FakePacketA("x"));
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            flow.packetReceived(mockSession, new FakePacketA("x"));
            assertThat(flow.getState()).isEqualTo(LoginFlow.FlowState.FAILED);

            flow.reset();
            assertThat(flow.getState()).isEqualTo(LoginFlow.FlowState.WAITING);
        }
    }

    @Nested
    class EventManagerIntegration {
        @Test
        void firesLoginFlowEvent() {
            EventManager mockEventManager = mock(EventManager.class);
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> true).then("cmd")
                .step(FakePacketA.class).match(p -> true).then("cmd2")
                .add().cooldown(0).eventManager(mockEventManager).build();

            flow.packetReceived(mockSession, new FakePacketA("x"));

            verify(mockEventManager, times(1)).callEvent(argThat(event -> {
                if (event instanceof xin.bbtt.mcbot.events.LoginFlowEvent e) {
                    return e.getStepIndex() == 1 && e.getFlowState() == LoginFlow.FlowState.WAITING;
                }
                return false;
            }));
        }
    }

    @Nested
    class StateChangeListener {
        @Test
        void stateChangeIsNotified() {
            List<LoginFlow.LoginFlowContext> changes = new ArrayList<>();
            LoginFlow flow = baseBuilder
                .step(FakePacketA.class).match(p -> true).then("cmd")
                .step(FakePacketA.class).match(p -> true).then("cmd2")
                .add().cooldown(0).onStateChange(changes::add).build();

            flow.packetReceived(mockSession, new FakePacketA("x"));
            assertThat(changes).hasSize(1);
            assertThat(changes.get(0).stepIndex()).isEqualTo(1);
            assertThat(changes.get(0).state()).isEqualTo(LoginFlow.FlowState.WAITING);

            flow.packetReceived(mockSession, new FakePacketA("x"));
            assertThat(changes).hasSize(2);
            assertThat(changes.get(1).stepIndex()).isEqualTo(2);
            assertThat(changes.get(1).state()).isEqualTo(LoginFlow.FlowState.COMPLETED);
        }
    }
}
