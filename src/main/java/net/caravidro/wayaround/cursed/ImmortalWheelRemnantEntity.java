package net.caravidro.wayaround.cursed;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ImmortalWheelRemnantEntity extends Entity {

    private static final EntityDataAccessor<Boolean> SHATTERED =
            SynchedEntityData.defineId(
                    ImmortalWheelRemnantEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    private int wheelDamage;

    public ImmortalWheelRemnantEntity(
            EntityType<? extends ImmortalWheelRemnantEntity> type,
            Level level
    ) {
        super(type, level);
        setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        builder.define(SHATTERED, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (isRemoved()) return;

        Vec3 velocity = getDeltaMovement();

        if (!isNoGravity()) {
            velocity = velocity.add(0.0, -0.045, 0.0);
        }

        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);

        Vec3 after = getDeltaMovement();

        if (onGround()) {
            setDeltaMovement(after.x * 0.58, 0.0, after.z * 0.58);
        } else {
            setDeltaMovement(after.scale(0.985));
        }

        clearFire();

        if (isShattered()
                && level() instanceof ServerLevel server) {

            if (tickCount % 4 == 0) {
                server.sendParticles(
                        ParticleTypes.ASH,
                        getX(),
                        getY() + 0.14,
                        getZ(),
                        6,
                        0.44,
                        0.08,
                        0.44,
                        0.012
                );

                server.sendParticles(
                        ParticleTypes.SMOKE,
                        getX(),
                        getY() + 0.12,
                        getZ(),
                        2,
                        0.34,
                        0.06,
                        0.34,
                        0.018
                );
            }

            if (tickCount >= 76) {
                server.sendParticles(
                        ParticleTypes.ASH,
                        getX(),
                        getY() + 0.12,
                        getZ(),
                        64,
                        0.78,
                        0.18,
                        0.78,
                        0.055
                );

                server.sendParticles(
                        ParticleTypes.LARGE_SMOKE,
                        getX(),
                        getY() + 0.12,
                        getZ(),
                        20,
                        0.56,
                        0.12,
                        0.56,
                        0.035
                );

                discard();
            }
        }
    }

    @Override
    public InteractionResult interact(
            Player player,
            InteractionHand hand
    ) {
        if (isShattered()) {
            return InteractionResult.CONSUME;
        }

        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        Vec3 origin =
                position()
                        .add(
                                0.0,
                                0.16,
                                0.0
                        );

        ImmortalWheelManager.bindFromRemnant(
                serverPlayer,
                wheelDamage,
                origin
        );

        discard();

        return InteractionResult.CONSUME;
    }

    public void setWheelDamage(int wheelDamage) {
        this.wheelDamage = Mth.clamp(wheelDamage, 0, 3);
    }

    public int wheelDamage() {
        return wheelDamage;
    }

    public void setShattered(boolean shattered) {
        entityData.set(SHATTERED, shattered);
    }

    public boolean isShattered() {
        return entityData.get(SHATTERED);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        wheelDamage =
                Mth.clamp(
                        tag.getInt("WheelDamage"),
                        0,
                        3
                );

        setShattered(
                tag.getBoolean("Shattered")
        );
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("WheelDamage", wheelDamage);
        tag.putBoolean("Shattered", isShattered());
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
