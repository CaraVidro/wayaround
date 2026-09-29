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
    );

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
        };
    }

    public Item rawMeat() {
        return switch (this) {
            case SARDINE ->
                    EcologyContent.RAW_SARDINE_MEAT.get();
            case SALMON ->
                    EcologyContent.RAW_SALMON_MEAT.get();
        };
    }

    public Item cookedMeat() {
        return switch (this) {
            case SARDINE ->
                    EcologyContent.COOKED_SARDINE_MEAT.get();
            case SALMON ->
                    EcologyContent.COOKED_SALMON_MEAT.get();
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
