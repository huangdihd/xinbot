package xin.bbtt.mcbot.JLine;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import lombok.Setter;
import org.jline.reader.LineReader;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class JLineConsoleAppender extends ConsoleAppender<ILoggingEvent> {
    @Setter
    private static LineReader lineReader;

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) return;

        Charset charset = StandardCharsets.UTF_8;

        if (encoder instanceof PatternLayoutEncoder ple) {
            charset = ple.getCharset();
        }

        byte[] bytes = getEncoder().encode(event);
        String logStr = new String(bytes, charset);

        if (lineReader != null) {
            lineReader.printAbove(logStr);
        } else {
            super.append(event);
        }

    }
}
