package net.caravidro.wayaround.ecology;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** Uses the existing feeding, fishing, growth and home/persistence systems. */
public class RegionalFishEntity extends AguaWorldFishEntity {
    private final RegionalFishSpecies species;
    public RegionalFishEntity(EntityType<? extends RegionalFishEntity> type, Level level, RegionalFishSpecies species) {
        super(type, level);
        this.species = species;
    }
    public RegionalFishSpecies species() { return species; }
    @Override public int getMaxSpawnClusterSize() { return species.clusterSize(); }
    @Override public ItemStack getBucketItemStack() {
        return new ItemStack(RegionalFishSpecies.BUCKETS.get(species).get());
    }
    @Override public void saveToBucketTag(ItemStack bucket) {
        super.saveToBucketTag(bucket);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> {
            CompoundTag ecology = new CompoundTag();
            // Home is deliberately reset when released in a new pond; size and meals survive.
            for (String key : getPersistentData().getAllKeys()) {
                if (key.startsWith("WayAroundFish") && !key.endsWith("Until"))
                    ecology.put(key, getPersistentData().get(key).copy());
            }
            tag.put("RegionalEcology", ecology);
        });
    }
    @Override public void loadFromBucketTag(CompoundTag tag) {
        super.loadFromBucketTag(tag);
        getPersistentData().merge(tag.getCompound("RegionalEcology"));
    }
}
