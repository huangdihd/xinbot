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
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.jline.reader.LineReader;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class JLineConsoleAppender extends ConsoleAppender<ILoggingEvent> {
    @Setter
    private static volatile LineReader lineReader;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) return;

        Charset charset = StandardCharsets.UTF_8;

        if (encoder instanceof PatternLayoutEncoder ple) {
            charset = ple.getCharset();
            super.setEncoder(encoder);
        }

        // 获取时间戳并格式化为青色
        String time = TIME_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp()).atZone(ZoneId.systemDefault()).toLocalTime());
        String coloredTime = Ansi.ansi().fg(Color.CYAN).a(time).reset().toString();
        
        // 获取日志级别并着色
        String level = event.getLevel().toString();
        String coloredLevel = level;
        switch (level) {
            case "ERROR":
                coloredLevel = Ansi.ansi().fg(Color.RED).a("ERROR").reset().toString();
                break;
            case "WARN":
                coloredLevel = Ansi.ansi().fg(Color.YELLOW).a("WARN").reset().toString();
                break;
            case "INFO":
                coloredLevel = Ansi.ansi().fg(Color.GREEN).a("INFO").reset().toString();
                break;
            case "DEBUG":
                coloredLevel = Ansi.ansi().fg(Color.BLUE).a("DEBUG").reset().toString();
                break;
            case "TRACE":
                coloredLevel = Ansi.ansi().fg(Color.MAGENTA).a("TRACE").reset().toString();
                break;
        }
        
        // 添加前缀
        String loggerName = event.getLoggerName();
        String prefix = "[XinBot]"; // 默认BOT前缀
        
        // 检查是否是插件日志
        if (loggerName != null) {
            // 如果loggerName包含"xin.bbtt.mcbot"，说明是BOT框架日志，使用[XinBot]前缀
            // 否则认为是插件日志，使用loggerName作为前缀
            if (!loggerName.startsWith("xin.bbtt.mcbot")) {
                prefix = "[" + loggerName + "]";
            }
        }
        
        // 为前缀添加黄色
        String coloredPrefix = Ansi.ansi().fg(Color.YELLOW).a(prefix).reset().toString();

        // 构建完整的日志消息
        String logStr = String.format("[%s %s] %s %s", 
                                     coloredTime, 
                                     coloredLevel, 
                                     coloredPrefix, 
                                     event.getFormattedMessage()) + System.lineSeparator();

        if (lineReader != null) {
            lineReader.printAbove(logStr);
        } else {
            super.append(event);
        }
    }
}