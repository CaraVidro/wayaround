package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * Preferred-latitude gate for all naturally spawning WayAround fauna.
 *
 * <p>Spawn eggs, buckets, commands and scripted/colony spawns stay usable.
 * Habitat/biome placement rules still run independently.</p>
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class RegionalAnimalSpawning {

    private RegionalAnimalSpawning() {
    }

    @SubscribeEvent
    public static void placement(
            MobSpawnEvent.SpawnPlacementCheck event
    ) {
        if (event.getSpawnType()
                != MobSpawnType.NATURAL
                && event.getSpawnType()
                        != MobSpawnType.CHUNK_GENERATION) {
            return;
        }

        String entityId =
                BuiltInRegistries.ENTITY_TYPE
                        .getKey(
                                event.getEntityType()
                        )
                        .toString();

        if (!AnimalClimateProfile.managed(
                entityId
        )) {
            return;
        }

        BlockPos pos =
                event.getPos();

        var level =
                event.getLevel();

        /*
         * A seagull may visit cold water in real life, but the WayAround gull
         * is the warm/temperate coastal scavenger. Snow/ice beaches are
         * deliberately reserved for later polar bird species.
         */
        if ("wayaround:seagull".equals(
                entityId
        )) {
            String biome =
                    level.getBiome(
                            pos
                    )
                            .unwrapKey()
                            .map(
                                    key ->
                                            key.location()
                                                    .getPath()
                            )
                            .orElse(
                                    ""
                            );

            if (biome.contains(
                    "snow"
            )
                    || biome.contains(
                    "frozen"
            )
                    || biome.contains(
                    "ice"
            )) {
                event.setResult(
                        MobSpawnEvent.SpawnPlacementCheck.Result.FAIL
                );

                return;
            }
        }

        double roll =
                level.getRandom()
                        .nextDouble();

        if (!AnimalClimateProfile.permits(
                entityId,
                pos.getZ(),
                roll
        )) {
            event.setResult(
                    MobSpawnEvent.SpawnPlacementCheck.Result.FAIL
            );
        }
    }
}
