package net.caravidro.wayaround.ecology;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Ocean sunfish. V1 deliberately reuses vanilla fish AI/animation semantics;
 * its unusual body is supplied by a lightweight custom renderer.
 */
public final class SunfishEntity extends AguaWorldFishEntity {

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
    public boolean hurt(
            DamageSource source,
            float amount
    ) {
        /*
         * A direct attack scars the sunfish rather than letting repeated sword
         * hits simply delete this giant animal. Environmental/non-entity damage
         * still uses normal Minecraft rules.
         */
        if (source.getEntity() != null
                && this.isAlive()) {
            float floor =
                    this.getMaxHealth()
                            * 0.58F;

            float available =
                    Math.max(
                            0.0F,
                            this.getHealth()
                                    - floor
                    );

            float scarDamage =
                    Math.max(
                            0.20F,
                            Math.min(
                                    amount * 0.14F,
                                    this.getMaxHealth()
                                            * 0.075F
                            )
                    );

            float applied =
                    Math.min(
                            available,
                            scarDamage
                    );

            boolean result =
                    super.hurt(
                            source,
                            applied > 0.0F
                                    ? applied
                                    : 0.01F
                    );

            if (this.getHealth()
                    < floor) {
                this.setHealth(
                        floor
                );
            }

            return result;
        }

        return super.hurt(
                source,
                amount
        );
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 3;
    }
}
