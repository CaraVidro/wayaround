package net.caravidro.wayaround.cursed;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

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

    public DesmartelarSlashEntity(
            EntityType<? extends DesmartelarSlashEntity> type,
            Level level
    ) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        builder.define(LENGTH, 8.0F);
        builder.define(WIDTH, 0.65F);
    }

    @Override
    public void tick() {
        super.tick();

        if (tickCount >= 8) {
            discard();
        }
    }

    public void configure(
            float length,
            float width
    ) {
        entityData.set(LENGTH, length);
        entityData.set(WIDTH, width);
    }

    public float length() {
        return entityData.get(LENGTH);
    }

    public float width() {
        return entityData.get(WIDTH);
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        entityData.set(
                LENGTH,
                tag.getFloat("Length")
        );

        entityData.set(
                WIDTH,
                tag.getFloat("Width")
        );
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        tag.putFloat("Length", length());
        tag.putFloat("Width", width());
    }
}
