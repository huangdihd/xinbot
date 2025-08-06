package xin.bbtt.mcbot;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class CLI {
    public static LineReader lineReader;

    public static void init() {
        try {
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .encoding("UTF-8")
                    .build();

            lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            JLineConsoleAppender.setLineReader(lineReader);

        } catch (Exception e) {
            System.err.println("Failed to initialize JLine: " + e.getMessage());
        }
    }
}