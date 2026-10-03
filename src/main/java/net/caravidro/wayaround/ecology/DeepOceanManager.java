package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.performance.PerformanceProfiler;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.accessory.AccessoryKind;
import net.caravidro.wayaround.accessory.AccessoryManager;
import net.caravidro.wayaround.accessory.AccessorySlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Player-facing abyss rules for vanilla deep-ocean biomes after Way Around's
 * trench pass has made them substantially deeper.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class DeepOceanManager {

    private DeepOceanManager() {
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        long wayperfStartedAt =
                PerformanceProfiler.begin(
                        PerformanceProfiler.Section.DEEP_OCEAN
                );

        try {
        long now =
                event.getServer()
                        .getTickCount();

        if ((now % 20L) != 0L) {
            return;
        }

        int cleanupBudget = 4;
        for (ServerPlayer player :
                event.getServer()
                        .getPlayerList()
                        .getPlayers()) {

            if (!player.isAlive()
                    || !isDeepOcean(
                    player
            )) {
                continue;
            }

            if (cleanupBudget-- > 0) {
                cleanLegacyFloatingOceanDecor(
                        player.serverLevel(),
                        player.blockPosition()
                );
            }

            if ((now % 100L) == 0L) {
                trimLegacyDeepOceanPopulation(
                        player.serverLevel(),
                        player.blockPosition()
                );
            }

            if (!player.isUnderWater()) {
                continue;
            }

            boolean divin =
                    AccessoryManager.equipped(
                            player,
                            AccessorySlot.TORSO
                    )
                            == AccessoryKind.DIVIN_SUIT;

            double depth =
                    player.serverLevel()
                            .getSeaLevel()
                            - player.getY();

            /*
             * Old builds granted the diving suit a long Night Vision refresh.
             * Strip that legacy effect immediately in the abyss so it cannot
             * fight the new depth-light curve for several seconds.
             */
            if (divin
                    && player.hasEffect(MobEffects.NIGHT_VISION)) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }

            /*
             * Darkness is now rendered as one smooth client fog curve.
             * The diving suit is pressure equipment, not magical night vision.
             */
            boolean pressureVehicle =
                    player.getVehicle()
                            instanceof DeepSeaCapsuleEntity
                            || player.getVehicle()
                            instanceof DeepSeaSubmarineEntity;

            if (pressureVehicle) {
                player.setAirSupply(
                        player.getMaxAirSupply()
                );
            }

            double safeDepth =
                    pressureVehicle
                            ? Double.MAX_VALUE
                            : divin
                            ? 58.0
                            : 34.0;

            if (depth > safeDepth) {
                float pressureDamage =
                        (float) Math.min(
                                8.0,
                                0.75
                                        + (depth - safeDepth)
                                        / 16.0
                        );

                player.hurt(
                        player.damageSources().generic(),
                        pressureDamage
                );
            }

            if (depth > 46.0) {
                var data =
                        player.getPersistentData();

                long nextSound =
                        data.getLong(
                                "WayAroundNextAbyssSound"
                        );

                if (now >= nextSound) {
                    net.minecraft.sounds.SoundEvent sound =
                            SoundEvents.AMBIENT_CAVE.value();

                    player.serverLevel()
                            .playSound(
                                    null,
                                    player.blockPosition(),
                                    sound,
                                    SoundSource.AMBIENT,
                                    0.38F
                                            + player.getRandom()
                                                    .nextFloat()
                                                    * 0.28F,
                                    0.42F
                                            + player.getRandom()
                                                    .nextFloat()
                                                    * 0.34F
                            );

                    data.putLong(
                            "WayAroundNextAbyssSound",
                            now
                                    + 180L
                                    + player.getRandom()
                                            .nextInt(520)
                    );
                }
            }
        }
    
        } finally {
            PerformanceProfiler.end(
                    PerformanceProfiler.Section.DEEP_OCEAN,
                    wayperfStartedAt
            );
        }
    }

    private static void trimLegacyDeepOceanPopulation(
            ServerLevel level,
            BlockPos center
    ) {
        /*
         * Safety net for worlds that spent time on the old "every fish is
         * persistent forever" builds. New wildlife can despawn normally, but
         * already-saved fish may still carry the persistence flag.
         */
        AABB area =
                new AABB(
                        center
                ).inflate(
                        96.0,
                        56.0,
                        96.0
                );

        List<AguaWorldFishEntity> fish =
                new ArrayList<>(
                        level.getEntitiesOfClass(
                                AguaWorldFishEntity.class,
                                area,
                                entity -> entity.isAlive()
                                        && !entity.hasCustomName()
                        )
                );

        final int hardCap =
                40;

        final int target =
                32;

        if (fish.size() <= hardCap) {
            return;
        }

        fish.sort(
                Comparator.comparingDouble(
                        (AguaWorldFishEntity entity) ->
                                entity.distanceToSqr(
                                        center.getX() + 0.5,
                                        center.getY() + 0.5,
                                        center.getZ() + 0.5
                                )
                ).reversed()
        );

        int remove =
                fish.size()
                        - target;

        for (AguaWorldFishEntity entity :
                fish) {
            if (remove <= 0) {
                break;
            }

            /*
             * Keep the nearby visible ecosystem intact; cull the accumulated
             * outer ring first, which is what hurts simulation/render cost.
             */
            if (entity.distanceToSqr(
                    center.getX() + 0.5,
                    center.getY() + 0.5,
                    center.getZ() + 0.5
            ) < 24.0 * 24.0) {
                continue;
            }

            entity.discard();
            remove--;
        }
    }

    private static void cleanLegacyFloatingOceanDecor(
            ServerLevel level,
            BlockPos center
    ) {
        int surface =
                level.getSeaLevel()
                        - 1;

        // At most four players get one column each per second, rather than
        // 81 full-height columns per player on the same twentieth-second tick.
        int column = Math.floorMod(level.getGameTime() / 20L + center.getX() * 31L + center.getZ(), 81);
        int x = center.getX() - 8 + (column % 9) * 2;
        int z = center.getZ() - 8 + (column / 9) * 2;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, surface, z);
        if (!level.hasChunkAt(cursor) || !isDeepOcean(level, cursor)) return;
        int floor = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
        int bottom = Math.max(level.getMinBuildHeight() + 1, floor + 1);
        int edits = 0;
        for (int y = surface; y >= bottom; y--) {
            cursor.setY(y);
            var state = level.getBlockState(cursor);
            if (state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)) {
                int root = y;
                while (root > level.getMinBuildHeight() + 1) {
                    var lower = level.getBlockState(new BlockPos(x, root - 1, z));
                    if (!lower.is(Blocks.KELP) && !lower.is(Blocks.KELP_PLANT)) break;
                    root--;
                }
                BlockPos rootPos = new BlockPos(x, root, z);
                if (!level.getBlockState(rootPos).canSurvive(level, rootPos)) {
                    for (int remove = y; remove >= root && edits < 48; remove--, edits++) {
                        level.setBlock(new BlockPos(x, remove, z), Blocks.WATER.defaultBlockState(), 50);
                    }
                }
                y = root;
            } else if ((state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS)) && !state.canSurvive(level, cursor)) {
                level.setBlock(cursor, Blocks.WATER.defaultBlockState(), 50);
                edits++;
                if (state.is(Blocks.TALL_SEAGRASS) && level.getBlockState(cursor.below()).is(Blocks.TALL_SEAGRASS)) {
                    level.setBlock(cursor.below(), Blocks.WATER.defaultBlockState(), 50);
                    edits++; y--;
                }
            }
            if (edits >= 48) return;
        }
    }

    public static boolean isDeepOcean(
            ServerLevel level,
            BlockPos pos
    ) {
        return DeepOceanBiomes.contains(level,pos);
    }

    public static boolean isDeepOcean(
            ServerPlayer player
    ) {
        return isDeepOcean(
                player.serverLevel(),
                player.blockPosition()
        );
    }
}
