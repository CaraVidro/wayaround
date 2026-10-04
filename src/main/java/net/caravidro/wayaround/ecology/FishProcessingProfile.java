package net.caravidro.wayaround.ecology;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.Item;

/**
 * Data-only processing profile for fish that participate in the physical
 * carcass system. Adding another species should mostly be a matter of adding
 * one enum entry and mapping its meat/whole-fish items here.
 */
public enum FishProcessingProfile {

    SARDINE(
            0,
            0.50F,
            0.78F,
            0.31F,
            0.64F,
            5.0F
    ),

    SALMON(
            1,
            1.05F,
            1.18F,
            0.82F,
            1.36F,
            4.2F
    ),
    COD(2,1.1F,1.0F,.6F,1.1F,4.0F),
    TROPICAL(3,1.1F,1.0F,.6F,1.1F,4.0F),
    PUFFER(4,1.1F,1.0F,.6F,1.1F,4.0F),
    SUNFISH(5,1.1F,1.0F,.6F,1.1F,6.0F),
    SHARK(6,1.1F,1.0F,.6F,1.1F,6.0F),
    MANTA(7,1.1F,1.0F,.6F,1.1F,6.0F),
    BARRACUDA(8,1.1F,1.0F,.6F,1.1F,4.0F),
    SEAHORSE(9,1.1F,1.0F,.6F,1.1F,4.0F),
    JELLYFISH(10,1.1F,1.0F,.6F,1.1F,4.0F),
    OARFISH(11,1.1F,1.0F,.6F,1.1F,6.0F),
    CLOWNFISH(12,1.1F,1.0F,.6F,1.1F,4.0F),
    FLYING_FISH(13,1.1F,1.0F,.6F,1.1F,4.0F),
    LANTERNFISH(14,1.1F,1.0F,.6F,1.1F,4.0F),
    MORAY_EEL(15,1.1F,1.0F,.6F,1.1F,4.0F),
    CARP(16,1.1F,1.0F,.6F,1.1F,4.0F),
    PERCH(17,1.1F,1.0F,.6F,1.1F,4.0F),
    TROUT(18,1.1F,1.0F,.6F,1.1F,4.0F),
    CATFISH(19,1.1F,1.0F,.6F,1.1F,4.0F),
    ARCHERFISH(20,1.1F,1.0F,.6F,1.1F,4.0F),
    ICEFISH(21,1.1F,1.0F,.6F,1.1F,4.0F),
    TOOTHFISH(22,1.1F,1.0F,.6F,1.1F,6.0F),
    ANGLERFISH(23,1.1F,1.0F,.6F,1.1F,4.0F);

    private final int networkId;
    private final float largeThreshold;
    private final float attractiveness;
    private final float smallCarryScale;
    private final float largeCarryScale;
    private final float meatYieldScale;

    FishProcessingProfile(
            int networkId,
            float largeThreshold,
            float attractiveness,
            float smallCarryScale,
            float largeCarryScale,
            float meatYieldScale
    ) {
        this.networkId = networkId;
        this.largeThreshold = largeThreshold;
        this.attractiveness = attractiveness;
        this.smallCarryScale = smallCarryScale;
        this.largeCarryScale = largeCarryScale;
        this.meatYieldScale = meatYieldScale;
    }

    public int networkId() {
        return networkId;
    }

    public float attractiveness() {
        return attractiveness;
    }

    public boolean isLarge(float bodyScale) {
        return bodyScale >= largeThreshold;
    }

    public float carryScale(boolean large) {
        return large
                ? largeCarryScale
                : smallCarryScale;
    }

    public int meatUnits(float bodyScale) {
        int base =
                Math.round(
                        Math.max(
                                0.18F,
                                bodyScale
                        )
                                * meatYieldScale
                );

        return switch (this) {
            case SARDINE ->
                    Math.max(
                            1,
                            Math.min(
                                    4,
                                    base
                            )
                    );
            case SALMON ->
                    Math.max(
                            2,
                            Math.min(
                                    8,
                                    base
                            )
                    );
            default -> Math.max(1,Math.min(12,base));
        };
    }

    public int boneCount(boolean large) {
        return switch (this) {
            case SARDINE ->
                    large
                            ? 2
                            : 1;
            case SALMON ->
                    large
                            ? 4
                            : 2;
            case JELLYFISH -> 0;
            default -> large?3:1;
        };
    }

    public Item rawMeat() {
        return switch (this) {
            case SARDINE ->
                    EcologyContent.RAW_SARDINE_MEAT.get();
            case SALMON ->
                    EcologyContent.RAW_SALMON_MEAT.get();
            default -> FishRemainsItems.raw(this);
        };
    }

    public Item cookedMeat() {
        return switch (this) {
            case SARDINE ->
                    EcologyContent.COOKED_SARDINE_MEAT.get();
            case SALMON ->
                    EcologyContent.COOKED_SALMON_MEAT.get();
            default -> FishRemainsItems.COOKED.get(this).get();
        };
    }

    public Item wholeItem(
            boolean cooked,
            boolean large
    ) {
        return switch (this) {
            case SARDINE ->
                    cooked
                            ? (
                            large
                                    ? EcologyContent.COOKED_LARGE_WHOLE_SARDINE.get()
                                    : EcologyContent.COOKED_WHOLE_SARDINE.get()
                    )
                            : (
                            large
                                    ? EcologyContent.LARGE_WHOLE_SARDINE.get()
                                    : EcologyContent.WHOLE_SARDINE.get()
                    );

            case SALMON ->
                    cooked
                            ? (
                            large
                                    ? EcologyContent.COOKED_LARGE_WHOLE_SALMON.get()
                                    : EcologyContent.COOKED_WHOLE_SALMON.get()
                    )
                            : (
                            large
                                    ? EcologyContent.LARGE_WHOLE_SALMON.get()
                                    : EcologyContent.WHOLE_SALMON.get()
                    );
            default -> FishRemainsItems.WHOLE.get(this).get();
        };
    }

    public static FishProcessingProfile fromFish(
            AbstractFish fish
    ) {
        if (fish instanceof SardineEntity) {
            return SARDINE;
        }

        if (fish.getType() == EntityType.SALMON) {
            return SALMON;
        }

        if(fish instanceof WhaleEntity || fish instanceof CleintonEntity)return null;
        if(fish instanceof RegionalFishEntity regional)return valueOf(regional.species().name());
        if(fish.getType()==EntityType.COD)return COD;
        if(fish.getType()==EntityType.TROPICAL_FISH)return TROPICAL;
        if(fish.getType()==EntityType.PUFFERFISH)return PUFFER;
        if(fish instanceof SunfishEntity)return SUNFISH;
        if(fish instanceof ReefSharkEntity)return SHARK;
        if(fish instanceof MantaRayEntity)return MANTA;
        if(fish instanceof BarracudaEntity)return BARRACUDA;
        if(fish instanceof SeahorseEntity)return SEAHORSE;
        if(fish instanceof JellyfishEntity)return JELLYFISH;
        if(fish instanceof OarfishEntity)return OARFISH;
        if(fish instanceof ClownfishEntity)return CLOWNFISH;
        if(fish instanceof FlyingFishEntity)return FLYING_FISH;
        if(fish instanceof LanternfishEntity)return LANTERNFISH;
        if(fish instanceof MorayEelEntity)return MORAY_EEL;
        return null;
    }

    public static FishProcessingProfile byNetworkId(
            int id
    ) {
        for (FishProcessingProfile profile :
                values()) {
            if (profile.networkId == id) {
                return profile;
            }
        }

        return SARDINE;
    }
}
