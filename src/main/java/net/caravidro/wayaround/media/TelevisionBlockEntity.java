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

        playbackStartGameTime =
                level == null
                        ? 0L
                        : level.getGameTime();

        sync();

        return true;
    }

    public void eject() {
        if (level == null
                || level.isClientSide
                || tape.isEmpty()) {

            return;
        }

        ItemStack released =
                tape;

        tape =
                ItemStack.EMPTY;

        playbackStartGameTime =
                -1L;

        double x =
                worldPosition.getX()
                        + 0.5;

        double y =
                worldPosition.getY()
                        + 0.35;

        double z =
                worldPosition.getZ()
                        + 0.5;

        if (getBlockState()
                .hasProperty(
                        TelevisionBlock.FACING
                )) {

            var facing =
                    getBlockState()
                            .getValue(
                                    TelevisionBlock.FACING
                            );

            x += facing.getStepX()
                    * 0.70;

            z += facing.getStepZ()
                    * 0.70;
        }

        Containers.dropItemStack(
                level,
                x,
                y,
                z,
                released
        );

        sync();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            TelevisionBlockEntity television
    ) {
        if (television.tape
                .isEmpty()
                || television.playbackStartGameTime
                < 0L) {

            return;
        }

        VhsData.Info info =
                VhsData.read(
                        television.tape
                )
                        .orElse(null);

        if (info == null) {
            television.eject();
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

            television.eject();
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

        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener>
            getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(
                this
        );
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
