package net.caravidro.wayaround.worldgen.weather.local;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Small ecological reactions to an approaching storm.
 *
 * This deliberately avoids replacing animal AI. It only nudges attention,
 * ambient sound and occasional short movement when the local weather field
 * becomes threatening.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class LivingWeatherEcology {

    private LivingWeatherEcology() {
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.LIVING_WEATHER
        )) {
            return;
        }

        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.dimension().equals(Level.OVERWORLD)) {
                continue;
            }

            Set<UUID> touched = new HashSet<>();

            for (var player : level.players()) {
                if (level.getBiome(player.blockPosition())
                        .is(WayAroundBiomes.ANTARCTIC_ICE_SHEET)) {
                    continue;
                }

                LocalWeatherField.Sample weather =
                        LocalWeatherField.sample(
                        level,
                                player.getX(),
                                player.getZ(),
                                level.getGameTime()
                        );

                float alert = weather.warning();

                if (alert < 0.20F) {
                    continue;
                }

                AABB area =
                        player.getBoundingBox()
                                .inflate(30.0, 14.0, 30.0);

                for (Animal animal :
                        level.getEntitiesOfClass(
                                Animal.class,
                                area
                        )) {

                    if (!touched.add(animal.getUUID())) {
                        continue;
                    }

                    /*
                     * The threatening cloud is generally upwind, so animals
                     * occasionally glance against the wind vector.
                     */
                    double lookX =
                            animal.getX()
                            - weather.windX() * 22.0;

                    double lookZ =
                            animal.getZ()
                            - weather.windZ() * 22.0;

                    double lookY =
                            animal.getEyeY()
                            + 5.0
                            + alert * 7.0;

                    animal.getLookControl().setLookAt(
                            lookX,
                            lookY,
                            lookZ,
                            28.0F,
                            24.0F
                    );

                    /*
                     * A storm front makes the biome audibly busier without
                     * turning every animal into a panic machine.
                     */
                    if (level.random.nextFloat() < 0.055F * alert) {
                        animal.playAmbientSound();
                    }

                    /*
                     * Strong fronts may make an animal reposition itself a
                     * few blocks. The normal goal system takes over again
                     * immediately afterwards.
                     */
                    if (alert > 0.56F
                            && level.random.nextFloat() < 0.055F * alert) {

                        double sideX = -weather.windZ();
                        double sideZ = weather.windX();

                        double forward =
                                (level.random.nextDouble() - 0.35)
                                * 5.0;

                        double side =
                                (level.random.nextDouble() - 0.5)
                                * 7.0;

                        animal.getNavigation().moveTo(
                                animal.getX()
                                        + weather.windX() * forward
                                        + sideX * side,
                                animal.getY(),
                                animal.getZ()
                                        + weather.windZ() * forward
                                        + sideZ * side,
                                0.95 + alert * 0.28
                        );
                    }
                }
            }
        }
    }
}
