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

import xin.bbtt.mcbot.config.BotConfigData;

import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipFile;

/**
 * Helpers for bundling a bot {@code config.conf} inside a modpack.
 *
 * <p>A config is only ever shipped after {@link #sanitize(BotConfigData)} clears
 * every secret (account credentials/session, owner and proxy credentials). The
 * blanked fields are filled back in interactively at install time by
 * {@link ModpackConfigSetup}.
 */
public final class ModpackConfig {
    /** Path of the bundled config entry inside a modpack archive. */
    public static final String ENTRY = "config/config.conf";

    /**
     * Blanks every sensitive field so the config can be shared safely:
     * account name/password/session, owner, and proxy username/password.
     * Non-sensitive settings (reconnect, plugin directory, translation,
     * online mode, proxy address/type) are preserved.
     */
    public static void sanitize(BotConfigData data) {
        if (data == null) return;
        data.setOwner("");

        BotConfigData.Account account = data.getAccount();
        if (account != null) {
            account.setName("");
            account.setPassword("");
            account.setFullSession(null);
        }

        BotConfigData.Proxy proxy = data.getProxy();
        if (proxy != null && proxy.getInfo() != null) {
            proxy.getInfo().setUsername("");
            proxy.getInfo().setPassword("");
        }
    }

    /** @return true if the modpack archive bundles a {@code config.conf}. */
    public static boolean isPresentIn(Path zipFile) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            return zip.getEntry(ENTRY) != null;
        }
    }

    private ModpackConfig() {}
}
