package net.caravidro.wayaround.worldgen.weather.fire;

import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A coarse voxel smoke parcel used only for wildfire long-distance LOD.
 *
 * It is an entity rather than a placed block so it can drift continuously with
 * the weather wind, rise, expand and dissipate without editing the terrain.
 */
public final class SmokeVolumeEntity
        extends Entity {

    private static final EntityDataAccessor<Float> BASE_SIZE =
            SynchedEntityData.defineId(
                    SmokeVolumeEntity.class,
                    EntityDataSerializers.FLOAT
            );

    private static final EntityDataAccessor<Integer> MAX_LIFE =
            SynchedEntityData.defineId(
                    SmokeVolumeEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Float> DARKNESS =
            SynchedEntityData.defineId(
                    SmokeVolumeEntity.class,
                    EntityDataSerializers.FLOAT
            );

    private int age;

    public SmokeVolumeEntity(
            EntityType<? extends SmokeVolumeEntity> type,
            Level level
    ) {
        super(
                type,
                level
        );

        noPhysics =
                true;

        setNoGravity(
                true
        );
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        builder.define(
                BASE_SIZE,
                1.0F
        );

        builder.define(
                MAX_LIFE,
                280
        );

        builder.define(
                DARKNESS,
                0.55F
        );
    }

    public void configure(
            float size,
            int lifetime,
            float darkness
    ) {
        entityData.set(
                BASE_SIZE,
                Mth.clamp(
                        size,
                        0.5F,
                        6.0F
                )
        );

        entityData.set(
                MAX_LIFE,
                Mth.clamp(
                        lifetime,
                        100,
                        720
                )
        );

        entityData.set(
                DARKNESS,
                Mth.clamp(
                        darkness,
                        0.0F,
                        1.0F
                )
        );
    }

    @Override
    public void tick() {
        super.tick();

        age++;

        int lifetime =
                maxLife();

        if (age >= lifetime) {
            if (!level().isClientSide) {
                discard();
            }
            return;
        }

        if (level().isClientSide) {
            return;
        }

        LocalWeatherField.Sample weather =
                LocalWeatherField.sample(
                        getX(),
                        getZ(),
                        level().getGameTime()
                );

        float life =
                age
                        / (float) lifetime;

        /*
         * Smoke rises fast at first, then wind increasingly dominates as the
         * parcel expands. Weather warning is also the synchronized gust/debug
         * intensity, so /wayaround windtest visibly pushes wildfire smoke.
         */
        double windSpeed =
                0.020
                        + weather.warning()
                                * 0.050
                        + baseSize()
                                * 0.0025;

        double rise =
                0.050
                        + baseSize()
                                * 0.006
                        - life
                                * 0.018;

        double turbulence =
                Math.sin(
                        tickCount
                                * 0.071
                                + getId()
                                        * 1.731
                )
                        * 0.006;

        double sideX =
                -weather.windZ()
                        * turbulence;

        double sideZ =
                weather.windX()
                        * turbulence;

        setPos(
                getX()
                        + weather.windX()
                                * windSpeed
                        + sideX,
                getY()
                        + Math.max(
                        0.018,
                        rise
                ),
                getZ()
                        + weather.windZ()
                                * windSpeed
                        + sideZ
        );
    }

    public float baseSize() {
        return entityData.get(
                BASE_SIZE
        );
    }

    public int maxLife() {
        return entityData.get(
                MAX_LIFE
        );
    }

    public float darkness() {
        return entityData.get(
                DARKNESS
        );
    }

    public float lifeFraction(
            float partialTick
    ) {
        return Mth.clamp(
                (
                        age
                                + partialTick
                )
                        / Math.max(
                        1.0F,
                        maxLife()
                ),
                0.0F,
                1.0F
        );
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        age =
                Math.max(
                        0,
                        tag.getInt(
                                "Age"
                        )
                );

        configure(
                tag.contains(
                        "BaseSize"
                )
                        ? tag.getFloat(
                        "BaseSize"
                )
                        : 1.0F,
                tag.contains(
                        "MaxLife"
                )
                        ? tag.getInt(
                        "MaxLife"
                )
                        : 280,
                tag.contains(
                        "Darkness"
                )
                        ? tag.getFloat(
                        "Darkness"
                )
                        : 0.55F
        );
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        tag.putInt(
                "Age",
                age
        );

        tag.putFloat(
                "BaseSize",
                baseSize()
        );

        tag.putInt(
                "MaxLife",
                maxLife()
        );

        tag.putFloat(
                "Darkness",
                darkness()
        );
    }
}
