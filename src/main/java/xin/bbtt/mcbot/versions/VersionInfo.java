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

package xin.bbtt.mcbot.versions;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

public record VersionInfo(
    Version latestVersion,
    URI downloadUrl,
    URI releaseUrl,
    Instant updatedAt
) {
    public VersionInfo {
        Objects.requireNonNull(latestVersion, "latestVersion");
        Objects.requireNonNull(downloadUrl, "downloadUrl");
        Objects.requireNonNull(releaseUrl, "releaseUrl");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
