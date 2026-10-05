package net.caravidro.wayaround.interaction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.StructuralCollapseS2CPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-authoritative structural collapse engine.
 *
 * <p>This is deliberately hierarchical. A building is scanned as a graph,
 * support nodes accumulate load, overloaded links fail, and only unsupported
 * connected regions become moving debris. The moving regions are rigid
 * snapshots, not thousands of physics entities.</p>
 */
public final class StructuralCollapseManager {

    public static final int DEFAULT_SCAN_RADIUS =
            16;

    public static final int MAX_SCAN_RADIUS =
            24;

    private static final int MAX_NODES =
            StructuralCollapseS2CPayload.MAX_BLOCKS;

    private static final int MAX_RIGID_CLUSTER =
            96;

    private static final int MAX_LOOSE_CLUSTER =
            8;

    private static final int MAX_FALL_DISTANCE =
            96;

    private static final double VISUAL_RANGE =
            192.0;

    private static final List<PendingCluster>
            PENDING =
            new ArrayList<>();

    private StructuralCollapseManager() {
    }

    public record CollapseResult(
            int scannedBlocks,
            int directlyFailedBlocks,
            int overloadedBlocks,
            int stableBlocks,
            int fallingBlocks,
            int clusters,
            int maxFallDistance
    ) {
        public boolean collapsedAnything() {
            return fallingBlocks > 0;
        }
    }

    /**
     * Public integration point for Blue, explosions, machines and debug tools.
     */
    public static CollapseResult applyDamage(
            ServerLevel level,
            StructuralDamage damage,
            int requestedScanRadius
    ) {
        int scanRadius =
                Mth.clamp(
                        requestedScanRadius,
                        6,
                        MAX_SCAN_RADIUS
                );

        BlockPos center =
                BlockPos.containing(
                        damage.origin()
                );

        Scan scan =
                scanStructure(
                        level,
                        center,
                        scanRadius
                );

        if (scan.nodes.isEmpty()) {
            return new CollapseResult(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        double damageRadius =
                Mth.clamp(
                        1.25
                                + Math.sqrt(
                                Math.max(
                                        0.0F,
                                        damage.amount()
                                )
                        )
                                * 0.42
                                + damage.impulse()
                                * 0.055,
                        1.25,
                        7.0
                );

        int directlyFailed =
                applyDirectDamage(
                        scan,
                        damage,
                        damageRadius
                );

        int overloaded =
                propagateLoadsAndFail(
                        scan
                );

        Set<Long> stable =
                findStableNodes(
                        scan
                );

        Set<Long> unsupported =
                new HashSet<>();

        Set<Long> failed =
                new HashSet<>();

        for (Node node :
                scan.nodes.values()) {
            if (node.failed) {
                failed.add(
                        node.pos.asLong()
                );
            } else if (!stable.contains(
                    node.pos.asLong()
            )) {
                unsupported.add(
                        node.pos.asLong()
                );
            }
        }

        List<List<Node>> groups =
                new ArrayList<>();

        groups.addAll(
                partition(
                        scan,
                        unsupported,
                        MAX_RIGID_CLUSTER
                )
        );

        groups.addAll(
                partition(
                        scan,
                        failed,
                        MAX_LOOSE_CLUSTER
                )
        );

        if (groups.isEmpty()) {
            return new CollapseResult(
                    scan.nodes.size(),
                    directlyFailed,
                    overloaded,
                    stable.size(),
                    0,
                    0,
                    0
            );
        }

        Set<Long> movingPositions =
                new HashSet<>();

        for (List<Node> group :
                groups) {
            for (Node node :
                    group) {
                movingPositions.add(
                        node.pos.asLong()
                );
            }
        }

        List<ClusterPlan> plans =
                new ArrayList<>();

        int fallingBlocks =
                0;

        int maxFall =
                0;

        long seedBase =
                level.getGameTime()
                        ^ center.asLong()
                        ^ Double.doubleToLongBits(
                        damage.amount()
                );

        int groupIndex =
                0;

        for (List<Node> group :
                groups) {
            if (group.isEmpty()) {
                continue;
            }

            boolean loose =
                    group.stream()
                            .allMatch(
                                    node -> node.failed
                            );

            int fallDistance =
                    computeFallDistance(
                            level,
                            group,
                            movingPositions
                    );

            if (fallDistance <= 0
                    && !loose) {
                // The region is disconnected in the structural graph but is
                // already physically resting on a broad external surface.
                // Keep it in-world instead of making it blink out.
                continue;
            }

            int fallTicks =
                    fallTicksFor(
                            fallDistance,
                            loose
                    );

            ClusterPlan plan =
                    new ClusterPlan(
                            group,
                            fallDistance,
                            fallTicks,
                            seedBase
                                    + groupIndex
                                    * 0x9E3779B97F4A7C15L,
                            !loose
                                    || fallDistance > 0
                    );

            plans.add(
                    plan
            );

            fallingBlocks +=
                    group.size();

            maxFall =
                    Math.max(
                            maxFall,
                            fallDistance
                    );

            groupIndex++;

            if (plans.size()
                    >= StructuralCollapseS2CPayload.MAX_CLUSTERS) {
                break;
            }
        }

        if (plans.isEmpty()) {
            return new CollapseResult(
                    scan.nodes.size(),
                    directlyFailed,
                    overloaded,
                    stable.size(),
                    0,
                    0,
                    0
            );
        }

        removeMovingBlocks(
                level,
                plans
        );

        List<StructuralCollapseS2CPayload.Cluster>
                payloadClusters =
                new ArrayList<>();

        for (ClusterPlan plan :
                plans) {
            long[] positions =
                    new long[
                            plan.nodes.size()
                            ];

            int[] stateIds =
                    new int[
                            plan.nodes.size()
                            ];

            for (int i = 0;
                 i < plan.nodes.size();
                 i++) {
                Node node =
                        plan.nodes.get(i);

                positions[i] =
                        node.pos.asLong();

                stateIds[i] =
                        Block.getId(
                                node.state
                        );
            }

            payloadClusters.add(
                    new StructuralCollapseS2CPayload.Cluster(
                            plan.fallDistance,
                            plan.fallTicks,
                            plan.seed,
                            positions,
                            stateIds
                    )
            );

            if (plan.settle
                    && plan.fallDistance > 0) {
                scheduleSettlement(
                        level,
                        plan
                );
            }
        }

        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.getX() + 0.5,
                center.getY() + 0.5,
                center.getZ() + 0.5,
                VISUAL_RANGE,
                new StructuralCollapseS2CPayload(
                        UUID.randomUUID(),
                        payloadClusters
                )
        );

        WayAround.LOGGER.info(
                "Structural collapse {} at {}: scanned={}, direct={}, overload={}, stable={}, falling={}, clusters={}, maxFall={}",
                damage.source(),
                center,
                scan.nodes.size(),
                directlyFailed,
                overloaded,
                stable.size(),
                fallingBlocks,
                plans.size(),
                maxFall
        );

        return new CollapseResult(
                scan.nodes.size(),
                directlyFailed,
                overloaded,
                stable.size(),
                fallingBlocks,
                plans.size(),
                maxFall
        );
    }

    /** A known detached slab reuses demolition's bounded connected partition and renderer.
     * The caller owns removal, NBT and settlement; no second authority may drop these blocks. */
    public record DetachedBlock(BlockPos pos, BlockState state) {}

    public static StructuralCollapseS2CPayload calvingVisuals(List<DetachedBlock> blocks,
            int fallDistance, int driftX, int driftZ, long seed) {
        if (blocks.size() > MAX_NODES || fallDistance < 0 || fallDistance > 512
                || Math.abs((long) driftX) > 24 || Math.abs((long) driftZ) > 24)
            throw new IllegalArgumentException("Unbounded calving snapshot");
        Map<Long, Node> nodes = new java.util.LinkedHashMap<>();
        for (DetachedBlock block : blocks) nodes.put(block.pos.asLong(),
                new Node(block.pos.immutable(), block.state, 1, 1, false));
        List<List<Node>> connected = partition(new Scan(nodes, 0), nodes.keySet(), MAX_RIGID_CLUSTER);
        // Pack short disconnected pieces together; don't silently lose isolated blocks at the cap.
        List<Node> ordered = new ArrayList<>(nodes.size());
        Set<Long> seen = new HashSet<>();
        for (List<Node> group : connected) for (Node node : group) {
            ordered.add(node); seen.add(node.pos.asLong());
        }
        for (Node node : nodes.values()) if (seen.add(node.pos.asLong())) ordered.add(node);
        List<StructuralCollapseS2CPayload.Cluster> clusters = new ArrayList<>();
        for (int start = 0; start < ordered.size(); start += MAX_RIGID_CLUSTER) {
            int size = Math.min(MAX_RIGID_CLUSTER, ordered.size() - start);
            long[] positions = new long[size]; int[] states = new int[size];
            for (int i = 0; i < size; i++) {
                Node node = ordered.get(start + i);
                positions[i] = node.pos.asLong(); states[i] = Block.getId(node.state);
            }
            clusters.add(new StructuralCollapseS2CPayload.Cluster(fallDistance,
                    RigidFallMotion.calvingTicks(fallDistance), seed + start, positions, states,
                    driftX, driftZ, true));
        }
        return new StructuralCollapseS2CPayload(UUID.randomUUID(), clusters);
    }

    public static void sendCalvingVisuals(ServerLevel level, BlockPos center,
            List<DetachedBlock> blocks, int fallDistance, int driftX, int driftZ) {
        PacketDistributor.sendToPlayersNear(level, null, center.getX() + .5, center.getY() + .5,
                center.getZ() + .5, VISUAL_RANGE,
                calvingVisuals(blocks, fallDistance, driftX, driftZ, level.getGameTime() ^ center.asLong()));
    }

    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        if (PENDING.isEmpty()) {
            return;
        }

        MinecraftServer server =
                event.getServer();

        long now =
                server.overworld()
                        .getGameTime();

        Iterator<PendingCluster> iterator =
                PENDING.iterator();

        while (iterator.hasNext()) {
            PendingCluster pending =
                    iterator.next();

            if (now < pending.settleAt) {
                continue;
            }

            ServerLevel level =
                    server.getLevel(
                            pending.dimension
                    );

            if (level == null) {
                iterator.remove();
                continue;
            }

            if (!allChunksReady(
                    level,
                    pending
            )) {
                if (now >= pending.expireAt) {
                    iterator.remove();
                } else {
                    pending.settleAt =
                            now + 20L;
                }

                continue;
            }

            settle(
                    level,
                    pending
            );

            iterator.remove();
        }
    }

    public static void clearAll() {
        PENDING.clear();
    }

    private static Scan scanStructure(
            ServerLevel level,
            BlockPos center,
            int radius
    ) {
        int minX =
                center.getX()
                        - radius;

        int maxX =
                center.getX()
                        + radius;

        int minY =
                Math.max(
                        level.getMinBuildHeight(),
                        center.getY()
                                - radius
                );

        int maxY =
                Math.min(
                        level.getMaxBuildHeight()
                                - 1,
                        center.getY()
                                + radius
                );

        int minZ =
                center.getZ()
                        - radius;

        int maxZ =
                center.getZ()
                        + radius;

        BlockPos seed =
                findSeed(
                        level,
                        center,
                        minX,
                        maxX,
                        minY,
                        maxY,
                        minZ,
                        maxZ
                );

        if (seed == null) {
            return new Scan(
                    Map.of(),
                    minY
            );
        }

        Map<Long, Node> nodes =
                new HashMap<>();

        Set<Long> visited =
                new HashSet<>();

        Deque<BlockPos> queue =
                new ArrayDeque<>();

        queue.add(
                seed
        );

        while (!queue.isEmpty()
                && nodes.size() < MAX_NODES) {
            BlockPos pos =
                    queue.removeFirst();

            if (!inside(
                    pos,
                    minX,
                    maxX,
                    minY,
                    maxY,
                    minZ,
                    maxZ
            )) {
                continue;
            }

            long packed =
                    pos.asLong();

            if (!visited.add(
                    packed
            )) {
                continue;
            }

            if (!level.hasChunkAt(
                    pos
            )) {
                continue;
            }

            BlockState state =
                    level.getBlockState(
                            pos
                    );

            if (!isStructuralCandidate(
                    level,
                    pos,
                    state
            )) {
                continue;
            }

            boolean protectedGround =
                    pos.getY()
                            <= minY + 1;

            Node node =
                    new Node(
                            pos.immutable(),
                            state,
                            materialMass(
                                    level,
                                    pos,
                                    state
                            ),
                            materialStrength(
                                    level,
                                    pos,
                                    state
                            ),
                            protectedGround
                    );

            nodes.put(
                    packed,
                    node
            );

            if (protectedGround) {
                // Terminal foundation layer. It can anchor whatever reached
                // it, but it does not flood-fill sideways through terrain.
                continue;
            }

            for (Direction direction :
                    Direction.values()) {
                BlockPos next =
                        pos.relative(
                                direction
                        );

                if (inside(
                        next,
                        minX,
                        maxX,
                        minY,
                        maxY,
                        minZ,
                        maxZ
                )) {
                    queue.addLast(
                            next
                    );
                }
            }
        }

        Scan scan =
                new Scan(
                        nodes,
                        minY
                );

        for (Node node :
                nodes.values()) {
            BlockPos below =
                    node.pos.below();

            node.anchor =
                    node.protectedGround
                            || (
                            !nodes.containsKey(
                                    below.asLong()
                            )
                                    && isStrongExternalSupport(
                                    level,
                                    below
                            )
                    );
        }

        return scan;
    }

    private static BlockPos findSeed(
            ServerLevel level,
            BlockPos center,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ
    ) {
        if (inside(
                center,
                minX,
                maxX,
                minY,
                maxY,
                minZ,
                maxZ
        )
                && isStructuralCandidate(
                level,
                center,
                level.getBlockState(
                        center
                )
        )) {
            return center.immutable();
        }

        BlockPos.MutableBlockPos cursor =
                new BlockPos.MutableBlockPos();

        for (int radius = 1;
             radius <= 3;
             radius++) {
            for (int dy = -radius;
                 dy <= radius;
                 dy++) {
                for (int dx = -radius;
                     dx <= radius;
                     dx++) {
                    for (int dz = -radius;
                         dz <= radius;
                         dz++) {
                        if (Math.max(
                                Math.abs(
                                        dx
                                ),
                                Math.max(
                                        Math.abs(
                                                dy
                                        ),
                                        Math.abs(
                                                dz
                                        )
                                )
                        ) != radius) {
                            continue;
                        }

                        cursor.set(
                                center.getX() + dx,
                                center.getY() + dy,
                                center.getZ() + dz
                        );

                        if (!inside(
                                cursor,
                                minX,
                                maxX,
                                minY,
                                maxY,
                                minZ,
                                maxZ
                        )
                                || !level.hasChunkAt(
                                cursor
                        )) {
                            continue;
                        }

                        BlockState state =
                                level.getBlockState(
                                        cursor
                                );

                        if (isStructuralCandidate(
                                level,
                                cursor,
                                state
                        )) {
                            return cursor.immutable();
                        }
                    }
                }
            }
        }

        return null;
    }

    private static boolean isStructuralCandidate(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        if (state.isAir()
                || state.is(
                BlockTags.LEAVES
        )
                || state.is(
                Blocks.BEDROCK
        )
                || state.is(
                Blocks.BARRIER
        )
                || state.is(
                Blocks.END_PORTAL_FRAME
        )
                || state.is(
                Blocks.STRUCTURE_BLOCK
        )
                || state.is(
                Blocks.JIGSAW
        )
                || level.getBlockEntity(
                pos
        ) != null) {
            return false;
        }

        float destroySpeed =
                state.getDestroySpeed(
                        level,
                        pos
                );

        if (destroySpeed < 0.0F) {
            return false;
        }

        return !state.getCollisionShape(
                level,
                pos
        )
                .isEmpty();
    }

    private static boolean isStrongExternalSupport(
            ServerLevel level,
            BlockPos pos
    ) {
        if (!level.hasChunkAt(
                pos
        )) {
            return false;
        }

        BlockState state =
                level.getBlockState(
                        pos
                );

        if (state.isAir()
                || state.getCollisionShape(
                level,
                pos
        )
                .isEmpty()) {
            return false;
        }

        return state.isFaceSturdy(
                level,
                pos,
                Direction.UP
        );
    }

    private static double materialMass(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        double hardness =
                Math.max(
                        0.0F,
                        state.getDestroySpeed(
                                level,
                                pos
                        )
                );

        double resistance =
                Math.max(
                        0.0F,
                        state.getBlock()
                                .getExplosionResistance()
                );

        double mass =
                0.75
                        + Math.min(
                        5.0,
                        hardness * 0.38
                )
                        + Math.min(
                        4.0,
                        resistance * 0.025
                );

        if (state.is(
                BlockTags.PLANKS
        )) {
            mass *=
                    0.72;
        }

        if (state.is(
                BlockTags.LOGS
        )) {
            mass *=
                    0.92;
        }

        return Math.max(
                0.35,
                mass
        );
    }

    private static double materialStrength(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        double hardness =
                Math.max(
                        0.0F,
                        state.getDestroySpeed(
                                level,
                                pos
                        )
                );

        double resistance =
                Math.max(
                        0.0F,
                        state.getBlock()
                                .getExplosionResistance()
                );

        double strength =
                1.35
                        + hardness
                        * 1.55
                        + Math.sqrt(
                        resistance
                )
                        * 0.92;

        if (state.is(
                BlockTags.LOGS
        )) {
            strength *=
                    1.28;
        }

        if (state.is(
                BlockTags.PLANKS
        )) {
            strength *=
                    0.88;
        }

        String path =
                net.minecraft.core.registries.BuiltInRegistries.BLOCK
                        .getKey(
                                state.getBlock()
                        )
                        .getPath();

        if (path.contains(
                "glass"
        )) {
            strength *=
                    0.32;
        }

        if (path.contains(
                "iron"
        )
                || path.contains(
                "steel"
        )) {
            strength *=
                    1.30;
        }

        return Math.max(
                0.5,
                strength
        );
    }

    private static int applyDirectDamage(
            Scan scan,
            StructuralDamage damage,
            double radius
    ) {
        int failed =
                0;

        double radiusSqr =
                radius
                        * radius;

        for (Node node :
                scan.nodes.values()) {
            if (node.protectedGround) {
                continue;
            }

            double distanceSqr =
                    Vec3.atCenterOf(
                            node.pos
                    )
                            .distanceToSqr(
                                    damage.origin()
                            );

            if (distanceSqr > radiusSqr) {
                continue;
            }

            double distance =
                    Math.sqrt(
                            distanceSqr
                    );

            double localForce =
                    damage.amount()
                            / (
                            1.0
                                    + distanceSqr
                                    * 0.80
                    )
                            + damage.impulse()
                            / (
                            1.0
                                    + distance
                                    * 1.45
                    );

            double threshold =
                    node.strength
                            * (
                            0.92
                                    + distance
                                    * 0.08
                    );

            if (localForce >= threshold) {
                node.failed =
                        true;

                node.directlyFailed =
                        true;

                failed++;
            }
        }

        return failed;
    }

    private static int propagateLoadsAndFail(
            Scan scan
    ) {
        List<Node> descending =
                new ArrayList<>(
                        scan.nodes.values()
                );

        descending.sort(
                Comparator.comparingInt(
                                (Node node) ->
                                        node.pos.getY()
                        )
                        .reversed()
        );

        int totalNewFailures =
                0;

        for (int iteration = 0;
             iteration < 4;
             iteration++) {
            for (Node node :
                    descending) {
                node.load =
                        node.mass;
            }

            for (Node node :
                    descending) {
                if (node.failed) {
                    continue;
                }

                List<Node> supports =
                        lowerSupports(
                                scan,
                                node
                        );

                if (supports.isEmpty()) {
                    if (!node.anchor) {
                        node.load +=
                                node.mass
                                        * 7.0;
                    }

                    continue;
                }

                double totalWeight =
                        0.0;

                for (Node support :
                        supports) {
                    totalWeight +=
                            support.strength;
                }

                double carried =
                        node.load
                                * 0.97;

                for (Node support :
                        supports) {
                    support.load +=
                            carried
                                    * (
                                    support.strength
                                            / totalWeight
                            );
                }
            }

            int iterationFailures =
                    0;

            for (Node node :
                    descending) {
                if (node.failed
                        || node.protectedGround) {
                    continue;
                }

                int braces =
                        lateralBraces(
                                scan,
                                node
                        );

                double capacity =
                        node.strength
                                * (
                                4.2
                                        + braces
                                        * 0.62
                        );

                if (node.anchor) {
                    capacity *=
                            2.20;
                }

                if (node.load > capacity) {
                    node.failed =
                            true;

                    iterationFailures++;

                    totalNewFailures++;
                }
            }

            if (iterationFailures == 0) {
                break;
            }
        }

        return totalNewFailures;
    }

    private static List<Node> lowerSupports(
            Scan scan,
            Node node
    ) {
        List<Node> supports =
                new ArrayList<>(
                        5
                );

        Node direct =
                aliveNode(
                        scan,
                        node.pos.below()
                );

        if (direct != null) {
            supports.add(
                    direct
            );

            return supports;
        }

        int y =
                node.pos.getY()
                        - 1;

        for (int dx = -2;
             dx <= 2;
             dx++) {
            for (int dz = -2;
                 dz <= 2;
                 dz++) {
                int manhattan =
                        Math.abs(
                                dx
                        )
                                + Math.abs(
                                dz
                        );

                if (manhattan == 0
                        || manhattan > 2) {
                    continue;
                }

                Node support =
                        aliveNode(
                                scan,
                                new BlockPos(
                                        node.pos.getX() + dx,
                                        y,
                                        node.pos.getZ() + dz
                                )
                        );

                if (support != null) {
                    supports.add(
                            support
                    );
                }
            }
        }

        return supports;
    }

    private static Node aliveNode(
            Scan scan,
            BlockPos pos
    ) {
        Node node =
                scan.nodes.get(
                        pos.asLong()
                );

        return node == null
                || node.failed
                ? null
                : node;
    }

    private static int lateralBraces(
            Scan scan,
            Node node
    ) {
        int braces =
                0;

        for (Direction direction :
                Direction.Plane.HORIZONTAL) {
            Node other =
                    scan.nodes.get(
                            node.pos.relative(
                                            direction
                                    )
                                    .asLong()
                    );

            if (other != null
                    && !other.failed) {
                braces++;
            }
        }

        return braces;
    }

    private static Set<Long> findStableNodes(
            Scan scan
    ) {
        Set<Long> stable =
                new HashSet<>();

        Deque<Node> queue =
                new ArrayDeque<>();

        for (Node node :
                scan.nodes.values()) {
            if (!node.failed
                    && node.anchor) {
                stable.add(
                        node.pos.asLong()
                );

                queue.addLast(
                        node
                );
            }
        }

        while (!queue.isEmpty()) {
            Node current =
                    queue.removeFirst();

            for (Direction direction :
                    Direction.values()) {
                BlockPos nextPos =
                        current.pos.relative(
                                direction
                        );

                Node next =
                        scan.nodes.get(
                                nextPos.asLong()
                        );

                if (next == null
                        || next.failed
                        || !stable.add(
                        next.pos.asLong()
                )) {
                    continue;
                }

                queue.addLast(
                        next
                );
            }
        }

        return stable;
    }

    private static List<List<Node>> partition(
            Scan scan,
            Set<Long> candidates,
            int maxCluster
    ) {
        List<List<Node>> result =
                new ArrayList<>();

        if (candidates.isEmpty()) {
            return result;
        }

        Set<Long> remaining =
                new HashSet<>(
                        candidates
                );

        while (!remaining.isEmpty()
                && result.size()
                < StructuralCollapseS2CPayload.MAX_CLUSTERS) {
            long seed =
                    remaining.iterator()
                            .next();

            Node seedNode =
                    scan.nodes.get(
                            seed
                    );

            if (seedNode == null) {
                remaining.remove(
                        seed
                );

                continue;
            }

            List<Node> group =
                    new ArrayList<>(
                            Math.min(
                                    maxCluster,
                                    remaining.size()
                            )
                    );

            Deque<Node> queue =
                    new ArrayDeque<>();

            queue.add(
                    seedNode
            );

            remaining.remove(
                    seed
            );

            while (!queue.isEmpty()
                    && group.size() < maxCluster) {
                Node current =
                        queue.removeFirst();

                group.add(
                        current
                );

                for (Direction direction :
                        Direction.values()) {
                    if (group.size()
                            + queue.size()
                            >= maxCluster) {
                        break;
                    }

                    long neighbor =
                            current.pos.relative(
                                            direction
                                    )
                                    .asLong();

                    if (!remaining.remove(
                            neighbor
                    )) {
                        continue;
                    }

                    Node next =
                            scan.nodes.get(
                                    neighbor
                            );

                    if (next != null) {
                        queue.addLast(
                                next
                        );
                    }
                }
            }

            result.add(
                    group
            );
        }

        return result;
    }

    private static int computeFallDistance(
            ServerLevel level,
            List<Node> group,
            Set<Long> movingPositions
    ) {
        Map<Long, Node> bottomByColumn =
                new HashMap<>();

        for (Node node :
                group) {
            long column =
                    (
                            (
                                    long
                            ) node.pos.getX()
                                    << 32
                    )
                            ^ (
                            node.pos.getZ()
                                    & 0xffffffffL
                    );

            Node current =
                    bottomByColumn.get(
                            column
                    );

            if (current == null
                    || node.pos.getY()
                    < current.pos.getY()) {
                bottomByColumn.put(
                        column,
                        node
                );
            }
        }

        List<Integer> clearances =
                new ArrayList<>(
                        bottomByColumn.size()
                );

        for (Node bottom :
                bottomByColumn.values()) {
            int clearance =
                    MAX_FALL_DISTANCE;

            for (int distance = 1;
                 distance <= MAX_FALL_DISTANCE;
                 distance++) {
                BlockPos target =
                        bottom.pos.below(
                                distance
                        );

                if (target.getY()
                        <= level.getMinBuildHeight()) {
                    clearance =
                            distance - 1;
                    break;
                }

                if (movingPositions.contains(
                        target.asLong()
                )) {
                    continue;
                }

                if (!level.hasChunkAt(
                        target
                )) {
                    clearance =
                            distance - 1;
                    break;
                }

                BlockState state =
                        level.getBlockState(
                                target
                        );

                if (isBroadObstacle(
                        level,
                        target,
                        state
                )) {
                    clearance =
                            distance - 1;
                    break;
                }
            }

            clearances.add(
                    clearance
            );
        }

        if (clearances.isEmpty()) {
            return 0;
        }

        clearances.sort(
                Integer::compareTo
        );

        int index;

        if (clearances.size() <= 4) {
            index =
                    0;
        } else {
            // A broad section needs meaningful support before its entire
            // rigid body stops. One fence/post/odd voxel no longer catches a
            // whole building in mid-air.
            index =
                    Math.min(
                            clearances.size() - 1,
                            Math.max(
                                    0,
                                    (int) Math.floor(
                                            (
                                                    clearances.size()
                                                            - 1
                                            )
                                                    * 0.30
                                    )
                            )
                    );
        }

        return Mth.clamp(
                clearances.get(
                        index
                ),
                0,
                MAX_FALL_DISTANCE
        );
    }

    private static boolean isBroadObstacle(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        if (state.isAir()
                || state.getCollisionShape(
                level,
                pos
        )
                .isEmpty()) {
            return false;
        }

        return state.isFaceSturdy(
                level,
                pos,
                Direction.UP
        );
    }

    private static int fallTicksFor(
            int distance,
            boolean loose
    ) {
        if (distance <= 0) {
            return loose
                    ? 10
                    : 1;
        }

        int ticks =
                8
                        + Mth.ceil(
                        Math.sqrt(
                                distance
                        )
                                * 5.0
                );

        if (loose) {
            ticks =
                    Math.max(
                            9,
                            ticks - 4
                    );
        }

        return Mth.clamp(
                ticks,
                8,
                70
        );
    }

    private static void removeMovingBlocks(
            ServerLevel level,
            List<ClusterPlan> plans
    ) {
        for (ClusterPlan plan :
                plans) {
            for (Node node :
                    plan.nodes) {
                if (!level.getBlockState(
                        node.pos
                )
                        .is(
                                node.state.getBlock()
                        )) {
                    continue;
                }

                level.setBlock(
                        node.pos,
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS
                );
            }
        }
    }

    private static void scheduleSettlement(
            ServerLevel level,
            ClusterPlan plan
    ) {
        List<Snapshot> snapshots =
                new ArrayList<>(
                        plan.nodes.size()
                );

        for (Node node :
                plan.nodes) {
            snapshots.add(
                    new Snapshot(
                            node.pos,
                            node.state
                    )
            );
        }

        snapshots.sort(
                Comparator.comparingInt(
                        snapshot ->
                                snapshot.pos.getY()
                )
        );

        long now =
                level.getServer()
                        .overworld()
                        .getGameTime();

        PENDING.add(
                new PendingCluster(
                        level.dimension(),
                        snapshots,
                        plan.fallDistance,
                        now
                                + plan.fallTicks,
                        now
                                + plan.fallTicks
                                + 240L
                )
        );
    }

    private static boolean allChunksReady(
            ServerLevel level,
            PendingCluster pending
    ) {
        for (Snapshot snapshot :
                pending.snapshots) {
            BlockPos target =
                    snapshot.pos.below(
                            pending.fallDistance
                    );

            if (!level.hasChunkAt(
                    target
            )) {
                return false;
            }
        }

        return true;
    }

    private static void settle(
            ServerLevel level,
            PendingCluster pending
    ) {
        for (Snapshot snapshot :
                pending.snapshots) {
            BlockPos target =
                    snapshot.pos.below(
                            pending.fallDistance
                    );

            BlockPos place =
                    findSettlementPosition(
                            level,
                            target
                    );

            if (place == null) {
                continue;
            }

            level.setBlock(
                    place,
                    snapshot.state,
                    Block.UPDATE_ALL
            );
        }
    }

    private static BlockPos findSettlementPosition(
            ServerLevel level,
            BlockPos target
    ) {
        for (int rise = 0;
             rise <= 3;
             rise++) {
            BlockPos candidate =
                    target.above(
                            rise
                    );

            BlockState state =
                    level.getBlockState(
                            candidate
                    );

            if (state.isAir()
                    || state.canBeReplaced()) {
                return candidate;
            }
        }

        return null;
    }

    private static boolean inside(
            BlockPos pos,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ
    ) {
        return pos.getX() >= minX
                && pos.getX() <= maxX
                && pos.getY() >= minY
                && pos.getY() <= maxY
                && pos.getZ() >= minZ
                && pos.getZ() <= maxZ;
    }

    private static final class Node {
        private final BlockPos pos;
        private final BlockState state;
        private final double mass;
        private final double strength;
        private final boolean protectedGround;

        private boolean anchor;
        private boolean failed;
        private boolean directlyFailed;
        private double load;

        private Node(
                BlockPos pos,
                BlockState state,
                double mass,
                double strength,
                boolean protectedGround
        ) {
            this.pos = pos;
            this.state = state;
            this.mass = mass;
            this.strength = strength;
            this.protectedGround = protectedGround;
        }
    }

    private record Scan(
            Map<Long, Node> nodes,
            int minY
    ) {
    }

    private record Snapshot(
            BlockPos pos,
            BlockState state
    ) {
    }

    private static final class PendingCluster {
        private final ResourceKey<Level> dimension;
        private final List<Snapshot> snapshots;
        private final int fallDistance;
        private long settleAt;
        private final long expireAt;

        private PendingCluster(
                ResourceKey<Level> dimension,
                List<Snapshot> snapshots,
                int fallDistance,
                long settleAt,
                long expireAt
        ) {
            this.dimension = dimension;
            this.snapshots = snapshots;
            this.fallDistance = fallDistance;
            this.settleAt = settleAt;
            this.expireAt = expireAt;
        }
    }

    private record ClusterPlan(
            List<Node> nodes,
            int fallDistance,
            int fallTicks,
            long seed,
            boolean settle
    ) {
    }
}
