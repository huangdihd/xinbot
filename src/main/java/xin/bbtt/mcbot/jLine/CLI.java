/*
 *   Copyright (C) 2024-2025 huangdihd
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

package xin.bbtt.mcbot.jLine;

import lombok.Getter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class CLI {
    @Getter
    private static LineReader lineReader;

    public static void init() {
        try {
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .encoding("UTF-8")
                    .build();

            lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .option(LineReader.Option.CASE_INSENSITIVE, true)
                    .completer(new JLineCommandCompleter())
                    .build();

            JLineConsoleAppender.setLineReader(lineReader);

        } catch (Exception e) {
            System.err.println("Failed to initialize JLine: " + e.getMessage());
        }
    }
}