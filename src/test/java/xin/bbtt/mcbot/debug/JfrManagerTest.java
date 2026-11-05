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

package xin.bbtt.mcbot.debug;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JfrManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void startAndStopCreatesRecordingFile() throws Exception {
        JfrManager manager = new JfrManager(tempDir.resolve("debug"));
        try {
            Path output = manager.start();

            assertThat(manager.isRunning()).isTrue();
            assertThat(manager.getCurrentFile()).isEqualTo(output);
            assertThat(output).hasParent(tempDir.resolve("debug").toAbsolutePath());
            assertThat(output.getFileName().toString()).endsWith(".jfr");

            assertThat(manager.stop()).isEqualTo(output);
            assertThat(manager.isRunning()).isFalse();
            assertThat(manager.getCurrentFile()).isNull();
            assertThat(manager.getLastFile()).isEqualTo(output);
            assertThat(output).isNotEmptyFile();
        } finally {
            if (manager.isRunning()) manager.stop();
        }
    }

    @Test
    void rejectsDuplicateStartAndStopWithoutRecording() throws Exception {
        JfrManager manager = new JfrManager(tempDir.resolve("debug"));
        assertThatThrownBy(manager::stop).isInstanceOf(IllegalStateException.class);

        manager.start();
        try {
            assertThatThrownBy(manager::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already running");
        } finally {
            manager.stop();
        }
        assertThatThrownBy(manager::stop).isInstanceOf(IllegalStateException.class);
    }
}
