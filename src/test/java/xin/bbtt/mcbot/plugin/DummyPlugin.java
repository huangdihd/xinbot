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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyPlugin implements Plugin {
    private static final Logger log = LoggerFactory.getLogger(DummyPlugin.class);
    
    public static int loadCount = 0;
    public static int enableCount = 0;
    public static int disableCount = 0;
    public static int unloadCount = 0;

    @Override
    public void onLoad() {
        loadCount++;
        log.info("DummyPlugin loaded");
    }

    @Override
    public void onEnable() {
        enableCount++;
        log.info("DummyPlugin enabled");
    }

    @Override
    public void onDisable() {
        disableCount++;
        log.info("DummyPlugin disabled");
    }

    @Override
    public void onUnload() {
        unloadCount++;
        log.info("DummyPlugin unloaded");
    }
    
    public static void reset() {
        loadCount = 0;
        enableCount = 0;
        disableCount = 0;
        unloadCount = 0;
    }
}
