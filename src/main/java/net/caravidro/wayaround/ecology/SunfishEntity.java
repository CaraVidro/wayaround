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
 * Ocean sunfish. V1 deliberately reuses vanilla fish AI/animation semantics;
 * its unusual body is supplied by a lightweight custom renderer.
 */
public final class SunfishEntity extends AbstractFish {

    public SunfishEntity(
            EntityType<? extends SunfishEntity> type,
            Level level
    ) {
        super(
                type,
                level
        );
    }

    @Override
    public ItemStack getBucketItemStack() {
        /*
         * Temporary bucket fallback until a dedicated sunfish bucket item is
         * worth adding. Natural/fishing gameplay does not depend on buckets.
         */
        return new ItemStack(
                Items.TROPICAL_FISH_BUCKET
        );
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.COD_FLOP;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.COD_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(
            DamageSource source
    ) {
        return SoundEvents.COD_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COD_DEATH;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 3;
    }
}
