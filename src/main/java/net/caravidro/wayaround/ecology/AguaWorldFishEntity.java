package net.caravidro.wayaround.ecology;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Small compatibility layer for Agua World species.
 *
 * All current custom ocean creatures participate in the same ecology engine:
 * food seeking, growth, schooling/migration and fishing. Individual species
 * only need to describe the pieces that are actually different.
 */
public abstract class AguaWorldFishEntity extends AbstractFish {

    protected AguaWorldFishEntity(
            EntityType<? extends AbstractFish> type,
            Level level
    ) {
        super(type, level);

        /*
         * Natural fish must participate in the normal population lifecycle.
         * Older builds forced every fish to persist forever, so spending time
         * in one deep-ocean region could only increase entity count.
         *
         * Bucketed/named/player-persisted fish still use vanilla custom
         * persistence; naturally spawned wildlife is allowed to despawn.
         */
    }

    @Override
    public boolean removeWhenFarAway(
            double distanceToClosestPlayer
    ) {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(
            double distance
    ) {
        double range =
                this instanceof WhaleEntity
                        ? 640.0
                        : (
                        this instanceof OarfishEntity
                                || this instanceof MantaRayEntity
                                ? 420.0
                                : 320.0
                );

        return distance
                <= range * range;
    }

    @Override
    public ItemStack getBucketItemStack() {
        // Buckets are not the primary capture mechanic in WayAround yet.
        return new ItemStack(Items.TROPICAL_FISH_BUCKET);
    }

    @Override
    protected SoundEvent getFlopSound() {
        if (this instanceof BarracudaEntity
                || this instanceof MorayEelEntity) {
            return SoundEvents.GUARDIAN_FLOP;
        }

        if (this instanceof FlyingFishEntity
                || this instanceof OarfishEntity) {
            return SoundEvents.SALMON_FLOP;
        }

        return SoundEvents.TROPICAL_FISH_FLOP;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (this instanceof WhaleEntity) {
            return SoundEvents.DOLPHIN_AMBIENT_WATER;
        }

        if (this instanceof JellyfishEntity
                || this instanceof LanternfishEntity) {
            return SoundEvents.GLOW_SQUID_AMBIENT;
        }

        if (this instanceof BarracudaEntity
                || this instanceof MorayEelEntity) {
            return SoundEvents.GUARDIAN_AMBIENT;
        }

        if (this instanceof FlyingFishEntity
                || this instanceof OarfishEntity) {
            return SoundEvents.SALMON_AMBIENT;
        }

        return SoundEvents.TROPICAL_FISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(
            DamageSource source
    ) {
        if (this instanceof WhaleEntity) {
            return SoundEvents.DOLPHIN_HURT;
        }

        if (this instanceof JellyfishEntity
                || this instanceof LanternfishEntity) {
            return SoundEvents.GLOW_SQUID_HURT;
        }

        if (this instanceof BarracudaEntity
                || this instanceof MorayEelEntity) {
            return SoundEvents.GUARDIAN_HURT;
        }

        if (this instanceof FlyingFishEntity
                || this instanceof OarfishEntity) {
            return SoundEvents.SALMON_HURT;
        }

        return SoundEvents.TROPICAL_FISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        if (this instanceof WhaleEntity) {
            return SoundEvents.DOLPHIN_DEATH;
        }

        if (this instanceof JellyfishEntity
                || this instanceof LanternfishEntity) {
            return SoundEvents.GLOW_SQUID_DEATH;
        }

        if (this instanceof BarracudaEntity
                || this instanceof MorayEelEntity) {
            return SoundEvents.GUARDIAN_DEATH;
        }

        if (this instanceof FlyingFishEntity
                || this instanceof OarfishEntity) {
            return SoundEvents.SALMON_DEATH;
        }

        return SoundEvents.TROPICAL_FISH_DEATH;
    }
}
