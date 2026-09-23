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

package xin.bbtt.mcbot.cli;

import java.util.List;

/**
 * Descriptor for a single CLI sub-command (a flag such as {@code --install}).
 *
 * <p>Adding a new sub-command is a matter of registering one of these in
 * {@link Cli}; the dispatcher handles alias lookup and argument-count
 * validation generically, so no per-command boilerplate is needed.
 *
 * @param aliases  the flags that trigger this command, e.g. {@code [--install, -i]}
 * @param minArity the minimum number of arguments expected after the flag
 * @param maxArity the maximum number of arguments expected after the flag
 * @param usageKey the {@link xin.bbtt.mcbot.LangManager} key printed when the
 *                 argument count is wrong
 * @param handler  the action to run with the arguments following the flag
 */
public record Subcommand(List<String> aliases, int minArity, int maxArity, String usageKey, Handler handler) {

    public Subcommand {
        if (minArity < 0 || maxArity < minArity) {
            throw new IllegalArgumentException("Invalid argument-count range");
        }
    }

    public boolean acceptsArgumentCount(int count) {
        return count >= minArity && count <= maxArity;
    }

    /**
     * The action behind a sub-command. Implementations are expected to report
     * their own (localized) errors and return {@code false} on failure rather
     * than throwing.
     */
    @FunctionalInterface
    public interface Handler {
        boolean run(String[] args);
    }
}
