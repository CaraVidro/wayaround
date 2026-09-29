package net.caravidro.wayaround.advancement;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.ecology.SunfishEntity;
import net.caravidro.wayaround.ecology.WhaleCarcassEntity;
import net.caravidro.wayaround.worldgen.geography.AntarcticField;
import net.caravidro.wayaround.worldgen.weather.fire.SmokeVolumeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * "Vista" emblems are deliberately tiny observational achievements.
 * They record that the player actually encountered a Way Around phenomenon,
 * without explaining hidden systems or rewarding mechanical knowledge.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class VistaAdvancementManager {

    private VistaAdvancementManager() {
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        long tick =
                event.getServer()
                        .getTickCount();

        if (tick % 20L != 0L) {
            return;
        }

        for (ServerPlayer player :
                event.getServer()
                        .getPlayerList()
                        .getPlayers()) {

            ServerLevel level =
                    player.serverLevel();

            if (level.dimension()
                    .equals(
                            Level.OVERWORLD
                    )
                    && AntarcticField.isAntarctic(
                    player.getBlockX(),
                    player.getBlockZ()
            )) {
                WayAroundAdvancements.vistaAntarctica(
                        player
                );
            }

            if (!level.getEntitiesOfClass(
                    WhaleCarcassEntity.class,
                    player.getBoundingBox()
                            .inflate(
                                    36.0
                            ),
                    carcass ->
                            carcass.isAlive()
                                    && player.hasLineOfSight(
                                    carcass
                            )
            ).isEmpty()) {
                WayAroundAdvancements.vistaWhaleCarcass(
                        player
                );
            }

            if (!level.getEntitiesOfClass(
                    SunfishEntity.class,
                    player.getBoundingBox()
                            .inflate(
                                    28.0
                            ),
                    fish ->
                            fish.isAlive()
                                    && player.hasLineOfSight(
                                    fish
                            )
            ).isEmpty()) {
                WayAroundAdvancements.vistaSunfish(
                        player
                );
            }

            if (!level.getEntitiesOfClass(
                    SmokeVolumeEntity.class,
                    player.getBoundingBox()
                            .inflate(
                                    128.0
                            )
            ).isEmpty()) {
                WayAroundAdvancements.vistaWildfireSmoke(
                        player
                );
            }
        }
    }
}
