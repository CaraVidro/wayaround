package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.WayAround;
import net.minecraft.world.entity.animal.AbstractFish;
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
    }
}
