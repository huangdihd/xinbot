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

package xin.bbtt.mcbot.LoginFlow;

import org.geysermc.mcprotocollib.network.packet.Packet;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A single step in a {@link LoginFlow}.
 *
 * @param <T> the packet type this step listens for
 * @param <S> the success packet type (may differ from T)
 */
class LoginFlowStep<T extends Packet, S extends Packet> {
    final Class<T> packetClass;
    final Predicate<T> predicate;
    final String commandTemplate;
    final Predicate<S> successPredicate;
    final Class<S> successPacketClass;
    final Consumer<?> onSuccess;
    final String description;
    final CommandType commandType;
    final Predicate<?> skipPredicate;

    enum CommandType {
        LOGIN,
        REGISTER,
        GENERIC
    }

    LoginFlowStep(Class<T> packetClass,
                  Predicate<T> predicate,
                  String commandTemplate,
                  Class<S> successPacketClass,
                  Predicate<S> successPredicate,
                  Consumer<?> onSuccess,
                  String description,
                  CommandType commandType,
                  Predicate<?> skipPredicate) {
        this.packetClass = Objects.requireNonNull(packetClass, "packetClass");
        this.predicate = Objects.requireNonNull(predicate, "predicate");
        this.commandTemplate = commandTemplate;
        this.successPacketClass = successPacketClass;
        this.successPredicate = successPredicate;
        this.onSuccess = onSuccess;
        this.description = description != null ? description : packetClass.getSimpleName();
        this.commandType = commandType != null ? commandType : CommandType.GENERIC;
        this.skipPredicate = skipPredicate;
    }

    boolean matches(Packet packet) {
        return predicate.test(packetClass.cast(packet));
    }

    boolean isSuccess(Packet packet) {
        if (successPredicate == null) return false;
        return successPredicate.test(successPacketClass.cast(packet));
    }

    @SuppressWarnings("unchecked")
    boolean shouldSkip(Packet packet) {
        if (skipPredicate == null) return false;
        return ((Predicate<T>) skipPredicate).test(packetClass.cast(packet));
    }
}
