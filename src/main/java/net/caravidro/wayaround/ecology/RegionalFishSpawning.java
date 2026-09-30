package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/** Natural habitat limits only: eggs, buckets, commands and existing fish remain usable. */
@EventBusSubscriber(modid = WayAround.MODID)
public final class RegionalFishSpawning {
    private RegionalFishSpawning() {}
    @SubscribeEvent
    public static void placement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION) return;
        String fish = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntityType()).toString();
        if (!FishHabitat.managed(fish)) return;
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_VEGETATION)) {
            if (fish.startsWith("wayaround:")) event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
            return;
        }
        var level = event.getLevel();
        BlockPos pos = event.getPos();
        var habitat = habitatAt(level, pos);
        if (habitat == null) return; // Other mods' biomes retain their own ecology.
        boolean water = level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER);
        if (!water || !habitat.permits(fish, level.getSeaLevel() - pos.getY())) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
        // Never force success: vanilla light, obstruction and spawn rules still apply.
    }
    public static FishHabitat habitatAt(ServerLevelAccessor level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey().map(key -> FishHabitat.forBiome(key.location().toString())).orElse(null);
    }
}
