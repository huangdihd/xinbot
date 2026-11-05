/*
 * Copyright (C) 2024-2026 huangdihd
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

import org.junit.jupiter.api.Test;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.command.Command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class JfrCommandExecutorTest {
    @Test
    void startAndStopSubcommandsControlRecording() throws Exception {
        var manager = Bot.INSTANCE.getJfrManager();
        JfrCommandExecutor executor = new JfrCommandExecutor();
        Command command = new Command("jfr", new String[0], "", "jfr start / jfr stop / jfr status");
        Path output = null;
        try {
            executor.onCommand(command, "jfr", new String[]{"start"});
            assertThat(manager.isRunning()).isTrue();

            executor.onCommand(command, "jfr", new String[]{"status"});
            executor.onCommand(command, "jfr", new String[]{"stop"});
            output = manager.getLastFile();

            assertThat(manager.isRunning()).isFalse();
            assertThat(output).isNotEmptyFile();
        } finally {
            if (manager.isRunning()) manager.stop();
            if (output == null) output = manager.getLastFile();
            if (output != null) {
                Files.deleteIfExists(output);
                Path debugDirectory = Paths.get("debug");
                if (Files.isDirectory(debugDirectory)) {
                    try (var files = Files.list(debugDirectory)) {
                        if (files.findAny().isEmpty()) Files.delete(debugDirectory);
                    }
                }
            }
        }
    }
}
