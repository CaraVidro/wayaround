package net.caravidro.wayaround.media.blackbox;

import java.util.List;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.MediaContent;
import net.caravidro.wayaround.worldstate.WorldEvent;
import net.caravidro.wayaround.worldstate.WorldEventTypes;
import net.caravidro.wayaround.worldstate.WorldStateService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BlackBoxBlockEntity
        extends BlockEntity {

    private UUID recordingId;
    private long startedAtMillis;
    private long startGameTime;
    private long durationTicks;
    private long lastWorldSequence;
    private boolean recording;
    private boolean sealed;

    public BlackBoxBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                MediaContent.BLACK_BOX_ENTITY.get(),
                pos,
                state
        );
    }

    public boolean startRecording(
            ServerPlayer player
    ) {
        if (!(level
                instanceof ServerLevel server)
                || sealed
                || recording
                || !isPowered()) {
            return false;
        }

        recordingId =
                UUID.randomUUID();

        startedAtMillis =
                System.currentTimeMillis();

        startGameTime =
                server.getGameTime();

        durationTicks =
                0L;

        List<WorldEvent> recent =
                WorldStateService.recent(
                        server.getServer(),
                        1
                );

        lastWorldSequence =
                recent.isEmpty()
                        ? 0L
                        : recent.getFirst()
                                .sequence();

        recording =
                true;

        try {
            BlackBoxRecordingStore.ensureOpen(
                    server.getServer(),
                    recordingId,
                    server.dimension()
                            .location(),
                    worldPosition,
                    startGameTime,
                    startedAtMillis
            );

            BlackBoxRecordingStore.marker(
                    recordingId,
                    startGameTime,
                    "POWER ON / RECORDING START"
            );

            BlackBoxManager.register(
                    this
            );

            CompoundTag history =
                    new CompoundTag();

            history.putUUID(
                    "recording",
                    recordingId
            );

            history.putString(
                    "operator",
                    player.getGameProfile()
                            .getName()
            );

            WorldStateService.record(
                    server,
                    WorldEventTypes.BLACK_BOX_RECORDING_STARTED,
                    worldPosition,
                    player.getUUID(),
                    history
            );

            setChanged();
            return true;

        } catch (Exception exception) {
            recording =
                    false;

            WayAround.LOGGER.warn(
                    "[BlackBox] failed to start recording: {}",
                    exception.getMessage()
            );

            return false;
        }
    }

    public ItemStack sealToItem(
            ServerPlayer player
    ) {
        if (!(level
                instanceof ServerLevel server)) {
            return new ItemStack(
                    MediaContent.BLACK_BOX_ITEM.get()
            );
        }

        if (recordingId == null) {
            recordingId =
                    UUID.randomUUID();

            startedAtMillis =
                    System.currentTimeMillis();

            startGameTime =
                    server.getGameTime();

            try {
                BlackBoxRecordingStore.ensureOpen(
                        server.getServer(),
                        recordingId,
                        server.dimension()
                                .location(),
                        worldPosition,
                        startGameTime,
                        startedAtMillis
                );

                BlackBoxRecordingStore.marker(
                        recordingId,
                        startGameTime,
                        "SEALED WITHOUT PRIOR RECORDING"
                );

            } catch (Exception exception) {
                WayAround.LOGGER.warn(
                        "[BlackBox] failed to create empty archive: {}",
                        exception.getMessage()
                );
            }
        }

        if (!sealed) {
            durationTicks =
                    Math.max(
                            durationTicks,
                            server.getGameTime()
                                    - startGameTime
                    );

            recording =
                    false;

            sealed =
                    true;

            BlackBoxManager.seal(
                    this
            );

            CompoundTag history =
                    new CompoundTag();

            history.putUUID(
                    "recording",
                    recordingId
            );

            history.putLong(
                    "durationTicks",
                    durationTicks
            );

            WorldStateService.record(
                    server,
                    WorldEventTypes.BLACK_BOX_SEALED,
                    worldPosition,
                    player.getUUID(),
                    history
            );

            setChanged();
        }

        ItemStack stack =
                new ItemStack(
                        MediaContent.BLACK_BOX_ITEM.get()
                );

        BlackBoxData.write(
                stack,
                new BlackBoxData.Info(
                        true,
                        recordingId,
                        startedAtMillis,
                        durationTicks,
                        server.dimension()
                                .location()
                                .toString(),
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ()
                )
        );

        return stack;
    }

    public void loadFromItem(
            ItemStack stack
    ) {
        BlackBoxData.read(
                stack
        ).ifPresent(
                info -> {
                    recordingId =
                            info.recordingId();

                    startedAtMillis =
                            info.startedAtMillis();

                    durationTicks =
                            info.durationTicks();

                    sealed =
                            info.sealed();

                    recording =
                            false;

                    setChanged();
                }
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            BlackBoxBlockEntity box
    ) {
        if (!(level
                instanceof ServerLevel server)) {
            return;
        }

        if (box.recording
                && !box.sealed) {
            BlackBoxManager.register(
                    box
            );

            if (server.getGameTime()
                    % 10L == 0L
                    && box.isCaptureEnabled()) {
                BlackBoxManager.captureWorldState(
                        box
                );
            }

            box.durationTicks =
                    Math.max(
                            box.durationTicks,
                            server.getGameTime()
                                    - box.startGameTime
                    );

            if (server.getGameTime()
                    % 20L == 0L) {
                box.setChanged();
            }
        }
    }

    public boolean isPowered() {
        return getBlockState()
                .getValue(
                        BlackBoxBlock.POWERED
                );
    }

    public boolean isCaptureEnabled() {
        return recording
                && !sealed
                && isPowered();
    }

    public boolean isRecording() {
        return recording;
    }

    public boolean isSealed() {
        return sealed;
    }

    public UUID recordingId() {
        return recordingId;
    }

    public long startGameTime() {
        return startGameTime;
    }

    public long startedAtMillis() {
        return startedAtMillis;
    }

    public long lastWorldSequence() {
        return lastWorldSequence;
    }

    public void setLastWorldSequence(
            long sequence
    ) {
        lastWorldSequence =
                Math.max(
                        lastWorldSequence,
                        sequence
                );

        setChanged();
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

        if (recordingId != null) {
            tag.putUUID(
                    "RecordingId",
                    recordingId
            );
        }

        tag.putLong(
                "StartedAtMillis",
                startedAtMillis
        );

        tag.putLong(
                "StartGameTime",
                startGameTime
        );

        tag.putLong(
                "DurationTicks",
                durationTicks
        );

        tag.putLong(
                "LastWorldSequence",
                lastWorldSequence
        );

        tag.putBoolean(
                "Recording",
                recording
        );

        tag.putBoolean(
                "Sealed",
                sealed
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

        recordingId =
                tag.hasUUID(
                        "RecordingId"
                )
                        ? tag.getUUID(
                        "RecordingId"
                )
                        : null;

        startedAtMillis =
                tag.getLong(
                        "StartedAtMillis"
                );

        startGameTime =
                tag.getLong(
                        "StartGameTime"
                );

        durationTicks =
                tag.getLong(
                        "DurationTicks"
                );

        lastWorldSequence =
                tag.getLong(
                        "LastWorldSequence"
                );

        recording =
                tag.getBoolean(
                        "Recording"
                );

        sealed =
                tag.getBoolean(
                        "Sealed"
                );
    }
}
