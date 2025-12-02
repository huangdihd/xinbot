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

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import lombok.Setter;
import org.jline.reader.LineReader;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class JLineConsoleAppender extends ConsoleAppender<ILoggingEvent> {
    @Setter
    private static volatile LineReader lineReader;

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) return;

        Charset charset = StandardCharsets.UTF_8;

        if (encoder instanceof PatternLayoutEncoder ple) {
            charset = ple.getCharset();
            super.setEncoder(encoder);
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
