package net.caravidro.wayaround.media;

import net.caravidro.wayaround.media.broadcast.BroadcastFrequency;
import net.caravidro.wayaround.media.broadcast.BroadcastManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TelevisionBlockEntity
        extends BlockEntity {

    public static final int PRE_ROLL_TICKS =
            60;

    private ItemStack tape =
            ItemStack.EMPTY;

    private long playbackStartGameTime =
            -1L;

    private boolean ejected;

    private int lastCountdownBeep =
            -1;

    private boolean startBeepPlayed;

    private int frequencyKHz =
            BroadcastFrequency.DEFAULT_KHZ;

    private float clientTapePrevious;
    private float clientTapeProgress;
    private boolean clientHadTape;

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

    public boolean hasTapeLoaded() {
        return !tape.isEmpty()
                && !ejected;
    }

    public int frequencyKHz() {
        return frequencyKHz;
    }

    public void tuneFrequency(int direction) {
        frequencyKHz =
                BroadcastFrequency.step(
                        frequencyKHz,
                        direction
                );

        sync();
    }

    public boolean isCountingDown() {
        return hasTapeLoaded()
                && level != null
                && playbackStartGameTime >= 0L
                && level.getGameTime()
                < playbackStartGameTime;
    }

    public boolean isPlaying() {
        return hasTapeLoaded()
                && level != null
                && playbackStartGameTime >= 0L
                && level.getGameTime()
                >= playbackStartGameTime;
    }

    public float tapeVisualProgress(
            float partialTick
    ) {
        return Mth.lerp(
                partialTick,
                clientTapePrevious,
                clientTapeProgress
        );
    }

    public int countdownNumber() {
        if (!isCountingDown()) {
            return 0;
        }

        long remaining =
                Math.max(
                        1L,
                        playbackStartGameTime
                                - level.getGameTime()
                );

        return (int) Math.max(
                1L,
                Math.min(
                        3L,
                        (remaining + 19L)
                                / 20L
                )
        );
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

        lastCountdownBeep =
                -1;

        startBeepPlayed =
                false;

        playbackStartGameTime =
                level == null
                        ? PRE_ROLL_TICKS
                        : level.getGameTime()
                                + PRE_ROLL_TICKS;

        updateEjectedBlockState(
                false
        );

        if (level != null
                && !level.isClientSide) {

            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.ITEM_FRAME_ADD_ITEM,
                    SoundSource.BLOCKS,
                    0.75F,
                    0.72F
            );

            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.IRON_TRAPDOOR_CLOSE,
                    SoundSource.BLOCKS,
                    0.30F,
                    1.35F
            );
        }

        sync();
        refreshLight();

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
        refreshLight();

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
        refreshLight();
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

        if (level != null
                && !level.isClientSide) {

            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                    SoundSource.BLOCKS,
                    0.85F,
                    0.68F
            );

            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.IRON_TRAPDOOR_OPEN,
                    SoundSource.BLOCKS,
                    0.32F,
                    1.20F
            );
        }

        sync();
        refreshLight();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            TelevisionBlockEntity television
    ) {
        if (level instanceof net.minecraft.server.level.ServerLevel server
                && level.getGameTime() % 20L == Math.floorMod(pos.asLong(), 20L)) {
            BroadcastManager.heartbeatTelevision(
                    server,
                    pos
            );
        }

        if (television.isCountingDown()) {
            int number =
                    television.countdownNumber();

            if (number
                    != television.lastCountdownBeep) {

                television.lastCountdownBeep =
                        number;

                level.playSound(
                        null,
                        pos,
                        SoundEvents.NOTE_BLOCK_HAT
                                .value(),
                        SoundSource.BLOCKS,
                        0.42F,
                        1.15F
                                + (
                                3 - number
                        ) * 0.09F
                );
            }

            return;
        }

        if (!television.isPlaying()) {
            return;
        }

        if (!television.startBeepPlayed) {
            television.startBeepPlayed =
                    true;

            level.playSound(
                    null,
                    pos,
                    SoundEvents.NOTE_BLOCK_PLING
                            .value(),
                    SoundSource.BLOCKS,
                    0.55F,
                    1.65F
            );
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
        boolean hasTape =
                !television.tape
                        .isEmpty();

        if (hasTape
                && !television.clientHadTape) {

            television.clientTapeProgress =
                    1.0F;

            television.clientTapePrevious =
                    1.0F;
        }

        television.clientHadTape =
                hasTape;

        television.clientTapePrevious =
                television.clientTapeProgress;

        if (hasTape) {
            float target =
                    television.ejected
                            ? 1.0F
                            : 0.0F;

            float speed =
                    television.ejected
                            ? 0.12F
                            : 0.16F;

            television.clientTapeProgress =
                    Mth.approach(
                            television.clientTapeProgress,
                            target,
                            speed
                    );

        } else {
            television.clientTapeProgress =
                    0.0F;

            television.clientTapePrevious =
                    0.0F;
        }

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

        tag.putInt(
                "BroadcastFrequencyKHz",
                frequencyKHz
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

        frequencyKHz =
                BroadcastFrequency.clamp(
                        tag.contains(
                                "BroadcastFrequencyKHz"
                        )
                                ? tag.getInt(
                                "BroadcastFrequencyKHz"
                        )
                                : BroadcastFrequency.DEFAULT_KHZ
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

        tag.putInt(
                "BroadcastFrequencyKHz",
                frequencyKHz
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

    private void refreshLight() {
        if (level != null) {
            level.getLightEngine()
                    .checkBlock(
                            worldPosition
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
