package net.caravidro.wayaround.ecology;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Coastal bird used by Agua World's shared marine interaction layer.
 *
 * A captured fish remains the real entity: it becomes a passenger and stays
 * visibly attached to the bird until the gull finally eats it.
 */
public final class SeagullEntity
        extends PathfinderMob
        implements FlyingAnimal {

    public SeagullEntity(
            EntityType<? extends SeagullEntity> type,
            Level level
    ) {
        super(type, level);
        this.moveControl =
                new FlyingMoveControl(
                        this,
                        18,
                        true
                );
        this.setNoGravity(true);

    }

    @Override
    public boolean removeWhenFarAway(
            double distanceToClosestPlayer
    ) {
        return !hasCustomName() && getMainHandItem().isEmpty();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new net.caravidro.wayaround.nature.BirdFoodTheftGoal(this));
        this.goalSelector.addGoal(
                4,
                new WaterAvoidingRandomFlyingGoal(
                        this,
                        1.05
                )
        );

        this.goalSelector.addGoal(
                6,
                new LookAtPlayerGoal(
                        this,
                        Player.class,
                        8.0F
                )
        );

        this.goalSelector.addGoal(
                7,
                new RandomLookAroundGoal(this)
        );
    }

    @Override
    protected PathNavigation createNavigation(
            Level level
    ) {
        FlyingPathNavigation nav=new FlyingPathNavigation(this,level);
        nav.setCanFloat(true);nav.setMaxVisitedNodesMultiplier(3);
        return nav;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.isInWaterOrBubble()) {
            /*
             * A gull is allowed to get wet during a fishing dive, but it must
             * never sit underwater until Minecraft's generic mob drowning
             * logic kills it.
             */
            this.setAirSupply(
                    this.getMaxAirSupply()
            );

            long diveUntil =
                    this.getPersistentData()
                            .getLong(
                                    "WayAroundGullDiveUntil"
                            );

            if (this.level().getGameTime()
                    > diveUntil) {
                Vec3 motion =
                        this.getDeltaMovement();

                this.setDeltaMovement(
                        motion.x * 0.72,
                        Math.max(
                                0.24,
                                motion.y + 0.12
                        ),
                        motion.z * 0.72
                );
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(
            double distance
    ) {
        return distance
                <= 384.0 * 384.0;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(
            DamageSource source
    ) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
    public Vec3 getPassengerRidingPosition(
            Entity passenger
    ) {
        Vec3 forward =
                this.getLookAngle()
                        .multiply(
                                0.32,
                                0.0,
                                0.32
                        );

        return this.position()
                .add(
                        forward.x,
                        -0.52,
                        forward.z
                );
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 6;
    }
}
