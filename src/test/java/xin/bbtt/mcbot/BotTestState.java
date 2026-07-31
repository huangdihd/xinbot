/*
 * Copyright (C) 2024-2026 huangdihd
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

package xin.bbtt.mcbot;

import java.lang.reflect.Field;

/**
 * Test-only access to {@link Bot}'s private state ({@code running}, {@code session}),
 * so plugin tests can simulate startup/runtime conditions on the Bot singleton.
 * Production code intentionally exposes no setters for these fields, so the
 * reflection is confined to this single helper.
 */
public final class BotTestState {

    private BotTestState() {
    }

    public static void setRunning(boolean running) throws ReflectiveOperationException {
        set("running", running);
    }

    public static void clearSession() throws ReflectiveOperationException {
        set("session", null);
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static void set(String fieldName, Object value) throws ReflectiveOperationException {
        Field field = Bot.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(Bot.INSTANCE, value);
    }
}
