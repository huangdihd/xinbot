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

package xin.bbtt.mcbot.util;

import org.junit.jupiter.api.Test;
import xin.bbtt.mcbot.Utils;

import static org.assertj.core.api.Assertions.assertThat;

public class UtilsTest {

    @Test
    void testParseColors() {
        // Test basic color replacement
        String input1 = "§aHello §cWorld";
        String expected1 = "\u001B[97m\u001B[92mHello \u001B[91mWorld\u001B[0m";
        assertThat(Utils.parseColors(input1)).isEqualTo(expected1);

        // Test format codes
        String input2 = "§lBold §oItalic";
        String expected2 = "\u001B[97m\u001B[1mBold \u001B[3mItalic\u001B[0m";
        assertThat(Utils.parseColors(input2)).isEqualTo(expected2);

        // Test combined codes
        String input3 = "§a§lGreen Bold";
        String expected3 = "\u001B[97m\u001B[92m\u001B[1mGreen Bold\u001B[0m";
        assertThat(Utils.parseColors(input3)).isEqualTo(expected3);

        // Test §r§ replaced with §
        String input4 = "Hello §r§aWorld";
        String expected4 = "\u001B[97mHello \u001B[92mWorld\u001B[0m";
        assertThat(Utils.parseColors(input4)).isEqualTo(expected4);
        
        // Test unknown color code
        String input5 = "§zUnknown";
        // Assuming unknown codes are ignored or kept verbatim, 
        // looking at the implementation, they are kept verbatim if not 0-f or k-r
        String expected5 = "\u001B[97m§zUnknown\u001B[0m";
        assertThat(Utils.parseColors(input5)).isEqualTo(expected5);
    }

    @Test
    void testGetOfflineUUID() {
        String playerName = "Notch";
        String expectedUUID = "b50ad385-829d-3141-a216-7e7d7539ba7f";
        assertThat(Utils.getOfflineUUID(playerName).toString()).isEqualTo(expectedUUID);
    }
}
