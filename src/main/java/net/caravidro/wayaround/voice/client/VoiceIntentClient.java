package net.caravidro.wayaround.voice.client;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.caravidro.wayaround.WayAround;
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

    private static final long CONTEXT_WINDOW_MS =
            3_000L;

    private static final long DECISION_DELAY_MS =
            600L;

    private static final long TRIGGER_COOLDOWN_MS =
            1_800L;

    private static String rollingContext =
            "";

    private static long contextExpiresAt;
    private static long pendingBlueAt;
    private static long lastTriggerAt;

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

        long now =
                System.currentTimeMillis();

        String normalized =
                normalize(
                        transcript
                );

        if (normalized.isBlank()) {
            return;
        }

        WayAround.LOGGER.info(
                "[Voice/Intent] bruto=\"{}\" normalizado=\"{}\"",
                transcript,
                normalized
        );

        if (now > contextExpiresAt) {
            rollingContext =
                    "";
            pendingBlueAt =
                    0L;
        }

        rollingContext =
                (
                        rollingContext
                                + " "
                                + normalized
                )
                        .trim();

        contextExpiresAt =
                now
                        + CONTEXT_WINDOW_MS;

        List<String> words =
                words(
                        rollingContext
                );

        WayAround.LOGGER.info(
                "[Voice/Intent] contexto=\"{}\"",
                rollingContext
        );

        if (containsCancellation(
                words
        )) {
            WayAround.LOGGER.info(
                    "[Voice/Intent] CANCELADO por palavra explicita"
            );

            clearPending();

            status(
                    "cancelado",
                    ChatFormatting.GRAY
            );

            return;
        }

        CommandMatch match =
                matchBlueCommand(
                        words
                );

        if (match.complete()) {
            /*
             * Do not fire instantly. A short semantic pause gives the player
             * time to add something like "na minha frente" in the next
             * utterance. V0 ignores those modifiers, but the state machine is
             * already ready for them.
             */
            pendingBlueAt =
                    now
                            + DECISION_DELAY_MS;

            WayAround.LOGGER.info(
                    "[Voice/Intent] BLUE completo -> PENDING por {} ms",
                    DECISION_DELAY_MS
            );

            status(
                    "BLUE entendido... aguardando contexto",
                    ChatFormatting.BLUE
            );

            return;
        }

        if (match.prefix()) {
            WayAround.LOGGER.info(
                    "[Voice/Intent] prefixo reconhecido; aguardando tecnica/BLUE"
            );

            status(
                    "tecnica imaginaria... aguardando tecnica",
                    ChatFormatting.AQUA
            );
        }
    }

    public static void tick() {
        if (!ENABLED
                || pendingBlueAt <= 0L) {

            return;
        }

        long now =
                System.currentTimeMillis();

        if (now < pendingBlueAt) {
            return;
        }

        pendingBlueAt =
                0L;

        if (now - lastTriggerAt
                < TRIGGER_COOLDOWN_MS) {

            clearPending();
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.getConnection()
                == null) {

            clearPending();
            return;
        }

        WayAround.LOGGER.info(
                "[Voice/Intent] DISPARANDO BLUE para o servidor"
        );

        PacketDistributor.sendToServer(
                new VoiceIntentC2SPayload(
                        VoiceIntentC2SPayload.BLUE
                )
        );

        lastTriggerAt =
                now;

        status(
                "BLUE",
                ChatFormatting.BLUE
        );

        clearPending();
    }

    private static CommandMatch matchBlueCommand(
            List<String> words
    ) {
        int technique =
                findApprox(
                        words,
                        "tecnica",
                        0,
                        2
                );

        if (technique < 0) {
            return CommandMatch.NONE;
        }

        int imaginary =
                findApprox(
                        words,
                        "imaginaria",
                        technique + 1,
                        3
                );

        if (imaginary < 0
                || imaginary
                - technique
                > 4) {

            return new CommandMatch(
                    true,
                    false
            );
        }

        int blue =
                findBlue(
                        words,
                        imaginary + 1
                );

        return new CommandMatch(
                true,
                blue >= 0
        );
    }

    private static int findApprox(
            List<String> words,
            String target,
            int start,
            int maximumDistance
    ) {
        for (int index =
                     Math.max(
                             0,
                             start
                     );
             index < words.size();
             index++) {

            String word =
                    words.get(
                            index
                    );

            if (distance(
                    word,
                    target
            )
                    <= maximumDistance) {

                return index;
            }
        }

        return -1;
    }

    private static int findBlue(
            List<String> words,
            int start
    ) {
        for (int index =
                     Math.max(
                             0,
                             start
                     );
             index < words.size();
             index++) {

            String word =
                    words.get(
                            index
                    );

            if (word.equals(
                    "blue"
            )
                    || distance(
                    word,
                    "azul"
            ) <= 1) {

                return index;
            }

            if (index + 1
                    < words.size()) {

                String joined =
                        word
                                + words.get(
                                index + 1
                        );

                if (distance(
                        joined,
                        "azul"
                )
                        <= 1) {

                    return index;
                }
            }
        }

        return -1;
    }

    private static boolean containsCancellation(
            List<String> words
    ) {
        for (String word :
                words) {

            if (word.equals("cancela")
                    || word.equals("cancelar")
                    || word.equals("cancele")) {

                return true;
            }
        }

        return false;
    }

    private static List<String> words(
            String normalized
    ) {
        List<String> result =
                new ArrayList<>();

        for (String word :
                normalized.split(
                        " "
                )) {

            if (!word.isBlank()) {
                result.add(
                        word
                );
            }
        }

        return result;
    }

    private static int distance(
            String a,
            String b
    ) {
        int[] previous =
                new int[
                        b.length()
                                + 1
                        ];

        int[] current =
                new int[
                        b.length()
                                + 1
                        ];

        for (int column = 0;
             column <= b.length();
             column++) {

            previous[column] =
                    column;
        }

        for (int row = 1;
             row <= a.length();
             row++) {

            current[0] =
                    row;

            for (int column = 1;
                 column <= b.length();
                 column++) {

                int substitution =
                        previous[
                                column - 1
                                ]
                                + (
                                a.charAt(
                                        row - 1
                                )
                                        == b.charAt(
                                        column - 1
                                )
                                        ? 0
                                        : 1
                        );

                int insertion =
                        current[
                                column - 1
                                ]
                                + 1;

                int deletion =
                        previous[
                                column
                                ]
                                + 1;

                current[column] =
                        Math.min(
                                substitution,
                                Math.min(
                                        insertion,
                                        deletion
                                )
                        );
            }

            int[] swap =
                    previous;

            previous =
                    current;

            current =
                    swap;
        }

        return previous[
                b.length()
                ];
    }

    private static void clearPending() {
        rollingContext =
                "";

        contextExpiresAt =
                0L;

        pendingBlueAt =
                0L;
    }

    private static void status(
            String text,
            ChatFormatting color
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        minecraft.player
                .displayClientMessage(
                        Component.literal(
                                        "[Intent] "
                                                + text
                                )
                                .withStyle(
                                        color
                                ),
                        true
                );
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

    private record CommandMatch(
            boolean prefix,
            boolean complete
    ) {
        private static final CommandMatch NONE =
                new CommandMatch(
                        false,
                        false
                );
    }
}
