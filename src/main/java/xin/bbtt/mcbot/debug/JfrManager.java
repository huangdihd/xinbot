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

import jdk.jfr.Recording;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JfrManager {
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final Path debugDirectory;
    private Recording recording;
    private Path currentFile;
    private Path lastFile;

    public JfrManager() {
        this(Paths.get("debug"));
    }

    JfrManager(Path debugDirectory) {
        this.debugDirectory = debugDirectory;
    }

    public synchronized Path start() throws IOException {
        if (recording != null) {
            throw new IllegalStateException("JFR recording is already running");
        }

        Files.createDirectories(debugDirectory);
        Path destination = debugDirectory
            .resolve("xinbot-" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".jfr")
            .toAbsolutePath();
        Recording newRecording = new Recording();
        try {
            newRecording.setName("Xinbot");
            newRecording.setToDisk(true);
            newRecording.setDestination(destination);
            newRecording.start();
        } catch (RuntimeException e) {
            newRecording.close();
            throw e;
        }

        recording = newRecording;
        currentFile = destination;
        lastFile = destination;
        return destination;
    }

    public synchronized Path stop() {
        if (recording == null) {
            throw new IllegalStateException("JFR recording is not running");
        }

        Recording activeRecording = recording;
        recording = null;
        currentFile = null;
        try {
            activeRecording.stop();
        } finally {
            activeRecording.close();
        }
        return lastFile;
    }

    public synchronized boolean isRunning() {
        return recording != null;
    }

    public synchronized Path getCurrentFile() {
        return currentFile;
    }

    public synchronized Path getLastFile() {
        return lastFile;
    }
}
