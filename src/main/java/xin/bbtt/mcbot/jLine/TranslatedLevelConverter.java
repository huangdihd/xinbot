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

package xin.bbtt.mcbot.jLine;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import xin.bbtt.mcbot.LangManager;

public class TranslatedLevelConverter extends ClassicConverter {
    @Override
    public String convert(ILoggingEvent event) {
        String levelStr = event.getLevel().toString();
        String key = "xinbot.level." + levelStr.toLowerCase();

        String translated = LangManager.get(key);

        return translated.equals(key) ? levelStr : translated;
        }
        }
