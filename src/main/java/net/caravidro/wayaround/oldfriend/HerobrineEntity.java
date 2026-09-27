package net.caravidro.wayaround.oldfriend;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * A temporary physical apparition used only for the more obvious late-stage
 * incidents. It is not a normal hostile mob and has no spawn egg.
 */
public final class HerobrineEntity
        extends PathfinderMob {

    public HerobrineEntity(
            EntityType<? extends HerobrineEntity> type,
            Level level
    ) {
        super(
                type,
                level
        );

        this.setPersistenceRequired();

        this.setItemInHand(
                InteractionHand.MAIN_HAND,
                new ItemStack(
                        Items.FLINT_AND_STEEL
                )
        );
    }

    @Override
    protected void registerGoals() {
        // OldFriendManager directs every intentional movement.
    }

    @Override
    public boolean removeWhenFarAway(
            double distanceToClosestPlayer
    ) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(
            double distance
    ) {
        return distance
                <= 256.0 * 256.0;
    }

    @Override
    public boolean hurt(
            DamageSource source,
            float amount
    ) {
        if (source.getEntity() != null) {
            this.discard();
            return true;
        }

        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level()
                .isClientSide) {
            OldFriendManager.tickHerobrine(
                    this
            );
        }
    }
}
