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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.Utils;
import xin.bbtt.mcbot.events.AnswerQuestionEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AnswerQuestionListener extends SessionAdapter {
private static final JsonObject questions;

static {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
            Objects.requireNonNull(AnswerQuestionListener.class.getClassLoader().getResourceAsStream("questions.json")),
            StandardCharsets.UTF_8))) {
        String content = reader.lines().collect(Collectors.joining("\n"));
        questions = JsonParser.parseString(content).getAsJsonObject();
    } catch (Exception e) {
        throw new RuntimeException("Failed to load questions.json", e);
    }
}

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!(packet instanceof ClientboundSystemChatPacket systemChatPacket)) return;
        String fullQuestion = Utils.toString(systemChatPacket.getContent());
        if (!fullQuestion.contains("丨")) return;
        String[] parts = fullQuestion.split("丨");
        if (parts.length != 2) return;
        String question = parts[0];
        String options = parts[1];
        if (!questions.has(question)) return;
        Pattern pattern = Pattern.compile(questions.get(question).getAsString());
        Matcher matcher = pattern.matcher(options);

        if (!matcher.find()) return;
        String answer = matcher.group(1);
        AnswerQuestionEvent answerQuestionEvent = new AnswerQuestionEvent(question, answer);

        if (!answerQuestionEvent.isDefaultActionCancelled())
            Bot.Instance.sendChatMessage(answerQuestionEvent.getAnswer());
    }
}
