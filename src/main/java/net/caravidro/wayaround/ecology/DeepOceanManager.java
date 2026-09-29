package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.accessory.AccessoryKind;
import net.caravidro.wayaround.accessory.AccessoryManager;
import net.caravidro.wayaround.accessory.AccessorySlot;
import net.minecraft.server.level.ServerPlayer;
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
                    || !player.isUnderWater()
                    || !isDeepOcean(
                    player
            )) {
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

    public static boolean isDeepOcean(
            ServerPlayer player
    ) {
        return player.serverLevel()
                .getBiome(
                        player.blockPosition()
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
}
