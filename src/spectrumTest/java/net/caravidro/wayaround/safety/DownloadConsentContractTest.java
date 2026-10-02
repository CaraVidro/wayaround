package net.caravidro.wayaround.safety;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class DownloadConsentContractTest {
    private static int checks;

    private DownloadConsentContractTest() {}

    public static void main(String[] args) throws Exception {
        exactGrantOnly();
        expirationIsEnforced();
        invalidGrantConstructionIsRejected();
        remoteHostPinningIsStrict();
        runtimeDownloadSinksAreKnown();
        loaderAutoUpdateRemainsDisabled();

        System.out.println(
                "PASS: "
                        + checks
                        + " informed-download consent/security checks"
        );
    }

    private static void exactGrantOnly() {
        var grant = new DownloadConsentContract.Grant(
                "recording:abc",
                4_096L,
                77L,
                1_000L
        );

        check(
                grant.matches(
                        "recording:abc",
                        4_096L,
                        77L,
                        999L
                ),
                "exact approved transfer must match"
        );

        check(
                !grant.matches(
                        "recording:def",
                        4_096L,
                        77L,
                        999L
                ),
                "different resource must not reuse approval"
        );

        check(
                !grant.matches(
                        "recording:abc",
                        4_097L,
                        77L,
                        999L
                ),
                "different byte count must not reuse approval"
        );

        check(
                !grant.matches(
                        "recording:abc",
                        4_096L,
                        78L,
                        999L
                ),
                "different token must not reuse approval"
        );
    }

    private static void expirationIsEnforced() {
        var grant = new DownloadConsentContract.Grant(
                "recording:abc",
                8_192L,
                123L,
                50L
        );

        check(
                grant.matches(
                        "recording:abc",
                        8_192L,
                        123L,
                        50L
                ),
                "grant remains valid at its exact expiry boundary"
        );

        check(
                !grant.matches(
                        "recording:abc",
                        8_192L,
                        123L,
                        51L
                ),
                "expired grant must fail closed"
        );
    }

    private static void invalidGrantConstructionIsRejected() {
        checkThrows(
                () -> new DownloadConsentContract.Grant(
                        "",
                        1L,
                        1L,
                        1L
                ),
                "blank resource is rejected"
        );

        checkThrows(
                () -> new DownloadConsentContract.Grant(
                        "x",
                        0L,
                        1L,
                        1L
                ),
                "zero byte count is rejected"
        );

        checkThrows(
                () -> new DownloadConsentContract.Grant(
                        "x",
                        1L,
                        0L,
                        1L
                ),
                "zero token is rejected"
        );
    }

    private static void remoteHostPinningIsStrict() {
        check(
                DownloadConsentContract.isExactHttpsHost(
                        URI.create("https://alphacephei.com/vosk/model.zip"),
                        "alphacephei.com"
                ),
                "exact HTTPS source host is allowed"
        );

        check(
                !DownloadConsentContract.isExactHttpsHost(
                        URI.create("http://alphacephei.com/vosk/model.zip"),
                        "alphacephei.com"
                ),
                "HTTP downgrade is rejected"
        );

        check(
                !DownloadConsentContract.isExactHttpsHost(
                        URI.create("https://cdn.alphacephei.com/vosk/model.zip"),
                        "alphacephei.com"
                ),
                "different redirect/CDN host requires a new explicit policy"
        );

        check(
                !DownloadConsentContract.isExactHttpsHost(
                        URI.create("https://user@alphacephei.com/vosk/model.zip"),
                        "alphacephei.com"
                ),
                "userinfo-bearing URL is rejected"
        );
    }

    /**
     * CI-level tripwire: every direct Java runtime network sink must remain in
     * the one reviewed Vosk class. Server recording transfer uses Minecraft's
     * already-established play connection and has its own exact consent grant.
     */
    private static void runtimeDownloadSinksAreKnown()
            throws Exception {

        Path sourceRoot =
                Path.of(
                        "src",
                        "main",
                        "java"
                );

        String allowed =
                "src/main/java/net/caravidro/wayaround/voice/client/VoskSpeechRecognizer.java";

        List<String> networkMarkers =
                List.of(
                        "java.net.http.HttpClient",
                        "java.net.http.HttpRequest",
                        "HttpClient.newBuilder(",
                        "java.net.URL",
                        "java.net.URLConnection",
                        "HttpURLConnection",
                        ".openConnection(",
                        ".openStream(",
                        "BodyHandlers.ofFile(",
                        "BodyHandlers.ofInputStream(",
                        "okhttp"
                );

        try (var paths =
                     Files.walk(
                             sourceRoot
                     )) {

            for (Path path :
                    paths.filter(
                                    Files::isRegularFile
                            )
                            .filter(
                                    candidate ->
                                            candidate.toString()
                                                    .endsWith(
                                                            ".java"
                                                    )
                            )
                            .toList()) {

                String normalized =
                        path.toString()
                                .replace(
                                        '\\',
                                        '/'
                                );

                String source =
                        Files.readString(
                                path
                        );

                boolean usesNetworkSink =
                        networkMarkers.stream()
                                .anyMatch(
                                        source::contains
                                );

                if (usesNetworkSink) {
                    check(
                            normalized.equals(
                                    allowed
                            ),
                            "unexpected runtime network/download sink: "
                                    + normalized
                    );
                }
            }
        }

        Path vosk =
                Path.of(
                        allowed
                );

        String voskSource =
                Files.readString(
                        vosk
                );

        check(
                voskSource.contains(
                        "public static void requestModelInstallConsent"
                ),
                "Vosk exposes a consent-screen entry point"
        );

        check(
                voskSource.contains(
                        "private static void startConsentedInstallAsync"
                ),
                "Vosk HTTP worker is private"
        );

        check(
                !voskSource.contains(
                        "public static void installWithUserConsentAsync"
                ),
                "legacy directly callable Vosk installer is gone"
        );

        check(
                voskSource.contains(
                        "HttpClient.Redirect.NEVER"
                ),
                "Vosk cannot silently redirect to an undisclosed host"
        );

        check(
                voskSource.contains(
                        "BodyHandlers.ofInputStream()"
                )
                        && !voskSource.contains(
                        "BodyHandlers.ofFile("
                ),
                "Vosk download is bounded before disk growth instead of unbounded ofFile"
        );
    }

    private static void loaderAutoUpdateRemainsDisabled()
            throws Exception {

        Path metadata =
                Path.of(
                        "src",
                        "main",
                        "templates",
                        "META-INF",
                        "neoforge.mods.toml"
                );

        Set<String> activeUpdateLines =
                Files.readAllLines(
                                metadata
                        )
                        .stream()
                        .map(
                                String::trim
                        )
                        .filter(
                                line ->
                                        !line.startsWith(
                                                "#"
                                        )
                        )
                        .filter(
                                line ->
                                        line.startsWith(
                                                "updateJSONURL"
                                        )
                        )
                        .collect(
                                java.util.stream.Collectors.toSet()
                        );

        check(
                activeUpdateLines.isEmpty(),
                "NeoForge automatic update JSON must stay disabled until it has an informed-consent flow"
        );
    }

    private static void checkThrows(
            Runnable action,
            String description
    ) {
        boolean threw = false;

        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            threw = true;
        }

        check(threw, description);
    }

    private static void check(
            boolean condition,
            String description
    ) {
        checks++;

        if (!condition) {
            throw new AssertionError(description);
        }
    }
}
