package xin.bbtt.mcbot.JLine;

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
                    .completer(new JLineServerCompleter())
                    .build();

            JLineConsoleAppender.setLineReader(lineReader);

        } catch (Exception e) {
            System.err.println("Failed to initialize JLine: " + e.getMessage());
        }
    }
}