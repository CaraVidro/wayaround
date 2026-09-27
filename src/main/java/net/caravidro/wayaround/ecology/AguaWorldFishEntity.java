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
         * Natural water mobs normally use vanilla distance despawning. Agua
         * World creatures are world inhabitants: unloaded chunks may stop
         * ticking them, but returning to that chunk must bring back the same
         * individual with the same size/meals/history.
         */
        this.setPersistenceRequired();
    }

    @Override
    public boolean removeWhenFarAway(
            double distanceToClosestPlayer
    ) {
        return false;
    }

    @Override
    public ItemStack getBucketItemStack() {
        // Buckets are not the primary capture mechanic in WayAround yet.
        return new ItemStack(Items.TROPICAL_FISH_BUCKET);
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
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.COD_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COD_DEATH;
    }
}
