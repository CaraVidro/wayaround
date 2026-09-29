package net.caravidro.wayaround.ecology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.accessory.AccessoryKind;
import net.caravidro.wayaround.accessory.AccessoryManager;
import net.caravidro.wayaround.accessory.AccessorySlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Player-facing abyss rules for vanilla deep-ocean biomes after Way Around's
 * trench pass has made them substantially deeper.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class DeepOceanManager {

    /*
     * The legacy floating-decoration cleanup is intentionally expensive: it
     * samples many vertical block columns. Once a loaded ocean chunk has been
     * repaired there is no reason to rescan it every two seconds while a player
     * swims in circles. Keep a per-level session cache and pay that cost once.
     */
    private static final Map<ServerLevel, Set<Long>> CLEANED_DECOR_CHUNKS =
            new HashMap<>();

    private DeepOceanManager() {
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        long now =
                event.getServer()
                        .getTickCount();

        if ((now % 10L) != 0L) {
            return;
        }

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

            /*
             * The cleanup is chunk-cached, so checking every ecology pulse is
             * cheap and removes stale floating decorations shortly after the
             * player crosses into a newly visited deep-ocean chunk.
             */
            cleanLegacyFloatingOceanDecorOnce(
                    player.serverLevel(),
                    player.blockPosition()
            );

            if (!player.isUnderWater()) {
                continue;
            }

            boolean divin =
                    AccessoryManager.equipped(
                            player,
                            AccessorySlot.TORSO
                    )
                            == AccessoryKind.DIVIN_SUIT;

            if (divin) {
                player.removeEffect(
                        MobEffects.DARKNESS
                );
                player.removeEffect(
                        MobEffects.BLINDNESS
                );

                player.addEffect(
                        new MobEffectInstance(
                                MobEffects.NIGHT_VISION,
                                260,
                                0,
                                true,
                                false,
                                false
                        )
                );

                continue;
            }

            double depth =
                    player.serverLevel()
                            .getSeaLevel()
                            - player.getY();

            if (depth > 38.0) {
                player.addEffect(
                        new MobEffectInstance(
                                MobEffects.DARKNESS,
                                80,
                                0,
                                true,
                                false,
                                false
                        )
                );
            }

            if (depth > 82.0) {
                player.addEffect(
                        new MobEffectInstance(
                                MobEffects.BLINDNESS,
                                36,
                                0,
                                true,
                                false,
                                false
                        )
                );
            }
        }
    }

    private static void cleanLegacyFloatingOceanDecorOnce(
            ServerLevel level,
            BlockPos center
    ) {
        int chunkX =
                center.getX() >> 4;

        int chunkZ =
                center.getZ() >> 4;

        long key =
                ((long) chunkX << 32)
                        ^ (chunkZ & 0xffffffffL);

        Set<Long> cleaned =
                CLEANED_DECOR_CHUNKS.computeIfAbsent(
                        level,
                        ignored -> new HashSet<>()
                );

        if (!cleaned.add(
                key
        )) {
            return;
        }

        cleanLegacyFloatingOceanDecor(
                level,
                chunkX,
                chunkZ
        );
    }

    private static void cleanLegacyFloatingOceanDecor(
            ServerLevel level,
            int chunkX,
            int chunkZ
    ) {
        int surface =
                level.getSeaLevel()
                        - 1;

        int minFloor =
                Math.max(
                        level.getMinBuildHeight() + 9,
                        -48
                );

        int baseX =
                chunkX << 4;

        int baseZ =
                chunkZ << 4;

        /*
         * The old cleanup sampled every second column and stopped at Y=-18.
         * That was enough to hide some leftovers, but not enough for trenches
         * whose new floor lives around Y=-24..-48. Repair the whole chunk once
         * and only touch natural trench debris / ocean decoration.
         */
        for (int localX = 0;
             localX < 16;
             localX++) {
            for (int localZ = 0;
                 localZ < 16;
                 localZ++) {

                int x =
                        baseX + localX;

                int z =
                        baseZ + localZ;

                BlockPos biomeProbe =
                        new BlockPos(
                                x,
                                Math.min(
                                        surface,
                                        -20
                                ),
                                z
                        );

                if (!isDeepOcean(
                        level,
                        biomeProbe
                )) {
                    continue;
                }

                double waveA =
                        Math.sin(
                                x * 0.031
                                        + z * 0.017
                        );

                double waveB =
                        Math.sin(
                                x * 0.011
                                        - z * 0.027
                        );

                int expectedFloor =
                        net.minecraft.util.Mth.clamp(
                                -38
                                        + (int) Math.round(
                                        waveA * 6.0
                                                + waveB * 5.0
                                ),
                                minFloor,
                                -24
                        );

                int cleanupBottom =
                        expectedFloor + 3;

                BlockPos.MutableBlockPos cursor =
                        new BlockPos.MutableBlockPos(
                                x,
                                surface,
                                z
                        );

                for (int y = surface;
                     y > cleanupBottom;
                     y--) {

                    cursor.setY(
                            y
                    );

                    var state =
                            level.getBlockState(
                                    cursor
                            );

                    if (state.is(
                            Blocks.WATER
                    )
                            || state.isAir()
                            || state.hasBlockEntity()) {
                        continue;
                    }

                    if (!legacyFloatingOceanMaterial(
                            state
                    )) {
                        continue;
                    }

                    level.setBlock(
                            cursor,
                            Blocks.WATER
                                    .defaultBlockState(),
                            2
                    );
                }
            }
        }
    }

    private static boolean legacyFloatingOceanMaterial(
            net.minecraft.world.level.block.state.BlockState state
    ) {
        var id =
                BuiltInRegistries.BLOCK
                        .getKey(
                                state.getBlock()
                        );

        String path =
                id.getPath();

        boolean oceanDecor =
                path.contains(
                        "kelp"
                )
                        || path.contains(
                        "seagrass"
                )
                        || path.contains(
                        "coral"
                )
                        || path.equals(
                        "sea_pickle"
                )
                        || (
                        id.getNamespace()
                                .equals(
                                        WayAround.MODID
                                )
                                && (
                                path.equals(
                                        "sea_cucumber"
                                )
                                        || path.equals(
                                        "sea_sponge"
                                )
                                        || path.equals(
                                        "sea_lettuce"
                                )
                                        || path.equals(
                                        "seagrass_tuft"
                                )
                        )
                );

        if (oceanDecor) {
            return true;
        }

        /*
         * Old trench chunks can also retain pieces of the former natural floor
         * suspended far above the new abyss. Restrict this to terrain blocks;
         * wood, machines, wreckage and block entities are deliberately spared.
         */
        return state.is(
                Blocks.SAND
        )
                || state.is(
                Blocks.GRAVEL
        )
                || state.is(
                Blocks.DIRT
        )
                || state.is(
                Blocks.CLAY
        )
                || state.is(
                Blocks.STONE
        )
                || state.is(
                Blocks.DEEPSLATE
        )
                || state.is(
                Blocks.TUFF
        )
                || state.is(
                Blocks.CALCITE
        )
                || state.is(
                Blocks.GRANITE
        )
                || state.is(
                Blocks.DIORITE
        )
                || state.is(
                Blocks.ANDESITE
        );
    }

    public static boolean isDeepOcean(
            ServerLevel level,
            BlockPos pos
    ) {
        return level.getBiome(
                        pos
                )
                .unwrapKey()
                .map(
                        key -> {
                            String path =
                                    key.location()
                                            .getPath();

                            return path.equals(
                                    "deep_ocean"
                            )
                                    || path.equals(
                                    "deep_cold_ocean"
                            )
                                    || path.equals(
                                    "deep_frozen_ocean"
                            )
                                    || path.equals(
                                    "deep_lukewarm_ocean"
                            );
                        }
                )
                .orElse(
                        false
                );
    }

    @SubscribeEvent
    public static void onServerStopped(
            ServerStoppedEvent event
    ) {
        CLEANED_DECOR_CHUNKS.clear();
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
