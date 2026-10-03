package net.caravidro.wayaround.ecology;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Drifting jellyfish with a permanent visual/biological morph.
 *
 * The morph is rolled once per individual, persisted to NBT and synced to
 * clients. Sting behavior is therefore learnable from appearance instead of
 * being random every time the player touches one.
 */
public final class JellyfishEntity extends AguaWorldFishEntity {

    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(
                    JellyfishEntity.class,
                    EntityDataSerializers.INT
            );

    private boolean variantInitialized;
    private int stingCooldown;

    public JellyfishEntity(
            EntityType<? extends JellyfishEntity> type,
            Level level
    ) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        super.defineSynchedData(builder);

        builder.define(
                VARIANT,
                JellyVariant.MOON.id()
        );
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide
                && !variantInitialized) {
            setVariant(
                    randomVariant()
            );

            variantInitialized =
                    true;
        }

        if (!level().isClientSide && variant()
                == JellyVariant.ABYSSAL_GIANT
                && tickCount % 20 == 0) {
            applyVariantScale();
        }

        if (stingCooldown > 0) {
            stingCooldown--;
        }
    }

    @Override
    public void push(
            Entity entity
    ) {
        super.push(entity);

        if (level().isClientSide
                || stingCooldown > 0
                || !variant().stings()
                || !(entity instanceof LivingEntity living)
                || entity instanceof JellyfishEntity
                || !living.isAlive()) {
            return;
        }

        float damage =
                variant().stingDamage();

        if (living.hurt(
                level().damageSources()
                        .mobAttack(
                                this
                        ),
                damage
        )) {
            stingCooldown =
                    14;

            level().playSound(
                    null,
                    blockPosition(),
                    SoundEvents.GUARDIAN_ATTACK,
                    SoundSource.NEUTRAL,
                    0.34F,
                    1.35F
                            + random.nextFloat()
                                    * 0.25F
            );
        }
    }

    public JellyVariant variant() {
        return JellyVariant.byId(
                entityData.get(
                        VARIANT
                )
        );
    }

    public void setVariant(
            JellyVariant variant
    ) {
        entityData.set(
                VARIANT,
                variant.id()
        );

        if (!level().isClientSide) {
            applyVariantScale();
        }
    }

    private void applyVariantScale() {
        if (variant()
                != JellyVariant.ABYSSAL_GIANT) {
            return;
        }

        AttributeInstance scale =
                getAttribute(
                        Attributes.SCALE
                );

        if (scale != null
                && scale.getBaseValue()
                        < 6.40) {
            scale.setBaseValue(
                    6.40
            );
            refreshDimensions();
        }
    }

    private JellyVariant randomVariant() {
        boolean abyss =
                getY() < 24.0
                        && level().getBiome(
                        blockPosition()
                ).unwrapKey()
                        .map(
                                key ->
                                        key.location()
                                                .getPath()
                                                .contains(
                                                        "deep"
                                                )
                        )
                        .orElse(
                                false
                        );

        if (abyss
                && random.nextFloat()
                        < 0.10F) {
            return JellyVariant.ABYSSAL_GIANT;
        }

        float roll =
                random.nextFloat();

        if (roll < 0.26F) {
            return JellyVariant.MOON;
        }

        if (roll < 0.45F) {
            return JellyVariant.GHOST;
        }

        if (roll < 0.63F) {
            return JellyVariant.ROSE;
        }

        if (roll < 0.78F) {
            return JellyVariant.AMBER;
        }

        if (roll < 0.93F) {
            return JellyVariant.VIOLET;
        }

        return JellyVariant.DEEP_RED;
    }

    @Override
    public void addAdditionalSaveData(
            CompoundTag tag
    ) {
        super.addAdditionalSaveData(tag);

        tag.putInt(
                "JellyVariant",
                variant().id()
        );
    }

    @Override
    public void readAdditionalSaveData(
            CompoundTag tag
    ) {
        super.readAdditionalSaveData(tag);

        if (tag.contains(
                "JellyVariant"
        )) {
            setVariant(
                    JellyVariant.byId(
                            tag.getInt(
                                    "JellyVariant"
                            )
                    )
            );

            variantInitialized =
                    true;
        }
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 16;
    }

    public enum JellyVariant {
        MOON(
                0,
                false,
                0.0F,
                0.78F
        ),
        GHOST(
                1,
                false,
                0.0F,
                1.42F
        ),
        ROSE(
                2,
                true,
                1.5F,
                1.02F
        ),
        AMBER(
                3,
                true,
                2.0F,
                0.66F
        ),
        VIOLET(
                4,
                true,
                2.6F,
                1.24F
        ),
        DEEP_RED(
                5,
                true,
                3.4F,
                0.90F
        ),
        ABYSSAL_GIANT(
                6,
                true,
                5.5F,
                4.80F
        );

        private final int id;
        private final boolean stings;
        private final float stingDamage;
        private final float tentacleScale;

        JellyVariant(
                int id,
                boolean stings,
                float stingDamage,
                float tentacleScale
        ) {
            this.id = id;
            this.stings = stings;
            this.stingDamage = stingDamage;
            this.tentacleScale = tentacleScale;
        }

        public int id() {
            return id;
        }

        public boolean stings() {
            return stings;
        }

        public float stingDamage() {
            return stingDamage;
        }

        public float tentacleScale() {
            return tentacleScale;
        }

        public static JellyVariant byId(
                int id
        ) {
            for (JellyVariant variant :
                    values()) {
                if (variant.id == id) {
                    return variant;
                }
            }

            return MOON;
        }
    }
}
