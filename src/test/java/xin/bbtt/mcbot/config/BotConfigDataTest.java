/*
 * Copyright (C) 2026 huangdihd
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

package xin.bbtt.mcbot.config;

import org.geysermc.mcprotocollib.network.ProxyInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotConfigDataTest {

    private static BotConfigData.Proxy.ProxyInfo newInfo() {
        return new BotConfigData.Proxy.ProxyInfo();
    }

    @Test
    void parsesValidTypeUppercase() {
        BotConfigData.Proxy.ProxyInfo info = newInfo();
        info.setType("SOCKS5");
        assertEquals(ProxyInfo.Type.SOCKS5, info.getType());
    }

    @Test
    void parsesValidTypeCaseInsensitively() {
        BotConfigData.Proxy.ProxyInfo info = newInfo();
        info.setType("socks5");
        assertEquals(ProxyInfo.Type.SOCKS5, info.getType());
    }

    @Test
    void nullTypeBecomesNull() {
        BotConfigData.Proxy.ProxyInfo info = newInfo();
        info.setType(null);
        assertNull(info.getType());
    }

    @Test
    void emptyTypeBecomesNull() {
        BotConfigData.Proxy.ProxyInfo info = newInfo();
        info.setType("");
        assertNull(info.getType());
    }

    @Test
    void invalidTypeThrowsListingValidValues() {
        BotConfigData.Proxy.ProxyInfo info = newInfo();
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> info.setType("SOCKS6"));
        // message should help the user by listing the accepted values
        assertTrue(ex.getMessage().contains("SOCKS5"));
    }
}
