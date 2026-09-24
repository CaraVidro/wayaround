package net.caravidro.wayaround.media;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TelevisionBlockEntity
        extends BlockEntity {

    private ItemStack tape =
            ItemStack.EMPTY;

    private long playbackStartGameTime =
            -1L;

    private boolean ejected;

    public TelevisionBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                MediaContent.TELEVISION_ENTITY.get(),
                pos,
                state
        );
    }

    public ItemStack tape() {
        return tape;
    }

    public long playbackStartGameTime() {
        return playbackStartGameTime;
    }

    public boolean isEjected() {
        return ejected
                && !tape.isEmpty();
    }

    public boolean isPlaying() {
        return !tape.isEmpty()
                && !ejected
                && playbackStartGameTime
                >= 0L;
    }

    public boolean insert(
            ItemStack source
    ) {
        if (!tape.isEmpty()
                || source.isEmpty()
                || !source.is(
                        MediaContent.VHS.get()
                )
                || VhsData.read(source)
                .isEmpty()) {

            return false;
        }

        tape =
                source.split(1);

        ejected =
                false;

        playbackStartGameTime =
                level == null
                        ? 0L
                        : level.getGameTime();

        updateEjectedBlockState(
                false
        );

        sync();

        return true;
    }

    public ItemStack takeEjected() {
        if (!isEjected()) {
            return ItemStack.EMPTY;
        }

        ItemStack result =
                tape;

        tape =
                ItemStack.EMPTY;

        ejected =
                false;

        playbackStartGameTime =
                -1L;

        updateEjectedBlockState(
                false
        );

        sync();

        return result;
    }

    public void dropTape() {
        if (level == null
                || level.isClientSide
                || tape.isEmpty()) {

            return;
        }

        ItemStack released =
                tape;

        tape =
                ItemStack.EMPTY;

        ejected =
                false;

        playbackStartGameTime =
                -1L;

        Containers.dropItemStack(
                level,
                worldPosition.getX()
                        + 0.5,
                worldPosition.getY()
                        + 0.35,
                worldPosition.getZ()
                        + 0.5,
                released
        );

        sync();
    }

    private void finishPlayback() {
        if (tape.isEmpty()
                || ejected) {

            return;
        }

        ejected =
                true;

        playbackStartGameTime =
                -1L;

        updateEjectedBlockState(
                true
        );

        sync();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            TelevisionBlockEntity television
    ) {
        if (!television.isPlaying()) {
            return;
        }

        VhsData.Info info =
                VhsData.read(
                        television.tape
                )
                        .orElse(null);

        if (info == null) {
            television.finishPlayback();
            return;
        }

        long durationTicks =
                Math.max(
                        1L,
                        (info.durationMillis()
                                + 49L)
                                / 50L
                );

        if (level.getGameTime()
                - television.playbackStartGameTime
                >= durationTicks) {

            television.finishPlayback();
        }
    }

    public static void clientTick(
            Level level,
            BlockPos pos,
            BlockState state,
            TelevisionBlockEntity television
    ) {
        MediaClientBridge.televisionTick(
                television
        );
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

        if (!tape.isEmpty()) {
            tag.put(
                    "Tape",
                    tape.saveOptional(
                            registries
                    )
            );
        }

        tag.putLong(
                "PlaybackStart",
                playbackStartGameTime
        );

        tag.putBoolean(
                "Ejected",
                ejected
        );
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

        tape =
                tag.contains(
                        "Tape",
                        Tag.TAG_COMPOUND
                )
                        ? ItemStack.parseOptional(
                                registries,
                                tag.getCompound(
                                        "Tape"
                                )
                        )
                        : ItemStack.EMPTY;

        playbackStartGameTime =
                tag.contains(
                        "PlaybackStart"
                )
                        ? tag.getLong(
                                "PlaybackStart"
                        )
                        : -1L;

        ejected =
                tag.getBoolean(
                        "Ejected"
                );
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        CompoundTag tag =
                super.getUpdateTag(
                        registries
                );

        if (!tape.isEmpty()) {
            tag.put(
                    "Tape",
                    tape.saveOptional(
                            registries
                    )
            );
        }

        tag.putLong(
                "PlaybackStart",
                playbackStartGameTime
        );

        tag.putBoolean(
                "Ejected",
                ejected
        );

        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener>
            getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(
                this
        );
    }

    private void updateEjectedBlockState(
            boolean value
    ) {
        if (level == null) {
            return;
        }

        BlockState state =
                getBlockState();

        if (state.hasProperty(
                TelevisionBlock.EJECTED
        )
                && state.getValue(
                        TelevisionBlock.EJECTED
                )
                != value) {

            level.setBlock(
                    worldPosition,
                    state.setValue(
                            TelevisionBlock.EJECTED,
                            value
                    ),
                    Block.UPDATE_ALL
            );
        }
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
                    Block.UPDATE_CLIENTS
            );
        }
    }
}
