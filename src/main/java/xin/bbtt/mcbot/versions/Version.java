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

import org.jspecify.annotations.NonNull;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public record Version(int major, int minor, int patch, VersionStage stage) implements Comparable<Version> {

    private static final Pattern VERSION_PATTERN = Pattern.compile(
        "^(\\d+)\\.(\\d+)\\.(\\d+)-([A-Za-z]+)$"
    );

    public static @NonNull Version from(@NonNull String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version.trim());

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                "Invalid version format: " + version
                    + ". Expected format: major.minor.patch-stage"
            );
        }

        try {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = Integer.parseInt(matcher.group(3));

            VersionStage stage = VersionStage.valueOf(
                matcher.group(4).toUpperCase(Locale.ROOT)
            );

            return new Version(major, minor, patch, stage);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Version number is too large: " + version,
                e
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Unknown version stage in: " + version,
                e
            );
        }
    }

    @Override
    public int compareTo(@NonNull Version o) {
        if (o.major != major) {
            return Integer.compare(major, o.major);
        }
        if (o.minor != minor) {
            return Integer.compare(minor, o.minor);
        }
        if (o.patch != patch) {
            return Integer.compare(patch, o.patch);
        }
        return stage.compareTo(o.stage);
    }

    public boolean isOlderThan(Version other) {
        return compareTo(other) < 0;
    }

    public boolean isNewerThan(Version other) {
        return compareTo(other) > 0;
    }

    public boolean isAtLeast(Version other) {
        return compareTo(other) >= 0;
    }

    public boolean isAtMost(Version other) {
        return compareTo(other) <= 0;
    }

    @Override
    public @NonNull String toString() {
        return String.format("%d.%d.%d-%s", major, minor, patch, stage);
    }
}
