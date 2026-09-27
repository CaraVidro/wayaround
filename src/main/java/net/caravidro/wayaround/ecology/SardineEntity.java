package net.caravidro.wayaround.ecology;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Small, fast schooling forage fish. */
public final class SardineEntity extends AguaWorldFishEntity {

    public SardineEntity(
            EntityType<? extends SardineEntity> type,
            Level level
    ) {
        super(type, level);
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(Items.COD_BUCKET);
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

    @Override
    public int getMaxSpawnClusterSize() {
        return 40;
    }
}
