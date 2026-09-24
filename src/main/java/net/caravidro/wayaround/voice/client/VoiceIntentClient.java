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
            5_000L;

    private static final long OUTPUT_WINDOW_MS =
            8_000L;

    private static final long DECISION_DELAY_MS =
            600L;

    private static final long TRIGGER_COOLDOWN_MS =
            650L;

    private static String rollingContext =
            "";

    private static long contextExpiresAt;
    private static long pendingBlueAt;
    private static long lastTriggerAt;

    /*
     * -1 = normal output.
     * 0..1 = forced size/power for the next summon only.
     */
    private static float pendingOutput =
            -1.0F;

    private static long outputExpiresAt;

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

        if (now > outputExpiresAt) {
            pendingOutput =
                    -1.0F;
        }

        List<String> currentWords =
                words(
                        normalized
                );

        OutputModifier output =
                detectOutput(
                        currentWords
                );

        if (output != OutputModifier.NONE) {
            pendingOutput =
                    output == OutputModifier.MAXIMUM
                            ? 1.0F
                            : 0.0F;

            outputExpiresAt =
                    now
                            + OUTPUT_WINDOW_MS;

            WayAround.LOGGER.info(
                    "[Voice/Intent] OUTPUT={} guardado por {} ms",
                    output,
                    OUTPUT_WINDOW_MS
            );

            status(
                    output == OutputModifier.MAXIMUM
                            ? "OUTPUT MAXIMO"
                            : "OUTPUT MINIMO",
                    ChatFormatting.LIGHT_PURPLE
            );
        }

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

            clearContext();

            status(
                    "cancelado",
                    ChatFormatting.GRAY
            );

            return;
        }

        /*
         * Active-Blue commands are interpreted semantically before summon.
         * They do not require a magic phrase and are intentionally broad.
         */
        if (looksLikeStop(
                currentWords
        )) {
            dispatch(
                    VoiceIntentC2SPayload.BLUE_STOP,
                    -1.0F,
                    "STOP"
            );

            clearContext();
            return;
        }

        if (looksLikeLaunch(
                currentWords
        )) {
            dispatch(
                    VoiceIntentC2SPayload.BLUE_LAUNCH,
                    -1.0F,
                    "LAUNCH"
            );

            clearContext();
            return;
        }

        if (looksLikeOrbit(
                currentWords
        )) {
            dispatch(
                    VoiceIntentC2SPayload.BLUE_ORBIT,
                    -1.0F,
                    "ORBIT"
            );

            clearContext();
            return;
        }

        CommandMatch match =
                matchBlueCommand(
                        words
                );

        if (match.complete()) {
            pendingBlueAt =
                    now
                            + DECISION_DELAY_MS;

            WayAround.LOGGER.info(
                    "[Voice/Intent] BLUE completo -> PENDING por {} ms output={}",
                    DECISION_DELAY_MS,
                    pendingOutput
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

        float output =
                now <= outputExpiresAt
                        ? pendingOutput
                        : -1.0F;

        dispatch(
                VoiceIntentC2SPayload.BLUE_SUMMON,
                output,
                output < 0.0F
                        ? "SUMMON BLUE"
                        : (
                        output >= 0.5F
                                ? "SUMMON BLUE / OUTPUT MAXIMO"
                                : "SUMMON BLUE / OUTPUT MINIMO"
                )
        );

        pendingOutput =
                -1.0F;

        outputExpiresAt =
                0L;

        clearContext();
    }

    private static void dispatch(
            byte intent,
            float output,
            String label
    ) {
        long now =
                System.currentTimeMillis();

        if (now - lastTriggerAt
                < TRIGGER_COOLDOWN_MS) {

            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.getConnection()
                == null) {

            return;
        }

        WayAround.LOGGER.info(
                "[Voice/Intent] DISPATCH {} output={}",
                label,
                output
        );

        PacketDistributor.sendToServer(
                new VoiceIntentC2SPayload(
                        intent,
                        output
                )
        );

        lastTriggerAt =
                now;

        status(
                label,
                intent == VoiceIntentC2SPayload.BLUE_STOP
                        ? ChatFormatting.GRAY
                        : ChatFormatting.BLUE
        );
    }

    private static boolean looksLikeOrbit(
            List<String> words
    ) {
        if (containsAny(
                words,
                "spin",
                "gira",
                "gire",
                "girar",
                "girando",
                "rode",
                "rodar",
                "rodando",
                "rotacao",
                "orbita",
                "orbite",
                "orbitar",
                "circula",
                "circule",
                "circular"
        )) {
            return true;
        }

        boolean protective =
                containsAny(
                        words,
                        "proteja",
                        "proteger",
                        "protege",
                        "defenda",
                        "defender",
                        "cubra",
                        "cobrir",
                        "guarde"
                );

        boolean around =
                containsAny(
                        words,
                        "redor",
                        "volta",
                        "lados",
                        "lado",
                        "todos",
                        "360",
                        "cercar",
                        "cercando"
                );

        if (protective
                && around) {

            return true;
        }

        return containsSequence(
                words,
                "em",
                "volta"
        )
                || containsSequence(
                words,
                "ao",
                "redor"
        );
    }

    private static boolean looksLikeLaunch(
            List<String> words
    ) {
        return containsAny(
                words,
                "solta",
                "soltar",
                "solte",
                "lanca",
                "lancar",
                "lance",
                "dispara",
                "disparar",
                "dispare",
                "atira",
                "atirar",
                "atire",
                "arremessa",
                "arremessar"
        );
    }

    private static boolean looksLikeStop(
            List<String> words
    ) {
        if (words.size() <= 4
                && containsAny(
                words,
                "pronto",
                "acabou",
                "pare",
                "parar",
                "termine",
                "termina",
                "desliga",
                "desligue",
                "suma"
        )) {

            return true;
        }

        return containsSequence(
                words,
                "acabe",
                "aqui"
        )
                || containsSequence(
                words,
                "pode",
                "parar"
        );
    }

    private static OutputModifier detectOutput(
            List<String> words
    ) {
        int output =
                findApprox(
                        words,
                        "output",
                        0,
                        2
                );

        if (output < 0
                && !containsSequence(
                words,
                "out",
                "put"
        )) {

            return OutputModifier.NONE;
        }

        if (findApprox(
                words,
                "maximo",
                0,
                2
        ) >= 0
                || containsAny(
                words,
                "maximum",
                "max"
        )) {

            return OutputModifier.MAXIMUM;
        }

        if (findApprox(
                words,
                "minimo",
                0,
                2
        ) >= 0
                || containsAny(
                words,
                "minimum",
                "min"
        )) {

            return OutputModifier.MINIMUM;
        }

        return OutputModifier.NONE;
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

            if (distance(
                    words.get(
                            index
                    ),
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
        return containsAny(
                words,
                "cancela",
                "cancelar",
                "cancele"
        );
    }

    private static boolean containsAny(
            List<String> words,
            String... candidates
    ) {
        for (String word :
                words) {

            for (String candidate :
                    candidates) {

                if (word.equals(
                        candidate
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean containsSequence(
            List<String> words,
            String first,
            String second
    ) {
        for (int index = 0;
             index + 1 < words.size();
             index++) {

            if (words.get(index)
                    .equals(first)
                    && words.get(index + 1)
                    .equals(second)) {

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

    private static void clearContext() {
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

    private enum OutputModifier {
        NONE,
        MINIMUM,
        MAXIMUM
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
