package net.caravidro.wayaround.thermal;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Cheap opportunistic discovery of ordinary vanilla heat sources.
 *
 * <p>Discovery stays bounded around players. Once a source is found, physical
 * power/temperature and enclosure behavior are handled by the universal
 * thermal API rather than by block-specific target-temperature pulses.</p>
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

                    EnvironmentalTemperature.applyBlockSource(
                            level,
                            pos,
                            state
                    );
                }
            }
        }
    }
}
