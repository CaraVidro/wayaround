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
    private static long pendingAt;
    private static byte pendingIntent;
    private static long lastTriggerAt;

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
            pendingAt =
                    0L;
            pendingIntent =
                    0;
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

        List<String> contextWords =
                words(
                        rollingContext
                );

        WayAround.LOGGER.info(
                "[Voice/Intent] contexto=\"{}\"",
                rollingContext
        );

        if (containsCancellation(
                currentWords
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
         * Free-form control of an already active Blue.
         */
        if (looksLikeFinish(
                currentWords
        )) {
            dispatch(
                    VoiceIntentC2SPayload.BLUE_STOP,
                    -1.0F,
                    "BLUE / ENCERRAR"
            );

            clearContext();
            return;
        }

        if (looksLikeHold(
                currentWords
        )) {
            dispatch(
                    VoiceIntentC2SPayload.BLUE_HOLD,
                    -1.0F,
                    "BLUE / PARAR"
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
                    "BLUE / LANCAR"
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
                    "BLUE / ORBITA"
            );

            clearContext();
            return;
        }

        TechniqueMatch red =
                matchTechnique(
                        contextWords,
                        "vermelho"
                );

        if (red.complete()) {
            pendingIntent =
                    VoiceIntentC2SPayload.RED_FIRE;

            pendingAt =
                    now
                            + 320L;

            WayAround.LOGGER.info(
                    "[Voice/Intent] RED completo -> PENDING"
            );

            status(
                    "VERMELHO entendido",
                    ChatFormatting.RED
            );

            return;
        }

        TechniqueMatch blue =
                matchTechnique(
                        contextWords,
                        "azul"
                );

        if (blue.complete()) {
            pendingIntent =
                    VoiceIntentC2SPayload.BLUE_SUMMON;

            pendingAt =
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

        if (blue.prefix()
                || red.prefix()) {
            WayAround.LOGGER.info(
                    "[Voice/Intent] prefixo reconhecido; aguardando tecnica"
            );

            status(
                    "tecnica imaginaria... aguardando tecnica",
                    ChatFormatting.AQUA
            );
        }
    }

    public static void tick() {
        if (!ENABLED
                || pendingAt <= 0L
                || pendingIntent == 0) {

            return;
        }

        long now =
                System.currentTimeMillis();

        if (now < pendingAt) {
            return;
        }

        byte intent =
                pendingIntent;

        pendingAt =
                0L;

        pendingIntent =
                0;

        if (intent
                == VoiceIntentC2SPayload.BLUE_SUMMON) {

            float output =
                    now <= outputExpiresAt
                            ? pendingOutput
                            : -1.0F;

            dispatch(
                    intent,
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

        } else if (intent
                == VoiceIntentC2SPayload.RED_FIRE) {

            dispatch(
                    intent,
                    -1.0F,
                    "TECNICA IMAGINARIA / VERMELHO"
            );
        }

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
                intent == VoiceIntentC2SPayload.RED_FIRE
                        ? ChatFormatting.RED
                        : intent == VoiceIntentC2SPayload.BLUE_STOP
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

        return protective
                && around
                || containsSequence(
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

    private static boolean looksLikeHold(
            List<String> words
    ) {
        if (containsAny(
                words,
                "pare",
                "parar",
                "congele",
                "congelar"
        )) {
            return true;
        }

        boolean stay =
                containsAny(
                        words,
                        "fica",
                        "fique",
                        "permanece",
                        "permaneca"
                );

        boolean still =
                containsAny(
                        words,
                        "parado",
                        "parada",
                        "ai",
                        "aqui",
                        "lugar"
                );

        return stay
                && still;
    }

    private static boolean looksLikeFinish(
            List<String> words
    ) {
        if (words.size() <= 5
                && containsAny(
                words,
                "pronto",
                "acabou",
                "termine",
                "termina",
                "desliga",
                "desligue",
                "suma",
                "encerra",
                "encerre",
                "finaliza",
                "finalize"
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
                "acabar"
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

    private static TechniqueMatch matchTechnique(
            List<String> words,
            String color
    ) {
        int technique =
                findApprox(
                        words,
                        "tecnica",
                        0,
                        2
                );

        if (technique < 0) {
            return TechniqueMatch.NONE;
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

            return new TechniqueMatch(
                    true,
                    false
            );
        }

        int colorIndex =
                color.equals(
                        "azul"
                )
                        ? findBlue(
                                words,
                                imaginary + 1
                        )
                        : findRed(
                                words,
                                imaginary + 1
                        );

        return new TechniqueMatch(
                true,
                colorIndex >= 0
        );
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
                    < words.size()
                    && distance(
                    word
                            + words.get(
                            index + 1
                    ),
                    "azul"
            ) <= 1) {

                return index;
            }
        }

        return -1;
    }

    private static int findRed(
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
                    "red"
            )
                    || distance(
                    word,
                    "vermelho"
            ) <= 2) {

                return index;
            }
        }

        return -1;
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

        pendingAt =
                0L;

        pendingIntent =
                0;
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

    private record TechniqueMatch(
            boolean prefix,
            boolean complete
    ) {
        private static final TechniqueMatch NONE =
                new TechniqueMatch(
                        false,
                        false
                );
    }
}
