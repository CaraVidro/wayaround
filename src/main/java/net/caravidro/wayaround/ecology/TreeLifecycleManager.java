package net.caravidro.wayaround.ecology;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.domain.VoidDomainManager;
import net.caravidro.wayaround.time.TimeAgingEngine;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.geography.AntarcticField;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Natural tree death + sideways collapse. Unobserved trees resolve instantly;
 * nearby players get a coarse staged collapse rather than a teleport.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class TreeLifecycleManager {

    private static final List<StandingDead> DEAD_TREES =
            new ArrayList<>();

    private static final List<PendingFall> FALLS =
            new ArrayList<>();

    private TreeLifecycleManager() {}

    public static void sampleTreeLife(
            ServerLevel level,
            BlockPos surface,
            RandomSource random
    ) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.TIME_AGING)
                || !supportsDeadWood(
                level,
                surface
        )) {
            return;
        }

        Tree tree =
                findTree(level, surface);

        if (tree == null) {
            return;
        }

        var temporal =
                TimeAgingEngine.sampleWorldSurface(
                        level,
                        tree.base(),
                        false
                );

        double ageDays =
                temporal.ageTicks()
                        / 24000.0;

        if (ageDays < 4.0) {
            return;
        }

        float leafHealth =
                leafHealth(
                        level,
                        tree
                );

        double chance =
                0.0008
                        + Math.max(0.0, ageDays - 6.0)
                                * 0.00015
                        + (1.0 - leafHealth)
                                * 0.006;

        if (random.nextDouble() < Math.min(0.02, chance)) {
            markDead(
                    level,
                    tree,
                    randomHorizontal(random),
                    random
            );
        }
    }

    public static void tryFallStandingDead(
            ServerLevel level,
            BlockPos anySegment,
            RandomSource random
    ) {
        BlockPos base =
                anySegment;

        while (level.getBlockState(
                base.below()
        ).is(
                EcologyContent.ROTTING_LOG.get()
        )) {
            base =
                    base.below();
        }

        for (StandingDead dead : DEAD_TREES) {
            if (dead.dimension()
                    .equals(
                            level.dimension()
                    )
                    && dead.base()
                    .equals(
                            base
                    )) {
                return;
            }
        }

        List<BlockPos> logs =
                new ArrayList<>();

        BlockPos cursor =
                base;

        while (logs.size() < 14
                && level.getBlockState(
                cursor
        ).is(
                EcologyContent.ROTTING_LOG.get()
        )
                && level.getBlockState(
                cursor
        ).getValue(
                RotatedPillarBlock.AXIS
        )
                == Direction.Axis.Y) {
            logs.add(
                    cursor.immutable()
            );

            cursor =
                    cursor.above();
        }

        if (logs.size() < 3) {
            return;
        }

        beginFall(
                level,
                new Tree(
                        base.immutable(),
                        List.copyOf(
                                logs
                        )
                ),
                randomHorizontal(
                        random
                )
        );
    }

    public static void trySeedFallenTree(
            ServerLevel level,
            BlockPos near,
            RandomSource random
    ) {
        if (!supportsDeadWood(
                level,
                near
        )
                || random.nextFloat() > 0.045F) {
            return;
        }

        Tree tree =
                findTree(level, near);

        if (tree != null
                && tree.height() >= 4
                && random.nextFloat() < 0.18F) {
            markDead(
                    level,
                    tree,
                    randomHorizontal(random),
                    random
            );
            return;
        }

        if (random.nextFloat() < 0.45F) {
            seedOldLog(
                    level,
                    near,
                    randomHorizontal(random),
                    2 + random.nextInt(4)
            );
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (!DEAD_TREES.isEmpty()) {
            Iterator<StandingDead> deadIterator =
                    DEAD_TREES.iterator();

            while (deadIterator.hasNext()) {
                StandingDead dead =
                        deadIterator.next();

                if (event.getServer().getTickCount()
                        < dead.fallAt()) {
                    continue;
                }

                ServerLevel level =
                        event.getServer()
                                .getLevel(
                                        dead.dimension()
                                );

                deadIterator.remove();

                if (level == null) {
                    continue;
                }

                beginFall(
                        level,
                        new Tree(
                                dead.base(),
                                dead.logs()
                        ),
                        dead.direction()
                );
            }
        }

        if (FALLS.isEmpty()) {
            return;
        }

        Iterator<PendingFall> iterator =
                FALLS.iterator();

        while (iterator.hasNext()) {
            PendingFall fall =
                    iterator.next();

            ServerLevel level =
                    event.getServer()
                            .getLevel(
                                    fall.dimension()
                            );

            if (level == null) {
                iterator.remove();
                continue;
            }

            if (event.getServer().getTickCount()
                    < fall.nextTick()) {
                continue;
            }

            if (fall.step()
                    >= fall.sources().size()) {
                iterator.remove();
                continue;
            }

            /*
             * The base log is removed as soon as a watched fall begins, so the
             * collapse never leaves that annoying one-block stump behind.
             * Remaining trunk pieces resolve bottom-up into the fallen trunk.
             */
            BlockPos source =
                    fall.sources()
                            .get(
                                    fall.step()
                            );

            level.removeBlock(
                    source,
                    false
            );

            placeFallenSegment(
                    level,
                    fall.base(),
                    fall.direction(),
                    fall.step() + 2,
                    fall.rot()
            );

            level.sendParticles(
                    ParticleTypes.POOF,
                    source.getX() + 0.5,
                    source.getY() + 0.5,
                    source.getZ() + 0.5,
                    5,
                    0.20,
                    0.20,
                    0.20,
                    0.01
            );

            fall.advance(
                    event.getServer().getTickCount()
                            + 2
            );
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        DEAD_TREES.clear();
        FALLS.clear();
    }

    /**
     * Dead wood is an ecological result, not a universal decoration.
     *
     * The old seeding fallback only needed a full support block, which meant
     * it could happily place rotting logs on Antarctic terrain and even on the
     * invisible Barrier floor of Void pocket-space. Restrict it to plausible
     * vegetated biomes and explicitly reject technical/polar spaces.
     */
    private static boolean supportsDeadWood(
            ServerLevel level,
            BlockPos pos
    ) {
        if (level == null
                || pos == null
                || !level.dimension()
                .equals(
                        Level.OVERWORLD
                )
                || VoidDomainManager.isInsidePocket(
                level,
                pos
        )
                || AntarcticField.isAntarctic(
                pos.getX(),
                pos.getZ()
        )
                || AntarcticField.isSouthernOcean(
                pos.getX(),
                pos.getZ()
        )) {
            return false;
        }

        String biome =
                level.getBiome(
                        pos
                )
                        .unwrapKey()
                        .map(
                                key -> key.location()
                                        .getPath()
                        )
                        .orElse(
                                ""
                        );

        if (biome.contains(
                "ocean"
        )
                || biome.contains(
                "beach"
        )
                || biome.contains(
                "desert"
        )
                || biome.contains(
                "badlands"
        )
                || biome.contains(
                "frozen"
        )
                || biome.contains(
                "snowy"
        )
                || biome.contains(
                "ice"
        )
                || biome.contains(
                "antarctic"
        )) {
            return false;
        }

        return biome.contains(
                "forest"
        )
                || biome.contains(
                "taiga"
        )
                || biome.contains(
                "jungle"
        )
                || biome.contains(
                "savanna"
        )
                || biome.contains(
                "plains"
        )
                || biome.contains(
                "meadow"
        )
                || biome.contains(
                "grove"
        )
                || biome.contains(
                "swamp"
        )
                || biome.contains(
                "river"
        )
                || biome.contains(
                "cherry"
        );
    }

    private static void markDead(
            ServerLevel level,
            Tree tree,
            Direction direction,
            RandomSource random
    ) {
        for (int i = 0; i < tree.logs().size(); i++) {
            BlockPos pos =
                    tree.logs().get(i);

            BlockState current =
                    level.getBlockState(pos);

            Direction.Axis axis =
                    current.hasProperty(
                            RotatedPillarBlock.AXIS
                    )
                            ? current.getValue(
                            RotatedPillarBlock.AXIS
                    )
                            : Direction.Axis.Y;

            level.setBlockAndUpdate(
                    pos,
                    EcologyContent.ROTTING_LOG.get()
                            .defaultBlockState()
                            .setValue(
                                    RotatedPillarBlock.AXIS,
                                    axis
                            )
                            .setValue(
                                    RottingLogBlock.ROT,
                                    i == 0
                                            ? 1
                                            : 0
                            )
            );
        }

        level.playSound(
                null,
                tree.base(),
                SoundEvents.AXE_STRIP,
                SoundSource.BLOCKS,
                0.55F,
                0.62F
        );

        DEAD_TREES.add(
                new StandingDead(
                        level.dimension(),
                        tree.base(),
                        direction,
                        List.copyOf(
                                tree.logs()
                        ),
                        level.getServer()
                                .getTickCount()
                                + 240
                                + random.nextInt(
                                1400
                        )
                )
        );
    }

    private static void beginFall(
            ServerLevel level,
            Tree tree,
            Direction direction
    ) {
        boolean watched =
                level.players()
                        .stream()
                        .anyMatch(
                                player ->
                                        player.distanceToSqr(
                                                tree.base().getX() + 0.5,
                                                tree.base().getY() + 0.5,
                                                tree.base().getZ() + 0.5
                                        ) <= 48.0 * 48.0
                                                && canSeeTree(
                                                level,
                                                player,
                                                tree.base()
                                        )
                        );

        level.playSound(
                null,
                tree.base(),
                SoundEvents.WOOD_BREAK,
                SoundSource.BLOCKS,
                1.2F,
                0.72F
        );

        BlockState trunk =
                level.getBlockState(
                        tree.base()
                );

        int rot =
                trunk.is(BlockTags.LOGS)
                        ? 0
                        : 1;

        if (!watched) {
            for (BlockPos source : tree.logs()) {
                level.removeBlock(
                        source,
                        false
                );
            }

            for (int i = 0; i < tree.logs().size(); i++) {
                placeFallenSegment(
                        level,
                        tree.base(),
                        direction,
                        i + 1,
                        rot
                );
            }

            return;
        }

        /*
         * Do not keep a stump during the observed animation. Move the base
         * segment onto the ground immediately, then stage the rest of the
         * trunk. This also means an interrupted/restarted fall cannot strand a
         * permanent one-block toquinho.
         */
        BlockPos baseSource =
                tree.logs()
                        .getFirst();

        level.removeBlock(
                baseSource,
                false
        );

        placeFallenSegment(
                level,
                tree.base(),
                direction,
                1,
                rot
        );

        List<BlockPos> remaining =
                new ArrayList<>(
                        tree.logs()
                                .subList(
                                        1,
                                        tree.logs()
                                                .size()
                                )
                );

        if (remaining.isEmpty()) {
            return;
        }

        FALLS.add(
                new PendingFall(
                        level.dimension(),
                        tree.base(),
                        direction,
                        List.copyOf(
                                remaining
                        ),
                        rot,
                        0,
                        level.getServer().getTickCount()
                )
        );
    }

    private static boolean canSeeTree(
            ServerLevel level,
            ServerPlayer player,
            BlockPos base
    ) {
        Vec3 target =
                Vec3.atCenterOf(
                        base.above()
                );

        var hit =
                level.clip(
                        new ClipContext(
                                player.getEyePosition(),
                                target,
                                ClipContext.Block.OUTLINE,
                                ClipContext.Fluid.NONE,
                                player
                        )
                );

        if (hit.getType()
                == HitResult.Type.MISS) {
            return true;
        }

        if (hit
                instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
            BlockPos hitPos =
                    blockHit.getBlockPos();

            return hitPos.distManhattan(
                    base
            )
                    <= 2;
        }

        return false;
    }

    private static void seedOldLog(
            ServerLevel level,
            BlockPos near,
            Direction direction,
            int length
    ) {
        for (int i = 0; i < length; i++) {
            placeFallenSegment(
                    level,
                    near.below(),
                    direction,
                    i + 1,
                    2
            );
        }
    }

    private static void placeFallenSegment(
            ServerLevel level,
            BlockPos base,
            Direction direction,
            int distance,
            int rot
    ) {
        int x =
                base.getX()
                        + direction.getStepX()
                                * distance;

        int z =
                base.getZ()
                        + direction.getStepZ()
                                * distance;

        int y =
                level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x,
                        z
                );

        BlockPos target =
                new BlockPos(
                        x,
                        y,
                        z
                );

        BlockPos support =
                target.below();

        if (!level.getBlockState(target)
                .canBeReplaced()
                || !level.getFluidState(target)
                        .isEmpty()
                || !level.getFluidState(support)
                        .isEmpty()
                || !level.getBlockState(support)
                        .isCollisionShapeFullBlock(
                                level,
                                support
                        )) {
            return;
        }

        level.setBlockAndUpdate(
                target,
                EcologyContent.ROTTING_LOG.get()
                        .defaultBlockState()
                        .setValue(
                                RotatedPillarBlock.AXIS,
                                direction.getAxis()
                        )
                        .setValue(
                                RottingLogBlock.ROT,
                                Math.min(3, rot)
                        )
        );
    }

    private static Tree findTree(
            ServerLevel level,
            BlockPos around
    ) {
        int top =
                Math.min(
                        level.getMaxBuildHeight() - 1,
                        around.getY() + 2
                );

        int bottom =
                Math.max(
                        level.getMinBuildHeight(),
                        around.getY() - 14
                );

        for (int y = top; y >= bottom; y--) {
            BlockPos probe =
                    new BlockPos(
                            around.getX(),
                            y,
                            around.getZ()
                    );

            if (!level.getBlockState(probe)
                    .is(BlockTags.LOGS)) {
                continue;
            }

            BlockPos base =
                    probe;

            while (level.getBlockState(base.below())
                    .is(BlockTags.LOGS)) {
                base =
                        base.below();
            }

            List<BlockPos> logs =
                    new ArrayList<>();

            BlockPos cursor =
                    base;

            while (logs.size() < 14
                    && level.getBlockState(cursor)
                            .is(BlockTags.LOGS)) {
                logs.add(
                        cursor.immutable()
                );
                cursor =
                        cursor.above();
            }

            if (logs.size() >= 3) {
                return new Tree(
                        base.immutable(),
                        List.copyOf(logs)
                );
            }
        }

        return null;
    }

    private static float leafHealth(
            ServerLevel level,
            Tree tree
    ) {
        BlockPos top =
                tree.logs()
                        .getLast();

        int leaves =
                0;

        int samples =
                0;

        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    samples++;

                    if (level.getBlockState(
                            top.offset(dx, dy, dz)
                    ).is(BlockTags.LEAVES)) {
                        leaves++;
                    }
                }
            }
        }

        return Math.min(
                1.0F,
                leaves
                        / (float) Math.max(
                        1,
                        samples / 5
                )
        );
    }

    private static Direction randomHorizontal(RandomSource random) {
        Direction[] directions = {
                Direction.NORTH,
                Direction.SOUTH,
                Direction.EAST,
                Direction.WEST
        };

        return directions[random.nextInt(directions.length)];
    }

    private record Tree(
            BlockPos base,
            List<BlockPos> logs
    ) {
        private int height() {
            return logs.size();
        }
    }

    private record StandingDead(
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
            BlockPos base,
            Direction direction,
            List<BlockPos> logs,
            int fallAt
    ) {
    }

    private static final class PendingFall {
        private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        private final BlockPos base;
        private final Direction direction;
        private final List<BlockPos> sources;
        private final int rot;
        private int step;
        private int nextTick;

        private PendingFall(
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                BlockPos base,
                Direction direction,
                List<BlockPos> sources,
                int rot,
                int step,
                int nextTick
        ) {
            this.dimension = dimension;
            this.base = base;
            this.direction = direction;
            this.sources = sources;
            this.rot = rot;
            this.step = step;
            this.nextTick = nextTick;
        }

        private net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension() {
            return dimension;
        }

        private BlockPos base() {
            return base;
        }

        private Direction direction() {
            return direction;
        }

        private List<BlockPos> sources() {
            return sources;
        }

        private int rot() {
            return rot;
        }

        private int step() {
            return step;
        }

        private int nextTick() {
            return nextTick;
        }

        private void advance(int tick) {
            step++;
            nextTick = tick;
        }
    }
}
