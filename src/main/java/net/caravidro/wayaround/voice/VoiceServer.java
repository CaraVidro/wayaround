package net.caravidro.wayaround.voice;

import net.caravidro.wayaround.performance.PerformanceProfiler;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.caravidro.wayaround.cursed.TukunaManager;
import net.caravidro.wayaround.media.blackbox.BlackBoxManager;
import net.caravidro.wayaround.media.broadcast.BroadcastManager;
import net.caravidro.wayaround.network.VoiceFrameS2CPayload;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class VoiceServer {

    private VoiceServer() {
    }

    private static final int MAX_PACKETS_PER_SECOND = 40;

    private static final Map<UUID, RateState> RATE_LIMIT =
            new ConcurrentHashMap<>();

    public static void relay(
            ServerPlayer sender,
            byte[] pcm
    ) {
        long wayperfStartedAt =
                PerformanceProfiler.begin(
                        PerformanceProfiler.Section.VOICE
                );

        try {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.VOICE_CHAT
        )) {
            return;
        }

        if (!sender.isAlive()) {
            RATE_LIMIT.remove(
                    sender.getUUID()
            );
            return;
        }

        if (pcm == null
                || pcm.length == 0
                || pcm.length > VoiceConstants.MAX_PACKET_BYTES) {
            return;
        }

        /*
         * During possession the host is literally only an observer. Their mic
         * is server-muted too, not merely hidden in the UI.
         */
        if (TukunaManager.isSilencedHost(
                sender
        )) {
            return;
        }

        if (!allowPacket(sender.getUUID())) {
            return;
        }

        if (containsSpeech(
                pcm
        )) {
            TukunaManager.pulseProjectedSpeech(
                    sender
            );
        }

        ServerPlayer projectionHost =
                TukunaManager.projectedVoiceHost(
                        sender
                );

        if (projectionHost != null) {
            BlackBoxManager.captureVoice(
                    projectionHost.serverLevel(),
                    projectionHost.position(),
                    sender.getUUID(),
                    sender.getGameProfile()
                            .getName(),
                    pcm
            );
        } else {
            BlackBoxManager.captureVoice(
                    sender.serverLevel(),
                    sender.position(),
                    sender.getUUID(),
                    sender.getGameProfile()
                            .getName(),
                    pcm
            );
        }

        BroadcastManager.captureVoice(
                sender,
                pcm
        );

        if (projectionHost != null) {
            relayProjectedGhost(
                    sender,
                    projectionHost,
                    pcm
            );
            return;
        }

        relayAround(
                sender,
                sender,
                pcm,
                false
        );
    
        } finally {
            PerformanceProfiler.end(
                    PerformanceProfiler.Section.VOICE,
                    wayperfStartedAt
            );
        }
    }

    private static void relayProjectedGhost(
            ServerPlayer ghost,
            ServerPlayer host,
            byte[] pcm
    ) {
        /*
         * A disembodied Tukuna can be on the other side of the world (or in
         * another dimension). Their voice is emitted around the host instead.
         * Never return microphone audio to its sender: it can feed back into capture.
         */
        double maxDistanceSqr =
                VoiceConstants.HEARING_RANGE_BLOCKS
                        * VoiceConstants.HEARING_RANGE_BLOCKS;

        VoiceFrameS2CPayload frame =
                new VoiceFrameS2CPayload(
                        pcm
                );

        for (ServerPlayer receiver :
                host.serverLevel()
                        .players()) {

            if (receiver == ghost
                    || !receiver.isAlive()
                    || receiver.distanceToSqr(
                    host
            ) > maxDistanceSqr) {
                continue;
            }

            PacketDistributor.sendToPlayer(
                    receiver,
                    frame
            );
        }
    }

    private static void relayAround(
            ServerPlayer sender,
            ServerPlayer anchor,
            byte[] pcm,
            boolean echoSender
    ) {
        double maxDistanceSqr =
                VoiceConstants.HEARING_RANGE_BLOCKS
                        * VoiceConstants.HEARING_RANGE_BLOCKS;

        VoiceFrameS2CPayload frame =
                new VoiceFrameS2CPayload(
                        pcm
                );

        for (ServerPlayer receiver :
                anchor.serverLevel()
                        .players()) {

            if (!receiver.isAlive()) {
                continue;
            }

            if (!echoSender
                    && receiver == sender) {
                continue;
            }

            if (receiver.distanceToSqr(
                    anchor
            ) > maxDistanceSqr) {
                continue;
            }

            PacketDistributor.sendToPlayer(
                    receiver,
                    frame
            );
        }
    }

    private static boolean containsSpeech(
            byte[] pcm
    ) {
        if (pcm.length < 2) {
            return false;
        }

        long squareSum =
                0L;

        int samples =
                pcm.length / 2;

        for (int index = 0;
             index < samples;
             index++) {
            int byteIndex =
                    index * 2;

            int sample =
                    (short) (
                            (pcm[byteIndex]
                                    & 0xFF)
                                    | (pcm[byteIndex + 1]
                                    << 8)
                    );

            squareSum +=
                    (long) sample
                            * sample;
        }

        /*
         * Compare mean-square energy directly. sqrt() was being paid for on
         * every voice frame only to compare against a constant RMS threshold.
         */
        double sampleThreshold =
                0.0065
                        * 32768.0;

        double requiredSquareSum =
                samples
                        * sampleThreshold
                        * sampleThreshold;

        /*
         * Lower than the client's voice-activation threshold on purpose: once
         * an utterance is already being transmitted, quiet syllables should
         * still move Tukuna's cheek mouth. Mic hiss / held PTT silence should
         * not.
         */
        return squareSum
                >= requiredSquareSum;
    }

    private static boolean allowPacket(UUID playerId) {
        long second =
                System.currentTimeMillis()
                        / 1000L;

        RateState state =
                RATE_LIMIT.computeIfAbsent(
                        playerId,
                        ignored ->
                                new RateState()
                );

        synchronized (state) {
            if (state.second != second) {
                state.second =
                        second;

                state.count =
                        0;
            }

            state.count++;

            return state.count
                    <= MAX_PACKETS_PER_SECOND;
        }
    }

    private static final class RateState {
        private long second = -1;
        private int count;
    }
}
