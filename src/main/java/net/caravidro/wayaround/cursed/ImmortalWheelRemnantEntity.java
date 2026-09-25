package net.caravidro.wayaround.cursed;

import net.caravidro.wayaround.content.WayAroundContent;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Physical dormant state of the Immortal Wheel.
 *
 * It is a real persistent entity rather than an ItemEntity: it never despawns,
 * is invulnerable, survives lava, stores the wheel's adaptation memory and can
 * only return to inventory by deliberate interaction.
 */
public final class ImmortalWheelRemnantEntity extends Entity {

    private CompoundTag memory =
            new CompoundTag();

    public ImmortalWheelRemnantEntity(
            EntityType<? extends ImmortalWheelRemnantEntity> type,
            Level level
    ) {
        super(
                type,
                level
        );

        setInvulnerable(
                true
        );
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
    }

    @Override
    public void tick() {
        super.tick();

        if (isRemoved()) {
            return;
        }

        Vec3 velocity =
                getDeltaMovement();

        if (!isNoGravity()) {
            velocity =
                    velocity.add(
                            0.0,
                            -0.045,
                            0.0
                    );
        }

        setDeltaMovement(
                velocity
        );

        move(
                MoverType.SELF,
                velocity
        );

        Vec3 after =
                getDeltaMovement();

        if (onGround()) {
            setDeltaMovement(
                    after.x * 0.58,
                    0.0,
                    after.z * 0.58
            );
        } else {
            setDeltaMovement(
                    after.scale(
                            0.985
                    )
            );
        }

        clearFire();
    }

    @Override
    public InteractionResult interact(
            Player player,
            InteractionHand hand
    ) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ItemStack wheel =
                new ItemStack(
                        WayAroundContent.IMMORTAL_WHEEL.get()
                );

        boolean received =
                serverPlayer.getInventory()
                        .add(
                                wheel
                        );

        if (!received
                && serverPlayer.getItemInHand(hand)
                        .isEmpty()) {

            serverPlayer.setItemInHand(
                    hand,
                    wheel
            );

            received =
                    true;
        }

        if (!received) {
            return InteractionResult.CONSUME;
        }

        Vec3 origin =
                position()
                        .add(
                                0.0,
                                0.16,
                                0.0
                        );

        ImmortalWheelManager.reactivateFromRemnant(
                serverPlayer,
                memory.copy(),
                origin
        );

        discard();

        return InteractionResult.CONSUME;
    }

    public void setMemory(
            CompoundTag memory
    ) {
        this.memory =
                memory == null
                        ? new CompoundTag()
                        : memory.copy();
    }

    public CompoundTag memory() {
        return memory.copy();
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        memory =
                tag.contains(
                        "WheelMemory"
                )
                        ? tag.getCompound(
                                "WheelMemory"
                        ).copy()
                        : new CompoundTag();
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        tag.put(
                "WheelMemory",
                memory.copy()
        );
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
