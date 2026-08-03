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

package xin.bbtt.mcbot.command;

import org.junit.jupiter.api.Test;
import xin.bbtt.mcbot.plugin.DummyLibPlugin;
import xin.bbtt.mcbot.plugin.DummyPlugin;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandManagerTest {

    private static final CommandExecutor NO_OP_EXECUTOR = new CommandExecutor() {
        @Override
        public void onCommand(Command command, String label, String[] args) {
        }
    };

    @Test
    void testTokenizeBasic() {
        String input = "say hello world";
        List<String> tokens = CommandManager.tokenize(input);
        
        assertThat(tokens).containsExactly("say", "hello", "world");
    }

    @Test
    void testTokenizeWithQuotes() {
        String input = "say \"hello world\"";
        List<String> tokens = CommandManager.tokenize(input);
        
        assertThat(tokens).containsExactly("say", "hello world");
    }

    @Test
    void testTokenizeWithSingleQuotes() {
        String input = "say 'hello world'";
        List<String> tokens = CommandManager.tokenize(input);
        
        assertThat(tokens).containsExactly("say", "hello world");
    }

    @Test
    void testTokenizeWithEscapedQuotes() {
        String input = "say \"hello \\\"world\\\"\"";
        List<String> tokens = CommandManager.tokenize(input);
        
        assertThat(tokens).containsExactly("say", "hello \"world\"");
    }

    @Test
    void testTokenizeWithMultipleSpaces() {
        String input = "say   hello    world";
        List<String> tokens = CommandManager.tokenize(input);
        
        assertThat(tokens).containsExactly("say", "hello", "world");
    }

    @Test
    void testTokenizeTrailingSpace() {
        String input = "say hello ";
        List<String> tokens = CommandManager.tokenize(input);
        
        assertThat(tokens).containsExactly("say", "hello", "");
    }
    
    @Test
    void testTokenizeEmpty() {
        String input = "";
        List<String> tokens = CommandManager.tokenize(input);
        
        assertThat(tokens).isEmpty();
    }

    @Test
    void uniquePluginCommandOnlyUsesShortNames() {
        CommandManager manager = new CommandManager();
        manager.registerCommand(new Command("bttb", new String[]{"base"}, "", ""),
                NO_OP_EXECUTOR, new DummyPlugin());

        assertThat(manager.getCommandNames("b")).contains("bttb", "base");
        assertThat(manager.getCommandNames(""))
                .doesNotContain("DummyPlugin:bttb", "DummyPlugin:base");
        assertThat(manager.getCommandNames("Dummy"))
                .containsExactlyInAnyOrder("DummyPlugin:bttb", "DummyPlugin:base");
    }

    @Test
    void qualifiedCompletionIsHiddenOnlyWhenBareAliasMatches() {
        CommandManager manager = new CommandManager();
        manager.registerCommand(new Command("dummy", null, "", ""),
                NO_OP_EXECUTOR, new DummyPlugin());

        assertThat(manager.getCommandNames("dumm")).containsExactly("dummy");
        assertThat(manager.getCommandNames("DummyPlugin"))
                .containsExactly("DummyPlugin:dummy");
    }

    @Test
    void conflictingPluginCommandsOnlyUseQualifiedNames() {
        CommandManager manager = new CommandManager();
        DummyPlugin first = new DummyPlugin();
        DummyLibPlugin second = new DummyLibPlugin();
        manager.registerCommand(new Command("shared", null, "", ""), NO_OP_EXECUTOR, first);
        manager.registerCommand(new Command("shared", null, "", ""), NO_OP_EXECUTOR, second);

        assertThat(manager.getCommandNames("sha"))
                .containsExactlyInAnyOrder("DummyPlugin:shared", "DummyLibPlugin:shared")
                .doesNotContain("shared");

        manager.unregisterAll(second);
        assertThat(manager.getCommandNames("sha")).containsExactly("shared");
    }

    @Test
    void conflictsAreCaseInsensitive() {
        CommandManager manager = new CommandManager();
        manager.registerCommand(new Command("Shared", null, "", ""),
                NO_OP_EXECUTOR, new DummyPlugin());
        manager.registerCommand(new Command("shared", null, "", ""),
                NO_OP_EXECUTOR, new DummyLibPlugin());

        assertThat(manager.getCommandNames("SHARED"))
                .containsExactlyInAnyOrder("DummyPlugin:Shared", "DummyLibPlugin:shared")
                .doesNotContain("Shared", "shared");
    }

    @Test
    void coreParticipatesInConflictsButUniqueAliasesStayShort() {
        CommandManager manager = new CommandManager();
        manager.registerCommand(new Command("say", null, "", ""),
                NO_OP_EXECUTOR, new DummyPlugin());

        assertThat(manager.getCommandNames("say"))
                .containsExactly("Core:say", "DummyPlugin:say");
        assertThat(manager.getCommandNames("chat")).containsExactly("chat");
    }
}
