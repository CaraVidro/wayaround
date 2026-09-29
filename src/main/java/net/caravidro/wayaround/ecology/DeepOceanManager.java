package net.caravidro.wayaround.ecology;

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

            if ((now % 40L) == 0L) {
                cleanLegacyFloatingOceanDecor(
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

    private static void cleanLegacyFloatingOceanDecor(
            ServerLevel level,
            BlockPos center
    ) {
        int surface =
                level.getSeaLevel()
                        - 1;

        int bottom =
                Math.max(
                        level.getMinBuildHeight() + 1,
                        -18
                );

        for (int dx = -12;
             dx <= 12;
             dx += 2) {
            for (int dz = -12;
                 dz <= 12;
                 dz += 2) {

                int x =
                        center.getX()
                                + dx;

                int z =
                        center.getZ()
                                + dz;

                BlockPos biomeProbe =
                        new BlockPos(
                                x,
                                Math.min(
                                        surface,
                                        center.getY()
                                ),
                                z
                        );

                if (!isDeepOcean(
                        level,
                        biomeProbe
                )) {
                    continue;
                }

                BlockPos.MutableBlockPos cursor =
                        new BlockPos.MutableBlockPos(
                                x,
                                surface,
                                z
                        );

                for (int y = surface;
                     y >= bottom;
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
                            || state.isAir()) {
                        continue;
                    }

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

    public static boolean isDeepOcean(
            ServerPlayer player
    ) {
        return isDeepOcean(
                player.serverLevel(),
                player.blockPosition()
        );
    }
}
