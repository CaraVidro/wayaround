package net.caravidro.wayaround.ecology;

import java.util.List;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Small schooling forage fish with a lateral-line panic relay.
 *
 * Death/carry/cooking no longer lives here. Those systems are shared through
 * FishCarcassEntity + FishProcessingProfile so future fish can opt in without
 * copying SardineEntity.
 */
public final class SardineEntity
        extends AguaWorldFishEntity {

    private static final EntityDataAccessor<Boolean> PANICKING =
            SynchedEntityData.defineId(
                    SardineEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    private long panicUntil;

    public SardineEntity(
            EntityType<? extends SardineEntity> type,
            Level level
    ) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        super.defineSynchedData(
                builder
        );

        builder.define(
                PANICKING,
                false
        );
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide
                && level()
                instanceof ServerLevel server) {
            tickSchoolInstinct(
                    server
            );
        }
    }

    private void tickSchoolInstinct(
            ServerLevel level
    ) {
        long now =
                level.getGameTime();

        boolean panicking =
                now < panicUntil;

        if (entityData.get(
                PANICKING
        ) != panicking) {
            entityData.set(
                    PANICKING,
                    panicking
            );
        }

        if ((tickCount % 5)
                != 0) {
            return;
        }

        List<SardineEntity> school =
                level.getEntitiesOfClass(
                        SardineEntity.class,
                        getBoundingBox()
                                .inflate(
                                        6.0,
                                        3.0,
                                        6.0
                                ),
                        other ->
                                other != this
                                        && other.isAlive()
                );

        if (!panicking
                && (tickCount % 20)
                == (getId() & 15)) {

            AbstractFish predator =
                    level.getEntitiesOfClass(
                                    AbstractFish.class,
                                    getBoundingBox()
                                            .inflate(
                                                    7.5,
                                                    4.0,
                                                    7.5
                                            ),
                                    fish ->
                                            fish != this
                                                    && fish.isAlive()
                                                    && fish instanceof AquaticPredator
                            )
                            .stream()
                            .min(
                                    java.util.Comparator.comparingDouble(
                                            this::distanceToSqr
                                    )
                            )
                            .orElse(
                                    null
                            );

            if (predator != null) {
                startleAwayFrom(
                        predator.position(),
                        85L
                );

                panicking =
                        true;
            }
        }

        if (school.isEmpty()) {
            return;
        }

        Vec3 center =
                Vec3.ZERO;

        Vec3 averageMotion =
                Vec3.ZERO;

        int panickedNeighbors =
                0;

        for (SardineEntity member :
                school) {
            center =
                    center.add(
                            member.position()
                    );

            averageMotion =
                    averageMotion.add(
                            member.getDeltaMovement()
                    );

            if (member.isPanicking()) {
                panickedNeighbors++;
            }
        }

        center =
                center.scale(
                        1.0
                                / school.size()
                );

        averageMotion =
                averageMotion.scale(
                        1.0
                                / school.size()
                );

        if (!panicking
                && panickedNeighbors > 0) {
            panicUntil =
                    Math.max(
                            panicUntil,
                            now + 45L
                    );

            entityData.set(
                    PANICKING,
                    true
            );

            panicking =
                    true;
        }

        Vec3 toCenter =
                center.subtract(
                        position()
                );

        Vec3 movement =
                getDeltaMovement();

        if (panicking) {
            Vec3 radial =
                    toCenter.lengthSqr()
                            > 0.0001
                                    ? toCenter.normalize()
                                    : Vec3.ZERO;

            Vec3 tangent =
                    new Vec3(
                            -radial.z,
                            0.0,
                            radial.x
                    );

            double sign =
                    ((getId() >> 1)
                            & 1) == 0
                                    ? 1.0
                                    : -1.0;

            setDeltaMovement(
                    movement.scale(
                                    0.86
                            )
                            .add(
                                    radial.scale(
                                            0.060
                                    )
                            )
                            .add(
                                    tangent.scale(
                                            0.075
                                                    * sign
                                    )
                            )
            );

            hasImpulse =
                    true;

            int relayed =
                    0;

            for (SardineEntity member :
                    school) {
                if (distanceToSqr(
                        member
                ) > 3.5 * 3.5) {
                    continue;
                }

                member.panicUntil =
                        Math.max(
                                member.panicUntil,
                                now + 32L
                        );

                member.entityData.set(
                        PANICKING,
                        true
                );

                if (++relayed >= 10) {
                    break;
                }
            }

            return;
        }

        Vec3 cohesion =
                toCenter.lengthSqr()
                        > 0.50
                                ? toCenter.normalize()
                                .scale(
                                        0.020
                                )
                                : Vec3.ZERO;

        Vec3 alignment =
                averageMotion.subtract(
                        movement
                )
                        .scale(
                                0.10
                        );

        setDeltaMovement(
                movement.add(
                        cohesion
                )
                        .add(
                                alignment
                        )
        );
    }

    public void startleAwayFrom(
            Vec3 source,
            long ticks
    ) {
        if (!isAlive()
                || level().isClientSide) {
            return;
        }

        panicUntil =
                Math.max(
                        panicUntil,
                        level().getGameTime()
                                + ticks
                );

        entityData.set(
                PANICKING,
                true
        );

        Vec3 away =
                position()
                        .subtract(
                                source
                        );

        if (away.lengthSqr()
                < 0.0001) {
            away =
                    new Vec3(
                            random.nextDouble()
                                    - 0.5,
                            random.nextDouble()
                                    * 0.25,
                            random.nextDouble()
                                    - 0.5
                    );
        }

        away =
                away.normalize();

        setDeltaMovement(
                getDeltaMovement()
                        .scale(
                                0.55
                        )
                        .add(
                                away.scale(
                                        0.34
                                )
                        )
        );

        hasImpulse =
                true;
    }

    public void startleFrom(
            Vec3 direction,
            long ticks
    ) {
        if (direction.lengthSqr()
                < 0.0001) {
            startleAwayFrom(
                    position()
                            .add(
                                    1.0,
                                    0.0,
                                    0.0
                            ),
                    ticks
            );

            return;
        }

        startleAwayFrom(
                position()
                        .subtract(
                                direction.normalize()
                        ),
                ticks
        );
    }

    public boolean isPanicking() {
        return entityData.get(
                PANICKING
        );
    }

    @Override
    public boolean hurt(
            DamageSource source,
            float amount
    ) {
        boolean result =
                super.hurt(
                        source,
                        amount
                );

        if (result
                && !level().isClientSide
                && level()
                instanceof ServerLevel server) {

            Vec3 sourcePos =
                    source.getEntity()
                            == null
                                    ? position()
                                    .add(
                                            random.nextDouble()
                                                    - 0.5,
                                            0.0,
                                            random.nextDouble()
                                                    - 0.5
                                    )
                                    : source.getEntity()
                                    .position();

            if (isAlive()) {
                startleAwayFrom(
                        sourcePos,
                        100L
                );
            }

            List<SardineEntity> neighbors =
                    server.getEntitiesOfClass(
                            SardineEntity.class,
                            getBoundingBox()
                                    .inflate(
                                            8.0,
                                            4.0,
                                            8.0
                                    ),
                            fish ->
                                    fish != this
                                            && fish.isAlive()
                    );

            int warned =
                    0;

            for (SardineEntity neighbor :
                    neighbors) {
                neighbor.startleAwayFrom(
                        sourcePos,
                        70L
                );

                if (++warned >= 18) {
                    break;
                }
            }
        }

        return result;
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(
                Items.COD_BUCKET
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
        return 40;
    }
}
