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
                EcologyContent.REEF_SHARK.get(),
                AbstractFish.createAttributes()
                        .add(Attributes.MAX_HEALTH, 24.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.30)
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
