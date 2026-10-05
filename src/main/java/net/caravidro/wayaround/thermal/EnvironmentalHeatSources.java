package net.caravidro.wayaround.thermal;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Cheap opportunistic sampling of ordinary vanilla heat sources.
 *
 * <p>This intentionally samples around players instead of scanning loaded
 * chunks. Fire is visually local gameplay, so unloaded wilderness does not need
 * high-fidelity thermal simulation.</p>
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class EnvironmentalHeatSources {

    private EnvironmentalHeatSources() {
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.THERMAL_SYSTEM
        )
                || event.getServer()
                .getTickCount()
                % 20L != 0L) {
            return;
        }

        for (ServerLevel level :
                event.getServer()
                        .getAllLevels()) {
            for (ServerPlayer player :
                    level.players()) {
                BlockPos origin =
                        player.blockPosition();

                for (int sample = 0;
                     sample < 24;
                     sample++) {
                    BlockPos pos =
                            origin.offset(
                                    level.random.nextInt(
                                            21
                                    ) - 10,
                                    level.random.nextInt(
                                            13
                                    ) - 6,
                                    level.random.nextInt(
                                            21
                                    ) - 10
                            );

                    BlockState state =
                            level.getBlockState(
                                    pos
                            );

                    double target =
                            sourceTemperature(
                                    state
                            );

                    if (target <= 0.0) {
                        continue;
                    }

                    EnvironmentalTemperature.pulseAbsolute(
                            level,
                            Vec3.atCenterOf(
                                    pos
                            ),
                            sourceRadius(
                                    state
                            ),
                            target
                    );
                }
            }
        }
    }

    private static double sourceTemperature(
            BlockState state
    ) {
        if (state.is(
                Blocks.LAVA
        )) {
            return 1100.0;
        }

        if (state.is(
                Blocks.FIRE
        )
                || state.is(
                Blocks.SOUL_FIRE
        )) {
            return 620.0;
        }

        if ((state.is(
                Blocks.CAMPFIRE
        )
                || state.is(
                Blocks.SOUL_CAMPFIRE
        ))
                && state.hasProperty(
                CampfireBlock.LIT
        )
                && state.getValue(
                CampfireBlock.LIT
        )) {
            return 310.0;
        }

        if (state.is(
                Blocks.MAGMA_BLOCK
        )) {
            return 180.0;
        }

        return 0.0;
    }

    private static double sourceRadius(
            BlockState state
    ) {
        if (state.is(
                Blocks.LAVA
        )) {
            return 5.0;
        }

        if (state.is(
                Blocks.FIRE
        )
                || state.is(
                Blocks.SOUL_FIRE
        )) {
            return 3.5;
        }

        return 2.5;
    }
}
