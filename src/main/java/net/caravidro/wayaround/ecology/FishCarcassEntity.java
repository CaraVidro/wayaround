package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Shared physical carcass for processable fish.
 *
 * This deliberately does NOT extend AbstractFish. A corpse should float, drift
 * and settle, but it should never inherit vanilla's land-flopping behavior.
 */
public final class FishCarcassEntity extends PathfinderMob {

    private static final EntityDataAccessor<Boolean> SETTLED = SynchedEntityData.defineId(FishCarcassEntity.class, EntityDataSerializers.BOOLEAN);
    public void setAbyssalSettled(boolean settled) { entityData.set(SETTLED,settled); }
    private static final EntityDataAccessor<Integer> PROFILE =
            SynchedEntityData.defineId(FishCarcassEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> COOKED =
            SynchedEntityData.defineId(FishCarcassEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LARGE =
            SynchedEntityData.defineId(FishCarcassEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CUT_OPEN =
            SynchedEntityData.defineId(FishCarcassEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MEAT_LEFT =
            SynchedEntityData.defineId(FishCarcassEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BODY_SCALE =
            SynchedEntityData.defineId(FishCarcassEntity.class, EntityDataSerializers.FLOAT);

    public FishCarcassEntity(
            EntityType<? extends FishCarcassEntity> type,
            Level level
    ) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        // Dead fish have no career goals.
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        super.defineSynchedData(builder);
        builder.define(SETTLED,false);
        builder.define(PROFILE, FishProcessingProfile.SARDINE.networkId());
        builder.define(COOKED, false);
        builder.define(LARGE, false);
        builder.define(CUT_OPEN, false);
        builder.define(MEAT_LEFT, 1);
        builder.define(BODY_SCALE, 1.0F);
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
                profile.isLarge(bodyScale)
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

        entityData.set(PROFILE, profile.networkId());
        entityData.set(BODY_SCALE, safeScale);
        entityData.set(COOKED, cooked);
        entityData.set(LARGE, large);
        entityData.set(CUT_OPEN, false);
        entityData.set(MEAT_LEFT, profile.meatUnits(safeScale));

        AttributeInstance scale =
                getAttribute(Attributes.SCALE);

        if (scale != null) {
            /*
             * Tiny fish used their visual body scale as the physical entity
             * scale too, producing a frustrating needle-sized click target.
             * Keep visual scale in BODY_SCALE, but give carcasses a sensible
             * minimum physical footprint.
             */
            float collisionScale =
                    Math.max(
                            0.62F,
                            safeScale
                    );

            scale.setBaseValue(
                    collisionScale
            );

            refreshDimensions();
        }

        setAirSupply(getMaxAirSupply());
        setDeltaMovement(Vec3.ZERO);
    }

    public FishProcessingProfile profile() {
        return FishProcessingProfile.byNetworkId(
                entityData.get(PROFILE)
        );
    }

    public float bodyScale() {
        return entityData.get(BODY_SCALE);
    }

    public boolean isCooked() {
        return entityData.get(COOKED);
    }

    public boolean isLargeCarcass() {
        return entityData.get(LARGE);
    }

    public boolean isCutOpen() {
        return entityData.get(CUT_OPEN);
    }

    public int meatLeft() {
        return entityData.get(MEAT_LEFT);
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
                        profile().meatUnits(bodyScale())
                );

        float remaining =
                Mth.clamp(
                        meatLeft() / (float) max,
                        0.0F,
                        1.0F
                );

        return profile().attractiveness()
                * (0.60F + remaining * 0.40F)
                * (isCooked() ? 0.76F : 1.0F);
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
        if (requested <= 0 || isSkeleton()) {
            return 0;
        }

        int consumed =
                Math.min(
                        requested,
                        meatLeft()
                );

        entityData.set(CUT_OPEN, true);
        entityData.set(MEAT_LEFT, meatLeft() - consumed);

        return consumed;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        setAirSupply(getMaxAirSupply());

        if (isInWaterOrBubble()) {
            setNoGravity(true);

            Vec3 current =
                    WaterDynamics.currentAround(
                            level(),
                            blockPosition()
                    );

            boolean deeplySubmerged =
                    level().getFluidState(
                                    blockPosition().above()
                            )
                            .is(FluidTags.WATER);

            /*
             * Bodies slowly rise instead of shooting upward. Once at the
             * surface the lift becomes tiny, so they visibly bob there.
             */
            double lift = entityData.get(SETTLED) ? -0.015 :
                    deeplySubmerged
                            ? 0.030
                            : 0.0015;

            Vec3 motion =
                    getDeltaMovement()
                            .multiply(
                                    0.80,
                                    0.48,
                                    0.80
                            )
                            .add(
                                    current.scale(0.12)
                            )
                            .add(
                                    0.0,
                                    lift,
                                    0.0
                            );

            setDeltaMovement(motion);

        } else {
            setNoGravity(false);

            if (onGround()) {
                Vec3 motion =
                        getDeltaMovement();

                /*
                 * No inherited fish-flop anymore; this damping just makes the
                 * body settle quickly instead of ice-skating over terrain.
                 */
                setDeltaMovement(
                        motion.x * 0.08,
                        Math.min(0.0, motion.y),
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
        // Processing is interaction-driven; random damage cannot erase a corpse.
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
                player.getItemInHand(hand);

        if (held.getItem() instanceof SwordItem) {
            if (isSkeleton()) {
                giveBones(player);
                return InteractionResult.SUCCESS;
            }

            ItemStack meat =
                    new ItemStack(
                            isCooked()
                                    ? profile().cookedMeat()
                                    : profile().rawMeat()
                    );

            if (!player.addItem(meat)) {
                player.drop(meat, false);
            }

            consumeFlesh(1);

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

        if (isSkeleton() && held.isEmpty()) {
            giveBones(player);
            return InteractionResult.SUCCESS;
        }

        if (!held.isEmpty() || isCutOpen()) {
            return InteractionResult.PASS;
        }

        if (isLargeCarcass()
                && (!player.getMainHandItem().isEmpty()
                || !player.getOffhandItem().isEmpty())) {
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

        if (!player.addItem(bones)) {
            player.drop(bones, false);
        }

        discard();
    }

    @Override
    public void addAdditionalSaveData(
            CompoundTag tag
    ) {
        super.addAdditionalSaveData(tag);

        tag.putBoolean("AbyssalSettled",entityData.get(SETTLED));
        tag.putInt("FishCarcassProfile", profile().networkId());
        tag.putBoolean("FishCarcassCooked", isCooked());
        tag.putBoolean("FishCarcassLarge", isLargeCarcass());
        tag.putBoolean("FishCarcassCutOpen", isCutOpen());
        tag.putInt("FishCarcassMeat", meatLeft());
        tag.putFloat("FishCarcassScale", bodyScale());
    }

    @Override
    public void readAdditionalSaveData(
            CompoundTag tag
    ) {
        super.readAdditionalSaveData(tag);

        setAbyssalSettled(tag.getBoolean("AbyssalSettled"));
        entityData.set(PROFILE, tag.getInt("FishCarcassProfile"));
        entityData.set(COOKED, tag.getBoolean("FishCarcassCooked"));
        entityData.set(LARGE, tag.getBoolean("FishCarcassLarge"));
        entityData.set(CUT_OPEN, tag.getBoolean("FishCarcassCutOpen"));
        entityData.set(
                MEAT_LEFT,
                Math.max(
                        0,
                        tag.getInt("FishCarcassMeat")
                )
        );

        float scaleValue =
                Math.max(
                        0.08F,
                        tag.getFloat("FishCarcassScale")
                );

        entityData.set(BODY_SCALE, scaleValue);

        AttributeInstance scale =
                getAttribute(Attributes.SCALE);

        if (scale != null) {
            scale.setBaseValue(scaleValue);
            refreshDimensions();
        }
    }
}
