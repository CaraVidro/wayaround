package net.caravidro.wayaround.ecology;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Passive coastal crab that mostly wanders around beaches and river banks. */
public final class CrabEntity extends PathfinderMob {

    public CrabEntity(
            EntityType<? extends CrabEntity> type,
            Level level
    ) {
        super(type, level);
        this.setPersistenceRequired();
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
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.45));
        goalSelector.addGoal(4, new RandomStrollGoal(this, 0.72, 70));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 5.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TURTLE_AMBIENT_LAND;
    }

    @Override
    protected SoundEvent getHurtSound(
            net.minecraft.world.damagesource.DamageSource source
    ) {
        return SoundEvents.TURTLE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TURTLE_DEATH;
    }
}
