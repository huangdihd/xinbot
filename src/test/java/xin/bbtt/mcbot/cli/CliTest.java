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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CliTest {
    @Test
    void parsesDebugFlagBeforeConfigPath() {
        Cli cli = Cli.create();

        String[] args = cli.parseArgs(new String[]{"--debug", "custom.conf"});

        assertThat(cli.isDebugMode()).isTrue();
        assertThat(args).containsExactly("custom.conf");
    }

    @Test
    void parsesDebugFlagAfterConfigPath() {
        Cli cli = Cli.create();

        String[] args = cli.parseArgs(new String[]{"custom.conf", "--debug"});

        assertThat(cli.isDebugMode()).isTrue();
        assertThat(args).containsExactly("custom.conf");
    }

    @Test
    void preservesOtherArgumentsAndResetsDebugState() {
        Cli cli = Cli.create();
        cli.parseArgs(new String[]{"--debug", "--debug"});

        String[] args = cli.parseArgs(new String[]{"--install", "bundle.zip"});

        assertThat(cli.isDebugMode()).isFalse();
        assertThat(args).containsExactly("--install", "bundle.zip");
    }
}
