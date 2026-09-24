package net.caravidro.wayaround.voice;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.caravidro.wayaround.network.VoiceFrameS2CPayload;
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
        if (pcm == null
                || pcm.length == 0
                || pcm.length > VoiceConstants.MAX_PACKET_BYTES) {
            return;
        }

        if (!allowPacket(sender.getUUID())) {
            return;
        }

        double maxDistanceSqr =
                VoiceConstants.HEARING_RANGE_BLOCKS
                        * VoiceConstants.HEARING_RANGE_BLOCKS;

        for (ServerPlayer receiver
                : sender.serverLevel().players()) {

            if (receiver == sender) {
                continue;
            }

            if (receiver.distanceToSqr(sender)
                    > maxDistanceSqr) {
                continue;
            }

            PacketDistributor.sendToPlayer(
                    receiver,
                    new VoiceFrameS2CPayload(pcm)
            );
        }
    }

    private static boolean allowPacket(UUID playerId) {
        long second = System.currentTimeMillis() / 1000L;

        RateState state =
                RATE_LIMIT.computeIfAbsent(
                        playerId,
                        ignored -> new RateState()
                );

        synchronized (state) {
            if (state.second != second) {
                state.second = second;
                state.count = 0;
            }

            state.count++;

            return state.count
                    <= MAX_PACKETS_PER_SECOND;
        }
    }

    private static final class RateState {
        private long second = -1;
        private int count = 0;
    }
}
