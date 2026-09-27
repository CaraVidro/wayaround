package net.caravidro.wayaround.time;

import java.util.HashSet;
import java.util.Set;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.assembly.AssemblyMachine;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Coarse loaded-world sampler. Time is reconstructed from elapsed game ticks,
 * so there is no reason to run aging every tick.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class TimeAgingTicker {

    private static final int INTERVAL = 200;
    private static final int MAX_MACHINES_PER_LEVEL = 160;
    private static final int WORLD_SAMPLES_PER_PLAYER = 8;

    private TimeAgingTicker() {}

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % INTERVAL != 0) {
            return;
        }

        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.TIME_AGING)) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            tickMachines(level);
            tickWorldWeathering(level);
        }
    }

    private static void tickMachines(ServerLevel level) {
        Set<Long> visited = new HashSet<>();
        int sampled = 0;

        outer:
        for (ServerPlayer player : level.players()) {
            int baseX = player.blockPosition().getX() >> 4;
            int baseZ = player.blockPosition().getZ() >> 4;

            for (int cx = baseX - 1; cx <= baseX + 1; cx++) {
                for (int cz = baseZ - 1; cz <= baseZ + 1; cz++) {
                    BlockPos probe = new BlockPos(
                            (cx << 4) + 8,
                            player.blockPosition().getY(),
                            (cz << 4) + 8
                    );

                    if (!level.hasChunkAt(probe)) {
                        continue;
                    }

                    for (BlockEntity blockEntity :
                            level.getChunk(cx, cz)
                                    .getBlockEntities()
                                    .values()) {

                        if (!(blockEntity instanceof AssemblyMachine machine)
                                || !visited.add(blockEntity.getBlockPos().asLong())) {
                            continue;
                        }

                        var sample = TimeAgingEngine.sampleAssembly(
                                level,
                                blockEntity.getBlockPos(),
                                machine,
                                machine.currentAssemblyLoad() > 0.025F
                        );

                        if (sample.wearFraction() > 0.0F) {
                            machine.applyAssemblyWear(sample.wearFraction());
                        }

                        if (++sampled >= MAX_MACHINES_PER_LEVEL) {
                            break outer;
                        }
                    }
                }
            }
        }
    }

    private static void tickWorldWeathering(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            BlockPos origin = player.blockPosition();

            for (int sampleIndex = 0;
                 sampleIndex < WORLD_SAMPLES_PER_PLAYER;
                 sampleIndex++) {

                BlockPos pos = origin.offset(
                        level.random.nextInt(49) - 24,
                        level.random.nextInt(17) - 8,
                        level.random.nextInt(49) - 24
                );

                if (!level.hasChunkAt(pos)) {
                    continue;
                }

                var state = level.getBlockState(pos);
                boolean supported =
                        state.is(Blocks.COBBLESTONE)
                                || state.is(Blocks.STONE_BRICKS)
                                || state.is(Blocks.COBBLESTONE_WALL)
                                || state.is(Blocks.STONE_BRICK_WALL);

                if (!supported) {
                    continue;
                }

                boolean active =
                        pos.distSqr(origin) <= 36.0;

                TemporalState temporal =
                        TimeAgingEngine.sampleWorldSurface(
                                level,
                                pos,
                                active
                        );

                if (temporal.organicGrowth() >= 0.58F) {
                    if (state.is(Blocks.COBBLESTONE)) {
                        level.setBlockAndUpdate(
                                pos,
                                Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                        );
                    } else if (state.is(Blocks.STONE_BRICKS)) {
                        level.setBlockAndUpdate(
                                pos,
                                Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                        );
                    } else if (state.is(Blocks.COBBLESTONE_WALL)) {
                        level.setBlockAndUpdate(
                                pos,
                                Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState()
                        );
                    } else if (state.is(Blocks.STONE_BRICK_WALL)) {
                        level.setBlockAndUpdate(
                                pos,
                                Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState()
                        );
                    }

                    TemporalAgingData.get(level).forget(pos);

                } else if (temporal.weathering() >= 0.72F
                        && temporal.organicGrowth() < 0.25F
                        && state.is(Blocks.STONE_BRICKS)) {

                    level.setBlockAndUpdate(
                            pos,
                            Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                    );

                    TemporalAgingData.get(level).forget(pos);
                }
            }
        }
    }
}
