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

import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.Utils;

import static xin.bbtt.mcbot.Utils.parseColors;

public class DisconnectReasonPrinter extends SessionAdapter {
    private static final Logger log = LoggerFactory.getLogger(DisconnectReasonPrinter.class.getSimpleName());

    @Override
    public void disconnected(DisconnectedEvent event) {
        Bot.Instance.setServer(null);
        log.info(parseColors(Utils.toString(event.getReason())));
        log.error(event.getCause().getMessage(), event.getCause());
    }
}
