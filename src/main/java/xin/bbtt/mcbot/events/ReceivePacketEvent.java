package xin.bbtt.mcbot.events;

import lombok.Getter;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import xin.bbtt.mcbot.event.Event;
import xin.bbtt.mcbot.event.HandlerList;

public class ReceivePacketEvent<T extends MinecraftPacket> extends Event {
    @Getter
    private final T packet;
    private final static HandlerList HANDLERS = new HandlerList();

    public ReceivePacketEvent(T packet) {
        this.packet = packet;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
