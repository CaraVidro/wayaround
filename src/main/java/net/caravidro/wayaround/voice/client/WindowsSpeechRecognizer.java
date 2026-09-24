package net.caravidro.wayaround.voice.client;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat;

import net.caravidro.wayaround.voice.VoiceConstants;

public final class WindowsSpeechRecognizer {

    private WindowsSpeechRecognizer() {
    }

    public record Result(
            String text,
            String error
    ) {
        public boolean success() {
            return text != null
                    && !text.isBlank();
        }
    }

    public static Result recognize(
            byte[] pcm
    ) {
        String osName =
                System.getProperty(
                        "os.name",
                        ""
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (!osName.contains("win")) {
            return new Result(
                    "",
                    "speech debug desta V0 usa o reconhecedor local do Windows"
            );
        }

        Path wav = null;
        Path script = null;

        try {
            wav =
                    Files.createTempFile(
                            "wayaround-voice-",
                            ".wav"
                    );

            script =
                    Files.createTempFile(
                            "wayaround-speech-",
                            ".ps1"
                    );

            long frameLength =
                    pcm.length
                            / VoiceConstants.BYTES_PER_SAMPLE
                            / VoiceConstants.CHANNELS;

            try (AudioInputStream stream =
                         new AudioInputStream(
                                 new ByteArrayInputStream(
                                         pcm
                                 ),
                                 VoiceConstants.audioFormat(),
                                 frameLength
                         )) {

                AudioSystem.write(
                        stream,
                        AudioFileFormat.Type.WAVE,
                        wav.toFile()
                );
            }

            Files.writeString(
                    script,
                    powershellScript(),
                    StandardCharsets.UTF_8
            );

            Process process =
                    new ProcessBuilder(
                            "powershell.exe",
                            "-NoLogo",
                            "-NoProfile",
                            "-NonInteractive",
                            "-ExecutionPolicy",
                            "Bypass",
                            "-File",
                            script.toAbsolutePath()
                                    .toString(),
                            wav.toAbsolutePath()
                                    .toString()
                    )
                            .redirectErrorStream(
                                    true
                            )
                            .start();

            boolean finished =
                    process.waitFor(
                            15,
                            TimeUnit.SECONDS
                    );

            if (!finished) {
                process.destroyForcibly();

                return new Result(
                        "",
                        "reconhecimento demorou demais e foi cancelado"
                );
            }

            String output =
                    new String(
                            process.getInputStream()
                                    .readAllBytes(),
                            StandardCharsets.UTF_8
                    )
                            .trim();

            if (output.startsWith(
                    "__WA_OK__"
            )) {
                String text =
                        output.substring(
                                "__WA_OK__".length()
                        )
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

            if (output.startsWith(
                    "__WA_ERROR__"
            )) {
                return new Result(
                        "",
                        output.substring(
                                "__WA_ERROR__".length()
                        )
                                .trim()
                );
            }

            if (output.isBlank()) {
                return new Result(
                        "",
                        "o reconhecedor nao retornou texto"
                );
            }

            return new Result(
                    "",
                    output
            );

        } catch (Exception exception) {
            return new Result(
                    "",
                    exception.getClass()
                            .getSimpleName()
                            + ": "
                            + exception.getMessage()
            );

        } finally {
            deleteQuietly(wav);
            deleteQuietly(script);
        }
    }

    private static String powershellScript() {
        return """
                $ErrorActionPreference = "Stop"
                [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
                try {
                    Add-Type -AssemblyName System.Speech

                    $recognizers = [System.Speech.Recognition.SpeechRecognitionEngine]::InstalledRecognizers()

                    $recognizerInfo = $recognizers |
                        Where-Object { $_.Culture.Name -eq "pt-BR" } |
                        Select-Object -First 1

                    if ($null -eq $recognizerInfo) {
                        $recognizerInfo = $recognizers |
                            Where-Object { $_.Culture.Name -like "pt-*" } |
                            Select-Object -First 1
                    }

                    if ($null -eq $recognizerInfo) {
                        [Console]::WriteLine("__WA_ERROR__Nenhum reconhecedor de fala em portugues foi encontrado no Windows")
                        exit 3
                    }

                    $engine = New-Object System.Speech.Recognition.SpeechRecognitionEngine($recognizerInfo)
                    $grammar = New-Object System.Speech.Recognition.DictationGrammar
                    $engine.LoadGrammar($grammar)
                    $engine.SetInputToWaveFile($args[0])

                    $parts = New-Object System.Collections.Generic.List[string]

                    while ($true) {
                        $result = $engine.Recognize()

                        if ($null -eq $result) {
                            break
                        }

                        if (-not [string]::IsNullOrWhiteSpace($result.Text)) {
                            $parts.Add($result.Text)
                        }
                    }

                    $engine.Dispose()

                    [Console]::WriteLine("__WA_OK__" + ($parts -join " "))
                }
                catch {
                    [Console]::WriteLine("__WA_ERROR__" + $_.Exception.Message)
                    exit 4
                }
                """;
    }

    private static void deleteQuietly(
            Path path
    ) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }
}
