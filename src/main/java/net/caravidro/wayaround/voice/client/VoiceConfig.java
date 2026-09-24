package net.caravidro.wayaround.voice.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.neoforged.fml.loading.FMLPaths;

public final class VoiceConfig {

    private VoiceConfig() {
    }

    private static boolean loaded = false;

    private static boolean enabled = false;
    private static String microphoneId = "";

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

        Properties properties = new Properties();

        try (InputStream input =
                     Files.newInputStream(path)) {

            properties.load(input);

            enabled = Boolean.parseBoolean(
                    properties.getProperty(
                            "enabled",
                            "false"
                    )
            );

            microphoneId =
                    properties.getProperty(
                            "microphone",
                            ""
                    );

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

    public static String getMicrophoneId() {
        ensureLoaded();
        return microphoneId;
    }

    public static synchronized void setEnabled(
            boolean value
    ) {
        ensureLoaded();
        enabled = value;
        save();
    }

    public static synchronized void setMicrophoneId(
            String value
    ) {
        ensureLoaded();
        microphoneId =
                value == null ? "" : value;
        save();
    }

    public static synchronized void save() {
        ensureLoaded();

        Properties properties = new Properties();

        properties.setProperty(
                "enabled",
                Boolean.toString(enabled)
        );

        properties.setProperty(
                "microphone",
                microphoneId
        );

        try {
            Files.createDirectories(
                    file().getParent()
            );

            try (OutputStream output =
                         Files.newOutputStream(file())) {

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
