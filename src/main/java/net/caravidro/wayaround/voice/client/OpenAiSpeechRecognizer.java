package net.caravidro.wayaround.voice.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.caravidro.wayaround.voice.VoiceConstants;

public final class OpenAiSpeechRecognizer {

    private OpenAiSpeechRecognizer() {
    }

    private static final URI ENDPOINT =
            URI.create(
                    "https://api.openai.com/v1/audio/transcriptions"
            );

    private static final HttpClient HTTP =
            HttpClient.newBuilder()
                    .connectTimeout(
                            Duration.ofSeconds(8)
                    )
                    .build();

    public record Result(
            String text,
            String error
    ) {
        public boolean success() {
            return text != null
                    && !text.isBlank();
        }
    }

    public static boolean hasApiKey() {
        String key =
                System.getenv(
                        "OPENAI_API_KEY"
                );

        return key != null
                && !key.isBlank();
    }

    public static Result recognize(
            byte[] pcm
    ) {
        if (pcm == null
                || pcm.length == 0) {

            return new Result(
                    "",
                    "audio vazio"
            );
        }

        String apiKey =
                System.getenv(
                        "OPENAI_API_KEY"
                );

        if (apiKey == null
                || apiKey.isBlank()) {

            return new Result(
                    "",
                    "OPENAI_API_KEY nao foi definida no ambiente"
            );
        }

        try {
            byte[] wav =
                    buildWavInMemory(
                            pcm
                    );

            String boundary =
                    "----WayAround"
                            + UUID.randomUUID()
                            .toString()
                            .replace(
                                    "-",
                                    ""
                            );

            byte[] body =
                    buildMultipartBody(
                            boundary,
                            wav
                    );

            HttpRequest request =
                    HttpRequest.newBuilder(
                                    ENDPOINT
                            )
                            .timeout(
                                    Duration.ofSeconds(
                                            25
                                    )
                            )
                            .header(
                                    "Authorization",
                                    "Bearer "
                                            + apiKey
                            )
                            .header(
                                    "Content-Type",
                                    "multipart/form-data; boundary="
                                            + boundary
                            )
                            .header(
                                    "Accept",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofByteArray(
                                                    body
                                            )
                            )
                            .build();

            HttpResponse<String> response =
                    HTTP.send(
                            request,
                            HttpResponse.BodyHandlers
                                    .ofString(
                                            StandardCharsets.UTF_8
                                    )
                    );

            if (response.statusCode()
                    < 200
                    || response.statusCode()
                    >= 300) {

                return new Result(
                        "",
                        friendlyHttpError(
                                response.statusCode(),
                                response.body()
                        )
                );
            }

            JsonObject json =
                    JsonParser
                            .parseString(
                                    response.body()
                            )
                            .getAsJsonObject();

            JsonElement textElement =
                    json.get(
                            "text"
                    );

            if (textElement == null
                    || textElement.isJsonNull()) {

                return new Result(
                        "",
                        "a API respondeu sem campo de transcricao"
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

        } catch (Exception exception) {
            return new Result(
                    "",
                    exception.getClass()
                            .getSimpleName()
                            + ": "
                            + exception.getMessage()
            );
        }
    }

    private static byte[] buildWavInMemory(
            byte[] pcm
    ) throws Exception {

        long frameLength =
                pcm.length
                        / VoiceConstants.BYTES_PER_SAMPLE
                        / VoiceConstants.CHANNELS;

        try (
                ByteArrayInputStream raw =
                        new ByteArrayInputStream(
                                pcm
                        );
                AudioInputStream audio =
                        new AudioInputStream(
                                raw,
                                VoiceConstants.audioFormat(),
                                frameLength
                        );
                ByteArrayOutputStream wav =
                        new ByteArrayOutputStream()
        ) {
            AudioSystem.write(
                    audio,
                    AudioFileFormat.Type.WAVE,
                    wav
            );

            return wav.toByteArray();
        }
    }

    private static byte[] buildMultipartBody(
            String boundary,
            byte[] wav
    ) throws Exception {

        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        writeTextField(
                out,
                boundary,
                "model",
                "gpt-transcribe"
        );

        writeTextField(
                out,
                boundary,
                "languages[]",
                "pt"
        );

        writeTextField(
                out,
                boundary,
                "keywords[]",
                "Way Around"
        );

        writeTextField(
                out,
                boundary,
                "keywords[]",
                "azul"
        );

        writeTextField(
                out,
                boundary,
                "keywords[]",
                "tecnica imaginaria"
        );

        writeTextField(
                out,
                boundary,
                "keywords[]",
                "Blue"
        );

        writeAscii(
                out,
                "--"
                        + boundary
                        + "\r\n"
        );

        writeAscii(
                out,
                "Content-Disposition: form-data; name=\"file\"; filename=\"wayaround-voice.wav\"\r\n"
        );

        writeAscii(
                out,
                "Content-Type: audio/wav\r\n\r\n"
        );

        out.write(wav);

        writeAscii(
                out,
                "\r\n--"
                        + boundary
                        + "--\r\n"
        );

        return out.toByteArray();
    }

    private static void writeTextField(
            ByteArrayOutputStream out,
            String boundary,
            String name,
            String value
    ) throws Exception {

        writeAscii(
                out,
                "--"
                        + boundary
                        + "\r\n"
        );

        writeAscii(
                out,
                "Content-Disposition: form-data; name=\""
                        + name
                        + "\"\r\n\r\n"
        );

        out.write(
                value.getBytes(
                        StandardCharsets.UTF_8
                )
        );

        writeAscii(
                out,
                "\r\n"
        );
    }

    private static void writeAscii(
            ByteArrayOutputStream out,
            String text
    ) throws Exception {

        out.write(
                text.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }

    private static String friendlyHttpError(
            int status,
            String body
    ) {
        if (status == 401) {
            return "API recusou a chave (401). Confira OPENAI_API_KEY.";
        }

        if (status == 429) {
            return "API sem cota ou com limite de requisicoes (429).";
        }

        String message = "";

        try {
            JsonObject json =
                    JsonParser
                            .parseString(
                                    body
                            )
                            .getAsJsonObject();

            JsonObject error =
                    json.has("error")
                            && json.get("error")
                            .isJsonObject()
                            ? json.getAsJsonObject(
                                    "error"
                            )
                            : null;

            if (error != null
                    && error.has(
                            "message"
                    )) {

                message =
                        error.get(
                                "message"
                        )
                                .getAsString();
            }

        } catch (Exception ignored) {
        }

        if (message.length() > 180) {
            message =
                    message.substring(
                            0,
                            180
                    )
                            + "...";
        }

        return "API respondeu HTTP "
                + status
                + (
                message.isBlank()
                        ? ""
                        : ": " + message
        );
    }
}
