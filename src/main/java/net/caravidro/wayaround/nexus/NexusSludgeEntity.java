package net.caravidro.wayaround.nexus;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class NexusSludgeEntity extends Entity {

    public NexusSludgeEntity(
            EntityType<? extends NexusSludgeEntity> type,
            Level level
    ) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 motion =
                getDeltaMovement()
                        .add(
                                0.0,
                                -0.045,
                                0.0
                        )
                        .scale(
                                0.985
                        );

        setDeltaMovement(motion);

        move(
                MoverType.SELF,
                motion
        );

        if (level().isClientSide) {
            return;
        }

        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }

        if (tickCount % 2 == 0) {
            level.sendParticles(
                    ParticleTypes.SMOKE,
                    getX(),
                    getY(),
                    getZ(),
                    1,
                    0.05,
                    0.05,
                    0.05,
                    0.0
            );

            level.sendParticles(
                    DustParticleOptions.REDSTONE,
                    getX(),
                    getY(),
                    getZ(),
                    1,
                    0.02,
                    0.02,
                    0.02,
                    0.0
            );
        }

        if (onGround()
                || horizontalCollision
                || tickCount > 160) {
            NexusEventManager.splashSludge(
                    level,
                    blockPosition()
            );

            level.playSound(
                    null,
                    blockPosition(),
                    SoundEvents.SLIME_SQUISH_SMALL,
                    SoundSource.BLOCKS,
                    1.1F,
                    0.55F
            );

            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
    }
}
