package net.caravidro.wayaround.voice.client;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.BlueClientEffects;
import net.caravidro.wayaround.client.BetaTechniqueClientEffects;
import net.caravidro.wayaround.network.VoiceIntentC2SPayload;
import net.caravidro.wayaround.spectrum.SpectrumAccess;
import net.caravidro.wayaround.spectrum.SpectrumType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class VoiceIntentClient {

    private VoiceIntentClient() {
    }

    private static final boolean ENABLED =
            true;

    /*
     * Chat and voice share the same intent parser. Chat is deliberately marked
     * while parsing so server-side chat listeners (Tukuna/Justice) remain the
     * single authority for their conversational state and are not triggered twice.
     */
    private static boolean handlingChatInput;
    private static boolean inputGuidanceShown;

    private static final long CONTEXT_WINDOW_MS =
            5_000L;

    private static final long OUTPUT_WINDOW_MS =
            8_000L;

    private static final long DECISION_DELAY_MS =
            600L;

    private static final long TRIGGER_COOLDOWN_MS =
            650L;

    private static final long COMBAT_HOT_MEMORY_MS =
            1_800L;

    private static final long REFLEX_STAGE_WINDOW_MS =
            2_200L;

    private static final long REFLEX_RETRIGGER_MS =
            1_450L;

    private static String rollingContext =
            "";

    private static long contextExpiresAt;
    private static long pendingAt;
    private static byte pendingIntent;
    private static long lastTriggerAt;

    private static float pendingOutput =
            -1.0F;

    private static float pendingUrgency =
            0.0F;

    private static long outputExpiresAt;

    private static long lastBlueSummonDispatchAt;
    private static float deferredPostSummonOutput =
            -1.0F;
    private static long deferredPostSummonOutputExpiresAt;

    private static long dualPreparedUntil;

    /*
     * Combat-reflex parser state. combatHotUntil is volatile because the audio
     * capture thread reads it while the render/client thread updates it.
     */
    private static volatile long combatHotUntil;

    private static long reflexTechniqueUntil;
    private static long reflexImaginaryUntil;
    private static long reflexDomainUntil;
    private static long lastReflexDispatchAt;

    private static long tukunaDesmarPreparedUntil;
    private static long lastTukunaReflexDispatchAt;

    public static boolean isEnabled() {
        return ENABLED;
    }

    /**
     * Voice transport does not require Vosk. Native speech recognition is only
     * useful when the local player actually owns a Spectrum with spoken
     * actions, or when speech debug was explicitly enabled.
     *
     * Keeping this check client-side avoids loading libvosk/model data for a
     * player who merely wants ordinary proximity voice chat.
     */
    public static boolean shouldRecognizeLocalPlayer() {
        if (!ENABLED) {
            return false;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {
            return false;
        }

        return SpectrumAccess.has(
                minecraft.player,
                SpectrumType.VOID
        )
                || SpectrumAccess.has(
                minecraft.player,
                SpectrumType.TUKUNA
        )
                || SpectrumAccess.has(
                minecraft.player,
                SpectrumType.JUSTICE
        );
    }

    public static boolean isCombatHot() {
        return System.currentTimeMillis()
                <= combatHotUntil;
    }

    public static boolean wantsSpeculativeRecognition() {
        return isCombatHot()
                || hasLocalTukunaSpectrum();
    }

    /**
     * Called by speculative STT snapshots while the utterance is STILL being
     * spoken. This is intentionally much narrower than normal intent parsing:
     * it only acts during the short "I am being attacked" window.
     */
    public static void handleSpeculativeTranscript(
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

        if (normalized.isBlank()) {
            return;
        }

        if (!handlingChatInput) {
            showInputGuidanceOnce();
        }

        long now =
                System.currentTimeMillis();

        if (hasLocalTukunaSpectrum()) {
            boolean full =
                    normalized.contains(
                            "desmartelar"
                    )
                            || normalized.contains(
                            "desmantelar"
                    );

            boolean prefix =
                    normalized.contains(
                            "desmar"
                    );

            if (full
                    && now - lastTukunaReflexDispatchAt
                    >= 900L) {

                lastTukunaReflexDispatchAt =
                        now;

                dispatch(
                        normalized.contains(
                                "fogo"
                        )
                                ? VoiceIntentC2SPayload.TUKUNA_DESMARTELAR_FIRE
                                : VoiceIntentC2SPayload.TUKUNA_DESMARTELAR,
                        -1.0F,
                        1.0F,
                        normalized.contains(
                                "fogo"
                        )
                                ? "TUKUNA / DESMARTELAR FOGO REFLEXO"
                                : "TUKUNA / DESMARTELAR REFLEXO"
                );

                tukunaDesmarPreparedUntil =
                        0L;

                status(
                        "DESMARTELAR",
                        ChatFormatting.DARK_RED
                );

                return;
            }

            if (prefix) {
                tukunaDesmarPreparedUntil =
                        now
                                + REFLEX_STAGE_WINDOW_MS;

                status(
                        "desmar... atento",
                        ChatFormatting.RED
                );
            }
        }

        if (!isCombatHot()) {
            return;
        }

        handleCombatReflex(
                normalized,
                now,
                1.0F
        );
    }

    public static void handleTranscript(
            String transcript
    ) {
        handleTranscript(
                transcript,
                null,
                0.0,
                0.0
        );
    }

    private static boolean handleExactSpectrumAction(String text) {
        var action = switch (text) {
            case "tecnica imaginaria azul", "tecnica imagina azul" -> net.caravidro.wayaround.spectrum.SpectrumAction.BLUE;
            case "tecnica imaginaria vermelho" -> net.caravidro.wayaround.spectrum.SpectrumAction.RED;
            case "azul orbitar", "orbitar azul" -> net.caravidro.wayaround.spectrum.SpectrumAction.BLUE_ORBIT;
            case "azul parar", "parar azul" -> net.caravidro.wayaround.spectrum.SpectrumAction.BLUE_HOLD;
            case "azul lancar", "lancar azul" -> net.caravidro.wayaround.spectrum.SpectrumAction.BLUE_LAUNCH;
            case "azul encerrar", "encerrar azul" -> net.caravidro.wayaround.spectrum.SpectrumAction.BLUE_END;
            case "azul potencia maxima" -> net.caravidro.wayaround.spectrum.SpectrumAction.BLUE_MAX;
            case "vermelho lancar", "lancar vermelho" -> net.caravidro.wayaround.spectrum.SpectrumAction.RED_LAUNCH;
            case "vermelho energia maxima" -> net.caravidro.wayaround.spectrum.SpectrumAction.RED_MAX;
            case "infinito reforcar", "reforcar infinito" -> net.caravidro.wayaround.spectrum.SpectrumAction.INFINITY_REINFORCE;
            case "infinito encerrar", "encerrar infinito" -> net.caravidro.wayaround.spectrum.SpectrumAction.INFINITY_OFF;
            case "ativar infinito" -> net.caravidro.wayaround.spectrum.SpectrumAction.INFINITY;
            case "preparar azul e vermelho" -> net.caravidro.wayaround.spectrum.SpectrumAction.DUAL;
            case "lancar roxo" -> net.caravidro.wayaround.spectrum.SpectrumAction.PURPLE;
            case "dominio void" -> net.caravidro.wayaround.spectrum.SpectrumAction.VOID_DOMAIN;
            default -> null;
        };
        if (action == null || Minecraft.getInstance().getConnection() == null) return false;
        PacketDistributor.sendToServer(new net.caravidro.wayaround.network.SpectrumInputPayload(action.id,(byte)0));
        return true;
    }

    public static void handleChatMessage(
            String message
    ) {
        if (!ENABLED
                || message == null
                || message.isBlank()) {
            return;
        }

        handlingChatInput = true;

        try {
            handleTranscript(
                    message,
                    null,
                    0.0,
                    0.0
            );
        } finally {
            handlingChatInput = false;
        }
    }

    public static void handleTranscript(
            String transcript,
            VoiceToneAnalyzer.ToneProfile profile,
            double blueEmphasis,
            double redEmphasis
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

        if (!handlingChatInput) {
            showInputGuidanceOnce();
        }

        double globalUrgency =
                VoiceToneAnalyzer.urgency(
                        profile
                );

        if (handleExactSpectrumAction(normalized)) return;
        // These phrases go once through the server speech listener (pacts/Fuga/combo).
        if (normalized.equals("preparar desmartelar") || normalized.equals("carregar desmartelar")
                || normalized.equals("soltar desmartelar") || normalized.equals("lancar desmartelar")
                || normalized.equals("combinar fogo") || normalized.equals("cancelar tecnica")
                || normalized.equals("dominio tukuna") || normalized.equals("expansao de dominio tukuna")) return;

        if (isCombatHot()
                && handleCombatReflex(
                normalized,
                now,
                (float) Math.max(
                        0.82,
                        globalUrgency
                )
        )) {
            return;
        }

        WayAround.LOGGER.info(
                "[Voice/Intent] bruto=\"{}\" normalizado=\"{}\" urgencia={} enfaseBlue={} enfaseRed={}",
                transcript,
                normalized,
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        globalUrgency
                ),
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        blueEmphasis
                ),
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        redEmphasis
                )
        );

        if (now > outputExpiresAt) {
            pendingOutput =
                    -1.0F;
        }

        List<String> currentWords =
                words(
                        normalized
                );

        if (!handlingChatInput) {
        /*
         * Tukuna words are intentionally simple and immediate. Unlike Blue,
         * "trocar" requires consent from TWO separate player UUIDs server-side,
         * so there is no reason to make the client guess intent beyond hearing
         * the explicit word.
         */
        if (containsAny(
                currentWords,
                "trocar",
                "troca"
        )) {
            dispatch(
                    VoiceIntentC2SPayload.TUKUNA_SWAP_CONFIRM,
                    -1.0F,
                    (float) globalUrgency,
                    "TUKUNA / TROCAR"
            );

            clearContext();
            return;
        }

        /*
         * Speculative recognition enters attentive mode as soon as "desmar"
         * appears. If the speaker actually finishes the utterance there, the
         * cutoff itself is treated as an intentional cast.
         */
        if (currentWords.contains(
                "desmar"
        )) {
            dispatch(
                    VoiceIntentC2SPayload.TUKUNA_DESMARTELAR,
                    -1.0F,
                    (float) globalUrgency,
                    "TUKUNA / DESMAR"
            );

            tukunaDesmarPreparedUntil = 0L;
            clearContext();
            return;
        }

        if (containsAny(
                currentWords,
                "desmartelar",
                "desmantelar"
        )) {
            boolean fire =
                    containsAny(
                            currentWords,
                            "fogo",
                            "chama",
                            "incendio",
                            "queimar"
                    );

            dispatch(
                    fire
                            ? VoiceIntentC2SPayload.TUKUNA_DESMARTELAR_FIRE
                            : VoiceIntentC2SPayload.TUKUNA_DESMARTELAR,
                    -1.0F,
                    (float) globalUrgency,
                    fire
                            ? "TUKUNA / DESMARTELAR FOGO"
                            : "TUKUNA / DESMARTELAR"
            );

            tukunaDesmarPreparedUntil =
                    0L;

            clearContext();
            return;
        }

        if (containsAny(
                currentWords,
                "fuga"
        )
                && currentWords.size() <= 4) {

            dispatch(
                    VoiceIntentC2SPayload.TUKUNA_FUGA,
                    -1.0F,
                    (float) globalUrgency,
                    "TUKUNA / FUGA"
            );

            clearContext();
            return;
        }

        }

        OutputModifier output =
                detectOutput(
                        currentWords
                );

        if (output == OutputModifier.MAXIMUM
                && BetaTechniqueClientEffects.hasLocalHeldRed()) {

            dispatch(
                    VoiceIntentC2SPayload.RED_MAXIMUM,
                    1.0F,
                    (float) globalUrgency,
                    "RED / ENERGIA MAXIMA"
            );

            clearContext();
            return;
        }

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

        if (looksLikeVoidDomain(
                currentWords,
                false
        )
                && !hasLocalJusticeSpectrum()) {
            dispatch(
                    VoiceIntentC2SPayload.VOID_DOMAIN_EXPAND,
                    -1.0F,
                    (float) globalUrgency,
                    "DOMINIO DE EXPANSAO / VOID"
            );

            clearContext();
            return;
        }

        InfinityEvidence infinity =
                detectInfinityEvidence(
                        currentWords,
                        globalUrgency
                );

        if (infinity.off()) {
            dispatch(
                    VoiceIntentC2SPayload.INFINITY_OFF,
                    -1.0F,
                    0.0F,
                    "INFINIDADE / DESATIVAR"
            );

            clearContext();
            return;
        }

        if (infinity.maxOn()) {
            dispatch(
                    VoiceIntentC2SPayload.INFINITY_ON,
                    1.0F,
                    (float) globalUrgency,
                    "INFINIDADE / MAXIMO"
            );

            clearContext();
            return;
        }

        if (infinity.evidence() > 0.001F) {
            dispatch(
                    VoiceIntentC2SPayload.INFINITY_REINFORCE,
                    infinity.evidence(),
                    (float) globalUrgency,
                    "INFINIDADE / CONFIANCA +"
            );

            WayAround.LOGGER.info(
                    "[Voice/Intent] INFINITY evidence={} reason={}",
                    String.format(
                            Locale.ROOT,
                            "%.2f",
                            infinity.evidence()
                    ),
                    infinity.reason()
            );

            clearContext();
            return;
        }

        if (looksLikePurpleVoid(
                currentWords
        )) {
            if (now <= dualPreparedUntil) {
                dispatch(
                        VoiceIntentC2SPayload.PURPLE_VOID,
                        -1.0F,
                        (float) globalUrgency,
                        "VAZIO ROXO"
                );

                dualPreparedUntil =
                        0L;

                clearContext();
                return;
            }

            WayAround.LOGGER.info(
                    "[Voice/Intent] VAZIO ROXO ignorado: nenhum BLUE+RED preparado recentemente"
            );
        }

        TechniqueMatch dualBlue =
                matchTechnique(
                        contextWords,
                        "azul"
                );

        TechniqueMatch dualRed =
                matchTechnique(
                        contextWords,
                        "vermelho"
                );

        if (dualBlue.complete()
                && dualRed.complete()) {

            pendingAt =
                    0L;

            pendingIntent =
                    0;

            dispatch(
                    VoiceIntentC2SPayload.DUAL_PREPARE,
                    pendingOutput,
                    (float) globalUrgency,
                    "BLUE + RED / AGUARDANDO VAZIO ROXO"
            );

            dualPreparedUntil =
                    now + 7_000L;

            pendingOutput =
                    -1.0F;

            outputExpiresAt =
                    0L;

            clearContext();
            return;
        }

        if (BetaTechniqueClientEffects.hasLocalHeldRed()
                && looksLikeLaunch(
                currentWords
        )) {

            dispatch(
                    VoiceIntentC2SPayload.RED_LAUNCH,
                    -1.0F,
                    (float) globalUrgency,
                    "RED / LANCAR"
            );

            clearContext();
            return;
        }

        /*
         * Free-form control belongs to the conversational context of an
         * already active Blue. This is the important difference between
         * "gira a roda" and "agora me proteja em todos os lados".
         */
        boolean controllingBlue =
                BlueClientEffects.hasLocalControllableBlue();

        /*
         * Vosk normally drops punctuation, so "azul, pare" becomes
         * "azul pare". Treat the technique name itself as an address/vocative
         * and route the following semantics as a control command. This does
         * not depend on the local BLUE snapshot; the server remains
         * authoritative and simply ignores the command if no BLUE exists.
         */
        boolean blueAddressed =
                findBlue(
                        currentWords,
                        0
                ) >= 0;

        if (blueAddressed
                && looksLikeFinish(
                currentWords
        )) {

            dispatch(
                    VoiceIntentC2SPayload.BLUE_STOP,
                    -1.0F,
                    0.0F,
                    "BLUE / ENCERRAR"
            );

            clearContext();
            return;
        }

        if (blueAddressed
                && looksLikeHold(
                currentWords
        )) {

            dispatch(
                    VoiceIntentC2SPayload.BLUE_HOLD,
                    -1.0F,
                    0.0F,
                    "BLUE / PARAR"
            );

            clearContext();
            return;
        }

        if (blueAddressed
                && looksLikeLaunch(
                currentWords
        )) {

            dispatch(
                    VoiceIntentC2SPayload.BLUE_LAUNCH,
                    -1.0F,
                    0.0F,
                    "BLUE / LANCAR"
            );

            clearContext();
            return;
        }

        if (blueAddressed
                && looksLikeOrbit(
                currentWords
        )) {

            dispatch(
                    VoiceIntentC2SPayload.BLUE_ORBIT,
                    -1.0F,
                    0.0F,
                    "BLUE / ORBITA"
            );

            clearContext();
            return;
        }

        /*
         * "Técnica imaginária: azul" followed immediately by
         * "output máximo" can arrive before the first BLUE snapshot reaches
         * the client. Keep the modifier briefly and apply it as soon as the
         * spawned BLUE becomes visible/controllable.
         */
        if (!controllingBlue
                && output != OutputModifier.NONE
                && now - lastBlueSummonDispatchAt
                <= 4_000L) {

            deferredPostSummonOutput =
                    pendingOutput;

            deferredPostSummonOutputExpiresAt =
                    now + 4_000L;

            WayAround.LOGGER.info(
                    "[Voice/Intent] OUTPUT pós-summon aguardando BLUE ativo: {}",
                    deferredPostSummonOutput
            );

            clearContext();
            return;
        }

        if (controllingBlue
                && output != OutputModifier.NONE) {

            dispatch(
                    VoiceIntentC2SPayload.BLUE_OUTPUT,
                    pendingOutput,
                    0.0F,
                    output == OutputModifier.MAXIMUM
                            ? "BLUE / OUTPUT MAXIMO"
                            : "BLUE / OUTPUT MINIMO"
            );

            pendingOutput =
                    -1.0F;

            outputExpiresAt =
                    0L;

            lastBlueSummonDispatchAt =
                    now;

            clearContext();
            return;
        }

        if (controllingBlue
                && looksLikeFinish(
                currentWords
        )) {
            dispatch(
                    VoiceIntentC2SPayload.BLUE_STOP,
                    -1.0F,
                    0.0F,
                    "BLUE / ENCERRAR"
            );

            clearContext();
            return;
        }

        if (controllingBlue
                && looksLikeHold(
                currentWords
        )) {
            dispatch(
                    VoiceIntentC2SPayload.BLUE_HOLD,
                    -1.0F,
                    0.0F,
                    "BLUE / PARAR"
            );

            clearContext();
            return;
        }

        if (controllingBlue
                && looksLikeLaunch(
                currentWords
        )) {
            dispatch(
                    VoiceIntentC2SPayload.BLUE_LAUNCH,
                    -1.0F,
                    0.0F,
                    "BLUE / LANCAR"
            );

            clearContext();
            return;
        }

        if (controllingBlue
                && looksLikeOrbit(
                currentWords
        )) {
            dispatch(
                    VoiceIntentC2SPayload.BLUE_ORBIT,
                    -1.0F,
                    0.0F,
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

            pendingUrgency =
                    effectiveUrgency(
                            globalUrgency,
                            redEmphasis
                    );

            long redDelay =
                    decisionDelay(
                            pendingUrgency,
                            320L
                    );

            pendingAt =
                    now
                            + redDelay;

            WayAround.LOGGER.info(
                    "[Voice/Intent] RED completo -> PENDING {} ms urgencia={}",
                    redDelay,
                    pendingUrgency
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

            pendingUrgency =
                    effectiveUrgency(
                            globalUrgency,
                            blueEmphasis
                    );

            long blueDelay =
                    decisionDelay(
                            pendingUrgency,
                            DECISION_DELAY_MS
                    );

            pendingAt =
                    now
                            + blueDelay;

            WayAround.LOGGER.info(
                    "[Voice/Intent] BLUE completo -> PENDING {} ms output={} urgencia={}",
                    blueDelay,
                    pendingOutput,
                    pendingUrgency
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
        if (!ENABLED) {
            return;
        }

        long now =
                System.currentTimeMillis();

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player != null
                && minecraft.player.hurtTime > 0) {

            combatHotUntil =
                    now
                            + COMBAT_HOT_MEMORY_MS;
        }

        if (deferredPostSummonOutput >= 0.0F) {
            if (now > deferredPostSummonOutputExpiresAt) {
                deferredPostSummonOutput =
                        -1.0F;

                deferredPostSummonOutputExpiresAt =
                        0L;

            } else if (BlueClientEffects
                    .hasLocalControllableBlue()) {

                dispatch(
                        VoiceIntentC2SPayload.BLUE_OUTPUT,
                        deferredPostSummonOutput,
                        0.0F,
                        deferredPostSummonOutput >= 0.5F
                                ? "BLUE / OUTPUT MAXIMO"
                                : "BLUE / OUTPUT MINIMO"
                );

                deferredPostSummonOutput =
                        -1.0F;

                deferredPostSummonOutputExpiresAt =
                        0L;
            }
        }

        if (pendingAt <= 0L
                || pendingIntent == 0) {

            return;
        }

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
                    pendingUrgency,
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
                    pendingUrgency,
                    "TECNICA IMAGINARIA / VERMELHO"
            );
        }

        clearContext();
    }

    private static boolean handleCombatReflex(
            String normalized,
            long now,
            float urgency
    ) {
        List<String> words =
                words(
                        normalized
                );

        if (words.isEmpty()) {
            return false;
        }

        boolean sawDomainPrefix =
                containsPrefix(
                        words,
                        "do",
                        2
                )
                        || containsPrefix(
                        words,
                        "dom",
                        3
                );

        boolean sawDomain =
                containsPrefix(
                        words,
                        "dominio",
                        5
                );

        if (sawDomainPrefix) {
            reflexDomainUntil =
                    now
                            + REFLEX_STAGE_WINDOW_MS;

            status(
                    "DO... dominio preparado",
                    ChatFormatting.WHITE
            );
        }

        if (sawDomain
                && (
                now <= reflexDomainUntil
                        || looksLikeVoidDomain(
                        words,
                        true
                )
        )) {

            boolean dispatched =
                    reflexDispatch(
                            VoiceIntentC2SPayload.VOID_DOMAIN_EXPAND,
                            -1.0F,
                            urgency,
                            "DOMINIO / REFLEXO",
                            ChatFormatting.WHITE,
                            now
                    );

            if (dispatched) {
                reflexDomainUntil =
                        0L;

                clearContext();
            }

            /*
             * Even when this exact word is the later final STT result and the
             * reflex cooldown suppresses a duplicate packet, consume it here.
             */
            return true;
        }

        if (containsPrefix(
                words,
                "tec",
                3
        )
                || containsPrefix(
                words,
                "tecnica",
                4
        )) {

            reflexTechniqueUntil =
                    now
                            + REFLEX_STAGE_WINDOW_MS;

            status(
                    "TEC... Azul/Vermelho preparados",
                    ChatFormatting.AQUA
            );
        }

        if (now <= reflexTechniqueUntil
                && (
                containsPrefix(
                        words,
                        "imag",
                        4
                )
                        || containsPrefix(
                        words,
                        "imaginaria",
                        6
                )
        )) {

            reflexImaginaryUntil =
                    now
                            + REFLEX_STAGE_WINDOW_MS;

            status(
                    "IMAGINARIA... gatilho armado",
                    ChatFormatting.LIGHT_PURPLE
            );
        }

        if (now <= reflexImaginaryUntil) {
            if (containsPrefix(
                    words,
                    "azu",
                    3
            )
                    || containsAny(
                    words,
                    "blue"
            )) {

                float output =
                        now <= outputExpiresAt
                                ? pendingOutput
                                : -1.0F;

                boolean dispatched =
                        reflexDispatch(
                                VoiceIntentC2SPayload.BLUE_SUMMON,
                                output,
                                urgency,
                                "BLUE / REFLEXO",
                                ChatFormatting.BLUE,
                                now
                        );

                if (dispatched) {
                    pendingOutput =
                            -1.0F;

                    outputExpiresAt =
                            0L;

                    reflexTechniqueUntil =
                            0L;

                    reflexImaginaryUntil =
                            0L;

                    lastBlueSummonDispatchAt =
                            now;

                    clearContext();
                }

                return true;
            }

            if (containsPrefix(
                    words,
                    "verm",
                    4
            )
                    || containsAny(
                    words,
                    "red"
            )) {

                boolean dispatched =
                        reflexDispatch(
                                VoiceIntentC2SPayload.RED_FIRE,
                                -1.0F,
                                urgency,
                                "VERMELHO / REFLEXO",
                                ChatFormatting.RED,
                                now
                        );

                if (dispatched) {
                    reflexTechniqueUntil =
                            0L;

                    reflexImaginaryUntil =
                            0L;

                    clearContext();
                }

                return true;
            }
        }

        return false;
    }

    private static boolean reflexDispatch(
            byte intent,
            float output,
            float urgency,
            String label,
            ChatFormatting color,
            long now
    ) {
        if (now - lastReflexDispatchAt
                < REFLEX_RETRIGGER_MS) {
            return false;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.getConnection()
                        == null) {
            return false;
        }

        PacketDistributor.sendToServer(
                new VoiceIntentC2SPayload(
                        intent,
                        output,
                        urgency
                )
        );

        lastReflexDispatchAt =
                now;

        lastTriggerAt =
                now;

        status(
                label,
                color
        );

        WayAround.LOGGER.info(
                "[Voice/Intent] REFLEX DISPATCH {} transcript-stage output={} urgency={}",
                label,
                output,
                urgency
        );

        return true;
    }

    private static boolean looksLikeVoidDomain(
            List<String> words,
            boolean combat
    ) {
        boolean domain =
                containsPrefix(
                        words,
                        "dominio",
                        5
                );

        if (!domain) {
            return false;
        }

        if (combat) {
            return true;
        }

        return containsPrefix(
                words,
                "expans",
                6
        )
                || containsAny(
                words,
                "expansion"
        );
    }

    private static boolean containsPrefix(
            List<String> words,
            String prefix,
            int minimumLength
    ) {
        for (String word :
                words) {

            if (word.length()
                    < minimumLength) {
                continue;
            }

            if (word.startsWith(
                    prefix
            )
                    || prefix.startsWith(
                    word
            )) {
                return true;
            }
        }

        return false;
    }

    private static float effectiveUrgency(
            double globalUrgency,
            double keywordEmphasis
    ) {
        /*
         * Strong emphasis on the technique's COLOR is treated as deliberate
         * commitment even when the rest of the sentence is calm.
         */
        double emphasisBoost =
                keywordEmphasis >= 0.78
                        ? 1.0
                        : keywordEmphasis >= 0.60
                                ? 0.86
                                : keywordEmphasis >= 0.42
                                        ? 0.64
                                        : keywordEmphasis;

        return (float) Math.max(
                0.0,
                Math.min(
                        1.0,
                        Math.max(
                                globalUrgency,
                                emphasisBoost
                        )
                )
        );
    }

    private static long decisionDelay(
            double urgency,
            long calmDelay
    ) {
        if (urgency >= 0.86) {
            return 10L;
        }

        if (urgency >= 0.68) {
            return Math.min(
                    calmDelay,
                    45L
            );
        }

        if (urgency >= 0.50) {
            return Math.min(
                    calmDelay,
                    130L
            );
        }

        if (urgency >= 0.32) {
            return Math.min(
                    calmDelay,
                    300L
            );
        }

        return calmDelay;
    }

    private static void dispatch(
            byte intent,
            float output,
            float urgency,
            String label
    ) {
        long now =
                System.currentTimeMillis();

        boolean conversationalInfinity =
                intent
                        == VoiceIntentC2SPayload.INFINITY_REINFORCE
                        || intent
                        == VoiceIntentC2SPayload.INFINITY_OFF;

        if (intent
                != VoiceIntentC2SPayload.BLUE_OUTPUT
                && !conversationalInfinity
                && now - lastTriggerAt
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
                "[Voice/Intent] DISPATCH {} output={} urgencia={}",
                label,
                output,
                urgency
        );

        PacketDistributor.sendToServer(
                new VoiceIntentC2SPayload(
                        intent,
                        output,
                        urgency
                )
        );

        if (intent
                != VoiceIntentC2SPayload.BLUE_OUTPUT
                && !conversationalInfinity) {

            lastTriggerAt =
                    now;
        }

        status(
                label,
                intent == VoiceIntentC2SPayload.RED_FIRE
                        ? ChatFormatting.RED
                        : intent == VoiceIntentC2SPayload.BLUE_STOP
                                ? ChatFormatting.GRAY
                                : ChatFormatting.BLUE
        );
    }

    private static InfinityEvidence detectInfinityEvidence(
            List<String> words,
            double urgency
    ) {
        int infinityIndex =
                findApprox(
                        words,
                        "infinidade",
                        0,
                        2
                );

        boolean hasInfinity =
                infinityIndex >= 0
                        || containsAny(
                        words,
                        "infinity"
                );

        if (!hasInfinity) {
            return InfinityEvidence.NONE;
        }

        boolean explicitOn =
                containsAny(
                        words,
                        "ativar",
                        "ative",
                        "ativa",
                        "ligar",
                        "ligue",
                        "liga"
                );

        boolean off =
                containsAny(
                        words,
                        "desliga",
                        "desligue",
                        "desativar",
                        "desative",
                        "cancela",
                        "cancele",
                        "acabe"
                )
                        || containsSequence(
                        words,
                        "sem",
                        "infinidade"
                );

        if (off) {
            return new InfinityEvidence(
                    0.0F,
                    true,
                    false,
                    "desativacao explicita"
            );
        }

        if (explicitOn) {
            return new InfinityEvidence(
                    1.0F,
                    false,
                    true,
                    "ativacao maxima explicita"
            );
        }

        boolean warning =
                containsAny(
                        words,
                        "aproximar",
                        "aproxime",
                        "aproxima",
                        "chegar",
                        "chegue",
                        "perto",
                        "encostar",
                        "encoste"
                )
                        || (
                        containsAny(
                                words,
                                "nao"
                        )
                                && containsAny(
                                words,
                                "pode",
                                "consegue",
                                "vai"
                        )
                );

        boolean possession =
                containsAny(
                        words,
                        "tenho",
                        "possuo",
                        "meu",
                        "minha",
                        "comigo",
                        "lado"
                );

        boolean protection =
                containsAny(
                        words,
                        "protege",
                        "proteger",
                        "proteja",
                        "defende",
                        "defender",
                        "defesa",
                        "escudo",
                        "segura",
                        "salva"
                );

        boolean selfReference =
                containsAny(
                        words,
                        "me",
                        "mim",
                        "meu",
                        "minha"
                );

        float evidence =
                0.05F;

        StringBuilder reason =
                new StringBuilder(
                        "mencao"
                );

        if (warning) {
            evidence +=
                    0.14F;

            reason.append(
                    "+limite"
            );
        }

        if (possession) {
            evidence +=
                    0.12F;

            reason.append(
                    "+posse"
            );
        }

        if (protection) {
            evidence +=
                    0.34F;

            reason.append(
                    "+protecao"
            );
        }

        if (selfReference
                && protection) {

            evidence +=
                    0.08F;
        }

        if (!warning
                && !possession
                && !protection
                && urgency < 0.48) {

            return InfinityEvidence.NONE;
        }

        evidence +=
                (float) Math.min(
                        0.08,
                        urgency * 0.08
                );

        return new InfinityEvidence(
                Math.min(
                        0.62F,
                        evidence
                ),
                false,
                false,
                reason.toString()
        );
    }

    private static boolean looksLikePurpleVoid(
            List<String> words
    ) {
        int voidIndex =
                findApprox(
                        words,
                        "vazio",
                        0,
                        1
                );

        int purpleIndex =
                findApprox(
                        words,
                        "roxo",
                        0,
                        1
                );

        boolean standalonePurple =
                purpleIndex >= 0
                        || findApprox(
                        words,
                        "russo",
                        0,
                        1
                ) >= 0
                        || containsAny(
                        words,
                        "roxa",
                        "rox",
                        "rocho",
                        "rouxo"
                );

        boolean portuguesePair =
                standalonePurple
                        && (
                        voidIndex >= 0
                                || words.size() <= 3
                );

        int englishVoid =
                indexOfAny(
                        words,
                        "void"
                );

        int englishPurple =
                indexOfAny(
                        words,
                        "purple"
                );

        boolean englishPair =
                englishVoid >= 0
                        && englishPurple >= 0
                        && Math.abs(
                        englishVoid
                                - englishPurple
                ) <= 2;

        return standalonePurple
                || portuguesePair
                || englishPair;
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
                "circular",
                "rodeia",
                "rodeie",
                "rodear",
                "contorna",
                "contorne",
                "contornar"
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
                        "guarde",
                        "proteção",
                        "protecao",
                        "escudo",
                        "cubrame",
                        "cubrir"
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
                        "cercando",
                        "costas",
                        "inteiro",
                        "completo",
                        "completamente"
                );

        boolean protectMe =
                protective
                        && (
                        around
                                || containsAny(
                                words,
                                "me",
                                "mim"
                        )
                );

        return protectMe
                || containsSequence(
                words,
                "em",
                "volta"
        )
                || containsSequence(
                words,
                "ao",
                "redor"
        )
                || containsSequence(
                words,
                "todos",
                "lados"
        )
                || containsSequence(
                words,
                "minhas",
                "costas"
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
                "arremessar",
                "manda",
                "mande",
                "envia",
                "envie",
                "vai"
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
                "finalize",
                "chega",
                "dispensa",
                "dispensado"
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
        )
                || containsSequence(
                words,
                "por",
                "aqui"
        )
                || containsSequence(
                words,
                "ja",
                "chega"
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

        boolean energyLike =
                containsAny(
                        words,
                        "energia",
                        "energetica",
                        "potencia",
                        "poder",
                        "forca",
                        "carga"
                );

        boolean hasControlWord =
                output >= 0
                        || containsSequence(
                        words,
                        "out",
                        "put"
                )
                        || energyLike;

        if (!hasControlWord) {
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
                "max",
                "total",
                "full"
        )
                || containsSequence(
                words,
                "cem",
                "porcento"
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
                "min",
                "zero"
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
                    || word.equals(
                    "vermelho"
            )
                    || word.equals(
                    "vermelha"
            )
                    || word.equals(
                    "vermeio"
            )
                    || (
                    word.length() >= 7
                            && distance(
                            word,
                            "vermelho"
                    ) <= 1
            )) {

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

    private static int indexOfAny(
            List<String> words,
            String... candidates
    ) {
        for (int index = 0;
             index < words.size();
             index++) {

            String word =
                    words.get(
                            index
                    );

            for (String candidate :
                    candidates) {

                if (word.equals(
                        candidate
                )) {
                    return index;
                }
            }
        }

        return -1;
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

    private static void showInputGuidanceOnce() {
        if (inputGuidanceShown) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        inputGuidanceShown = true;

        minecraft.player.displayClientMessage(
                Component.literal(
                        "[Way Around] Precisão (regras, números, frases exatas): prefira o chat. "
                                + "RP e comandos naturais: Voice Chat funciona melhor. "
                                + "O chat também usa o mesmo interpretador de intenções."
                ).withStyle(
                        ChatFormatting.AQUA
                ),
                false
        );
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

        pendingUrgency =
                0.0F;
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
        String repaired =
                repairCommonMojibake(
                        text
                );

        String decomposed =
                Normalizer.normalize(
                        repaired,
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

    private static String repairCommonMojibake(
            String text
    ) {
        if (text == null
                || (
                text.indexOf('Ã') < 0
                        && text.indexOf('Â') < 0
        )) {

            return text;
        }

        try {
            String repaired =
                    new String(
                            text.getBytes(
                                    StandardCharsets.ISO_8859_1
                            ),
                            StandardCharsets.UTF_8
                    );

            return repaired.indexOf(
                    '\uFFFD'
            ) >= 0
                    ? text
                    : repaired;

        } catch (Exception ignored) {
            return text;
        }
    }

    private record InfinityEvidence(
            float evidence,
            boolean off,
            boolean maxOn,
            String reason
    ) {
        private static final InfinityEvidence NONE =
                new InfinityEvidence(
                        0.0F,
                        false,
                        false,
                        "none"
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
    private static boolean hasLocalTukunaSpectrum() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return false;
        }

        return SpectrumAccess.has(minecraft.player, SpectrumType.TUKUNA);
    }

    private static boolean hasLocalJusticeSpectrum() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return false;
        }

        return SpectrumAccess.has(minecraft.player, SpectrumType.JUSTICE);
    }

}

