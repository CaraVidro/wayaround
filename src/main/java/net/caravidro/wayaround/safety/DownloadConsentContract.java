package net.caravidro.wayaround.safety;

import java.net.URI;
import java.util.Objects;

/**
 * Small, registry-free consent contract shared by runtime download paths.
 *
 * A grant is valid only for the exact resource, exact byte count, exact
 * one-time token and a bounded lifetime. It is deliberately independent of
 * Minecraft classes so the contract can be regression-tested without booting
 * the game.
 */
public final class DownloadConsentContract {

    private DownloadConsentContract() {}

    public record Grant(
            String resourceId,
            long bytes,
            long token,
            long expiresAt
    ) {
        public Grant {
            Objects.requireNonNull(resourceId, "resourceId");

            if (resourceId.isBlank()) {
                throw new IllegalArgumentException("resourceId must not be blank");
            }

            if (bytes <= 0L) {
                throw new IllegalArgumentException("bytes must be positive");
            }

            if (token == 0L) {
                throw new IllegalArgumentException("token must be non-zero");
            }

            if (expiresAt <= 0L) {
                throw new IllegalArgumentException("expiresAt must be positive");
            }
        }

        public boolean matches(
                String resourceId,
                long bytes,
                long token,
                long now
        ) {
            return this.resourceId.equals(resourceId)
                    && this.bytes == bytes
                    && this.token == token
                    && now >= 0L
                    && now <= expiresAt;
        }
    }

    /**
     * Remote model downloads are pinned to one HTTPS host. Redirects are
     * disabled separately at the HttpClient boundary.
     */
    public static boolean isExactHttpsHost(
            URI uri,
            String expectedHost
    ) {
        if (uri == null
                || expectedHost == null
                || expectedHost.isBlank()) {
            return false;
        }

        int port = uri.getPort();

        return "https".equalsIgnoreCase(uri.getScheme())
                && expectedHost.equalsIgnoreCase(uri.getHost())
                && uri.getUserInfo() == null
                && (port == -1 || port == 443);
    }
}
