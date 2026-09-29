package net.caravidro.wayaround.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ocean sunfish with persistent scars and a rare surface-basking routine.
 *
 * A basking fish actually travels toward a nearby surface, lies on one side for
 * a while and then rights itself. The state is synchronized so every client sees
 * the same odd little ocean event.
 */
public final class SunfishEntity extends AguaWorldFishEntity {

    private static final EntityDataAccessor<Boolean> BASKING =
            SynchedEntityData.defineId(
                    SunfishEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    private static final EntityDataAccessor<Integer> SCAR_STAGE =
            SynchedEntityData.defineId(
                    SunfishEntity.class,
                    EntityDataSerializers.INT
            );

    private int baskTicks;
    private int baskApproachTicks;
    private int baskSurfaceY;
    private static final String NATURAL_SCARS_INITIALIZED =
            "WayAroundSunfishNaturalScars";

    private int baskCooldown;
    private int baskKnockbackGrace;

    private float prevBaskBlend;
    private float baskBlend;

    public SunfishEntity(
            EntityType<? extends SunfishEntity> type,
            Level level
    ) {
        super(
                type,
                level
        );

        baskCooldown =
                700
                        + level.random.nextInt(
                        1500
                );
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        super.defineSynchedData(
                builder
        );

        builder.define(
                BASKING,
                false
        );

        builder.define(
                SCAR_STAGE,
                0
        );
    }

    @Override
    public void tick() {
        prevBaskBlend =
                baskBlend;

        super.tick();

        float target =
                isBasking()
                        ? 1.0F
                        : 0.0F;

        baskBlend +=
                (
                        target
                                - baskBlend
                )
                        * 0.16F;

        if (level().isClientSide) {
            return;
        }

        initializeNaturalScars();
        tickBasking();
    }

    private void initializeNaturalScars() {
        CompoundTag data =
                getPersistentData();

        if (data.getBoolean(
                NATURAL_SCARS_INITIALIZED
        )) {
            return;
        }

        data.putBoolean(
                NATURAL_SCARS_INITIALIZED,
                true
        );

        /*
         * Ocean sunfish are often found carrying evidence of old encounters.
         * Most spawned animals get at least one mark, while heavy scarring is
         * still uncommon enough to make an individual visually memorable.
         */
        float roll =
                random.nextFloat();

        int naturalStage =
                roll < 0.08F
                        ? 4
                        : roll < 0.24F
                        ? 3
                        : roll < 0.53F
                        ? 2
                        : roll < 0.90F
                        ? 1
                        : 0;

        if (naturalStage > scarStage()) {
            entityData.set(
                    SCAR_STAGE,
                    naturalStage
            );
        }
    }

    private void tickBasking() {
        if (isBasking()) {
            getNavigation().stop();

            baskTicks--;

            Vec3 motion =
                    getDeltaMovement();

            if (baskKnockbackGrace > 0) {
                baskKnockbackGrace--;
            }

            if (isInWaterOrBubble()) {
                double targetY =
                        baskSurfaceY
                                - 0.14;

                double vertical =
                        Mth.clamp(
                                (
                                        targetY
                                                - getY()
                                )
                                        * 0.08,
                                -0.035,
                                0.035
                        );

                double horizontalDrag =
                        baskKnockbackGrace > 0
                                ? 0.94
                                : 0.78;

                setDeltaMovement(
                        motion.x
                                * horizontalDrag,
                        vertical,
                        motion.z
                                * horizontalDrag
                );

            } else {
                /*
                 * If a hit launches the basking fish clear of the water, keep
                 * the basking state and let ordinary gravity handle the arc.
                 * He has accepted the situation.
                 */
                setDeltaMovement(
                        motion.x * 0.96,
                        motion.y,
                        motion.z * 0.96
                );
            }

            if (baskTicks <= 0) {
                setBasking(
                        false
                );

                baskCooldown =
                        4200
                                + random.nextInt(
                                5200
                        );
            }

            return;
        }

        if (baskApproachTicks > 0) {
            baskApproachTicks--;

            getNavigation().moveTo(
                    getX(),
                    baskSurfaceY - 0.22,
                    getZ(),
                    0.78
            );

            if (Math.abs(
                    getY()
                            - (
                            baskSurfaceY
                                    - 0.22
                    )
            ) < 1.15
                    && isSurfaceWater(
                    baskSurfaceY
            )) {

                getNavigation().stop();

                baskTicks =
                        180
                                + random.nextInt(
                                240
                        );

                setBasking(
                        true
                );

                return;
            }

            if (baskApproachTicks <= 0) {
                baskCooldown =
                        1000
                                + random.nextInt(
                                1800
                        );
            }

            return;
        }

        if (baskCooldown > 0) {
            baskCooldown--;
            return;
        }

        /*
         * Do not roll every tick. This gives the behaviour a rare, memorable
         * cadence even in oceans containing several sunfish.
         */
        int scars =
                scarStage();

        baskCooldown =
                Math.max(
                        260,
                        860
                                + random.nextInt(
                                1500
                        )
                                - scars
                                        * 155
                );

        float baskChance =
                Mth.clamp(
                        0.20F
                                + scars
                                        * 0.18F,
                        0.20F,
                        0.92F
                );

        if (random.nextFloat()
                > baskChance) {
            return;
        }

        int surface =
                findNearbySurface();

        if (surface
                == Integer.MIN_VALUE) {
            return;
        }

        baskSurfaceY =
                surface;

        baskApproachTicks =
                220;
    }

    private int findNearbySurface() {
        BlockPos.MutableBlockPos cursor =
                blockPosition()
                        .mutable();

        for (int dy = 0;
             dy <= 12;
             dy++) {

            cursor.set(
                    blockPosition()
                            .getX(),
                    blockPosition()
                            .getY()
                            + dy,
                    blockPosition()
                            .getZ()
            );

            if (!level().getFluidState(
                    cursor
            ).is(
                    FluidTags.WATER
            )) {

                BlockPos water =
                        cursor.below();

                if (level().getFluidState(
                        water
                ).is(
                        FluidTags.WATER
                )) {
                    return cursor.getY();
                }

                return Integer.MIN_VALUE;
            }
        }

        return Integer.MIN_VALUE;
    }

    private boolean isSurfaceWater(
            int airY
    ) {
        BlockPos air =
                BlockPos.containing(
                        getX(),
                        airY,
                        getZ()
                );

        return !level().getFluidState(
                air
        ).is(
                FluidTags.WATER
        )
                && level().getFluidState(
                air.below()
        ).is(
                FluidTags.WATER
        );
    }

    public boolean isBasking() {
        return entityData.get(
                BASKING
        );
    }

    private void setBasking(
            boolean basking
    ) {
        entityData.set(
                BASKING,
                basking
        );
    }

    public float baskBlend(
            float partialTick
    ) {
        return Mth.lerp(
                partialTick,
                prevBaskBlend,
                baskBlend
        );
    }

    public int scarStage() {
        return entityData.get(
                SCAR_STAGE
        );
    }

    private void addScar() {
        entityData.set(
                SCAR_STAGE,
                Math.min(
                        4,
                        scarStage()
                                + 1
                )
        );
    }

    @Override
    public void knockback(
            double strength,
            double x,
            double z
    ) {
        super.knockback(
                strength,
                x,
                z
        );

        if (isBasking()) {
            /*
             * The impact is real and visible, but it does not cancel the odd
             * surface-basking state. For a short window we preserve most of
             * the horizontal impulse before the fish drifts back toward the
             * surface.
             */
            baskKnockbackGrace =
                    18;

            getNavigation()
                    .stop();
        }
    }

    @Override
    public ItemStack getBucketItemStack() {
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
        if (source.getEntity() != null
                && this.isAlive()) {

            /*
             * A basking sunfish is spectacularly committed to the bit.
             * Damage can scar it, but does not make it immediately right
             * itself or leave the surface.
             */
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
                                    amount
                                            * 0.14F,
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

            if (result) {
                addScar();
            }

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
    public void addAdditionalSaveData(
            CompoundTag tag
    ) {
        super.addAdditionalSaveData(
                tag
        );

        tag.putBoolean(
                "Basking",
                isBasking()
        );

        tag.putInt(
                "BaskTicks",
                baskTicks
        );

        tag.putInt(
                "BaskApproachTicks",
                baskApproachTicks
        );

        tag.putInt(
                "BaskSurfaceY",
                baskSurfaceY
        );

        tag.putInt(
                "BaskCooldown",
                baskCooldown
        );

        tag.putInt(
                "ScarStage",
                scarStage()
        );
    }

    @Override
    public void readAdditionalSaveData(
            CompoundTag tag
    ) {
        super.readAdditionalSaveData(
                tag
        );

        setBasking(
                tag.getBoolean(
                        "Basking"
                )
        );

        baskTicks =
                tag.getInt(
                        "BaskTicks"
                );

        baskApproachTicks =
                tag.getInt(
                        "BaskApproachTicks"
                );

        baskSurfaceY =
                tag.getInt(
                        "BaskSurfaceY"
                );

        baskCooldown =
                Math.max(
                        0,
                        tag.getInt(
                                "BaskCooldown"
                        )
                );

        entityData.set(
                SCAR_STAGE,
                Mth.clamp(
                        tag.getInt(
                                "ScarStage"
                        ),
                        0,
                        4
                )
        );
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 3;
    }
}
