package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Shared physical carcass for processable fish.
 *
 * The corpse floats toward the surface in water, follows a little of the local
 * current, settles under gravity on land, can be carried/cooked/carved, and can
 * lose flesh when other fish scavenge it.
 */
public final class FishCarcassEntity
        extends AbstractFish {

    private static final EntityDataAccessor<Integer> PROFILE =
            SynchedEntityData.defineId(
                    FishCarcassEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Boolean> COOKED =
            SynchedEntityData.defineId(
                    FishCarcassEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    private static final EntityDataAccessor<Boolean> LARGE =
            SynchedEntityData.defineId(
                    FishCarcassEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    private static final EntityDataAccessor<Boolean> CUT_OPEN =
            SynchedEntityData.defineId(
                    FishCarcassEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    private static final EntityDataAccessor<Integer> MEAT_LEFT =
            SynchedEntityData.defineId(
                    FishCarcassEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Float> BODY_SCALE =
            SynchedEntityData.defineId(
                    FishCarcassEntity.class,
                    EntityDataSerializers.FLOAT
            );

    public FishCarcassEntity(
            EntityType<? extends FishCarcassEntity> type,
            Level level
    ) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        super.defineSynchedData(builder);

        builder.define(
                PROFILE,
                FishProcessingProfile.SARDINE.networkId()
        );

        builder.define(COOKED, false);
        builder.define(LARGE, false);
        builder.define(CUT_OPEN, false);
        builder.define(MEAT_LEFT, 1);
        builder.define(BODY_SCALE, 1.0F);
    }

    @Override
    protected void registerGoals() {
        // The dead continue to be impressively unambitious.
    }

    @Override
    public boolean removeWhenFarAway(
            double distanceToClosestPlayer
    ) {
        return false;
    }

    public void initialize(
            FishProcessingProfile profile,
            float bodyScale,
            boolean cooked
    ) {
        initialize(
                profile,
                bodyScale,
                cooked,
                profile.isLarge(
                        bodyScale
                )
        );
    }

    public void initialize(
            FishProcessingProfile profile,
            float bodyScale,
            boolean cooked,
            boolean large
    ) {
        float safeScale =
                Mth.clamp(
                        bodyScale,
                        0.08F,
                        8.0F
                );

        entityData.set(
                PROFILE,
                profile.networkId()
        );

        entityData.set(
                BODY_SCALE,
                safeScale
        );

        entityData.set(
                COOKED,
                cooked
        );

        entityData.set(
                LARGE,
                large
        );

        entityData.set(
                CUT_OPEN,
                false
        );

        entityData.set(
                MEAT_LEFT,
                profile.meatUnits(
                        safeScale
                )
        );

        AttributeInstance scale =
                getAttribute(
                        Attributes.SCALE
                );

        if (scale != null) {
            scale.setBaseValue(
                    safeScale
            );

            refreshDimensions();
        }

        setAirSupply(
                getMaxAirSupply()
        );

        setDeltaMovement(
                Vec3.ZERO
        );
    }

    public FishProcessingProfile profile() {
        return FishProcessingProfile.byNetworkId(
                entityData.get(
                        PROFILE
                )
        );
    }

    public float bodyScale() {
        return entityData.get(
                BODY_SCALE
        );
    }

    public boolean isCooked() {
        return entityData.get(
                COOKED
        );
    }

    public boolean isLargeCarcass() {
        return entityData.get(
                LARGE
        );
    }

    public boolean isCutOpen() {
        return entityData.get(
                CUT_OPEN
        );
    }

    public int meatLeft() {
        return entityData.get(
                MEAT_LEFT
        );
    }

    public boolean isSkeleton() {
        return meatLeft() <= 0;
    }

    public float attractiveness() {
        if (isSkeleton()) {
            return 0.0F;
        }

        int max =
                Math.max(
                        1,
                        profile().meatUnits(
                                bodyScale()
                        )
                );

        float remaining =
                Mth.clamp(
                        meatLeft()
                                / (float) max,
                        0.0F,
                        1.0F
                );

        float cookedFactor =
                isCooked()
                        ? 0.76F
                        : 1.0F;

        return profile().attractiveness()
                * (
                0.60F
                        + remaining
                                * 0.40F
        )
                * cookedFactor;
    }

    public ItemStack meatParticleStack() {
        return new ItemStack(
                isCooked()
                        ? profile().cookedMeat()
                        : profile().rawMeat()
        );
    }

    public int consumeFlesh(
            int requested
    ) {
        if (requested <= 0
                || isSkeleton()) {
            return 0;
        }

        int consumed =
                Math.min(
                        requested,
                        meatLeft()
                );

        entityData.set(
                CUT_OPEN,
                true
        );

        entityData.set(
                MEAT_LEFT,
                meatLeft()
                        - consumed
        );

        return consumed;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        setAirSupply(
                getMaxAirSupply()
        );

        if (isInWaterOrBubble()) {
            setNoGravity(
                    true
            );

            Vec3 current =
                    WaterDynamics.currentAround(
                            level(),
                            blockPosition()
                    );

            boolean stillSubmerged =
                    level().getFluidState(
                                    blockPosition()
                                            .above()
                            )
                            .is(
                                    FluidTags.WATER
                            );

            double lift =
                    stillSubmerged
                            ? 0.034
                            : 0.002;

            Vec3 motion =
                    getDeltaMovement()
                            .multiply(
                                    0.78,
                                    0.44,
                                    0.78
                            )
                            .add(
                                    current.scale(
                                            0.13
                                    )
                            )
                            .add(
                                    0.0,
                                    lift,
                                    0.0
                            );

            setDeltaMovement(
                    motion
            );

        } else {
            setNoGravity(
                    false
            );

            if (onGround()) {
                Vec3 motion =
                        getDeltaMovement();

                /*
                 * AbstractFish normally likes doing the dramatic land-flop.
                 * Corpses are, regrettably, no longer available for acting.
                 */
                setDeltaMovement(
                        motion.x * 0.08,
                        Math.min(
                                0.0,
                                motion.y
                        ),
                        motion.z * 0.08
                );
            }
        }
    }

    @Override
    public boolean hurt(
            DamageSource source,
            float amount
    ) {
        /*
         * Processing is interaction-driven. A stray sword swing, explosion or
         * predator bite must not delete the corpse entity itself.
         */
        return false;
    }

    @Override
    protected InteractionResult mobInteract(
            Player player,
            InteractionHand hand
    ) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack held =
                player.getItemInHand(
                        hand
                );

        if (held.getItem()
                instanceof SwordItem) {

            if (isSkeleton()) {
                giveBones(
                        player
                );

                return InteractionResult.SUCCESS;
            }

            ItemStack meat =
                    new ItemStack(
                            isCooked()
                                    ? profile().cookedMeat()
                                    : profile().rawMeat()
                    );

            if (!player.addItem(
                    meat
            )) {
                player.drop(
                        meat,
                        false
                );
            }

            consumeFlesh(
                    1
            );

            level().playSound(
                    null,
                    blockPosition(),
                    SoundEvents.SHEEP_SHEAR,
                    SoundSource.PLAYERS,
                    0.55F,
                    0.72F
            );

            return InteractionResult.SUCCESS;
        }

        if (isSkeleton()
                && held.isEmpty()) {
            giveBones(
                    player
            );

            return InteractionResult.SUCCESS;
        }

        if (!held.isEmpty()
                || isCutOpen()) {
            return InteractionResult.PASS;
        }

        if (isLargeCarcass()
                && (
                !player.getMainHandItem()
                        .isEmpty()
                        || !player.getOffhandItem()
                        .isEmpty()
        )) {
            return InteractionResult.FAIL;
        }

        ItemStack whole =
                new ItemStack(
                        profile().wholeItem(
                                isCooked(),
                                isLargeCarcass()
                        )
                );

        if (isLargeCarcass()) {
            player.setItemInHand(
                    InteractionHand.MAIN_HAND,
                    whole
            );

        } else {
            player.setItemInHand(
                    hand,
                    whole
            );
        }

        discard();

        return InteractionResult.SUCCESS;
    }

    private void giveBones(
            Player player
    ) {
        ItemStack bones =
                new ItemStack(
                        Items.BONE,
                        profile().boneCount(
                                isLargeCarcass()
                        )
                );

        if (!player.addItem(
                bones
        )) {
            player.drop(
                    bones,
                    false
            );
        }

        discard();
    }

    @Override
    public ItemStack getBucketItemStack() {
        return ItemStack.EMPTY;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.COD_FLOP;
    }

    @Override
    public void addAdditionalSaveData(
            CompoundTag tag
    ) {
        super.addAdditionalSaveData(
                tag
        );

        tag.putInt(
                "FishCarcassProfile",
                profile().networkId()
        );

        tag.putBoolean(
                "FishCarcassCooked",
                isCooked()
        );

        tag.putBoolean(
                "FishCarcassLarge",
                isLargeCarcass()
        );

        tag.putBoolean(
                "FishCarcassCutOpen",
                isCutOpen()
        );

        tag.putInt(
                "FishCarcassMeat",
                meatLeft()
        );

        tag.putFloat(
                "FishCarcassScale",
                bodyScale()
        );
    }

    @Override
    public void readAdditionalSaveData(
            CompoundTag tag
    ) {
        super.readAdditionalSaveData(
                tag
        );

        entityData.set(
                PROFILE,
                tag.getInt(
                        "FishCarcassProfile"
                )
        );

        entityData.set(
                COOKED,
                tag.getBoolean(
                        "FishCarcassCooked"
                )
        );

        entityData.set(
                LARGE,
                tag.getBoolean(
                        "FishCarcassLarge"
                )
        );

        entityData.set(
                CUT_OPEN,
                tag.getBoolean(
                        "FishCarcassCutOpen"
                )
        );

        entityData.set(
                MEAT_LEFT,
                Math.max(
                        0,
                        tag.getInt(
                                "FishCarcassMeat"
                        )
                )
        );

        float scaleValue =
                Math.max(
                        0.08F,
                        tag.getFloat(
                                "FishCarcassScale"
                        )
                );

        entityData.set(
                BODY_SCALE,
                scaleValue
        );

        AttributeInstance scale =
                getAttribute(
                        Attributes.SCALE
                );

        if (scale != null) {
            scale.setBaseValue(
                    scaleValue
            );

            refreshDimensions();
        }
    }
}
