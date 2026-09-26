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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.caravidro.wayaround.WayAround;
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

    private static final long MAX_MODEL_DOWNLOAD_BYTES =
            96L * 1024L * 1024L;

    private static final long MAX_MODEL_EXTRACTED_BYTES =
            512L * 1024L * 1024L;

    private static final AtomicBoolean PREPARING =
            new AtomicBoolean(false);

    private static final AtomicBoolean WARMUP_STARTED =
            new AtomicBoolean(false);

    private static volatile Model model;
    private static volatile String lastError = "";

    public record WordTiming(
            String word,
            double start,
            double end,
            double confidence
    ) {
    }

    public record Result(
            String text,
            String error,
            List<WordTiming> words
    ) {
        public Result(
                String text,
                String error
        ) {
            this(
                    text,
                    error,
                    List.of()
            );
        }

        public boolean success() {
            return text != null
                    && !text.isBlank();
        }
    }

    private record PreparedAudio(
            byte[] pcm,
            double offsetSeconds
    ) {
    }

    public static boolean isModelInstalled() {
        return isValidModelRoot(
                modelDirectory()
        );
    }

    public static boolean isPreparing() {
        return PREPARING.get();
    }

    public static void warmUpAsync() {
        if (model != null
                || !WARMUP_STARTED.compareAndSet(
                        false,
                        true
                )) {

            return;
        }

        Thread thread =
                new Thread(
                        () -> {
                            long started =
                                    System.nanoTime();

                            WayAround.LOGGER.info(
                                    "[Voice/Vosk] warmup iniciado; modelDir={}",
                                    modelDirectory()
                                            .toAbsolutePath()
                            );

                            try {
                                ensureModel();

                                WayAround.LOGGER.info(
                                        "[Voice/Vosk] warmup pronto em {} ms",
                                        (
                                                System.nanoTime()
                                                        - started
                                        )
                                                / 1_000_000L
                                );

                            } catch (Throwable throwable) {
                                logNativeFailure(
                                        "warmup",
                                        throwable
                                );

                            } finally {
                                WARMUP_STARTED.set(
                                        false
                                );
                            }
                        },
                        "WayAround-VoskWarmup"
                );

        thread.setDaemon(true);
        thread.start();
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
            long started =
                    System.nanoTime();

            Model localModel =
                    ensureModel();

            long modelReady =
                    System.nanoTime();

            PreparedAudio prepared =
                    preprocessForRecognition(
                            downsample48kTo16k(
                                    pcm48k
                            )
                    );

            byte[] pcm16k =
                    prepared.pcm();

            long preprocessed =
                    System.nanoTime();

            WayAround.LOGGER.info(
                    "[Voice/Vosk] model={}ms preprocess={}ms input48={} bytes input16={} bytes",
                    (modelReady - started)
                            / 1_000_000L,
                    (preprocessed - modelReady)
                            / 1_000_000L,
                    pcm48k.length,
                    pcm16k.length
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

                final int chunkBytes =
                        8_000;

                for (int offset = 0;
                     offset < pcm16k.length;
                     offset += chunkBytes) {

                    int length =
                            Math.min(
                                    chunkBytes,
                                    pcm16k.length
                                            - offset
                            );

                    byte[] chunk =
                            java.util.Arrays.copyOfRange(
                                    pcm16k,
                                    offset,
                                    offset + length
                            );

                    recognizer.acceptWaveForm(
                            chunk,
                            chunk.length
                    );
                }

                String jsonText =
                        recognizer.getFinalResult();

                WayAround.LOGGER.info(
                    "[Voice/Vosk] decode finalizado em {} ms; json={}",
                    (
                            System.nanoTime()
                                    - preprocessed
                    )
                            / 1_000_000L,
                    shorten(
                            jsonText,
                            180
                    )
                );

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
                        "",
                        parseWords(
                                json,
                                prepared.offsetSeconds()
                        )
                );
            }

        } catch (Throwable throwable) {
            String message =
                    describeThrowable(
                            throwable
                    );

            lastError = message;

            logNativeFailure(
                    "recognize",
                    throwable
            );

            return new Result(
                    "",
                    message
            );
        }
    }

    private static List<WordTiming> parseWords(
            JsonObject json,
            double offsetSeconds
    ) {
        if (!json.has(
                "result"
        )
                || !json.get(
                "result"
        )
                .isJsonArray()) {

            return List.of();
        }

        List<WordTiming> words =
                new ArrayList<>();

        for (JsonElement element :
                json.getAsJsonArray(
                        "result"
                )) {

            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject word =
                    element.getAsJsonObject();

            if (!word.has(
                    "word"
            )) {
                continue;
            }

            double start =
                    word.has("start")
                            ? word.get("start")
                            .getAsDouble()
                            : 0.0;

            double end =
                    word.has("end")
                            ? word.get("end")
                            .getAsDouble()
                            : start;

            double confidence =
                    word.has("conf")
                            ? word.get("conf")
                            .getAsDouble()
                            : 0.0;

            words.add(
                    new WordTiming(
                            word.get("word")
                                    .getAsString(),
                            start
                                    + offsetSeconds,
                            end
                                    + offsetSeconds,
                            confidence
                    )
            );
        }

        return List.copyOf(
                words
        );
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

        long downloadedBytes =
                Files.size(
                        download
                );

        if (downloadedBytes <= 0L
                || downloadedBytes
                        > MAX_MODEL_DOWNLOAD_BYTES) {
            Files.deleteIfExists(
                    download
            );

            throw new IllegalStateException(
                    "download do modelo com tamanho inesperado: "
                            + downloadedBytes
                            + " bytes"
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

    private static Path findExtractedModelRoot(
            Path staging
    ) throws Exception {

        Path named =
                staging.resolve(
                        MODEL_NAME
                );

        if (isValidModelRoot(
                named
        )) {
            return named;
        }

        if (isValidModelRoot(
                staging
        )) {
            return staging;
        }

        try (var children =
                     Files.list(
                             staging
                     )) {

            return children
                    .filter(
                            Files::isDirectory
                    )
                    .filter(
                            VoskSpeechRecognizer
                                    ::isValidModelRoot
                    )
                    .findFirst()
                    .orElse(null);
        }
    }

    private static boolean isValidModelRoot(
            Path root
    ) {
        if (root == null
                || !Files.isDirectory(
                        root
                )) {

            return false;
        }

        boolean v2 =
                Files.isRegularFile(
                        root.resolve("am")
                                .resolve(
                                        "final.mdl"
                                )
                )
                        && Files.isRegularFile(
                                root.resolve("conf")
                                        .resolve(
                                                "model.conf"
                                        )
                        );

        boolean v1 =
                Files.isRegularFile(
                        root.resolve(
                                "final.mdl"
                        )
                )
                        && Files.isRegularFile(
                                root.resolve(
                                        "mfcc.conf"
                                )
                        )
                        && (
                        Files.isRegularFile(
                                root.resolve(
                                        "HCLG.fst"
                                )
                        )
                                || (
                                Files.isRegularFile(
                                        root.resolve(
                                                "HCLr.fst"
                                        )
                                )
                                        && Files.isRegularFile(
                                                root.resolve(
                                                        "Gr.fst"
                                                )
                                        )
                        )
                );

        return v1 || v2;
    }

    private static void unzipSecurely(
            Path archive,
            Path destination
    ) throws Exception {

        Path normalizedDestination =
                destination
                        .toAbsolutePath()
                        .normalize();

        long extractedBytes =
                0L;

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

                    long entryBytes =
                            copyZipEntryBounded(
                                    zip,
                                    target,
                                    MAX_MODEL_EXTRACTED_BYTES
                            );

                    extractedBytes +=
                            entryBytes;

                    if (extractedBytes
                            > MAX_MODEL_EXTRACTED_BYTES) {
                        throw new IllegalStateException(
                                "modelo Vosk expandiu alem do limite de seguranca"
                        );
                    }
                }

                zip.closeEntry();
            }
        }
    }

    private static long copyZipEntryBounded(
            ZipInputStream zip,
            Path target,
            long remainingLimit
    ) throws Exception {
        long written =
                0L;

        byte[] buffer =
                new byte[
                        16 * 1024
                        ];

        try (var output =
                     Files.newOutputStream(
                             target
                     )) {
            int read;

            while ((read = zip.read(
                    buffer
            ))
                    >= 0) {
                if (read == 0) {
                    continue;
                }

                written +=
                        read;

                if (written
                        > remainingLimit) {
                    throw new IllegalStateException(
                            "entrada do ZIP do Vosk excedeu o limite de seguranca"
                    );
                }

                output.write(
                        buffer,
                        0,
                        read
                );
            }
        }

        return written;
    }

    private static PreparedAudio preprocessForRecognition(
            byte[] pcm
    ) {
        int sampleCount =
                pcm.length / 2;

        if (sampleCount < 160) {
            return new PreparedAudio(
                    pcm,
                    0.0
            );
        }

        int[] samples =
                new int[
                        sampleCount
                        ];

        long sum =
                0L;

        for (int index = 0;
             index < sampleCount;
             index++) {

            int sample =
                    readSample(
                            pcm,
                            index
                    );

            samples[index] =
                    sample;

            sum +=
                    sample;
        }

        double dc =
                sum
                        / (double) sampleCount;

        double square =
                0.0;

        for (int index = 0;
             index < sampleCount;
             index++) {

            int centered =
                    (int) Math.round(
                            samples[index]
                                    - dc
                    );

            samples[index] =
                    centered;

            square +=
                    (double) centered
                            * centered;
        }

        double rms =
                Math.sqrt(
                        square
                                / sampleCount
                );

        /*
         * Vosk suffers badly when a quiet Windows input is fed almost raw.
         * Keep the gain bounded so a hissy microphone does not become a wall
         * of noise.
         */
        double gain =
                rms <= 1.0
                        ? 1.0
                        : Math.max(
                                0.80,
                                Math.min(
                                        4.0,
                                        6200.0 / rms
                                )
                        );

        final int frameSamples =
                320;

        double activityThreshold =
                Math.max(
                        260.0,
                        Math.min(
                                1500.0,
                                rms * 0.20
                        )
                );

        int firstActive =
                0;

        int lastActive =
                sampleCount;

        boolean found =
                false;

        for (int start = 0;
             start < sampleCount;
             start += frameSamples) {

            int end =
                    Math.min(
                            sampleCount,
                            start + frameSamples
                    );

            double frameSquare =
                    0.0;

            for (int index = start;
                 index < end;
                 index++) {

                frameSquare +=
                        (double) samples[index]
                                * samples[index];
            }

            double frameRms =
                    Math.sqrt(
                            frameSquare
                                    / Math.max(
                                            1,
                                            end - start
                                    )
                    );

            if (frameRms
                    >= activityThreshold) {

                if (!found) {
                    firstActive =
                            start;
                    found =
                            true;
                }

                lastActive =
                        end;
            }
        }

        if (found) {
            int padding =
                    1600;

            firstActive =
                    Math.max(
                            0,
                            firstActive
                                    - padding
                    );

            lastActive =
                    Math.min(
                            sampleCount,
                            lastActive
                                    + padding
                    );

        } else {
            firstActive =
                    0;
            lastActive =
                    sampleCount;
        }

        int outputSamples =
                Math.max(
                        0,
                        lastActive
                                - firstActive
                );

        byte[] output =
                new byte[
                        outputSamples * 2
                        ];

        for (int out = 0;
             out < outputSamples;
             out++) {

            int centered =
                    samples[
                            firstActive
                                    + out
                            ];

            /*
             * DC was already removed above. Preserve the speech waveform here:
             * a derivative-like filter made consonants brittle and hurt Vosk.
             */
            int amplified =
                    (int) Math.round(
                            centered
                                    * gain
                    );

            amplified =
                    Math.max(
                            Short.MIN_VALUE,
                            Math.min(
                                    Short.MAX_VALUE,
                                    amplified
                            )
                    );

            output[out * 2] =
                    (byte) (
                            amplified
                                    & 0xFF
                    );

            output[out * 2 + 1] =
                    (byte) (
                            (amplified >> 8)
                                    & 0xFF
                    );
        }

        return new PreparedAudio(
                output,
                firstActive
                        / (double) RECOGNITION_SAMPLE_RATE
        );
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

    private static void logNativeFailure(
            String stage,
            Throwable throwable
    ) {
        WayAround.LOGGER.error(
                "[Voice/Vosk] FALHA NATIVA stage={} os={} arch={} java={} java.library.path={} error={}",
                stage,
                System.getProperty(
                        "os.name"
                ),
                System.getProperty(
                        "os.arch"
                ),
                System.getProperty(
                        "java.version"
                ),
                System.getProperty(
                        "java.library.path"
                ),
                describeThrowable(
                        throwable
                ),
                throwable
        );
    }

    private static String describeThrowable(
            Throwable throwable
    ) {
        if (throwable == null) {
            return "erro desconhecido";
        }

        StringBuilder builder =
                new StringBuilder();

        Throwable current =
                throwable;

        int depth = 0;

        while (current != null
                && depth < 6) {

            if (depth > 0) {
                builder.append(
                        " <- "
                );
            }

            builder.append(
                    current.getClass()
                            .getName()
            );

            String message =
                    current.getMessage();

            if (message != null
                    && !message.isBlank()) {

                builder.append(
                        ": "
                );

                builder.append(
                        message
                );
            }

            current =
                    current.getCause();

            depth++;
        }

        return shorten(
                builder.toString(),
                800
        );
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
