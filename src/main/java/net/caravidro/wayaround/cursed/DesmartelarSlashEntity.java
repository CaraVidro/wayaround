package net.caravidro.wayaround.cursed;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class DesmartelarSlashEntity extends Entity {

    private static final EntityDataAccessor<Float> LENGTH =
            SynchedEntityData.defineId(
                    DesmartelarSlashEntity.class,
                    EntityDataSerializers.FLOAT
            );

    private static final EntityDataAccessor<Float> WIDTH =
            SynchedEntityData.defineId(
                    DesmartelarSlashEntity.class,
                    EntityDataSerializers.FLOAT
            );

    private static final EntityDataAccessor<Float> ROLL =
            SynchedEntityData.defineId(
                    DesmartelarSlashEntity.class,
                    EntityDataSerializers.FLOAT
            );

    private static final EntityDataAccessor<Boolean> FIRE =
            SynchedEntityData.defineId(
                    DesmartelarSlashEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    public DesmartelarSlashEntity(
            EntityType<? extends DesmartelarSlashEntity> type,
            Level level
    ) {
        super(
                type,
                level
        );

        setNoGravity(
                true
        );
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        builder.define(
                LENGTH,
                5.0F
        );

        builder.define(
                WIDTH,
                0.65F
        );

        builder.define(
                ROLL,
                0.0F
        );

        builder.define(
                FIRE,
                false
        );
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 velocity =
                getDeltaMovement();

        setPos(
                getX() + velocity.x,
                getY() + velocity.y,
                getZ() + velocity.z
        );

        if (tickCount >= 10) {
            discard();
        }
    }

    public void configure(
            float length,
            float width,
            float roll,
            boolean fire,
            Vec3 velocity
    ) {
        entityData.set(
                LENGTH,
                length
        );

        entityData.set(
                WIDTH,
                width
        );

        entityData.set(
                ROLL,
                roll
        );

        entityData.set(
                FIRE,
                fire
        );

        setDeltaMovement(
                velocity
        );
    }

    public float length() {
        return entityData.get(
                LENGTH
        );
    }

    public float width() {
        return entityData.get(
                WIDTH
        );
    }

    public float roll() {
        return entityData.get(
                ROLL
        );
    }

    public boolean fiery() {
        return entityData.get(
                FIRE
        );
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        entityData.set(
                LENGTH,
                tag.getFloat(
                        "Length"
                )
        );

        entityData.set(
                WIDTH,
                tag.getFloat(
                        "Width"
                )
        );

        entityData.set(
                ROLL,
                tag.getFloat(
                        "Roll"
                )
        );

        entityData.set(
                FIRE,
                tag.getBoolean(
                        "Fire"
                )
        );
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        tag.putFloat(
                "Length",
                length()
        );

        tag.putFloat(
                "Width",
                width()
        );

        tag.putFloat(
                "Roll",
                roll()
        );

        tag.putBoolean(
                "Fire",
                fiery()
        );
    }
}
