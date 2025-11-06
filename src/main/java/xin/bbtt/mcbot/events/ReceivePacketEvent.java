package xin.bbtt.mcbot.events;

import lombok.Getter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import xin.bbtt.mcbot.event.Event;
import xin.bbtt.mcbot.event.HandlerList;

public class ReceivePacketEvent<T extends Packet> extends Event {
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
