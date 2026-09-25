package net.caravidro.wayaround.voice.client;

import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.JusticeVoiceStatementC2SPayload;
import net.caravidro.wayaround.network.TukunaVoiceStatementC2SPayload;
import net.neoforged.neoforge.network.PacketDistributor;
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

    public static void submitSpeculative(
            byte[] pcm
    ) {
        if (pcm == null
                || pcm.length == 0
                || WORKER.getActiveCount() > 0
                || !WORKER.getQueue()
                        .isEmpty()) {
            return;
        }

        byte[] copy =
                pcm.clone();

        WORKER.execute(
                () ->
                        processSpeculative(
                                copy
                        )
        );
    }

    private static void processSpeculative(
            byte[] pcm
    ) {
        VoskSpeechRecognizer.Result recognition;

        try {
            recognition =
                    VoskSpeechRecognizer.recognize(
                            pcm
                    );

        } catch (Throwable throwable) {
            return;
        }

        if (!recognition.success()) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        minecraft.execute(
                () ->
                        VoiceIntentClient.handleSpeculativeTranscript(
                                BrazilianPortugueseSpeechNormalizer.refine(
                                        recognition.text()
                                )
                        )
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

        long recognitionMillis =
                (
                        System.nanoTime()
                                - started
                )
                        / 1_000_000L;

        if (!recognition.success()) {
            WayAround.LOGGER.warn(
                    "[Voice/STT] Falha em {} ms: {}",
                    recognitionMillis,
                    recognition.error()
            );

            if (VoiceConfig
                    .isDebugSpeechEnabled()) {

                showClientMessage(
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

            return;
        }

        WayAround.LOGGER.info(
                "[Voice/STT] Entendido em {} ms: \"{}\"",
                recognitionMillis,
                recognition.text()
        );

        long toneStarted =
                System.nanoTime();

        VoiceToneAnalyzer.ToneProfile profile =
                VoiceToneAnalyzer.analyze(
                        pcm
                );

        double urgency =
                VoiceToneAnalyzer.urgency(
                        profile
                );

        VoiceToneAnalyzer.KeywordEmphasis blueEmphasis =
                VoiceToneAnalyzer.analyzeKeyword(
                        pcm,
                        recognition.words(),
                        profile,
                        "azul",
                        "blue"
                );

        VoiceToneAnalyzer.KeywordEmphasis redEmphasis =
                VoiceToneAnalyzer.analyzeKeyword(
                        pcm,
                        recognition.words(),
                        profile,
                        "vermelho",
                        "red"
                );

        long toneMillis =
                (
                        System.nanoTime()
                                - toneStarted
                )
                        / 1_000_000L;

        WayAround.LOGGER.info(
                "[Voice/Tone] {} ms urgencia={} blueEmphasis={} redEmphasis={}",
                toneMillis,
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        urgency
                ),
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        blueEmphasis.score()
                ),
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        redEmphasis.score()
                )
        );

        Minecraft minecraft =
                Minecraft.getInstance();

        String refinedTranscript =
                BrazilianPortugueseSpeechNormalizer.refine(
                        recognition.text()
                );

        minecraft.execute(
                () -> {
                    VoiceIntentClient.handleTranscript(
                            refinedTranscript,
                            profile,
                            blueEmphasis.score(),
                            redEmphasis.score()
                    );

                    if (minecraft.player != null
                            && minecraft.getConnection()
                            != null
                            && !refinedTranscript.isBlank()) {

                        PacketDistributor.sendToServer(
                                new JusticeVoiceStatementC2SPayload(
                                        refinedTranscript
                                )
                        );

                        PacketDistributor.sendToServer(
                                new TukunaVoiceStatementC2SPayload(
                                        refinedTranscript
                                )
                        );
                    }
                }
        );

        if (!VoiceConfig
                .isDebugSpeechEnabled()) {

            return;
        }

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
                                    "tom=%s | urgencia=%d%% | enfase azul=%d%% | vermelho=%d%% | pitch %.0f->%.0f Hz",
                                    profile.tone(),
                                    Math.round(
                                            urgency
                                                    * 100.0
                                    ),
                                    Math.round(
                                            blueEmphasis.score()
                                                    * 100.0
                                    ),
                                    Math.round(
                                            redEmphasis.score()
                                                    * 100.0
                                    ),
                                    profile.startPitch(),
                                    profile.endPitch()
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
