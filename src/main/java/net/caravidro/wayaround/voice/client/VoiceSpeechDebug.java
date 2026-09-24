package net.caravidro.wayaround.voice.client;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class VoiceSpeechDebug {

    private VoiceSpeechDebug() {
    }

    private static final ExecutorService WORKER =
            Executors.newSingleThreadExecutor(
                    runnable -> {
                        Thread thread =
                                new Thread(
                                        runnable,
                                        "WayAround-SpeechDebug"
                                );

                        thread.setDaemon(true);

                        return thread;
                    }
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

        WORKER.execute(
                () -> process(copy)
        );
    }

    private static void process(
            byte[] pcm
    ) {
        VoiceToneAnalyzer.ToneProfile profile =
                VoiceToneAnalyzer.analyze(
                        pcm
                );

        WindowsSpeechRecognizer.Result recognition =
                WindowsSpeechRecognizer.recognize(
                        pcm
                );

        Minecraft minecraft =
                Minecraft.getInstance();

        minecraft.execute(
                () -> {
                    if (!VoiceConfig
                            .isDebugSpeechEnabled()) {

                        return;
                    }

                    if (!recognition.success()) {
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

                        return;
                    }

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
}
