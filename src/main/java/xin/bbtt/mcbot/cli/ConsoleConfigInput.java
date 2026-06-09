/*
 *   Copyright (C) 2024-2026 huangdihd
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

package xin.bbtt.mcbot.cli;

import xin.bbtt.mcbot.LangManager;
import xin.bbtt.mcbot.modpack.ModpackConfigSetup;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Reads modpack-setup values from the terminal, masking secret input via
 * {@link Console#readPassword} when a console is attached, and falling back to
 * standard input when it is not (e.g. piped input).
 */
final class ConsoleConfigInput implements ModpackConfigSetup.Input {
    private final BufferedReader fallback = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    @Override
    public String read(String promptKey, boolean secret) {
        String prompt = LangManager.get(promptKey) + " ";
        Console console = System.console();
        if (console != null) {
            String value = secret ? new String(console.readPassword("%s", prompt)) : console.readLine("%s", prompt);
            return value == null ? "" : value.trim();
        }
        System.out.print(prompt);
        System.out.flush();
        try {
            String line = fallback.readLine();
            return line == null ? "" : line.trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
