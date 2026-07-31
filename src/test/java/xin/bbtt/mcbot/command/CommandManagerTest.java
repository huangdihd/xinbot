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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandManagerTest {

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
}
