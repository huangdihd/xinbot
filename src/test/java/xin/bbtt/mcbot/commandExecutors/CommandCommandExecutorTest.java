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

package xin.bbtt.mcbot.commandExecutors;

import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.command.Command;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommandCommandExecutorTest {

    @AfterEach
    void cleanUp() throws ReflectiveOperationException {
        setSession(null);
        Thread.interrupted();
    }

    @Test
    void interruptedCompletionRemovesListenerAndPreservesInterrupt() throws Exception {
        ClientSession session = mock(ClientSession.class);
        when(session.isConnected()).thenReturn(true);
        setSession(session);

        Thread.currentThread().interrupt();
        assertThat(new CommandCommandExecutor().onTabComplete(
                new Command("command", null, "", ""), "command", new String[]{"say"}))
                .isEmpty();
        assertThat(Thread.currentThread().isInterrupted()).isTrue();

        ArgumentCaptor<SessionListener> listener = ArgumentCaptor.forClass(SessionListener.class);
        verify(session).addListener(listener.capture());
        verify(session).removeListener(listener.getValue());
    }

    private static void setSession(ClientSession session) throws ReflectiveOperationException {
        Field field = Bot.class.getDeclaredField("session");
        field.setAccessible(true);
        field.set(Bot.INSTANCE, session);
    }
}
