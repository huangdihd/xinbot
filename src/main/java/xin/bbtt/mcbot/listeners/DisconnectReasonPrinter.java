package xin.bbtt.mcbot.listeners;

import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Utils;

import static xin.bbtt.mcbot.Utils.parseColors;

public class DisconnectReasonPrinter extends SessionAdapter {
    private static final Logger log = LoggerFactory.getLogger(DisconnectReasonPrinter.class.getSimpleName());

    @Override
    public void disconnected(DisconnectedEvent event) {
        log.info(parseColors(Utils.toString(event.getReason())));
        log.error(event.getCause().getMessage(), event.getCause());
    }
}
