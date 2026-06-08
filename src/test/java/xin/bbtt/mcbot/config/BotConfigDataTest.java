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
