package net.caravidro.wayaround.ecology;

import java.util.List;

import net.caravidro.wayaround.ecology.ai.LivingFaunaManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Small schooling forage fish with a lateral-line panic relay.
 *
 * A lethal hit leaves a physical carcass instead of converting the animal
 * directly into loot. The carcass can be carried, cooked whole, carved with
 * any sword, and finally reduced to bones.
 */
public final class SardineEntity extends AguaWorldFishEntity {

    private static final EntityDataAccessor<Boolean> PANICKING =
            SynchedEntityData.defineId(SardineEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CARCASS =
            SynchedEntityData.defineId(SardineEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> COOKED =
            SynchedEntityData.defineId(SardineEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LARGE_CARCASS =
            SynchedEntityData.defineId(SardineEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CUT_OPEN =
            SynchedEntityData.defineId(SardineEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MEAT_LEFT =
            SynchedEntityData.defineId(SardineEntity.class, EntityDataSerializers.INT);

    private long panicUntil;

    public SardineEntity(EntityType<? extends SardineEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PANICKING, false);
        builder.define(CARCASS, false);
        builder.define(COOKED, false);
        builder.define(LARGE_CARCASS, false);
        builder.define(CUT_OPEN, false);
        builder.define(MEAT_LEFT, 0);
    }

    @Override
    public void tick() {
        if (isCarcass()) {
            setNoAi(true);
            setNoGravity(true);
        }

        super.tick();

        if (isCarcass()) {
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            setAirSupply(getMaxAirSupply());
            return;
        }

        if (!level().isClientSide && level() instanceof ServerLevel server) {
            tickSchoolInstinct(server);
        }
    }

    private void tickSchoolInstinct(ServerLevel level) {
        long now = level.getGameTime();
        boolean panicking = now < panicUntil;

        if (entityData.get(PANICKING) != panicking) {
            entityData.set(PANICKING, panicking);
        }

        if ((tickCount % 5) != 0) {
            return;
        }

        List<SardineEntity> school =
                level.getEntitiesOfClass(
                        SardineEntity.class,
                        getBoundingBox().inflate(6.0, 3.0, 6.0),
                        other -> other != this && other.isAlive() && !other.isCarcass()
                );

        if (!panicking && (tickCount % 20) == (getId() & 15)) {
            AbstractFish predator =
                    level.getEntitiesOfClass(
                                    AbstractFish.class,
                                    getBoundingBox().inflate(7.5, 4.0, 7.5),
                                    fish -> fish != this
                                            && fish.isAlive()
                                            && fish instanceof AquaticPredator
                            )
                            .stream()
                            .min(java.util.Comparator.comparingDouble(this::distanceToSqr))
                            .orElse(null);

            if (predator != null) {
                startleAwayFrom(predator.position(), 85L);
                panicking = true;
            }
        }

        if (school.isEmpty()) {
            return;
        }

        Vec3 center = Vec3.ZERO;
        Vec3 averageMotion = Vec3.ZERO;
        int panickedNeighbors = 0;

        for (SardineEntity member : school) {
            center = center.add(member.position());
            averageMotion = averageMotion.add(member.getDeltaMovement());

            if (member.isPanicking()) {
                panickedNeighbors++;
            }
        }

        center = center.scale(1.0 / school.size());
        averageMotion = averageMotion.scale(1.0 / school.size());

        if (!panicking && panickedNeighbors > 0) {
            panicUntil = Math.max(panicUntil, now + 45L);
            entityData.set(PANICKING, true);
            panicking = true;
        }

        Vec3 toCenter = center.subtract(position());
        Vec3 movement = getDeltaMovement();

        if (panicking) {
            /*
             * Threatened sardines compress and orbit the local center instead
             * of fleeing as parallel arrows. This forms a nervous bait ball.
             */
            Vec3 radial =
                    toCenter.lengthSqr() > 0.0001
                            ? toCenter.normalize()
                            : Vec3.ZERO;

            Vec3 tangent = new Vec3(-radial.z, 0.0, radial.x);
            double sign = ((getId() >> 1) & 1) == 0 ? 1.0 : -1.0;

            setDeltaMovement(
                    movement.scale(0.86)
                            .add(radial.scale(0.060))
                            .add(tangent.scale(0.075 * sign))
            );

            hasImpulse = true;

            int relayed = 0;
            for (SardineEntity member : school) {
                if (distanceToSqr(member) > 3.5 * 3.5) {
                    continue;
                }

                member.panicUntil = Math.max(member.panicUntil, now + 32L);
                member.entityData.set(PANICKING, true);

                if (++relayed >= 10) {
                    break;
                }
            }

            return;
        }

        Vec3 cohesion =
                toCenter.lengthSqr() > 0.50
                        ? toCenter.normalize().scale(0.020)
                        : Vec3.ZERO;

        Vec3 alignment = averageMotion.subtract(movement).scale(0.10);

        setDeltaMovement(movement.add(cohesion).add(alignment));
    }

    public void startleAwayFrom(Vec3 source, long ticks) {
        if (isCarcass() || level().isClientSide) {
            return;
        }

        panicUntil = Math.max(panicUntil, level().getGameTime() + ticks);
        entityData.set(PANICKING, true);

        Vec3 away = position().subtract(source);

        if (away.lengthSqr() < 0.0001) {
            away = new Vec3(
                    random.nextDouble() - 0.5,
                    random.nextDouble() * 0.25,
                    random.nextDouble() - 0.5
            );
        }

        away = away.normalize();

        setDeltaMovement(
                getDeltaMovement()
                        .scale(0.55)
                        .add(away.scale(0.34))
        );

        hasImpulse = true;
    }

    public void startleFrom(Vec3 direction, long ticks) {
        if (direction.lengthSqr() < 0.0001) {
            startleAwayFrom(position().add(1.0, 0.0, 0.0), ticks);
            return;
        }

        startleAwayFrom(position().subtract(direction.normalize()), ticks);
    }

    public boolean isPanicking() {
        return entityData.get(PANICKING);
    }

    public boolean isCarcass() {
        return entityData.get(CARCASS);
    }

    public boolean isCarcassCooked() {
        return entityData.get(COOKED);
    }

    public boolean isLargeCarcass() {
        return entityData.get(LARGE_CARCASS);
    }

    public boolean isCutOpen() {
        return entityData.get(CUT_OPEN);
    }

    public int meatLeft() {
        return entityData.get(MEAT_LEFT);
    }

    public void restoreAsCarcass(boolean cooked, boolean large) {
        setHealth(Math.max(1.0F, getMaxHealth() * 0.20F));
        entityData.set(CARCASS, true);
        entityData.set(COOKED, cooked);
        entityData.set(LARGE_CARCASS, large);
        entityData.set(CUT_OPEN, false);
        entityData.set(MEAT_LEFT, large ? 3 : 1);
        setNoAi(true);
        setNoGravity(true);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
    }

    private void becomeCarcass() {
        float size = LivingFaunaManager.fishSize(this);
        boolean large = size >= 0.50F;

        restoreAsCarcass(false, large);

        entityData.set(
                MEAT_LEFT,
                Math.max(1, Math.min(4, Math.round(size * 5.0F)))
        );

        playSound(
                SoundEvents.COD_DEATH,
                0.75F,
                0.90F + random.nextFloat() * 0.15F
        );
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isCarcass()) {
            return false;
        }

        if (!level().isClientSide && amount >= getHealth()) {
            becomeCarcass();
            return true;
        }

        boolean result = super.hurt(source, amount);

        if (result
                && !level().isClientSide
                && level() instanceof ServerLevel server) {
            Vec3 sourcePos =
                    source.getEntity() == null
                            ? position().add(
                                    random.nextDouble() - 0.5,
                                    0.0,
                                    random.nextDouble() - 0.5
                            )
                            : source.getEntity().position();

            startleAwayFrom(sourcePos, 100L);

            List<SardineEntity> neighbors =
                    server.getEntitiesOfClass(
                            SardineEntity.class,
                            getBoundingBox().inflate(8.0, 4.0, 8.0),
                            fish -> fish != this && !fish.isCarcass()
                    );

            int warned = 0;
            for (SardineEntity neighbor : neighbors) {
                neighbor.startleAwayFrom(sourcePos, 70L);

                if (++warned >= 18) {
                    break;
                }
            }
        }

        return result;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isCarcass()) {
            return super.mobInteract(player, hand);
        }

        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (held.getItem() instanceof SwordItem) {
            if (meatLeft() <= 0) {
                giveBones(player);
                return InteractionResult.SUCCESS;
            }

            ItemStack meat =
                    new ItemStack(
                            isCarcassCooked()
                                    ? EcologyContent.COOKED_SARDINE_MEAT.get()
                                    : EcologyContent.RAW_SARDINE_MEAT.get()
                    );

            if (!player.addItem(meat)) {
                player.drop(meat, false);
            }

            entityData.set(CUT_OPEN, true);
            entityData.set(MEAT_LEFT, Math.max(0, meatLeft() - 1));

            playSound(SoundEvents.SHEEP_SHEAR, 0.55F, 0.75F);
            return InteractionResult.SUCCESS;
        }

        if (meatLeft() <= 0 && held.isEmpty()) {
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

        ItemStack whole = new ItemStack(wholeItem());

        if (isLargeCarcass()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, whole);
        } else {
            player.setItemInHand(hand, whole);
        }

        discard();
        return InteractionResult.SUCCESS;
    }

    private net.minecraft.world.item.Item wholeItem() {
        if (isCarcassCooked()) {
            return isLargeCarcass()
                    ? EcologyContent.COOKED_LARGE_WHOLE_SARDINE.get()
                    : EcologyContent.COOKED_WHOLE_SARDINE.get();
        }

        return isLargeCarcass()
                ? EcologyContent.LARGE_WHOLE_SARDINE.get()
                : EcologyContent.WHOLE_SARDINE.get();
    }

    private void giveBones(Player player) {
        ItemStack bones = new ItemStack(Items.BONE, isLargeCarcass() ? 2 : 1);

        if (!player.addItem(bones)) {
            player.drop(bones, false);
        }

        discard();
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("SardineCarcass", isCarcass());
        tag.putBoolean("SardineCooked", isCarcassCooked());
        tag.putBoolean("SardineLargeCarcass", isLargeCarcass());
        tag.putBoolean("SardineCutOpen", isCutOpen());
        tag.putInt("SardineMeatLeft", meatLeft());
        tag.putLong("SardinePanicUntil", panicUntil);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(CARCASS, tag.getBoolean("SardineCarcass"));
        entityData.set(COOKED, tag.getBoolean("SardineCooked"));
        entityData.set(LARGE_CARCASS, tag.getBoolean("SardineLargeCarcass"));
        entityData.set(CUT_OPEN, tag.getBoolean("SardineCutOpen"));
        entityData.set(MEAT_LEFT, Math.max(0, tag.getInt("SardineMeatLeft")));
        panicUntil = Math.max(0L, tag.getLong("SardinePanicUntil"));

        if (isCarcass()) {
            setNoAi(true);
            setNoGravity(true);
        }
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 40;
    }
}
