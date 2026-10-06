package net.caravidro.wayaround.accessory;

import net.caravidro.wayaround.content.OddityContent;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class FlyingTopHatEntity extends Entity {

    private static final EntityDataAccessor<Integer> WEAR =
            SynchedEntityData.defineId(
                    FlyingTopHatEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Integer> MATERIAL =
            SynchedEntityData.defineId(
                    FlyingTopHatEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Integer> SIZE =
            SynchedEntityData.defineId(
                    FlyingTopHatEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Integer> EXTRAS =
            SynchedEntityData.defineId(
                    FlyingTopHatEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<ItemStack> HAT_STACK =
            SynchedEntityData.defineId(FlyingTopHatEntity.class, EntityDataSerializers.ITEM_STACK);
    private int groundedTicks;

    /** Owns one real stack throughout flight, save/reload and recovery. */
    public void setHatStack(ItemStack stack) {
        if (!(stack.getItem() instanceof AccessoryItem item)
                || item.kind().slot() != AccessorySlot.HEAD
                || !item.kind().windLoose())
            throw new IllegalArgumentException("Only wind-loose head accessories may be transferred");
        entityData.set(HAT_STACK, stack.copyWithCount(1));
        setWear(stack.getDamageValue());
        if (item.kind() == AccessoryKind.ENGINEER_CAP)
            setCustomization(AccessoryCustomizationData.read(stack, item.kind()));
    }

    public AccessoryKind hatKind() {
        ItemStack stack = entityData.get(HAT_STACK);
        return stack.getItem() instanceof AccessoryItem item ? item.kind() : AccessoryKind.ENGINEER_CAP;
    }

    public ItemStack hatStack() {
        ItemStack stack = entityData.get(HAT_STACK);
        if (!stack.isEmpty()) return stack.copy();
        // Compatibility with flying engineer hats saved before complete-stack ownership.
        stack = OddityContent.accessoryStack(AccessoryKind.ENGINEER_CAP);
        AccessoryWear.setWear(stack, AccessoryKind.ENGINEER_CAP, wear());
        AccessoryCustomizationData.write(stack, AccessoryKind.ENGINEER_CAP,
                new AccessoryCustomizationData.Config(material(), size(), extras(), 4));
        return stack;
    }

    public FlyingTopHatEntity(
            EntityType<? extends FlyingTopHatEntity> type,
            Level level
    ) {
        super(
                type,
                level
        );
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        builder.define(HAT_STACK, ItemStack.EMPTY);
        builder.define(
                WEAR,
                0
        );

        builder.define(
                MATERIAL,
                0
        );

        builder.define(
                SIZE,
                2
        );

        builder.define(
                EXTRAS,
                AccessoryCustomizationData.EXTRA_GEARS
                        | AccessoryCustomizationData.EXTRA_CLOCK
        );
    }

    public int wear() {
        return entityData.get(
                WEAR
        );
    }

    public void setWear(
            int wear
    ) {
        entityData.set(
                WEAR,
                Math.max(
                        0,
                        wear
                )
        );
    }

    public void setCustomization(
            AccessoryCustomizationData.Config config
    ) {
        AccessoryCustomizationData.Config safe =
                AccessoryCustomizationData.sanitize(
                        AccessoryKind.ENGINEER_CAP,
                        config
                );

        entityData.set(
                MATERIAL,
                safe.material()
        );
        entityData.set(
                SIZE,
                safe.size()
        );
        entityData.set(
                EXTRAS,
                safe.extras()
        );
    }

    public int material() {
        return entityData.get(
                MATERIAL
        );
    }

    public int size() {
        return entityData.get(
                SIZE
        );
    }

    public int extras() {
        return entityData.get(
                EXTRAS
        );
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 velocity =
                getDeltaMovement();

        float windX =
                0.0F;

        float windZ =
                0.0F;

        float windStrength =
                0.20F;

        if (level().dimension()
                .equals(
                        Level.OVERWORLD
                )) {
            LocalWeatherField.Sample sample =
                    LocalWeatherField.sample(
                            getX(),
                            getZ(),
                            level().getGameTime()
                    );

            windX =
                    sample.windX();

            windZ =
                    sample.windZ();

            windStrength +=
                    sample.warning()
                            * 0.80F;
        }

        velocity =
                velocity.add(
                                windX
                                        * 0.0045
                                        * windStrength,
                                -0.041,
                                windZ
                                        * 0.0045
                                        * windStrength
                        )
                        .scale(
                                onGround()
                                        ? 0.79
                                        : 0.985
                        );

        setDeltaMovement(
                velocity
        );

        move(
                MoverType.SELF,
                velocity
        );

        if (onGround()) {
            groundedTicks++;
        } else {
            groundedTicks =
                    0;
        }

        if (!level().isClientSide
                && (
                groundedTicks > 48
                        || tickCount > 260
        )) {
            dropHat();
        }
    }

    @Override
    public void playerTouch(
            Player player
    ) {
        super.playerTouch(
                player
        );

        if (level().isClientSide
                || !isAlive()) {
            return;
        }

        ItemStack stack =
                hatStack();

        if (stack.isEmpty()) {
            discard();
            return;
        }

        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(
                    InteractionHand.MAIN_HAND,
                    stack
            );
            discard();
            return;
        }

        if (player.getInventory().add(stack)) {
            discard();
        }
    }

    private void dropHat() {
        ItemStack stack = hatStack();
        if (!stack.isEmpty()) {
            ItemEntity item =
                    new ItemEntity(
                            level(),
                            getX(),
                            getY()
                                    + 0.08,
                            getZ(),
                            stack
                    );

            item.setDeltaMovement(
                    getDeltaMovement()
                            .scale(
                                    0.20
                            )
                );

            if (!level().addFreshEntity(item)) return; // Keep ownership if transfer fails.
        }

        discard();
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        if (tag.contains("HatStack")) {
            ItemStack stack = ItemStack.parseOptional(registryAccess(), tag.getCompound("HatStack"));
            if (!stack.isEmpty()) setHatStack(stack);
        }
        setWear(
                tag.getInt(
                        "Wear"
                )
        );

        groundedTicks =
                tag.getInt(
                        "GroundedTicks"
                );

        setCustomization(
                new AccessoryCustomizationData.Config(
                        tag.getInt(
                                "Material"
                        ),
                        tag.contains(
                                "Size"
                        )
                                ? tag.getInt(
                                "Size"
                        )
                                : 2,
                        tag.contains(
                                "Extras"
                        )
                                ? tag.getInt(
                                "Extras"
                        )
                                : AccessoryCustomizationData.EXTRA_GEARS
                                | AccessoryCustomizationData.EXTRA_CLOCK,
                        4
                )
        );
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        tag.put("HatStack", hatStack().save(registryAccess()));
        tag.putInt(
                "Wear",
                wear()
        );

        tag.putInt(
                "GroundedTicks",
                groundedTicks
        );

        tag.putInt(
                "Material",
                material()
        );

        tag.putInt(
                "Size",
                size()
        );

        tag.putInt(
                "Extras",
                extras()
        );
    }
}
