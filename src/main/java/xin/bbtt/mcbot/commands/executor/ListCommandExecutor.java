/*
 *   Copyright (C) 2026 huangdihd
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

package xin.bbtt.mcbot.commands.executor;

import org.geysermc.mcprotocollib.auth.GameProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.TabExecutor;

import java.util.List;
import java.util.stream.Collectors;

public class ListCommandExecutor extends TabExecutor {
    private final static Logger log = LoggerFactory.getLogger(ListCommandExecutor.class.getSimpleName());
    @Override
    public void onCommand(Command command, String label, String[] args) {

        if (args.length == 1 &&args[0].equals("uuid")) {
            log.info("There are {} players online: {}", Bot.Instance.players.size(),
                    Bot.Instance.players.values().parallelStream().map((
                            gameProfile -> gameProfile.getName() + "(" + gameProfile.getId() + ")")
                    ).collect(Collectors.joining(", ")
                    )
            );
            return;
        }
        log.info("There are {} players online: {}", Bot.Instance.players.size(),
                Bot.Instance.players.values().parallelStream().map(GameProfile::getName).collect(Collectors.joining(", "))
        );
    }

    @Override
    public List<String> onTabComplete(Command command, String label, String[] args) {
        return List.of("uuid");
    }
}
