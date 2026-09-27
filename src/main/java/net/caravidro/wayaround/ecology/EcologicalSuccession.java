package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.time.TimeAgingEngine;
import net.caravidro.wayaround.time.TemporalState;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Runtime succession layer: rain grows vegetation, damp surfaces moss over,
 * rivers deposit sediment and every server session immediately seeds loaded
 * terrain so an old world does not need newly generated chunks to feel alive.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class EcologicalSuccession {

    private static final int RUNTIME_INTERVAL =
            80;

    private EcologicalSuccession() {}

    @SubscribeEvent
    public static void started(
            ServerStartedEvent event
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.LIVING_VEGETATION
        )) {
            return;
        }

        ServerLevel level =
                event.getServer()
                        .overworld();

        BlockPos spawn =
                level.getSharedSpawnPos();

        int centerX =
                spawn.getX() >> 4;

        int centerZ =
                spawn.getZ() >> 4;

        for (int cx = centerX - 1; cx <= centerX + 1; cx++) {
            for (int cz = centerZ - 1; cz <= centerZ + 1; cz++) {
                BlockPos probe =
                        new BlockPos(
                                (cx << 4) + 8,
                                spawn.getY(),
                                (cz << 4) + 8
                        );

                if (!level.hasChunkAt(
                        probe
                )) {
                    continue;
                }

                if (EcologyWorldData.get(level)
                        .markSeeded(
                                cx,
                                cz
                        )) {
                    seedChunk(
                            level,
                            cx,
                            cz
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_VEGETATION)) {
            return;
        }

        int tick = event.getServer().getTickCount();

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.dimension().equals(Level.OVERWORLD)) {
                continue;
            }

            seedLoadedPlayerArea(level);

            if (tick % RUNTIME_INTERVAL == 0) {
                mutateAroundPlayers(level);
            }
        }
    }

    private static void seedLoadedPlayerArea(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            int centerX = player.blockPosition().getX() >> 4;
            int centerZ = player.blockPosition().getZ() >> 4;

            for (int cx = centerX - 1; cx <= centerX + 1; cx++) {
                for (int cz = centerZ - 1; cz <= centerZ + 1; cz++) {
                    BlockPos probe = new BlockPos(
                            (cx << 4) + 8,
                            player.blockPosition().getY(),
                            (cz << 4) + 8
                    );

                    if (!level.hasChunkAt(probe)) {
                        continue;
                    }

                    if (!EcologyWorldData.get(level)
                            .markSeeded(
                                    cx,
                                    cz
                            )) {
                        continue;
                    }

                    seedChunk(level, cx, cz);
                }
            }
        }
    }

    private static void seedChunk(
            ServerLevel level,
            int chunkX,
            int chunkZ
    ) {
        RandomSource random = level.random;

        for (int i = 0; i < 72; i++) {
            int x = (chunkX << 4) + random.nextInt(16);
            int z = (chunkZ << 4) + random.nextInt(16);
            BlockPos surface = surface(level, x, z);

            initialSurfacePass(level, surface, random);

            if (i % 9 == 0) {
                TreeLifecycleManager.trySeedFallenTree(
                        level,
                        surface,
                        random
                );
            }
        }
    }

    private static void initialSurfacePass(
            ServerLevel level,
            BlockPos surface,
            RandomSource random
    ) {
        BlockPos ground =
                ecologyGround(
                        level,
                        surface
                );

        BlockState state = level.getBlockState(ground);
        boolean wet = humid(level, ground);

        if (state.is(Blocks.DIRT)
                && level.getBlockState(surface).isAir()
                && random.nextFloat() < 0.48F) {
            level.setBlockAndUpdate(
                    ground,
                    Blocks.GRASS_BLOCK.defaultBlockState()
            );
            state = level.getBlockState(ground);
        }

        if (wet) {
            mossify(level, ground, random, 0.34F);
        }

        seedGroundPlant(
                level,
                ground,
                random,
                wet,
                0.34F
        );

        seedRiverMaterial(
                level,
                ground,
                random,
                true
        );
    }

    private static void mutateAroundPlayers(ServerLevel level) {
        RandomSource random = level.random;

        for (ServerPlayer player : level.players()) {
            for (int i = 0; i < 26; i++) {
                int x = player.blockPosition().getX()
                        + random.nextInt(65) - 32;

                int z = player.blockPosition().getZ()
                        + random.nextInt(65) - 32;

                BlockPos surface = surface(level, x, z);
                BlockPos ground =
                        ecologyGround(
                                level,
                                surface
                        );

                if (!level.hasChunkAt(ground)) {
                    continue;
                }

                boolean raining =
                        level.isRainingAt(surface);

                boolean wet =
                        humid(level, ground);

                TemporalState temporal =
                        null;

                if (WorldFeatureRuntime.serverEnabled(
                        WorldFeature.TIME_AGING
                )) {
                    temporal =
                            TimeAgingEngine.sampleWorldSurface(
                                    level,
                                    ground,
                                    false
                            );
                }

                if (raining) {
                    growAfterRain(
                            level,
                            ground,
                            random
                    );
                }

                float longTermGrowth =
                        temporal == null
                                ? 0.0F
                                : temporal.organicGrowth();

                if (wet
                        && random.nextFloat()
                        < 0.24F + longTermGrowth * 0.36F) {
                    mossify(
                            level,
                            ground,
                            random,
                            0.20F
                                    + longTermGrowth
                                            * 0.48F
                    );
                }

                if ((raining || wet || longTermGrowth > 0.10F)
                        && random.nextFloat()
                        < 0.28F + longTermGrowth * 0.30F) {
                    seedGroundPlant(
                            level,
                            ground,
                            random,
                            wet,
                            0.18F
                    );
                }

                if (random.nextFloat()
                        < (raining ? 0.26F : 0.10F)) {
                    seedRiverMaterial(
                            level,
                            ground,
                            random,
                            false
                    );
                }

                float recruitment =
                        temporal == null
                                ? 0.0F
                                : EcologyRules.recruitment(
                                temporal.organicGrowth(),
                                temporal.moistureMemory()
                        );

                if ((raining || recruitment > 0.34F)
                        && random.nextFloat()
                        < 0.015F
                                + recruitment
                                        * 0.018F) {
                    tryPlaceSapling(
                            level,
                            ground,
                            random
                    );
                }

                if (random.nextFloat() < 0.022F) {
                    TreeLifecycleManager.sampleTreeLife(
                            level,
                            surface,
                            random
                    );
                }
            }
        }
    }

    private static void growAfterRain(
            ServerLevel level,
            BlockPos ground,
            RandomSource random
    ) {
        BlockState state = level.getBlockState(ground);
        BlockPos above = ground.above();

        if (state.is(Blocks.DIRT)
                && level.getBlockState(above).isAir()
                && level.getMaxLocalRawBrightness(above) >= 8) {
            level.setBlockAndUpdate(
                    ground,
                    Blocks.GRASS_BLOCK.defaultBlockState()
            );
            state = level.getBlockState(ground);
        }

        if (state.is(Blocks.GRASS_BLOCK)
                && level.getBlockState(above).isAir()
                && random.nextFloat() < 0.30F) {
            BlockState growth =
                    random.nextFloat() < 0.18F
                            ? Blocks.FERN.defaultBlockState()
                            : Blocks.SHORT_GRASS.defaultBlockState();

            if (growth.canSurvive(level, above)) {
                level.setBlockAndUpdate(
                        above,
                        growth
                );
            }
        }
    }

    private static void mossify(
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            float chance
    ) {
        if (random.nextFloat() > chance) {
            return;
        }

        BlockState state = level.getBlockState(pos);

        if (state.is(Blocks.COBBLESTONE)) {
            level.setBlockAndUpdate(
                    pos,
                    Blocks.MOSSY_COBBLESTONE.defaultBlockState()
            );
            return;
        }

        if (state.is(Blocks.STONE_BRICKS)) {
            level.setBlockAndUpdate(
                    pos,
                    Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
            );
            return;
        }

        BlockPos above = pos.above();

        if ((state.is(Blocks.STONE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.MUD)
                || state.is(Blocks.ROOTED_DIRT))
                && level.getBlockState(above).isAir()
                && Blocks.MOSS_CARPET.defaultBlockState()
                        .canSurvive(level, above)) {

            level.setBlockAndUpdate(
                    above,
                    Blocks.MOSS_CARPET.defaultBlockState()
            );
        }
    }

    private static void seedGroundPlant(
            ServerLevel level,
            BlockPos ground,
            RandomSource random,
            boolean wet,
            float chance
    ) {
        BlockPos above = ground.above();

        if (!level.getBlockState(above).isAir()
                || random.nextFloat() > chance) {
            return;
        }

        BlockState groundState =
                level.getBlockState(ground);

        boolean fertile =
                groundState.is(Blocks.GRASS_BLOCK)
                        || groundState.is(Blocks.DIRT)
                        || groundState.is(Blocks.PODZOL)
                        || groundState.is(Blocks.MOSS_BLOCK)
                        || groundState.is(Blocks.MUD);

        if (!fertile) {
            return;
        }

        BlockState plant;

        float pick = random.nextFloat();

        if (wet && pick < 0.36F) {
            plant =
                    EcologyContent.DAMP_FERN.get()
                            .defaultBlockState();
        } else if (wet && pick < 0.62F) {
            plant =
                    EcologyContent.RIVER_SPRIG.get()
                            .defaultBlockState();
        } else if (pick < 0.82F) {
            plant =
                    EcologyContent.WOODLAND_SORREL.get()
                            .defaultBlockState();
        } else {
            plant =
                    random.nextBoolean()
                            ? Blocks.FERN.defaultBlockState()
                            : Blocks.SHORT_GRASS.defaultBlockState();
        }

        if (plant.canSurvive(level, above)) {
            level.setBlockAndUpdate(
                    above,
                    plant
            );
        }
    }

    private static void seedRiverMaterial(
            ServerLevel level,
            BlockPos ground,
            RandomSource random,
            boolean initial
    ) {
        BlockPos waterPos =
                ground.above();

        if (!level.getFluidState(waterPos)
                .is(FluidTags.WATER)) {
            return;
        }

        Vec3 flow =
                WaterDynamics.current(
                        level,
                        waterPos
                );

        double speed =
                WaterDynamics.speed(flow);

        boolean deposition =
                speed < 0.095
                        || WaterDynamics.hitsObstacle(
                        level,
                        waterPos,
                        flow
                );

        BlockState floor =
                level.getBlockState(ground);

        float sandChance =
                initial
                        ? 0.20F
                        : 0.055F;

        if (deposition
                && random.nextFloat() < sandChance
                && (floor.is(Blocks.DIRT)
                || floor.is(Blocks.GRAVEL)
                || floor.is(Blocks.CLAY)
                || floor.is(Blocks.MUD))) {

            level.setBlockAndUpdate(
                    ground,
                    Blocks.SAND.defaultBlockState()
            );
        }

        BlockState waterState =
                level.getBlockState(waterPos);

        if (waterState.getFluidState()
                .is(FluidTags.WATER)
                && !waterState.is(EcologyContent.RIVER_PEBBLES.get())
                && random.nextFloat() < (initial ? 0.24F : 0.075F)) {

            Direction facing =
                    WaterDynamics.dominantDirection(flow);

            if (facing == null) {
                Direction[] dirs = {
                        Direction.NORTH,
                        Direction.SOUTH,
                        Direction.EAST,
                        Direction.WEST
                };
                facing = dirs[random.nextInt(dirs.length)];
            }

            level.setBlockAndUpdate(
                    waterPos,
                    EcologyContent.RIVER_PEBBLES.get()
                            .defaultBlockState()
                            .setValue(
                                    RiverPebbleBlock.COUNT,
                                    1 + random.nextInt(4)
                            )
                            .setValue(
                                    RiverPebbleBlock.FACING,
                                    facing
                            )
                            .setValue(
                                    RiverPebbleBlock.WATERLOGGED,
                                    true
                            )
            );
        }
    }

    private static void tryPlaceSapling(
            ServerLevel level,
            BlockPos ground,
            RandomSource random
    ) {
        BlockPos above =
                ground.above();

        if (!level.getBlockState(above).isAir()) {
            return;
        }

        BlockState groundState =
                level.getBlockState(ground);

        if (!groundState.is(Blocks.GRASS_BLOCK)
                && !groundState.is(Blocks.DIRT)
                && !groundState.is(Blocks.PODZOL)) {
            return;
        }

        String biome =
                level.getBiome(ground)
                        .unwrapKey()
                        .map(key -> key.location().getPath())
                        .orElse("");

        BlockState sapling =
                biome.contains("taiga")
                        || biome.contains("grove")
                        ? Blocks.SPRUCE_SAPLING.defaultBlockState()
                        : biome.contains("birch")
                                ? Blocks.BIRCH_SAPLING.defaultBlockState()
                                : biome.contains("jungle")
                                        ? Blocks.JUNGLE_SAPLING.defaultBlockState()
                                        : biome.contains("savanna")
                                                ? Blocks.ACACIA_SAPLING.defaultBlockState()
                                                : Blocks.OAK_SAPLING.defaultBlockState();

        if (sapling.canSurvive(level, above)) {
            level.setBlockAndUpdate(
                    above,
                    sapling
            );
        }
    }

    private static boolean humid(
            ServerLevel level,
            BlockPos pos
    ) {
        if (level.isRainingAt(pos.above())) {
            return true;
        }

        for (Direction direction : Direction.values()) {
            for (int distance = 1; distance <= 3; distance++) {
                if (level.getFluidState(
                        pos.relative(direction, distance)
                ).is(FluidTags.WATER)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static BlockPos ecologyGround(
            ServerLevel level,
            BlockPos surface
    ) {
        BlockPos cursor =
                surface.below();

        int depth =
                0;

        while (depth < 24
                && level.getFluidState(
                cursor
        ).is(FluidTags.WATER)) {
            cursor =
                    cursor.below();

            depth++;
        }

        return cursor;
    }

    private static BlockPos surface(
            ServerLevel level,
            int x,
            int z
    ) {
        int y =
                level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x,
                        z
                );

        return new BlockPos(
                x,
                y,
                z
        );
    }
}
