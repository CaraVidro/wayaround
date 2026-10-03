package net.caravidro.wayaround.nature;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.ecology.EcologyContent;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid=WayAround.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class BirdSpawnPlacements {
    @SubscribeEvent public static void register(RegisterSpawnPlacementsEvent event) {
        for (var type : java.util.List.of(NatureContent.HUMMINGBIRD.get(), NatureContent.THRUSH.get(), NatureContent.PARROT.get(), NatureContent.CROW.get()))
            event.register(type, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                    (t,l,reason,p,random)->WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_VEGETATION)
                            && l.getRawBrightness(p,0)>=8 && l.getBlockState(p).isAir() && l.getBlockState(p.above()).isAir()
                            && (l.getBlockState(p.below()).is(BlockTags.LEAVES)||l.getBlockState(p.below()).is(BlockTags.DIRT)),
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(EcologyContent.SEAGULL.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t,l,reason,p,random)->WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_VEGETATION)
                        && l.getRawBrightness(p,0)>=8 && l.getBlockState(p).isAir() && l.getBlockState(p.above()).isAir()
                        && l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
