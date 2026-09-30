package net.caravidro.wayaround.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.caravidro.wayaround.worldconfig.*;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("wayaround_fish")
@PrefixGameTestTemplate(false)
public final class RegionalFishGameTests {
    @GameTest(template="wayaround_crushing:assembly_test", batch="fish", timeoutTicks=80)
    public static void speciesBucketsAndPersistence(GameTestHelper helper) {
        for (var species : RegionalFishSpecies.values()) {
            var fish = RegionalFishSpecies.TYPES.get(species).get().create(helper.getLevel());
            helper.assertTrue(fish != null && fish.getMaxHealth() == (float) species.health, "Registered attributes " + species);
            fish.getPersistentData().putFloat("WayAroundFishSize", 1.25F);
            fish.getPersistentData().putInt("WayAroundFishMeals", 17);
            fish.getPersistentData().putLong("WayAroundEcologyHome", 42);
            var bucket = fish.getBucketItemStack();
            fish.saveToBucketTag(bucket);
            var other = RegionalFishSpecies.TYPES.get(species).get().create(helper.getLevel());
            other.loadFromBucketTag(bucket.get(DataComponents.BUCKET_ENTITY_DATA).copyTag());
            helper.assertTrue(bucket.is(RegionalFishSpecies.BUCKETS.get(species).get()), "Species-specific bucket");
            helper.assertTrue(other.species() == species && other.getPersistentData().getInt("WayAroundFishMeals") == 17
                    && other.getPersistentData().getFloat("WayAroundFishSize") == 1.25F, "Bucket preserves growth and species");
            helper.assertTrue(!other.getPersistentData().contains("WayAroundEcologyHome"), "Release resets old home");
            CompoundTag saved = new CompoundTag();
            fish.saveWithoutId(saved);
            other.load(saved);
            helper.assertTrue(other.isPersistenceRequired() && other.getPersistentData().getInt("WayAroundFishMeals") == 17,
                    "World save preserves ecology");
            helper.assertTrue(net.caravidro.wayaround.ecology.ai.LivingFaunaManager.meatForFish(fish)
                    .is(RegionalFishSpecies.MEAT.get(species).get()), "Fishing/drop meat identity");
        }
        helper.succeed();
    }
    @GameTest(template="wayaround_crushing:assembly_test", batch="fish", timeoutTicks=80)
    public static void habitatEntriesExist(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (var habitat : FishHabitat.values()) for (String biomeId : habitat.biomes) {
            var biome = registry.get(ResourceLocation.parse(biomeId));
            helper.assertTrue(biome != null, "Biome exists " + biomeId);
            var spawns = biome.getMobSettings().getMobs(MobCategory.WATER_AMBIENT).unwrap();
            for (String fish : habitat.fish) helper.assertTrue(spawns.stream().anyMatch(entry ->
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entry.type).toString().equals(fish)),
                    biomeId + " is missing " + fish);
        }
        helper.succeed();
    }
    @GameTest(template="wayaround_crushing:assembly_test", batch="fish", timeoutTicks=80)
    public static void naturalToggleDoesNotBlockBuckets(GameTestHelper helper) {
        var previous = WorldFeatureRuntime.serverCopy();
        try {
            WorldFeatureRuntime.applyServer(WorldFeatureSettings.allDisabled());
            var type = RegionalFishSpecies.TYPES.get(RegionalFishSpecies.CARP).get();
            var natural = new MobSpawnEvent.SpawnPlacementCheck(type, helper.getLevel(), MobSpawnType.NATURAL,
                    helper.absolutePos(new BlockPos(2,2,2)), helper.getLevel().random, true);
            RegionalFishSpawning.placement(natural);
            helper.assertTrue(!natural.getPlacementCheckResult(), "Feature toggle disables new natural fish");
            var bucket = new MobSpawnEvent.SpawnPlacementCheck(type, helper.getLevel(), MobSpawnType.BUCKET,
                    helper.absolutePos(new BlockPos(2,2,2)), helper.getLevel().random, true);
            RegionalFishSpawning.placement(bucket);
            helper.assertTrue(bucket.getPlacementCheckResult(), "Player release remains available");
        } finally { WorldFeatureRuntime.applyServer(previous); }
        helper.succeed();
    }
}
