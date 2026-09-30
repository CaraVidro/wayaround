package net.caravidro.wayaround.ecology;

import java.util.EnumMap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Species definitions also drive registration, attributes, buckets and rendering. */
public enum RegionalFishSpecies {
    CARP("carp", 0x8B713D, 0xD9B76C, 0.72F, 0.4F, 6, 0.23),
    PERCH("perch", 0x647B3E, 0xD77B32, 0.58F, 0.36F, 5, 0.28),
    TROUT("trout", 0x748B82, 0xD47E91, 0.72F, 0.3F, 5, 0.34),
    CATFISH("catfish", 0x504D43, 0xC1B591, 0.85F, 0.32F, 8, 0.22),
    ARCHERFISH("archerfish", 0xC7CAB2, 0x252C31, 0.5F, 0.34F, 4, 0.28),
    ICEFISH("icefish", 0xBFD5D4, 0x678A9D, 0.65F, 0.28F, 5, 0.24),
    TOOTHFISH("toothfish", 0x444D58, 0x9DADB5, 1.25F, 0.48F, 16, 0.25),
    ANGLERFISH("anglerfish", 0x24212F, 0x9BEBCB, 0.72F, 0.5F, 10, 0.18);

    public final String id;
    public final int bodyColor, accentColor;
    public final float width, height;
    public final double health, speed;
    RegionalFishSpecies(String id, int bodyColor, int accentColor, float width, float height,
                        double health, double speed) {
        this.id = id; this.bodyColor = bodyColor; this.accentColor = accentColor;
        this.width = width; this.height = height; this.health = health; this.speed = speed;
    }
    public static final EnumMap<RegionalFishSpecies, DeferredHolder<EntityType<?>, EntityType<RegionalFishEntity>>> TYPES = new EnumMap<>(RegionalFishSpecies.class);
    public static final EnumMap<RegionalFishSpecies, DeferredItem<SpawnEggItem>> EGGS = new EnumMap<>(RegionalFishSpecies.class);
    public static final EnumMap<RegionalFishSpecies, DeferredItem<MobBucketItem>> BUCKETS = new EnumMap<>(RegionalFishSpecies.class);
    public static final EnumMap<RegionalFishSpecies, DeferredItem<Item>> MEAT = new EnumMap<>(RegionalFishSpecies.class);
    public static final EnumMap<RegionalFishSpecies, DeferredItem<Item>> COOKED = new EnumMap<>(RegionalFishSpecies.class);
    static {
        for (var species : values()) {
            TYPES.put(species, EcologyContent.ENTITIES.register(species.id, () -> EntityType.Builder
                    .<RegionalFishEntity>of((type, level) -> species == TOOTHFISH || species == ANGLERFISH
                            ? new RegionalPredatorEntity(type, level, species)
                            : new RegionalFishEntity(type, level, species), MobCategory.WATER_AMBIENT)
                    .sized(species.width, species.height).clientTrackingRange(10)
                    .build("wayaround:" + species.id)));
            EGGS.put(species, EcologyContent.ITEMS.register(species.id + "_spawn_egg", () ->
                    new SpawnEggItem(TYPES.get(species).get(), species.bodyColor, species.accentColor, new Item.Properties())));
            BUCKETS.put(species, EcologyContent.ITEMS.register(species.id + "_bucket", () ->
                    new MobBucketItem(TYPES.get(species).get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH,
                            new Item.Properties().stacksTo(1))));
            MEAT.put(species, EcologyContent.ITEMS.register("raw_" + species.id + "_meat", () ->
                    new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(.15F).build()))));
            COOKED.put(species, EcologyContent.ITEMS.register("cooked_" + species.id + "_meat", () ->
                    new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(5).saturationModifier(.6F).build()))));
        }
    }
    public static void bootstrap() { /* forces registration before bus attachment */ }
    public int clusterSize() { return this == ANGLERFISH || this == TOOTHFISH ? 2 : 6; }
}
