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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

public final class UpdateChecker {

    public static final String VERSION_URL =
        "https://xinbot.shouldbe.top/data/version.json";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private UpdateChecker() {
    }

    public static @NonNull VersionInfo fetchLatestVersionInfo() {
        return fetchLatestVersionInfo(VERSION_URL);
    }

    public static @NonNull VersionInfo fetchLatestVersionInfo(
        @NonNull String versionUrl
    ) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(versionUrl))
            .timeout(Duration.ofSeconds(15))
            .header("Accept", "application/json")
            .GET()
            .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                    "Failed to fetch version information: HTTP "
                        + response.statusCode()
                );
            }

            VersionResponse data = OBJECT_MAPPER.readValue(
                response.body(),
                VersionResponse.class
            );

            return new VersionInfo(
                Version.from(data.latestVersion()),
                URI.create(data.downloadUrl()),
                URI.create(data.releaseUrl()),
                Instant.parse(data.updatedAt())
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                "Fetching version information was interrupted",
                e
            );
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException(
                "Failed to fetch version information from " + versionUrl,
                e
            );
        }
    }

    private record VersionResponse(
        @JsonProperty("latest_version")
        String latestVersion,

        @JsonProperty("download_url")
        String downloadUrl,

        @JsonProperty("release_url")
        String releaseUrl,

        @JsonProperty("updated_at")
        String updatedAt
    ) {
    }
}
