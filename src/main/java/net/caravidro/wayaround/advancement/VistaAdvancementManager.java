package net.caravidro.wayaround.advancement;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.ecology.JellyfishEntity;
import net.caravidro.wayaround.ecology.OarfishEntity;
import net.caravidro.wayaround.ecology.SunfishEntity;
import net.caravidro.wayaround.ecology.WhaleCarcassEntity;
import net.caravidro.wayaround.ecology.WhaleEntity;
import net.caravidro.wayaround.oldfriend.HerobrineEntity;
import net.caravidro.wayaround.worldgen.geography.AntarcticField;
import net.caravidro.wayaround.worldgen.weather.fire.SmokeVolumeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
                    )) {

                if (AntarcticField.isSouthernOcean(
                        player.getBlockX(),
                        player.getBlockZ()
                )) {
                    WayAroundAdvancements.vistaSouthernOcean(
                            player
                    );
                }

                if (AntarcticField.isAntarctic(
                        player.getBlockX(),
                        player.getBlockZ()
                )) {
                    WayAroundAdvancements.vistaAntarctica(
                            player
                    );
                }
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

            var visibleSunfish =
                    level.getEntitiesOfClass(
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
                    );

            if (!visibleSunfish.isEmpty()) {
                WayAroundAdvancements.vistaSunfish(
                        player
                );
            }

            if (visibleSunfish.stream()
                    .anyMatch(
                            fish ->
                                    fish.isBasking()
                                            && actuallySeen(
                                            player,
                                            fish,
                                            34.0,
                                            0.42
                                    )
                    )) {
                WayAroundAdvancements.vistaSunfishBasking(
                        player
                );
            }

            if (level.getEntitiesOfClass(
                    SmokeVolumeEntity.class,
                    player.getBoundingBox()
                            .inflate(
                                    128.0
                            )
            ).stream()
                    .anyMatch(
                            smoke ->
                                    actuallySeen(
                                            player,
                                            smoke,
                                            128.0,
                                            0.50
                                    )
                    )) {
                WayAroundAdvancements.vistaWildfireSmoke(
                        player
                );
            }

            if (!level.getEntitiesOfClass(
                    JellyfishEntity.class,
                    player.getBoundingBox()
                            .inflate(
                                    26.0
                            ),
                    jelly ->
                            jelly.isAlive()
                                    && player.hasLineOfSight(
                                    jelly
                            )
            ).isEmpty()) {
                WayAroundAdvancements.vistaJellyfish(
                        player
                );
            }

            if (!level.getEntitiesOfClass(
                    OarfishEntity.class,
                    player.getBoundingBox()
                            .inflate(
                                    34.0
                            ),
                    fish ->
                            fish.isAlive()
                                    && player.hasLineOfSight(
                                    fish
                            )
            ).isEmpty()) {
                WayAroundAdvancements.vistaOarfish(
                        player
                );
            }

            if (!level.getEntitiesOfClass(
                    WhaleEntity.class,
                    player.getBoundingBox()
                            .inflate(
                                    54.0
                            ),
                    whale ->
                            whale.isAlive()
                                    && player.hasLineOfSight(
                                    whale
                            )
            ).isEmpty()) {
                WayAroundAdvancements.vistaWhale(
                        player
                );
            }

            if (!level.getEntitiesOfClass(
                    HerobrineEntity.class,
                    player.getBoundingBox()
                            .inflate(
                                    48.0
                            ),
                    oldFriend ->
                            oldFriend.isAlive()
                                    && player.hasLineOfSight(
                                    oldFriend
                            )
            ).isEmpty()) {
                WayAroundAdvancements.vistaOldFriend(
                        player
                );
            }

        }
    }

    private static boolean actuallySeen(
            ServerPlayer player,
            Entity target,
            double maxDistance,
            double minimumDot
    ) {
        Vec3 eye =
                player.getEyePosition();

        Vec3 targetPoint =
                target.getBoundingBox()
                        .getCenter();

        Vec3 delta =
                targetPoint.subtract(
                        eye
                );

        double distanceSquared =
                delta.lengthSqr();

        if (distanceSquared
                > maxDistance
                        * maxDistance
                || distanceSquared
                        < 0.0001) {
            return false;
        }

        Vec3 direction =
                delta.normalize();

        double dot =
                player.getLookAngle()
                        .normalize()
                        .dot(
                                direction
                        );

        return dot >= minimumDot
                && player.hasLineOfSight(
                target
        );
    }
}
