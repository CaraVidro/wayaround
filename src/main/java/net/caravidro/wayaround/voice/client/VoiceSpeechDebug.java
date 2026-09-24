package net.caravidro.wayaround.voice.client;

import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.caravidro.wayaround.WayAround;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class VoiceSpeechDebug {

    private VoiceSpeechDebug() {
    }

    private static final ThreadPoolExecutor WORKER =
            new ThreadPoolExecutor(
                    1,
                    1,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(6),
                    runnable -> {
                        Thread thread =
                                new Thread(
                                        runnable,
                                        "WayAround-SpeechDebug"
                                );

                        thread.setDaemon(true);

                        return thread;
                    },
                    new ThreadPoolExecutor.DiscardOldestPolicy()
            );

    public static void submit(
            byte[] pcm
    ) {
        if (pcm == null
                || pcm.length == 0) {
            return;
        }

        byte[] copy =
                pcm.clone();

        if (!VoskSpeechRecognizer
                .isModelInstalled()
                && !VoskSpeechRecognizer
                .isPreparing()) {

            showClientMessage(
                    Component.literal(
                                    "[Voice Debug] "
                            )
                            .withStyle(
                                    ChatFormatting.AQUA
                            )
                            .append(
                                    Component.literal(
                                            "baixando o modelo PT-BR gratuito (31 MB); isso acontece so uma vez..."
                                    )
                                    .withStyle(
                                            ChatFormatting.GRAY
                                    )
                            )
            );
        }

        WayAround.LOGGER.info(
                "[Voice/STT] QUEUED bytes={} queue={}",
                copy.length,
                WORKER.getQueue()
                        .size()
        );

        WORKER.execute(
                () -> process(copy)
        );
    }

    private static void process(
            byte[] pcm
    ) {
        long started =
                System.nanoTime();

        WayAround.LOGGER.info(
                "[Voice/STT] BEGIN bytes={}",
                pcm.length
        );

        /*
         * Recognition comes first. Intent should never wait for the much more
         * expensive pitch/tone analysis.
         */
        VoskSpeechRecognizer.Result recognition;

        try {
            recognition =
                    VoskSpeechRecognizer.recognize(
                            pcm
                    );

        } catch (Throwable throwable) {
            WayAround.LOGGER.error(
                    "[Voice/STT] worker morreu fora do recognizer: {}: {}",
                    throwable.getClass()
                            .getName(),
                    throwable.getMessage(),
                    throwable
            );

            return;
        }

        long recognizedAt =
                System.nanoTime();

        long recognitionMillis =
                (recognizedAt - started)
                        / 1_000_000L;

        if (recognition.success()) {
            WayAround.LOGGER.info(
                    "[Voice/STT] Entendido em {} ms: \"{}\"",
                    recognitionMillis,
                    recognition.text()
            );
        } else {
            WayAround.LOGGER.warn(
                    "[Voice/STT] Falha em {} ms: {}",
                    recognitionMillis,
                    recognition.error()
            );
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        minecraft.execute(
                () -> {
                    if (recognition.success()) {
                        VoiceIntentClient.handleTranscript(
                                recognition.text()
                        );

                    } else if (VoiceConfig
                            .isDebugSpeechEnabled()) {

                        minecraft.gui
                                .getChat()
                                .addMessage(
                                        Component.literal(
                                                        "[Voice Debug] "
                                                )
                                                .withStyle(
                                                        ChatFormatting.RED
                                                )
                                                .append(
                                                        Component.literal(
                                                                recognition.error()
                                                        )
                                                        .withStyle(
                                                                ChatFormatting.GRAY
                                                        )
                                                )
                                );
                    }
                }
        );

        if (!recognition.success()
                || !VoiceConfig
                .isDebugSpeechEnabled()) {

            return;
        }

        long toneStarted =
                System.nanoTime();

        VoiceToneAnalyzer.ToneProfile profile =
                VoiceToneAnalyzer.analyze(
                        pcm
                );

        long toneMillis =
                (System.nanoTime()
                        - toneStarted)
                        / 1_000_000L;

        WayAround.LOGGER.info(
                "[Voice/Tone] analisado em {} ms",
                toneMillis
        );

        minecraft.execute(
                () -> {
                    String expressive =
                            VoiceToneAnalyzer.applyExpression(
                                    recognition.text(),
                                    profile
                            );

                    minecraft.gui
                            .getChat()
                            .addMessage(
                                    Component.literal(
                                                    "[Voice Debug] "
                                            )
                                            .withStyle(
                                                    ChatFormatting.AQUA
                                            )
                                            .append(
                                                    Component.literal(
                                                            expressive
                                                    )
                                                    .withStyle(
                                                            ChatFormatting.WHITE
                                                    )
                                            )
                            );

                    String metrics =
                            String.format(
                                    Locale.ROOT,
                                    "tom=%s | intencao=%d%% | pitch %.0f->%.0f Hz | variacao %.0f Hz | vogal segura %.2fs",
                                    profile.tone(),
                                    Math.round(
                                            profile.intent()
                                                    * 100.0
                                    ),
                                    profile.startPitch(),
                                    profile.endPitch(),
                                    profile.pitchRange(),
                                    profile.holdSeconds()
                            );

                    minecraft.gui
                            .getChat()
                            .addMessage(
                                    Component.literal(
                                                    "  > "
                                                            + metrics
                                            )
                                            .withStyle(
                                                    ChatFormatting.DARK_GRAY
                                            )
                            );
                }
        );
    }

    private static void showClientMessage(
            Component message
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        minecraft.execute(
                () ->
                        minecraft.gui
                                .getChat()
                                .addMessage(
                                        message
                                )
        );
    }
}
