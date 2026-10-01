package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.war.WarProjectileEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * Cleiton.
 *
 * He is intentionally not part of natural fish ecology. He is a normal-ish
 * wandering mob shaped like a fish with four legs, except that the universe
 * has apparently agreed that killing or moving him is not allowed.
 */
public final class CleintonEntity
        extends PathfinderMob {

    public CleintonEntity(
            EntityType<? extends CleintonEntity> type,
            Level level
    ) {
        super(
                type,
                level
        );

        setPersistenceRequired();

        setPathfindingMalus(
                PathType.WATER,
                0.0F
        );

        setPathfindingMalus(
                PathType.WATER_BORDER,
                0.0F
        );
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(
                0,
                new FloatGoal(
                        this
                )
        );

        goalSelector.addGoal(
                4,
                new RandomStrollGoal(
                        this,
                        0.86
                )
        );

        goalSelector.addGoal(
                7,
                new LookAtPlayerGoal(
                        this,
                        Player.class,
                        8.0F
                )
        );

        goalSelector.addGoal(
                8,
                new RandomLookAroundGoal(
                        this
                )
        );
    }

    @Override
    protected PathNavigation createNavigation(
            Level level
    ) {
        return new AmphibiousPathNavigation(
                this,
                level
        );
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(
            double distanceToClosestPlayer
    ) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void knockback(
            double strength,
            double x,
            double z
    ) {
        // Cleiton has decided momentum does not apply to him.
    }

    @Override
    public boolean hurt(
            DamageSource source,
            float amount
    ) {
        Entity direct =
                source.getDirectEntity();

        if (!level().isClientSide
                && direct instanceof Projectile projectile) {

            Vec3 velocity =
                    projectile.getDeltaMovement();

            if (velocity.lengthSqr() > 0.0001) {
                Vec3 reflected =
                        velocity.scale(
                                -1.18
                        ).add(
                                0.0,
                                0.035,
                                0.0
                        );

                projectile.setOwner(
                        this
                );

                projectile.setDeltaMovement(
                        reflected
                );

                projectile.setPos(
                        projectile.position()
                                .add(
                                        reflected.normalize()
                                                .scale(
                                                        0.42
                                                )
                                )
                );

                projectile.hurtMarked =
                        true;
            }
        }

        /*
         * WarProjectileEntity reflects before its impact damage path, so it
         * never reaches this method. Ordinary arrows/tridents/etc. are handled
         * above. Everything else simply fails to damage Cleiton.
         */
        return false;
    }

    @Override
    public boolean isInvulnerableTo(
            DamageSource source
    ) {
        return true;
    }

    public void reflectWarProjectile(
            WarProjectileEntity projectile
    ) {
        projectile.reflectFrom(
                this
        );
    }
}
