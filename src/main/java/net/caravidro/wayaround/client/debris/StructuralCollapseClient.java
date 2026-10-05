package net.caravidro.wayaround.client.debris;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import java.util.Random;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.interaction.RigidFallMotion;
import net.caravidro.wayaround.network.StructuralCollapseS2CPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Renders server-authored structural debris as rigid block groups.
 *
 * <p>No Entity is created. There is no collision shape, shadow entity,
 * pathfinding identity, network tick or per-block physics query while a
 * building is falling.</p>
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class StructuralCollapseClient {

    private static final double RENDER_DISTANCE_SQR =
            220.0
                    * 220.0;

    private static final int MAX_ACTIVE_BLOCKS =
            StructuralCollapseS2CPayload.MAX_BLOCKS
                    * 2;

    private static final List<ClientCluster>
            ACTIVE =
            new ArrayList<>();

    private StructuralCollapseClient() {
    }

    public static void receive(
            StructuralCollapseS2CPayload payload
    ) {
        if (payload == null
                || !payload.isSane()) {
            return;
        }

        int activeBlocks =
                ACTIVE.stream()
                        .mapToInt(
                                cluster ->
                                        cluster.positions.length
                        )
                        .sum();

        // All calving groups share one transform. Cull completely enclosed opaque ice once,
        // across group boundaries, rather than paying a block-model/light query every frame.
        Set<Long> opaqueIce = new HashSet<>();
        for (var encoded : payload.clusters()) if (encoded.calving())
            for (int i = 0; i < encoded.blockCount(); i++)
                if (Block.stateById(encoded.stateIds()[i]).canOcclude()) opaqueIce.add(encoded.positions()[i]);

        for (StructuralCollapseS2CPayload.Cluster encoded :
                payload.clusters()) {
            if (encoded.blockCount() == 0
                    || activeBlocks
                    >= MAX_ACTIVE_BLOCKS) {
                break;
            }

            int count =
                    Math.min(
                            encoded.blockCount(),
                            MAX_ACTIVE_BLOCKS
                                    - activeBlocks
                    );

            BlockPos[] positions =
                    new BlockPos[
                            count
                            ];

            BlockState[] states =
                    new BlockState[
                            count
                            ];

            int valid =
                    0;

            for (int i = 0;
                 i < count;
                 i++) {
                BlockState state =
                        Block.stateById(
                                encoded.stateIds()[i]
                        );

                if (state == null
                        || state.isAir()) {
                    continue;
                }

                BlockPos position = BlockPos.of(encoded.positions()[i]);
                if (encoded.calving() && state.canOcclude()) {
                    boolean enclosed = true;
                    for (Direction side : Direction.values())
                        if (!opaqueIce.contains(position.relative(side).asLong())) { enclosed = false; break; }
                    if (enclosed) continue;
                }

                positions[valid] =
                        BlockPos.of(
                                encoded.positions()[i]
                        );

                states[valid] =
                        state;

                valid++;
            }

            if (valid == 0) {
                continue;
            }

            if (valid != positions.length) {
                positions =
                        java.util.Arrays.copyOf(
                                positions,
                                valid
                        );

                states =
                        java.util.Arrays.copyOf(
                                states,
                                valid
                        );
            }

            ClientCluster cluster =
                    new ClientCluster(
                            payload.eventId(),
                            positions,
                            states,
                            encoded.fallDistance(),
                            encoded.fallTicks(),
                            encoded.seed(), encoded.driftX(), encoded.driftZ(), encoded.calving()
                    );

            ACTIVE.add(
                    cluster
            );

            activeBlocks +=
                    valid;

            if (!cluster.calving) emitFractureDust(cluster);
        }
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            ACTIVE.clear();
            return;
        }

        Iterator<ClientCluster> iterator =
                ACTIVE.iterator();

        while (iterator.hasNext()) {
            ClientCluster cluster =
                    iterator.next();

            cluster.age++;

            if (!cluster.calving && cluster.age <= 12
                    && (
                    cluster.age
                            & 1
            ) == 0) {
                emitRisingDust(
                        cluster
                );
            }

            if (!cluster.impactEmitted
                    && cluster.age
                    >= cluster.fallTicks) {
                cluster.impactEmitted =
                        true;

                if (!cluster.calving) emitImpactDust(cluster);
            }

            if (cluster.age
                    > cluster.fallTicks + 2) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void render(
            RenderLevelStageEvent event
    ) {
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || ACTIVE.isEmpty()) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        Vec3 camera =
                event.getCamera()
                        .getPosition();

        float partialTick =
                event.getPartialTick()
                        .getGameTimeDeltaPartialTick(
                                true
                        );

        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers()
                        .bufferSource();

        int rendered =
                0;

        for (ClientCluster cluster :
                ACTIVE) {
            if (cluster.center.distanceToSqr(
                    camera
            )
                    > RENDER_DISTANCE_SQR) {
                continue;
            }

            double drop = cluster.drop(partialTick);
            double drift = RigidFallMotion.drift(drop, cluster.fallDistance);
            if (event.getFrustum() != null && !event.getFrustum().isVisible(
                    cluster.bounds.move(cluster.driftX * drift, -drop, cluster.driftZ * drift))) continue;
            int packedLight = LevelRenderer.getLightColor(minecraft.level,
                    BlockPos.containing(cluster.center.x + cluster.driftX * drift,
                            cluster.center.y - drop, cluster.center.z + cluster.driftZ * drift));

            int stride =
                    rendered > 2600
                            ? 2
                            : 1;

            for (int i = 0;
                 i < cluster.positions.length;
                 i += stride) {
                BlockPos pos =
                        cluster.positions[i];

                BlockState state =
                        cluster.states[i];

                double worldY =
                        pos.getY()
                                - drop;

                double worldX = pos.getX() + cluster.driftX * drift;
                double worldZ = pos.getZ() + cluster.driftZ * drift;
                double dx=worldX+.5-camera.x,dy=worldY+.5-camera.y,dz=worldZ+.5-camera.z;
                if(dx*dx+dy*dy+dz*dz>RENDER_DISTANCE_SQR)continue;

                event.getPoseStack()
                        .pushPose();

                event.getPoseStack()
                        .translate(
                                worldX - camera.x,
                                worldY
                                        - camera.y,
                                worldZ - camera.z
                        );

                minecraft.getBlockRenderer()
                        .renderSingleBlock(
                                state,
                                event.getPoseStack(),
                                buffers,
                                packedLight,
                                OverlayTexture.NO_OVERLAY
                        );

                event.getPoseStack()
                        .popPose();

                rendered++;
            }
        }

        if (rendered > 0) {
            buffers.endBatch();
        }
    }

    private static void emitFractureDust(
            ClientCluster cluster
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        int count =
                Mth.clamp(
                        cluster.positions.length
                                / 3,
                        8,
                        34
                );

        for (int i = 0;
             i < count;
             i++) {
            int index =
                    cluster.random.nextInt(
                            cluster.positions.length
                    );

            BlockPos pos =
                    cluster.positions[index];

            BlockState state =
                    cluster.states[index];

            minecraft.level.addParticle(
                    new BlockParticleOption(
                            ParticleTypes.BLOCK,
                            state
                    ),
                    pos.getX()
                            + cluster.random.nextDouble(),
                    pos.getY()
                            + cluster.random.nextDouble(),
                    pos.getZ()
                            + cluster.random.nextDouble(),
                    (
                            cluster.random.nextDouble()
                                    - 0.5
                    )
                            * 0.12,
                    0.05
                            + cluster.random.nextDouble()
                                    * 0.10,
                    (
                            cluster.random.nextDouble()
                                    - 0.5
                    )
                            * 0.12
            );
        }
    }

    private static void emitRisingDust(
            ClientCluster cluster
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || cluster.positions.length == 0) {
            return;
        }

        int emissions =
                Mth.clamp(
                        cluster.positions.length
                                / 40,
                        1,
                        5
                );

        double drop =
                cluster.drop(
                        0.0F
                );

        for (int i = 0;
             i < emissions;
             i++) {
            BlockPos pos =
                    cluster.positions[
                            cluster.random.nextInt(
                                    cluster.positions.length
                            )
                            ];

            minecraft.level.addParticle(
                    cluster.random.nextInt(
                            4
                    ) == 0
                            ? ParticleTypes.CAMPFIRE_COSY_SMOKE
                            : ParticleTypes.POOF,
                    pos.getX() + cluster.driftX * RigidFallMotion.drift(drop, cluster.fallDistance)
                            + cluster.random.nextDouble(),
                    pos.getY()
                            - drop
                            + 0.35,
                    pos.getZ() + cluster.driftZ * RigidFallMotion.drift(drop, cluster.fallDistance)
                            + cluster.random.nextDouble(),
                    (
                            cluster.random.nextDouble()
                                    - 0.5
                    )
                            * 0.04,
                    0.07
                            + cluster.random.nextDouble()
                                    * 0.09,
                    (
                            cluster.random.nextDouble()
                                    - 0.5
                    )
                            * 0.04
            );
        }
    }

    private static void emitImpactDust(
            ClientCluster cluster
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || cluster.positions.length == 0) {
            return;
        }

        int count =
                Mth.clamp(
                        18
                                + cluster.positions.length
                                        / 2,
                        20,
                        90
                );

        for (int i = 0;
             i < count;
             i++) {
            BlockPos pos =
                    cluster.positions[
                            cluster.random.nextInt(
                                    cluster.positions.length
                            )
                            ];

            double angle =
                    cluster.random.nextDouble()
                            * Math.PI
                            * 2.0;

            double speed =
                    0.04
                            + cluster.random.nextDouble()
                                    * 0.18;

            minecraft.level.addParticle(
                    cluster.calving ? ParticleTypes.SNOWFLAKE : i % 5 == 0
                            ? ParticleTypes.CAMPFIRE_COSY_SMOKE
                            : ParticleTypes.POOF,
                    pos.getX() + cluster.driftX + 0.5,
                    pos.getY()
                            - cluster.fallDistance
                            + 0.15,
                    pos.getZ() + cluster.driftZ + 0.5,
                    Math.cos(
                            angle
                    )
                            * speed,
                    0.08
                            + cluster.random.nextDouble()
                                    * 0.24,
                    Math.sin(
                            angle
                    )
                            * speed
            );
        }
    }

    private static final class ClientCluster {
        private final UUID eventId;
        private final BlockPos[] positions;
        private final BlockState[] states;
        private final int fallDistance;
        private final int fallTicks;
        private final long seed;
        private final int driftX, driftZ;
        private final boolean calving;
        private final Vec3 center;
        private final AABB bounds;
        private final Random random;

        private int age;
        private boolean impactEmitted;

        private ClientCluster(
                UUID eventId,
                BlockPos[] positions,
                BlockState[] states,
                int fallDistance,
                int fallTicks,
                long seed, int driftX, int driftZ, boolean calving
        ) {
            this.eventId = eventId;
            this.positions = positions;
            this.states = states;
            this.fallDistance = fallDistance;
            this.fallTicks = Math.max(
                    1,
                    fallTicks
            );
            this.seed = seed;
            this.driftX = driftX; this.driftZ = driftZ; this.calving = calving;
            this.center = computeCenter(
                    positions
            );
            AABB bounds = new AABB(positions[0]);
            for (BlockPos pos : positions) bounds = bounds.minmax(new AABB(pos));
            this.bounds = bounds;
            this.random = new Random(
                    seed
            );
        }

        private double drop(
                float partialTick
        ) {
            if (calving) return RigidFallMotion.calvingDrop(age + partialTick, fallDistance);
            double t =
                    Mth.clamp(
                            (
                                    age
                                            + partialTick
                            )
                                    / (
                                    double
                            ) fallTicks,
                            0.0,
                            1.0
                    );

            // Accelerating rigid fall: one transform for the whole section.
            double eased =
                    t
                            * t;

            return fallDistance
                    * eased;
        }

        private static Vec3 computeCenter(
                BlockPos[] positions
        ) {
            double x =
                    0.0;

            double y =
                    0.0;

            double z =
                    0.0;

            for (BlockPos pos :
                    positions) {
                x +=
                        pos.getX()
                                + 0.5;

                y +=
                        pos.getY()
                                + 0.5;

                z +=
                        pos.getZ()
                                + 0.5;
            }

            double divisor =
                    Math.max(
                            1,
                            positions.length
                    );

            return new Vec3(
                    x / divisor,
                    y / divisor,
                    z / divisor
            );
        }
    }
}
