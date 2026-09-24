package net.caravidro.wayaround.voice.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.neoforged.fml.loading.FMLPaths;

public final class VoiceConfig {

    public enum ActivationMode {
        PUSH_TO_TALK,
        VOICE_ACTIVATION;

        public ActivationMode next() {
            return this == PUSH_TO_TALK
                    ? VOICE_ACTIVATION
                    : PUSH_TO_TALK;
        }
    }

    private VoiceConfig() {
    }

    private static boolean loaded = false;

    private static boolean enabled = false;
    private static boolean debugSpeechEnabled = false;
    private static String microphoneId = "";
    private static String speakerId = "";
    private static ActivationMode activationMode =
            ActivationMode.PUSH_TO_TALK;

    private static Path file() {
        return FMLPaths.CONFIGDIR
                .get()
                .resolve("wayaround-voice.properties");
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded = true;

        Path path = file();

        if (!Files.exists(path)) {
            return;
        }

        Properties properties =
                new Properties();

        try (InputStream input =
                     Files.newInputStream(path)) {

            properties.load(input);

            enabled =
                    Boolean.parseBoolean(
                            properties.getProperty(
                                    "enabled",
                                    "false"
                            )
                    );

            debugSpeechEnabled =
                    Boolean.parseBoolean(
                            properties.getProperty(
                                    "debugSpeech",
                                    "false"
                            )
                    );

            microphoneId =
                    properties.getProperty(
                            "microphone",
                            ""
                    );

            speakerId =
                    properties.getProperty(
                            "speaker",
                            ""
                    );

            try {
                activationMode =
                        ActivationMode.valueOf(
                                properties.getProperty(
                                                "activationMode",
                                                "PUSH_TO_TALK"
                                        )
                                        .trim()
                                        .toUpperCase()
                        );

            } catch (Exception ignored) {
                activationMode =
                        ActivationMode.PUSH_TO_TALK;
            }

        } catch (IOException exception) {
            System.err.println(
                    "[WayAround Voice] Nao foi possivel ler a configuracao: "
                            + exception.getMessage()
            );
        }
    }

    public static boolean isEnabled() {
        ensureLoaded();
        return enabled;
    }

    public static boolean isDebugSpeechEnabled() {
        ensureLoaded();
        return debugSpeechEnabled;
    }

    public static ActivationMode getActivationMode() {
        ensureLoaded();
        return activationMode;
    }

    public static boolean isVoiceActivation() {
        return getActivationMode()
                == ActivationMode.VOICE_ACTIVATION;
    }

    public static String getMicrophoneId() {
        ensureLoaded();
        return microphoneId;
    }

    public static String getSpeakerId() {
        ensureLoaded();
        return speakerId;
    }

    public static synchronized void setEnabled(
            boolean value
    ) {
        ensureLoaded();
        enabled = value;
        save();
    }

    public static synchronized void setDebugSpeechEnabled(
            boolean value
    ) {
        ensureLoaded();
        debugSpeechEnabled = value;
        save();
    }

    public static synchronized void setActivationMode(
            ActivationMode value
    ) {
        ensureLoaded();
        activationMode =
                value == null
                        ? ActivationMode.PUSH_TO_TALK
                        : value;
        save();
    }

    public static synchronized void setSpeakerId(
            String value
    ) {
        ensureLoaded();
        speakerId =
                value == null
                        ? ""
                        : value;
        save();
    }

    public static synchronized void setMicrophoneId(
            String value
    ) {
        ensureLoaded();
        microphoneId =
                value == null
                        ? ""
                        : value;
        save();
    }

    public static synchronized void save() {
        ensureLoaded();

        Properties properties =
                new Properties();

        properties.setProperty(
                "enabled",
                Boolean.toString(
                        enabled
                )
        );

        properties.setProperty(
                "debugSpeech",
                Boolean.toString(
                        debugSpeechEnabled
                )
        );

        properties.setProperty(
                "activationMode",
                activationMode.name()
        );

        properties.setProperty(
                "microphone",
                microphoneId
        );

        properties.setProperty(
                "speaker",
                speakerId
        );

        try {
            Files.createDirectories(
                    file().getParent()
            );

            try (OutputStream output =
                         Files.newOutputStream(
                                 file()
                         )) {

                properties.store(
                        output,
                        "Way Around Voice Chat"
                );
            }

        } catch (IOException exception) {
            System.err.println(
                    "[WayAround Voice] Nao foi possivel salvar a configuracao: "
                            + exception.getMessage()
            );
        }
    }
}
