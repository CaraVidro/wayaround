package net.caravidro.wayaround.advancement;

import java.util.function.Predicate;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.domain.VoidDomainManager;
import net.caravidro.wayaround.ecology.JellyfishEntity;
import net.caravidro.wayaround.ecology.OarfishEntity;
import net.caravidro.wayaround.ecology.WhaleEntity;
import net.caravidro.wayaround.ecology.ai.LivingFaunaManager;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Pufferfish;
import net.caravidro.wayaround.ecology.SunfishEntity;
import net.caravidro.wayaround.ecology.WhaleCarcassEntity;
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
 * Vista emblems are observations, not passive proximity detectors.
 *
 * Creature emblems require the player to be reasonably close, have line of
 * sight and actually face the subject. Large landscape phenomena keep their
 * broader rules because the "subject" is the environment itself.
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

            /*
             * Void pocket-space physically lives at remote Overworld
             * coordinates. Never interpret those technical coordinates as
             * real geography: otherwise entering a Domain can accidentally
             * unlock Southern Ocean / Antarctica Vista emblems.
             */
            if (level.dimension()
                    .equals(
                            Level.OVERWORLD
                    )
                    && !VoidDomainManager.isInsideDomain(
                    player
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

            if (seenNearby(
                    level,
                    player,
                    WhaleCarcassEntity.class,
                    26.0,
                    0.55,
                    carcass -> true
            )) {
                WayAroundAdvancements.vistaWhaleCarcass(
                        player
                );
            }

            if (seenNearby(
                    level,
                    player,
                    SunfishEntity.class,
                    22.0,
                    0.55,
                    fish -> true
            )) {
                WayAroundAdvancements.vistaSunfish(
                        player
                );
            }

            if (seenNearby(
                    level,
                    player,
                    SunfishEntity.class,
                    20.0,
                    0.68,
                    SunfishEntity::isBasking
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

            if (seenNearby(
                    level,
                    player,
                    Pufferfish.class,
                    14.0,
                    0.62,
                    puffer -> true
            )) {
                WayAroundAdvancements.vistaPufferHint(
                        player
                );
            }

            if (seenNearby(
                    level,
                    player,
                    JellyfishEntity.class,
                    18.0,
                    0.55,
                    jelly -> true
            )) {
                WayAroundAdvancements.vistaJellyfish(
                        player
                );
            }

            if (seenNearby(
                    level,
                    player,
                    AbstractFish.class,
                    30.0,
                    0.55,
                    fish ->
                            !(fish instanceof WhaleEntity)
                                    && LivingFaunaManager.fishSize(
                                    fish
                            ) >= 4.0F
            )) {
                WayAroundAdvancements.vistaGiantFish(
                        player
                );
            }

            if (seenNearby(
                    level,
                    player,
                    WhaleEntity.class,
                    38.0,
                    0.42,
                    whale -> true
            )) {
                WayAroundAdvancements.vistaWhale(
                        player
                );
            }

            if (seenNearby(
                    level,
                    player,
                    HerobrineEntity.class,
                    34.0,
                    0.60,
                    oldFriend -> true
            )) {
                WayAroundAdvancements.vistaOldFriend(
                        player
                );
            }
        }
    }

    private static <T extends Entity> boolean seenNearby(
            ServerLevel level,
            ServerPlayer player,
            Class<T> type,
            double maxDistance,
            double minimumDot,
            Predicate<T> extra
    ) {
        return level.getEntitiesOfClass(
                        type,
                        player.getBoundingBox()
                                .inflate(
                                        maxDistance
                                ),
                        entity ->
                                entity.isAlive()
                                        && extra.test(
                                        entity
                                )
                                        && actuallySeen(
                                        player,
                                        entity,
                                        maxDistance,
                                        minimumDot
                                )
                )
                .stream()
                .findAny()
                .isPresent();
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
