package net.caravidro.wayaround.voice.client;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.caravidro.wayaround.voice.VoiceConstants;
import net.neoforged.fml.loading.FMLPaths;
import org.vosk.Model;
import org.vosk.Recognizer;

public final class VoskSpeechRecognizer {

    private VoskSpeechRecognizer() {
    }

    private static final String MODEL_NAME =
            "vosk-model-small-pt-0.3";

    private static final URI MODEL_URL =
            URI.create(
                    "https://alphacephei.com/vosk/models/"
                            + MODEL_NAME
                            + ".zip"
            );

    private static final float RECOGNITION_SAMPLE_RATE =
            16_000.0f;

    private static final AtomicBoolean PREPARING =
            new AtomicBoolean(false);

    private static volatile Model model;
    private static volatile String lastError = "";

    public record Result(
            String text,
            String error
    ) {
        public boolean success() {
            return text != null
                    && !text.isBlank();
        }
    }

    public static boolean isModelInstalled() {
        return isValidModelRoot(
                modelDirectory()
        );
    }

    public static boolean isPreparing() {
        return PREPARING.get();
    }

    public static String statusText() {
        if (model != null) {
            return "Modelo local: pronto";
        }

        if (PREPARING.get()) {
            return "Modelo local: baixando/preparando...";
        }

        if (isModelInstalled()) {
            return "Modelo local: instalado";
        }

        if (!lastError.isBlank()) {
            return "Modelo local: erro - "
                    + shorten(
                            lastError,
                            52
                    );
        }

        return "Modelo local: sera baixado (31 MB)";
    }

    public static Result recognize(
            byte[] pcm48k
    ) {
        if (pcm48k == null
                || pcm48k.length < 2) {

            return new Result(
                    "",
                    "audio vazio"
            );
        }

        try {
            Model localModel =
                    ensureModel();

            byte[] pcm16k =
                    downsample48kTo16k(
                            pcm48k
                    );

            if (pcm16k.length < 2) {
                return new Result(
                        "",
                        "audio curto demais"
                );
            }

            try (Recognizer recognizer =
                         new Recognizer(
                                 localModel,
                                 RECOGNITION_SAMPLE_RATE
                         )) {

                recognizer.setWords(true);

                recognizer.acceptWaveForm(
                        pcm16k,
                        pcm16k.length
                );

                String jsonText =
                        recognizer.getFinalResult();

                JsonObject json =
                        JsonParser
                                .parseString(
                                        jsonText
                                )
                                .getAsJsonObject();

                JsonElement textElement =
                        json.get("text");

                if (textElement == null
                        || textElement.isJsonNull()) {

                    return new Result(
                            "",
                            "Vosk respondeu sem texto"
                    );
                }

                String text =
                        textElement
                                .getAsString()
                                .trim();

                if (text.isBlank()) {
                    return new Result(
                            "",
                            "nenhuma fala reconhecida"
                    );
                }

                return new Result(
                        text,
                        ""
                );
            }

        } catch (Exception exception) {
            String message =
                    exception.getClass()
                            .getSimpleName()
                            + ": "
                            + exception.getMessage();

            lastError = message;

            return new Result(
                    "",
                    message
            );
        }
    }

    private static Model ensureModel()
            throws Exception {

        Model existing = model;

        if (existing != null) {
            return existing;
        }

        synchronized (VoskSpeechRecognizer.class) {
            if (model != null) {
                return model;
            }

            PREPARING.set(true);

            try {
                if (!isModelInstalled()) {
                    downloadAndInstallModel();
                }

                model =
                        new Model(
                                modelDirectory()
                                        .toAbsolutePath()
                                        .toString()
                        );

                lastError = "";

                return model;

            } finally {
                PREPARING.set(false);
            }
        }
    }

    private static void downloadAndInstallModel()
            throws Exception {

        Path base =
                modelsDirectory();

        Files.createDirectories(base);

        Path download =
                base.resolve(
                        MODEL_NAME
                                + ".download.zip"
                );

        Path staging =
                base.resolve(
                        MODEL_NAME
                                + ".installing"
                );

        deleteTree(staging);
        Files.deleteIfExists(download);

        HttpClient client =
                HttpClient.newBuilder()
                        .followRedirects(
                                HttpClient.Redirect.NORMAL
                        )
                        .connectTimeout(
                                Duration.ofSeconds(12)
                        )
                        .build();

        HttpRequest request =
                HttpRequest.newBuilder(
                                MODEL_URL
                        )
                        .timeout(
                                Duration.ofMinutes(3)
                        )
                        .header(
                                "User-Agent",
                                "WayAround-Voice/1"
                        )
                        .GET()
                        .build();

        HttpResponse<Path> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers
                                .ofFile(download)
                );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            Files.deleteIfExists(download);

            throw new IllegalStateException(
                    "download do modelo respondeu HTTP "
                            + response.statusCode()
            );
        }

        Files.createDirectories(staging);

        unzipSecurely(
                download,
                staging
        );

        Path extracted =
                findExtractedModelRoot(
                        staging
                );

        if (extracted == null) {
            throw new IllegalStateException(
                    "modelo PT-BR baixado, mas os arquivos do Vosk nao foram encontrados"
            );
        }

        deleteTree(
                modelDirectory()
        );

        Files.move(
                extracted,
                modelDirectory(),
                StandardCopyOption.REPLACE_EXISTING
        );

        Files.deleteIfExists(download);
        deleteTree(staging);
    }

    private static void unzipSecurely(
            Path archive,
            Path destination
    ) throws Exception {

        Path normalizedDestination =
                destination
                        .toAbsolutePath()
                        .normalize();

        try (
                InputStream raw =
                        Files.newInputStream(
                                archive
                        );
                ZipInputStream zip =
                        new ZipInputStream(
                                raw,
                                StandardCharsets.UTF_8
                        )
        ) {
            ZipEntry entry;

            while ((entry = zip.getNextEntry())
                    != null) {

                Path target =
                        normalizedDestination
                                .resolve(
                                        entry.getName()
                                )
                                .normalize();

                if (!target.startsWith(
                        normalizedDestination
                )) {
                    throw new IllegalStateException(
                            "arquivo invalido dentro do ZIP do modelo"
                    );
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(
                            target
                    );

                } else {
                    Path parent =
                            target.getParent();

                    if (parent != null) {
                        Files.createDirectories(
                                parent
                        );
                    }

                    Files.copy(
                            zip,
                            target,
                            StandardCopyOption
                                    .REPLACE_EXISTING
                    );
                }

                zip.closeEntry();
            }
        }
    }

    private static byte[] downsample48kTo16k(
            byte[] source
    ) {
        if ((int) VoiceConstants.SAMPLE_RATE
                == (int) RECOGNITION_SAMPLE_RATE) {

            return source.clone();
        }

        if ((int) VoiceConstants.SAMPLE_RATE
                != 48_000) {

            throw new IllegalStateException(
                    "downsampler esperado para 48 kHz, recebido "
                            + (int) VoiceConstants.SAMPLE_RATE
            );
        }

        int sourceSamples =
                source.length / 2;

        int outputSamples =
                sourceSamples / 3;

        byte[] output =
                new byte[
                        outputSamples * 2
                        ];

        for (int index = 0;
             index < outputSamples;
             index++) {

            int sourceIndex =
                    index * 3;

            int a =
                    readSample(
                            source,
                            sourceIndex
                    );

            int b =
                    readSample(
                            source,
                            sourceIndex + 1
                    );

            int c =
                    readSample(
                            source,
                            sourceIndex + 2
                    );

            int mixed =
                    (a + b + c) / 3;

            output[index * 2] =
                    (byte) (
                            mixed & 0xFF
                    );

            output[index * 2 + 1] =
                    (byte) (
                            (mixed >> 8)
                                    & 0xFF
                    );
        }

        return output;
    }

    private static int readSample(
            byte[] pcm,
            int sampleIndex
    ) {
        int byteIndex =
                sampleIndex * 2;

        int low =
                pcm[byteIndex]
                        & 0xFF;

        int high =
                pcm[byteIndex + 1];

        return (short) (
                low
                        | (high << 8)
        );
    }

    private static Path modelsDirectory() {
        return FMLPaths.CONFIGDIR
                .get()
                .resolve("wayaround")
                .resolve("voice-models");
    }

    private static Path modelDirectory() {
        return modelsDirectory()
                .resolve(MODEL_NAME);
    }

    private static void deleteTree(
            Path root
    ) throws Exception {

        if (root == null
                || !Files.exists(root)) {

            return;
        }

        try (var paths =
                     Files.walk(root)) {

            paths.sorted(
                            Comparator.reverseOrder()
                    )
                    .forEach(
                            path -> {
                                try {
                                    Files.deleteIfExists(
                                            path
                                    );
                                } catch (Exception exception) {
                                    throw new RuntimeException(
                                            exception
                                    );
                                }
                            }
                    );
        } catch (RuntimeException exception) {
            if (exception.getCause()
                    instanceof Exception cause) {

                throw cause;
            }

            throw exception;
        }
    }

    private static String shorten(
            String text,
            int maximum
    ) {
        if (text == null) {
            return "";
        }

        if (text.length() <= maximum) {
            return text;
        }

        return text.substring(
                0,
                Math.max(
                        0,
                        maximum - 3
                )
        )
                + "...";
    }
}
