package net.caravidro.wayaround.industrial.mining;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Player-proximity world generation for WayAround mining regions.
 *
 * The geological address is deterministic from the world seed, but underground
 * geometry is not materialized just because the surface chunk exists.
 */
public final class DeferredMiningManager {
    private static final Map<ServerLevel, Set<BlockPos>> ACTIVE =
            new IdentityHashMap<>();

    private static final int UNDERGROUND_HALF_HEIGHT = 9;
    private static final int ORE_HALF_HEIGHT = 12;

    private DeferredMiningManager() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.MINING_REGIONS)) {
            ACTIVE.clear();
            return;
        }

        MinecraftServer server = event.getServer();
        long tick = server.getTickCount();

        if (tick % 10L == 0L) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.serverLevel().dimension() == Level.OVERWORLD) {
                    scanPlayer(player);
                }
            }
        }

        for (ServerLevel level : server.getAllLevels()) {
            processActive(level);
        }
    }

    public static void onBlockBroken(ServerLevel level, BlockPos broken) {
        if (level.dimension() != Level.OVERWORLD
                || !WorldFeatureRuntime.enabled(level, WorldFeature.MINING_REGIONS)) {
            return;
        }

        int cellX = MiningRegionRules.cell(broken.getX());
        int cellZ = MiningRegionRules.cell(broken.getZ());

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Optional<MiningRegionRules.Region> optional =
                        MiningRegionRules.region(level.getSeed(), cellX + dx, cellZ + dz);

                if (optional.isEmpty()) continue;
                MiningRegionRules.Region region = optional.get();
                if (region.openPit()) continue;

                double horizontal = horizontalDistance(
                        broken.getX(),
                        broken.getZ(),
                        region.centerX(),
                        region.centerZ()
                );

                if (horizontal <= region.radius() + 12
                        && Math.abs(broken.getY() - region.undergroundY()) <= 16) {
                    awaken(level, region);
                }
            }
        }
    }

    public static Optional<MiningRegionRules.Region> nearestRegion(
            ServerLevel level,
            BlockPos origin,
            int maxCells) {

        int cellX = MiningRegionRules.cell(origin.getX());
        int cellZ = MiningRegionRules.cell(origin.getZ());
        MiningRegionRules.Region best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int radius = 0; radius <= Math.max(1, maxCells); radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0
                            && Math.abs(dx) != radius
                            && Math.abs(dz) != radius) {
                        continue;
                    }

                    Optional<MiningRegionRules.Region> optional =
                            MiningRegionRules.region(
                                    level.getSeed(),
                                    cellX + dx,
                                    cellZ + dz
                            );

                    if (optional.isEmpty()) continue;

                    MiningRegionRules.Region region = optional.get();
                    double distance = horizontalDistance(
                            origin.getX(),
                            origin.getZ(),
                            region.centerX(),
                            region.centerZ()
                    );

                    if (distance < bestDistance) {
                        best = region;
                        bestDistance = distance;
                    }
                }
            }

            if (best != null && bestDistance < radius * MiningRegionRules.CELL_SIZE) {
                break;
            }
        }

        return Optional.ofNullable(best);
    }

    public static boolean forceAwaken(ServerPlayer player) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) {
            return false;
        }

        Optional<MiningRegionRules.Region> nearest =
                nearestRegion(
                        player.serverLevel(),
                        player.blockPosition(),
                        12
                );

        if (nearest.isEmpty()) {
            return false;
        }

        MiningRegionRules.Region region = nearest.get();

        if (horizontalDistance(
                player.getX(),
                player.getZ(),
                region.centerX(),
                region.centerZ()
        ) > 160.0) {
            return false;
        }

        awaken(
                player.serverLevel(),
                region
        );

        return true;
    }

    public static void clearAll() {
        ACTIVE.clear();
    }

    private static void scanPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();
        int cellX = MiningRegionRules.cell(playerPos.getX());
        int cellZ = MiningRegionRules.cell(playerPos.getZ());

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Optional<MiningRegionRules.Region> optional =
                        MiningRegionRules.region(level.getSeed(), cellX + dx, cellZ + dz);

                if (optional.isEmpty()) continue;
                MiningRegionRules.Region region = optional.get();

                if (shouldWake(level, playerPos, region)) {
                    awaken(level, region);
                }
            }
        }
    }

    private static boolean shouldWake(
            ServerLevel level,
            BlockPos player,
            MiningRegionRules.Region region) {

        double centerDistance = horizontalDistance(
                player.getX(),
                player.getZ(),
                region.centerX(),
                region.centerZ()
        );

        if (region.openPit()) {
            return centerDistance <= 72.0;
        }

        int centerSurface = level.getHeight(
                Heightmap.Types.WORLD_SURFACE,
                region.centerX(),
                region.centerZ()
        ) - 1;

        double entryDistance = horizontalDistance(
                player.getX(),
                player.getZ(),
                region.entryX(),
                region.entryZ()
        );

        boolean nearEntrance =
                entryDistance <= 32.0
                        && player.getY() >= centerSurface - 18;

        boolean undergroundApproach =
                centerDistance <= region.radius() + 22
                        && player.getY() <= centerSurface - 10
                        && Math.abs(player.getY() - region.undergroundY()) <= 30;

        return nearEntrance || undergroundApproach;
    }

    private static void awaken(
            ServerLevel level,
            MiningRegionRules.Region region) {

        BlockPos anchor = anchorPos(level, region);

        if (!regionLoaded(level, anchor, region.radius() + 3)) {
            return;
        }

        if (level.getBlockEntity(anchor) instanceof MiningRegionAnchorBlockEntity existing) {
            if (!existing.complete()) {
                ACTIVE.computeIfAbsent(level, ignored -> new LinkedHashSet<>()).add(anchor.immutable());
            }
            return;
        }

        BlockState current = level.getBlockState(anchor);
        if (!naturalRock(current)) {
            return;
        }

        if (!level.setBlock(
                anchor,
                MiningContent.REGION_ANCHOR.get().defaultBlockState(),
                2
        )) {
            return;
        }

        if (level.getBlockEntity(anchor) instanceof MiningRegionAnchorBlockEntity created) {
            created.configure(
                    region.kind(),
                    region.openPit(),
                    region.radius()
            );

            ACTIVE.computeIfAbsent(level, ignored -> new LinkedHashSet<>()).add(anchor.immutable());

            level.playSound(
                    null,
                    region.entryX() + 0.5,
                    Math.max(anchor.getY(), level.getSeaLevel()) + 0.5,
                    region.entryZ() + 0.5,
                    SoundEvents.STONE_BREAK,
                    SoundSource.BLOCKS,
                    0.18F,
                    0.58F
            );
        }
    }

    private static BlockPos anchorPos(
            ServerLevel level,
            MiningRegionRules.Region region) {

        if (!region.openPit()) {
            int y = Mth.clamp(
                    region.undergroundY(),
                    level.getMinBuildHeight() + 8,
                    level.getMaxBuildHeight() - 16
            );

            return new BlockPos(region.centerX(), y, region.centerZ());
        }

        int surface = level.getHeight(
                Heightmap.Types.WORLD_SURFACE,
                region.centerX(),
                region.centerZ()
        ) - 1;

        int pitDepth = 7 + region.radius() / 4;
        int floor = Math.max(
                level.getMinBuildHeight() + 8,
                surface - pitDepth
        );

        return new BlockPos(
                region.centerX(),
                floor - 3,
                region.centerZ()
        );
    }

    private static boolean regionLoaded(
            ServerLevel level,
            BlockPos center,
            int radius) {

        return level.hasChunkAt(center.offset(radius, 0, radius))
                && level.hasChunkAt(center.offset(radius, 0, -radius))
                && level.hasChunkAt(center.offset(-radius, 0, radius))
                && level.hasChunkAt(center.offset(-radius, 0, -radius));
    }

    private static void processActive(ServerLevel level) {
        Set<BlockPos> anchors = ACTIVE.get(level);
        if (anchors == null || anchors.isEmpty()) return;

        ArrayList<BlockPos> done = new ArrayList<>();
        int processed = 0;

        for (BlockPos pos : anchors) {
            if (processed++ >= 4) break;

            if (!level.hasChunkAt(pos)
                    || !(level.getBlockEntity(pos) instanceof MiningRegionAnchorBlockEntity anchor)) {
                done.add(pos);
                continue;
            }

            if (anchor.complete()) {
                done.add(pos);
                continue;
            }

            Optional<MiningRegionRules.Region> optional =
                    MiningRegionRules.region(
                            level.getSeed(),
                            MiningRegionRules.cell(pos.getX()),
                            MiningRegionRules.cell(pos.getZ())
                    );

            if (optional.isEmpty()) {
                anchor.completeNow();
                done.add(pos);
                continue;
            }

            MiningRegionRules.Region region = optional.get();

            if (region.centerX() != pos.getX()
                    || region.centerZ() != pos.getZ()) {
                anchor.completeNow();
                done.add(pos);
                continue;
            }

            switch (anchor.stage()) {
                case STRUCTURE -> structureStep(level, pos, anchor, region);
                case ORES -> oreStep(level, pos, anchor, region);
                case STRUCTURES -> structureProps(level, pos, anchor, region);
                case MOBS -> mobStep(level, pos, anchor, region);
                case COMPLETE -> done.add(pos);
            }
        }

        anchors.removeAll(done);
        if (anchors.isEmpty()) ACTIVE.remove(level);
    }

    private static void structureStep(
            ServerLevel level,
            BlockPos anchorPos,
            MiningRegionAnchorBlockEntity anchor,
            MiningRegionRules.Region region) {

        if (anchor.openPit()) {
            int radius = anchor.radius();
            int width = radius * 2 + 1;
            int total = width * width;
            int cursor = anchor.cursor();
            int budget = 48;

            while (cursor < total && budget-- > 0) {
                int localX = cursor % width;
                int localZ = cursor / width;
                cursor++;

                int dx = localX - radius;
                int dz = localZ - radius;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) continue;

                int x = anchorPos.getX() + dx;
                int z = anchorPos.getZ() + dz;
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;

                double normalized = distance / radius;
                double bowl = 1.0 - normalized;
                bowl = bowl * bowl * (3.0 - 2.0 * bowl);
                int depth = Math.max(1, (int) Math.round((7 + radius / 4.0) * bowl));
                int floor = Math.max(anchorPos.getY() + 3, surface - depth);

                for (int y = surface; y > floor; y--) {
                    BlockPos carve = new BlockPos(x, y, z);
                    if (carve.equals(anchorPos)) continue;
                    if (carvable(level.getBlockState(carve))) {
                        level.setBlock(carve, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }

            anchor.cursor(cursor);

            if (cursor >= total) {
                anchor.advance();
            }

            return;
        }

        int radius = anchor.radius();
        int width = radius * 2 + 1;
        int height = UNDERGROUND_HALF_HEIGHT * 2 + 1;
        int total = width * width * height;
        int cursor = anchor.cursor();
        int budget = 340;

        while (cursor < total && budget-- > 0) {
            int plane = width * width;
            int localY = cursor / plane;
            int rest = cursor % plane;
            int localZ = rest / width;
            int localX = rest % width;
            cursor++;

            int dx = localX - radius;
            int dz = localZ - radius;
            int dy = localY - UNDERGROUND_HALF_HEIGHT;

            if (!insideUndergroundShape(dx, dy, dz, radius)) continue;

            BlockPos carve = anchorPos.offset(dx, dy, dz);
            if (carve.equals(anchorPos)) continue;
            if (!level.hasChunkAt(carve)) continue;

            BlockState state = level.getBlockState(carve);
            if (carvable(state)) {
                level.setBlock(carve, Blocks.CAVE_AIR.defaultBlockState(), 2);
            }
        }

        anchor.cursor(cursor);

        if (cursor >= total) {
            carveEntrance(level, anchorPos, region);
            anchor.advance();

            level.playSound(
                    null,
                    anchorPos,
                    SoundEvents.DEEPSLATE_BREAK,
                    SoundSource.BLOCKS,
                    0.42F,
                    0.58F
            );
        }
    }

    private static boolean insideUndergroundShape(
            int dx,
            int dy,
            int dz,
            int radius) {

        double ry = UNDERGROUND_HALF_HEIGHT;
        double rz = radius * 0.72;

        double main =
                dx * dx / (double) (radius * radius)
                        + dy * dy / (ry * ry)
                        + dz * dz / (rz * rz);

        double lobeRadius = radius * 0.58;
        double left =
                (dx + radius * 0.48) * (dx + radius * 0.48) / (lobeRadius * lobeRadius)
                        + dy * dy / 36.0
                        + dz * dz / (lobeRadius * lobeRadius);

        double right =
                (dx - radius * 0.42) * (dx - radius * 0.42) / (lobeRadius * lobeRadius)
                        + (dy + 1.5) * (dy + 1.5) / 30.25
                        + dz * dz / (lobeRadius * lobeRadius);

        return main < 1.0 || left < 1.0 || right < 1.0;
    }

    private static void carveEntrance(
            ServerLevel level,
            BlockPos anchor,
            MiningRegionRules.Region region) {

        int surface = level.getHeight(
                Heightmap.Types.WORLD_SURFACE,
                region.entryX(),
                region.entryZ()
        ) - 1;

        int targetY = anchor.getY() + 1;
        int dx = anchor.getX() - region.entryX();
        int dz = anchor.getZ() - region.entryZ();
        int dy = targetY - surface;
        int steps = Math.max(
                1,
                Math.max(
                        Math.max(Math.abs(dx), Math.abs(dz)),
                        Math.abs(dy)
                )
        );

        for (int step = 0; step <= steps; step++) {
            double t = step / (double) steps;
            int x = Mth.floor(region.entryX() + dx * t);
            int y = Mth.floor(surface + dy * t);
            int z = Mth.floor(region.entryZ() + dz * t);

            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = 0; oy <= 2; oy++) {
                    BlockPos carve = new BlockPos(x + ox, y + oy, z);
                    if (!level.hasChunkAt(carve) || carve.equals(anchor)) continue;
                    if (carvable(level.getBlockState(carve))) {
                        level.setBlock(carve, Blocks.CAVE_AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static void oreStep(
            ServerLevel level,
            BlockPos anchorPos,
            MiningRegionAnchorBlockEntity anchor,
            MiningRegionRules.Region region) {

        int radius = anchor.radius();
        int width = radius * 2 + 1;
        int halfHeight = anchor.openPit() ? ORE_HALF_HEIGHT : UNDERGROUND_HALF_HEIGHT + 3;
        int height = halfHeight * 2 + 1;
        int total = width * width * height;
        int cursor = anchor.cursor();
        int budget = 420;

        while (cursor < total && budget-- > 0) {
            int plane = width * width;
            int localY = cursor / plane;
            int rest = cursor % plane;
            int localZ = rest / width;
            int localX = rest % width;
            cursor++;

            int dx = localX - radius;
            int dz = localZ - radius;
            int dy = localY - halfHeight;

            if (dx * dx + dz * dz > radius * radius) continue;

            BlockPos pos = anchorPos.offset(dx, dy + (anchor.openPit() ? 8 : 0), dz);
            if (pos.equals(anchorPos) || !level.hasChunkAt(pos)) continue;

            BlockState state = level.getBlockState(pos);
            if (!naturalRock(state) || !exposed(level, pos)) continue;

            long hash = MiningRegionRules.localHash(
                    level.getSeed(),
                    region,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    0x4F524553L
            );

            int roll = (int) Math.floorMod(hash, 1000L);

            if (roll < 24) {
                level.setBlock(
                        pos,
                        MiningContent.block(anchor.kind()).defaultBlockState(),
                        2
                );

                if (level.getBlockEntity(pos) instanceof ComplexOreBlockEntity complex) {
                    int reserve = 48 + (int) Math.floorMod(hash >>> 11, 113L);
                    complex.initializeReserve(reserve);
                }

            } else if (roll < 205) {
                level.setBlock(
                        pos,
                        anchor.kind().oreState(pos.getY()),
                        2
                );
            }
        }

        anchor.cursor(cursor);

        if (cursor >= total) {
            anchor.advance();
        }
    }

    private static boolean exposed(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!level.hasChunkAt(neighbor)) continue;
            if (level.getBlockState(neighbor).isAir()) return true;
        }
        return false;
    }

    private static void structureProps(
            ServerLevel level,
            BlockPos anchorPos,
            MiningRegionAnchorBlockEntity anchor,
            MiningRegionRules.Region region) {

        if (anchor.openPit()) {
            int floorY = anchorPos.getY() + 4;

            for (int offset : new int[] {-6, 6}) {
                BlockPos post = new BlockPos(
                        anchorPos.getX() + offset,
                        floorY,
                        anchorPos.getZ()
                );

                if (level.getBlockState(post).isAir()) {
                    level.setBlock(post, Blocks.OAK_LOG.defaultBlockState(), 3);
                }

                BlockPos rail = new BlockPos(
                        anchorPos.getX() + offset / 2,
                        floorY,
                        anchorPos.getZ() + 2
                );

                if (level.getBlockState(rail).isAir()
                        && level.getBlockState(rail.below()).isFaceSturdy(level, rail.below(), Direction.UP)) {
                    level.setBlock(rail, Blocks.RAIL.defaultBlockState(), 3);
                }
            }

        } else {
            int radius = anchor.radius();

            for (int x = -radius + 5; x <= radius - 5; x += 7) {
                BlockPos floor = findFloor(level, anchorPos.offset(x, 3, 0), 12);
                if (floor == null) continue;

                int beamY = floor.getY() + 4;

                for (int z : new int[] {-3, 3}) {
                    for (int y = floor.getY() + 1; y <= beamY; y++) {
                        BlockPos support = new BlockPos(
                                anchorPos.getX() + x,
                                y,
                                anchorPos.getZ() + z
                        );

                        if (level.getBlockState(support).isAir()) {
                            level.setBlock(support, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), 2);
                        }
                    }
                }

                for (int z = -3; z <= 3; z++) {
                    BlockPos beam = new BlockPos(
                            anchorPos.getX() + x,
                            beamY,
                            anchorPos.getZ() + z
                    );

                    if (level.getBlockState(beam).isAir()) {
                        level.setBlock(beam, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), 2);
                    }
                }

                BlockPos rail = new BlockPos(
                        anchorPos.getX() + x,
                        floor.getY() + 1,
                        anchorPos.getZ()
                );

                if (level.getBlockState(rail).isAir()
                        && level.getBlockState(rail.below()).isFaceSturdy(level, rail.below(), Direction.UP)) {
                    level.setBlock(rail, Blocks.RAIL.defaultBlockState(), 3);
                }
            }
        }

        anchor.advance();
    }

    private static BlockPos findFloor(
            ServerLevel level,
            BlockPos start,
            int maxDown) {

        BlockPos.MutableBlockPos cursor = start.mutable();

        for (int i = 0; i < maxDown; i++) {
            if (!level.hasChunkAt(cursor)) return null;

            if (!level.getBlockState(cursor).isAir()
                    && level.getBlockState(cursor.above()).isAir()) {
                return cursor.immutable();
            }

            cursor.move(Direction.DOWN);
        }

        return null;
    }

    private static void mobStep(
            ServerLevel level,
            BlockPos anchorPos,
            MiningRegionAnchorBlockEntity anchor,
            MiningRegionRules.Region region) {

        if (!anchor.openPit()) {
            for (int i = 0; i < 2; i++) {
                var bat = EntityType.BAT.create(level);
                if (bat != null) {
                    bat.moveTo(
                            anchorPos.getX() + 0.5 + i * 2.0,
                            anchorPos.getY() + 3.0,
                            anchorPos.getZ() + 0.5,
                            0.0F,
                            0.0F
                    );
                    level.addFreshEntity(bat);
                }
            }

            var zombie = EntityType.ZOMBIE.create(level);
            if (zombie != null) {
                zombie.moveTo(
                        anchorPos.getX() + 4.5,
                        anchorPos.getY() + 1.0,
                        anchorPos.getZ() + 2.5,
                        0.0F,
                        0.0F
                );
                level.addFreshEntity(zombie);
            }
        }

        level.sendParticles(
                ParticleTypes.POOF,
                anchorPos.getX() + 0.5,
                anchorPos.getY() + 2.0,
                anchorPos.getZ() + 0.5,
                8,
                1.0,
                0.5,
                1.0,
                0.03
        );

        anchor.advance();
    }

    private static boolean carvable(BlockState state) {
        return naturalRock(state)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY);
    }

    private static boolean naturalRock(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.IRON_ORE)
                || state.is(Blocks.DEEPSLATE_IRON_ORE)
                || state.is(Blocks.GOLD_ORE)
                || state.is(Blocks.DEEPSLATE_GOLD_ORE)
                || state.is(Blocks.COPPER_ORE)
                || state.is(Blocks.DEEPSLATE_COPPER_ORE)
                || state.is(Blocks.COAL_ORE)
                || state.is(Blocks.DEEPSLATE_COAL_ORE);
    }

    private static double horizontalDistance(
            int x1,
            int z1,
            int x2,
            int z2) {

        double dx = x1 - x2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
