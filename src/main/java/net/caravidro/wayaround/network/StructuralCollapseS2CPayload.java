package net.caravidro.wayaround.network;

import java.util.List;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * One server-authored structural collapse snapshot.
 *
 * <p>The server owns structural analysis, removal and final settlement.
 * Clients only animate rigid groups of the original block states. This keeps
 * collision, shadows and per-fragment physics out of the hot path.</p>
 */
public record StructuralCollapseS2CPayload(
        UUID eventId,
        List<Cluster> clusters
) implements CustomPacketPayload {

    public static final int MAX_CLUSTERS = 64;
    public static final int MAX_BLOCKS = 4096;

    public record Cluster(
            int fallDistance,
            int fallTicks,
            long seed,
            long[] positions,
            int[] stateIds,
            int driftX, int driftZ, boolean calving
    ) {
        public Cluster(int fallDistance, int fallTicks, long seed, long[] positions, int[] stateIds) {
            this(fallDistance, fallTicks, seed, positions, stateIds, 0, 0, false);
        }

        public Cluster {
            if (positions == null
                    || stateIds == null
                    || positions.length != stateIds.length) {
                throw new IllegalArgumentException(
                        "Structural collapse cluster arrays must match."
                );
            }

            positions = positions.clone();
            stateIds = stateIds.clone();
            fallDistance = Math.max(0, fallDistance);
            fallTicks = Math.max(1, fallTicks);
        }

        public int blockCount() {
            return positions.length;
        }
    }

    public StructuralCollapseS2CPayload {
        eventId = eventId == null
                ? new UUID(0L, 0L)
                : eventId;

        clusters = clusters == null
                ? List.of()
                : List.copyOf(clusters);
    }

    public static final Type<StructuralCollapseS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "structural_collapse"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            StructuralCollapseS2CPayload
    > STREAM_CODEC =
            StreamCodec.of(
                    StructuralCollapseS2CPayload::encode,
                    StructuralCollapseS2CPayload::decode
            );

    private static void encode(
            RegistryFriendlyByteBuf buffer,
            StructuralCollapseS2CPayload payload
    ) {
        buffer.writeUUID(
                payload.eventId()
        );

        int clusterCount =
                Math.min(
                        MAX_CLUSTERS,
                        payload.clusters().size()
                );

        buffer.writeVarInt(
                clusterCount
        );

        int writtenBlocks =
                0;

        for (int i = 0;
             i < clusterCount;
             i++) {
            Cluster cluster =
                    payload.clusters().get(i);

            int remaining =
                    MAX_BLOCKS
                            - writtenBlocks;

            int blockCount =
                    Math.min(
                            remaining,
                            cluster.blockCount()
                    );

            buffer.writeVarInt(
                    cluster.fallDistance()
            );

            buffer.writeVarInt(
                    cluster.fallTicks()
            );

            buffer.writeLong(cluster.seed());
            buffer.writeVarInt(cluster.driftX());
            buffer.writeVarInt(cluster.driftZ());
            buffer.writeBoolean(cluster.calving());

            buffer.writeVarInt(
                    blockCount
            );

            for (int block = 0;
                 block < blockCount;
                 block++) {
                buffer.writeLong(
                        cluster.positions()[block]
                );

                buffer.writeVarInt(
                        cluster.stateIds()[block]
                );
            }

            writtenBlocks +=
                    blockCount;

            if (writtenBlocks
                    >= MAX_BLOCKS) {
                break;
            }
        }
    }

    private static StructuralCollapseS2CPayload decode(
            RegistryFriendlyByteBuf buffer
    ) {
        UUID eventId =
                buffer.readUUID();

        int clusterCount =
                buffer.readVarInt();

        if (clusterCount < 0
                || clusterCount > MAX_CLUSTERS) {
            throw new IllegalArgumentException(
                    "Invalid structural collapse cluster count: "
                            + clusterCount
            );
        }

        java.util.ArrayList<Cluster> clusters =
                new java.util.ArrayList<>(
                        clusterCount
                );

        int totalBlocks =
                0;

        for (int i = 0;
             i < clusterCount;
             i++) {
            int fallDistance =
                    buffer.readVarInt();

            int fallTicks =
                    buffer.readVarInt();

            long seed =
                    buffer.readLong();

            int driftX = buffer.readVarInt();
            int driftZ = buffer.readVarInt();
            boolean calving = buffer.readBoolean();
            int blockCount = buffer.readVarInt();

            if (blockCount < 0
                    || totalBlocks + blockCount
                    > MAX_BLOCKS) {
                throw new IllegalArgumentException(
                        "Invalid structural collapse block count."
                );
            }

            long[] positions =
                    new long[
                            blockCount
                            ];

            int[] stateIds =
                    new int[
                            blockCount
                            ];

            for (int block = 0;
                 block < blockCount;
                 block++) {
                positions[block] =
                        buffer.readLong();

                stateIds[block] =
                        buffer.readVarInt();
            }

            totalBlocks +=
                    blockCount;

            clusters.add(
                    new Cluster(
                            fallDistance,
                            fallTicks,
                            seed,
                            positions,
                            stateIds, driftX, driftZ, calving
                    )
            );
        }

        return new StructuralCollapseS2CPayload(
                eventId,
                clusters
        );
    }

    public boolean isSane() {
        if (clusters.size()
                > MAX_CLUSTERS) {
            return false;
        }

        int blocks =
                0;

        for (Cluster cluster :
                clusters) {
            blocks +=
                    cluster.blockCount();

            if (blocks > MAX_BLOCKS
                    || cluster.fallTicks() > (cluster.calving() ? 660 : 240)
                    || cluster.fallDistance() > (cluster.calving() ? 512 : 160)
                    || Math.abs((long) cluster.driftX()) > 24
                    || Math.abs((long) cluster.driftZ()) > 24) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            StructuralCollapseS2CPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleStructuralCollapse(
                payload,
                context
        );
    }
}
