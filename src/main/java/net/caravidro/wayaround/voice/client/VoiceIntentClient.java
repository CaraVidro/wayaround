package net.caravidro.wayaround.voice.client;

import java.text.Normalizer;
import java.util.Locale;

import net.caravidro.wayaround.network.VoiceIntentC2SPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class VoiceIntentClient {

    private VoiceIntentClient() {
    }

    private static final boolean ENABLED =
            true;

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static void handleTranscript(
            String transcript
    ) {
        if (!ENABLED
                || transcript == null
                || transcript.isBlank()) {

            return;
        }

        String normalized =
                normalize(
                        transcript
                );

        /*
         * V0 de intenção: propositalmente rígido.
         * Só a frase-comando isolada dispara; mencionar BLUE em uma frase
         * normal não ativa nada.
         */
        if (!normalized.equals(
                "tecnica imaginaria azul"
        )) {
            return;
        }

        PacketDistributor.sendToServer(
                new VoiceIntentC2SPayload(
                        VoiceIntentC2SPayload.BLUE
                )
        );

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player != null) {
            minecraft.player
                    .displayClientMessage(
                            Component.literal(
                                            "[Intent] BLUE"
                                    )
                                    .withStyle(
                                            ChatFormatting.BLUE
                                    ),
                            true
                    );
        }
    }

    private static String normalize(
            String text
    ) {
        String decomposed =
                Normalizer.normalize(
                        text,
                        Normalizer.Form.NFD
                );

        return decomposed
                .replaceAll(
                        "\\p{M}+",
                        ""
                )
                .toLowerCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "[^a-z0-9]+",
                        " "
                )
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }
}
