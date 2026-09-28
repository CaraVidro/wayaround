package net.caravidro.wayaround.media;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PlacedPhotoBlockEntity
        extends BlockEntity {

    private ItemStack photo =
            ItemStack.EMPTY;

    public PlacedPhotoBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                MediaContent.PLACED_PHOTO_ENTITY.get(),
                pos,
                state
        );
    }

    public ItemStack photo() {
        return photo;
    }

    public void setPhoto(
            ItemStack stack
    ) {
        photo =
                stack.copyWithCount(
                        1
                );

        sync();
    }

    public ItemStack takePhoto() {
        ItemStack result =
                photo;

        photo =
                ItemStack.EMPTY;

        sync();

        return result;
    }

    private void sync() {
        setChanged();

        if (level != null) {
            BlockState state =
                    getBlockState();

            level.sendBlockUpdated(
                    worldPosition,
                    state,
                    state,
                    3
            );
        }
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(
                tag,
                registries
        );

        if (!photo.isEmpty()) {
            tag.put(
                    "Photo",
                    photo.save(
                            registries
                    )
            );
        }
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(
                tag,
                registries
        );

        photo =
                tag.contains(
                        "Photo",
                        Tag.TAG_COMPOUND
                )
                        ? ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "Photo"
                        )
                )
                        : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        CompoundTag tag =
                new CompoundTag();

        saveAdditional(
                tag,
                registries
        );

        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(
                this
        );
    }
}
