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

package xin.bbtt.mcbot.modpack;

import xin.bbtt.mcbot.config.BotConfig;
import xin.bbtt.mcbot.config.BotConfigData;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interactively fills the credentials and owner that
 * {@link ModpackConfig#sanitize} blanked when a bundled config is installed,
 * then writes the completed {@code config.conf} back to disk.
 *
 * <p>Input is abstracted via {@link Input} so the flow can be driven by a real
 * console at runtime and by a stub in tests.
 */
public final class ModpackConfigSetup {

    /** Supplies values for the prompted fields. */
    @FunctionalInterface
    public interface Input {
        /**
         * @param promptKey a {@link xin.bbtt.mcbot.LangManager} key describing the field
         * @param secret    whether the value is sensitive (e.g. a password) and should be masked
         * @return the entered value (may be empty)
         */
        String read(String promptKey, boolean secret);
    }

    /**
     * Prompts for the blanked fields and saves the config.
     *
     * @param configFile the freshly extracted {@code config.conf} to complete
     * @param input      the source of user-entered values
     * @throws IOException if the config cannot be read or written
     */
    public static void complete(Path configFile, Input input) throws IOException {
        BotConfig config = new BotConfig(configFile.toString());
        BotConfigData data = config.getConfigData();

        data.setOwner(input.read("xinbot.modpack.setup.owner", false));

        BotConfigData.Account account = data.getAccount();
        if (account != null) {
            account.setName(input.read("xinbot.modpack.setup.account_name", false));
            account.setPassword(input.read("xinbot.modpack.setup.account_password", true));
        }

        BotConfigData.Proxy proxy = data.getProxy();
        if (proxy != null && proxy.isEnable() && proxy.getInfo() != null) {
            proxy.getInfo().setUsername(input.read("xinbot.modpack.setup.proxy_username", false));
            proxy.getInfo().setPassword(input.read("xinbot.modpack.setup.proxy_password", true));
        }

        config.saveToFile();
    }

    private ModpackConfigSetup() {}
}
