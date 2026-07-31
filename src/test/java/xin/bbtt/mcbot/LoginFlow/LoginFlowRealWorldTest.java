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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LoginFlowRealWorldTest {

    private Session mockSession;
    private List<String> sentCommands;
    private LoginFlowBuilder baseBuilder;

    static class FakePacketA implements Packet {
        private final String data;
        FakePacketA(String data) { this.data = data; }
        String getData() { return data; }
    }

    @BeforeEach
    void setUp() {
        mockSession = mock(Session.class);
        sentCommands = new ArrayList<>();
        baseBuilder = LoginFlow.builder(sentCommands::add);
    }

    @Test
    void twoBtXinLoginPattern() {
        AtomicReference<String> loginEvent = new AtomicReference<>();

        LoginFlow flow = baseBuilder
            .templateExpander(t -> t.replace("{password}", "mypassword"))
            .step(FakePacketA.class)
                .match(p -> p.getData().contains("注册"))
                .then("reg {password} {password}")
                .describe("Register")
            .step(FakePacketA.class)
                .match(p -> p.getData().contains("登陆成功"))
                .onSuccess(p -> loginEvent.set("success"))
                .describe("Wait for login success")
            .add().cooldown(0).build();

        flow.packetReceived(mockSession, new FakePacketA("请注册"));
        assertThat(sentCommands).containsExactly("reg mypassword mypassword");
        assertThat(flow.getCurrentStepIndex()).isEqualTo(1);

        flow.packetReceived(mockSession, new FakePacketA("登陆成功"));
        assertThat(loginEvent.get()).isEqualTo("success");
        assertThat(flow.getState()).isEqualTo(LoginFlow.FlowState.COMPLETED);
    }

    @Test
    void threeC3uLoginPattern() {
        AtomicReference<String> loginEvent = new AtomicReference<>();

        LoginFlow flow = baseBuilder
            .templateExpander(t -> t.replace("{password}", "pass123"))
            .step(FakePacketA.class)
                .match(p -> p.getData().contains("使用指令登录"))
                .then("l {password}")
                .describe("Send login command")
            .step(FakePacketA.class)
                .match(p -> p.getData().contains("成功登录"))
                .onSuccess(p -> loginEvent.set("success"))
                .describe("Wait for login success")
            .add().cooldown(0).build();

        flow.packetReceived(mockSession, new FakePacketA("§c使用指令登录: /login <password>"));
        assertThat(sentCommands).containsExactly("l pass123");

        flow.packetReceived(mockSession, new FakePacketA("§2§l成功登录!"));
        assertThat(loginEvent.get()).isEqualTo("success");
        assertThat(flow.getState()).isEqualTo(LoginFlow.FlowState.COMPLETED);
    }

    @Test
    void registerThenLoginThenJoinPattern() {
        List<String> events = new ArrayList<>();

        LoginFlow flow = baseBuilder
            .templateExpander(t -> t.replace("{password}", "pw123"))
            .step(FakePacketA.class)
                .match(p -> p.getData().contains("register"))
                .then("reg {password} {password}")
                .onSuccess(p -> events.add("registered"))
            .step(FakePacketA.class)
                .match(p -> p.getData().contains("login success"))
                .onSuccess(p -> events.add("logged_in"))
            .step(FakePacketA.class)
                .match(p -> p.getData().contains("join"))
                .then("join")
                .onSuccess(p -> events.add("joined"))
            .add().cooldown(0).build();

        flow.packetReceived(mockSession, new FakePacketA("please register"));
        flow.packetReceived(mockSession, new FakePacketA("login success"));
        flow.packetReceived(mockSession, new FakePacketA("click join"));

        assertThat(sentCommands).containsExactly("reg pw123 pw123", "join");
        assertThat(events).containsExactly("registered", "logged_in", "joined");
        assertThat(flow.getState()).isEqualTo(LoginFlow.FlowState.COMPLETED);
    }
}
