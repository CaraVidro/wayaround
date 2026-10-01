package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.WayAround;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.tags.FluidTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        bus = EventBusSubscriber.Bus.MOD
)
public final class EcologyEntityAttributes {

    private EcologyEntityAttributes() {
    }

    @SubscribeEvent
    public static void spawnPlacements(
            RegisterSpawnPlacementsEvent event
    ) {
        for (var species : RegionalFishSpecies.values()) {
            event.register(RegionalFishSpecies.TYPES.get(species).get(), SpawnPlacementTypes.IN_WATER,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, reason, pos, random) -> level.getFluidState(pos).is(FluidTags.WATER)
                            && level.getFluidState(pos.above()).is(FluidTags.WATER),
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
        event.register(
                EcologyContent.SUNFISH.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (
                        type,
                        level,
                        reason,
                        pos,
                        random
                ) ->
                        level.getFluidState(
                                pos
                        ).is(
                                FluidTags.WATER
                        )
                                && level.getFluidState(
                                pos.above()
                        ).is(
                                FluidTags.WATER
                        ),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );


        event.register(
                EcologyContent.SARDINE.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER)
                                && level.getFluidState(pos.above()).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                EcologyContent.REEF_SHARK.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER)
                                && level.getFluidState(pos.above()).is(FluidTags.WATER)
                                && level.getFluidState(pos.below()).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                EcologyContent.MANTA_RAY.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER)
                                && level.getFluidState(pos.above()).is(FluidTags.WATER)
                                && level.getFluidState(pos.below()).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                EcologyContent.BARRACUDA.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER)
                                && level.getFluidState(pos.above()).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                EcologyContent.SEAHORSE.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                EcologyContent.JELLYFISH.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER)
                                && level.getFluidState(pos.above()).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                EcologyContent.OARFISH.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER)
                                && level.getFluidState(pos.above()).is(FluidTags.WATER)
                                && level.getFluidState(pos.below()).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                EcologyContent.CLOWNFISH.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                EcologyContent.FLYING_FISH.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                EcologyContent.LANTERNFISH.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER)
                                && level.getFluidState(pos.above()).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                EcologyContent.MORAY_EEL.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                EcologyContent.WHALE.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER)
                                && level.getFluidState(pos.above()).is(FluidTags.WATER)
                                && level.getFluidState(pos.below()).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                EcologyContent.SPERM_WHALE.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER)
                                && level.getFluidState(pos.above()).is(FluidTags.WATER)
                                && level.getFluidState(pos.below()).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                EcologyContent.SEAGULL.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> {
                    var floor =
                            level.getBlockState(
                                    pos.below()
                            );

                    boolean shore =
                            floor.is(Blocks.SAND)
                                    || floor.is(Blocks.GRAVEL)
                                    || floor.is(Blocks.STONE)
                                    || floor.is(Blocks.SNOW_BLOCK);

                    if (!shore) {
                        return false;
                    }

                    for (int dx = -5; dx <= 5; dx++) {
                        for (int dz = -5; dz <= 5; dz++) {
                            if (level.getFluidState(
                                    pos.offset(dx, -1, dz)
                            ).is(FluidTags.WATER)
                                    || level.getFluidState(
                                    pos.offset(dx, 0, dz)
                            ).is(FluidTags.WATER)) {
                                return true;
                            }
                        }
                    }

                    return false;
                },
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                EcologyContent.CRAB.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> {
                    var floor =
                            level.getBlockState(
                                    pos.below()
                            );

                    boolean shoreFloor =
                            floor.is(Blocks.SAND)
                                    || floor.is(Blocks.GRAVEL)
                                    || floor.is(Blocks.STONE)
                                    || floor.is(Blocks.MUD);

                    if (!shoreFloor) {
                        return false;
                    }

                    for (var direction :
                            net.minecraft.core.Direction.Plane.HORIZONTAL) {
                        if (level.getFluidState(
                                pos.relative(direction)
                        ).is(FluidTags.WATER)
                                || level.getFluidState(
                                pos.relative(direction).below()
                        ).is(FluidTags.WATER)) {
                            return true;
                        }
                    }

                    return false;
                },
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    @SubscribeEvent
    public static void attributes(
            EntityAttributeCreationEvent event
    ) {
        for (var species : RegionalFishSpecies.values()) {
            event.put(RegionalFishSpecies.TYPES.get(species).get(), AbstractFish.createAttributes()
                    .add(Attributes.MAX_HEALTH, species.health).add(Attributes.MOVEMENT_SPEED, species.speed).build());
        }
        event.put(
                EcologyContent.SUNFISH.get(),
                AbstractFish.createAttributes()
                        .build()
        );

        event.put(
                EcologyContent.SARDINE.get(),
                AbstractFish.createAttributes()
                        .add(Attributes.MAX_HEALTH, 2.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.34)
                        .build()
        );

        event.put(
                EcologyContent.FISH_CARCASS.get(),
                Mob.createMobAttributes()
                        .add(Attributes.MAX_HEALTH, 1.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.0)
                        .build()
        );

        event.put(
                EcologyContent.REEF_SHARK.get(),
                AbstractFish.createAttributes()
                        .add(Attributes.MAX_HEALTH, 24.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.30)
                        .build()
        );

        event.put(
                EcologyContent.MANTA_RAY.get(),
                AbstractFish.createAttributes()
                        .add(Attributes.MAX_HEALTH, 18.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.24)
                        .build()
        );

        event.put(
                EcologyContent.BARRACUDA.get(),
                AbstractFish.createAttributes()
                        .add(Attributes.MAX_HEALTH, 12.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.38)
                        .build()
        );

        event.put(
                EcologyContent.SEAHORSE.get(),
                AbstractFish.createAttributes()
                        .add(Attributes.MAX_HEALTH, 3.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.16)
                        .build()
        );

        event.put(
                EcologyContent.JELLYFISH.get(),
                AbstractFish.createAttributes()
                        .add(Attributes.MAX_HEALTH, 6.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.12)
                        .build()
        );

        event.put(
                EcologyContent.OARFISH.get(),
                AbstractFish.createAttributes()
                        .add(Attributes.MAX_HEALTH, 26.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.20)
                        .build()
        );

        event.put(
                EcologyContent.CLOWNFISH.get(),
                AbstractFish.createAttributes()
                        .add(Attributes.MAX_HEALTH, 4.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.28)
                        .build()
        );

        event.put(
                EcologyContent.FLYING_FISH.get(),
                AbstractFish.createAttributes()
                        .add(Attributes.MAX_HEALTH, 5.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.34)
                        .build()
        );

        event.put(
                EcologyContent.LANTERNFISH.get(),
                AbstractFish.createAttributes()
                        .add(Attributes.MAX_HEALTH, 4.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.25)
                        .build()
        );

        event.put(
                EcologyContent.CLEINTON.get(),
                Mob.createMobAttributes()
                        .add(Attributes.MAX_HEALTH, 20.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.26)
                        .add(Attributes.FOLLOW_RANGE, 24.0)
                        .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                        .build()
        );

        event.put(
                EcologyContent.MORAY_EEL.get(),
                AbstractFish.createAttributes()
                        .add(Attributes.MAX_HEALTH, 18.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.27)
                        .build()
        );

        event.put(
                EcologyContent.WHALE.get(),
                AbstractFish.createAttributes()
                        .add(Attributes.MAX_HEALTH, 80.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.18)
                        .build()
        );

        event.put(
                EcologyContent.SPERM_WHALE.get(),
                AbstractFish.createAttributes()
                        .add(Attributes.MAX_HEALTH, 140.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.16)
                        .build()
        );

        event.put(
                EcologyContent.WHALE_CARCASS.get(),
                Mob.createMobAttributes()
                        .add(Attributes.MAX_HEALTH, 40.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.0)
                        .build()
        );

        event.put(
                EcologyContent.SPERM_WHALE_CARCASS.get(),
                Mob.createMobAttributes()
                        .add(Attributes.MAX_HEALTH, 56.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.0)
                        .build()
        );

        event.put(
                EcologyContent.SEAGULL.get(),
                Mob.createMobAttributes()
                        .add(Attributes.MAX_HEALTH, 8.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.26)
                        .add(Attributes.FLYING_SPEED, 0.46)
                        .add(Attributes.FOLLOW_RANGE, 32.0)
                        .build()
        );

        event.put(
                EcologyContent.CRAB.get(),
                Mob.createMobAttributes()
                        .add(Attributes.MAX_HEALTH, 7.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.18)
                        .build()
        );
    }
}
