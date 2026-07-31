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

package xin.bbtt.mcbot.plugin;

import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import xin.bbtt.mcbot.Server;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

public class DummyMetaPlugin implements MetaPlugin {

    // Shared lifecycle event log so tests can assert ordering across plugins.
    public static final List<String> events = new ArrayList<>();

    @Override
    public void onLoad() {
        events.add("meta-load");
    }

    @Override
    public void onEnable() {
        events.add("meta-enable");
    }

    @Override
    public void onDisable() {
        events.add("meta-disable");
    }

    @Override
    public void onUnload() {
        events.add("meta-unload");
    }

    @Override
    public SocketAddress getServerSocketAddress() {
        return null;
    }

    @Override
    public Server getServer(ClientboundLoginPacket loginPacket) {
        return null;
    }

    public static void reset() {
        events.clear();
    }
}
