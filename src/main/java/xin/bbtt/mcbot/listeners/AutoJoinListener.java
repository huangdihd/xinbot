/*
 *   Copyright (C) 2024-2025 huangdihd
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

package xin.bbtt.mcbot.listeners;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.text.TextComponent;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.Server;
import xin.bbtt.mcbot.events.ClickJoinItemEvent;

public class AutoJoinListener extends SessionAdapter {
    private static final Logger log = LoggerFactory.getLogger(AutoJoinListener.class.getSimpleName());
    private int containerId = -1;
    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundOpenScreenPacket openScreenPacket) recordContainer(openScreenPacket);
        if (packet instanceof ClientboundContainerSetContentPacket containerSetContentPacket) clickButton(containerSetContentPacket, session);
    }

    private void recordContainer(ClientboundOpenScreenPacket openScreenPacket) {
        if (Bot.Instance.getServer() != Server.Login) return;
        if (!(openScreenPacket.getTitle() instanceof TextComponent title)) return;
        if (!title.content().contains("Game") && !title.content().contains("戏") && !title.content().contains("队") && !title.content().contains("入")) return;
        containerId = openScreenPacket.getContainerId();
        log.debug("Recorded container id {}", containerId);
    }

    private void clickButton(ClientboundContainerSetContentPacket containerSetContentPacket, Session session) {
        if (Bot.Instance.getServer() != Server.Login) return;
        if (!(containerSetContentPacket.getContainerId() == containerId)) return;
        ItemStack[] items = containerSetContentPacket.getItems();
        for (int slot = 0; slot < items.length; slot++) {
            ItemStack itemStack = items[slot];
            if (itemStack == null) continue;
            if (!itemStack.toString().contains("Game") && !itemStack.toString().contains("戏") && !itemStack.toString().contains("队") && !itemStack.toString().contains("入")) continue;
            Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectOpenHashMap<>();
            changedSlots.put(slot, null);
            ClickJoinItemEvent clickJoinItemEvent = new ClickJoinItemEvent();
            Bot.Instance.getPluginManager().events().callEvent(clickJoinItemEvent);
            if (clickJoinItemEvent.isDefaultActionCancelled()) return;
            session.send(new ServerboundContainerClickPacket(
                    containerId,
                    containerSetContentPacket.getStateId(),
                    slot,
                    ContainerActionType.CLICK_ITEM,
                    ClickItemAction.LEFT_CLICK,
                    itemStack,
                    changedSlots
            ));
            containerId = -1;
            return;
        }
    }
}
