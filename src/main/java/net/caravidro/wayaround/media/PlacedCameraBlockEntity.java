package net.caravidro.wayaround.media;

import java.util.UUID;

import net.caravidro.wayaround.media.broadcast.BroadcastFrequency;
import net.caravidro.wayaround.media.broadcast.BroadcastManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PlacedCameraBlockEntity extends BlockEntity {
    private static final int COUNTDOWN_TICKS = 60;

    private UUID owner;
    private long startGameTime = -1L;
    private Direction facing = Direction.NORTH;
    private int lastBeep = -1;
    private boolean started;
    private boolean integratedAntenna;
    private int frequencyKHz = BroadcastFrequency.DEFAULT_KHZ;

    public PlacedCameraBlockEntity(BlockPos pos, BlockState state) {
        super(MediaContent.PLACED_CAMERA_ENTITY.get(), pos, state);
    }

    public void arm(UUID owner, Direction facing) {
        this.owner = owner;
        this.facing = facing;
        this.startGameTime = level == null ? COUNTDOWN_TICKS : level.getGameTime() + COUNTDOWN_TICKS;
        this.lastBeep = -1;
        this.started = false;
        setChanged();
    }

    public boolean isOwner(UUID id) {
        return owner != null && owner.equals(id);
    }

    public Direction facing() {
        return facing;
    }

    public boolean hasIntegratedAntenna() {
        return integratedAntenna;
    }

    public int frequencyKHz() {
        return frequencyKHz;
    }

    public double integratedRangeBlocks() {
        return 960.0;
    }

    public boolean installIntegratedAntenna() {
        if (integratedAntenna) return false;
        integratedAntenna = true;
        setChanged();
        return true;
    }

    public void setFrequencyKHz(
            int frequencyKHz
    ) {
        this.frequencyKHz =
                BroadcastFrequency.clamp(
                        frequencyKHz
                );

        setChanged();
    }

    public void tuneFrequency(int direction) {
        frequencyKHz = BroadcastFrequency.step(frequencyKHz, direction);
        setChanged();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            PlacedCameraBlockEntity camera
    ) {
        if (camera.owner == null || camera.startGameTime < 0L) return;

        if (!camera.started) {
            long remaining = camera.startGameTime - level.getGameTime();

            if (remaining > 0L) {
                int number = (int) Math.max(1L, Math.min(3L, (remaining + 19L) / 20L));
                if (number != camera.lastBeep) {
                    camera.lastBeep = number;
                    level.playSound(
                            null,
                            pos,
                            SoundEvents.NOTE_BLOCK_HAT.value(),
                            SoundSource.BLOCKS,
                            0.50F,
                            1.05F + (3 - number) * 0.10F
                    );
                }
                return;
            }

            camera.started = true;
            camera.setChanged();

            level.playSound(
                    null,
                    pos,
                    SoundEvents.NOTE_BLOCK_PLING.value(),
                    SoundSource.BLOCKS,
                    0.65F,
                    1.65F
            );
        }

        if (camera.started
                && level instanceof ServerLevel server
                && level.getGameTime() % 20L == Math.floorMod(pos.asLong(), 20L)) {
            BroadcastManager.broadcastPlacedCamera(server, camera);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putLong("StartGameTime", startGameTime);
        tag.putString("Facing", facing.getName());
        tag.putInt("LastBeep", lastBeep);
        tag.putBoolean("Started", started);
        tag.putBoolean("IntegratedAntenna", integratedAntenna);
        tag.putInt("BroadcastFrequencyKHz", frequencyKHz);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        startGameTime = tag.getLong("StartGameTime");
        facing = Direction.byName(tag.getString("Facing"));
        if (facing == null || !facing.getAxis().isHorizontal()) facing = Direction.NORTH;
        lastBeep = tag.getInt("LastBeep");
        started = tag.getBoolean("Started");
        integratedAntenna = tag.getBoolean("IntegratedAntenna");
        frequencyKHz = BroadcastFrequency.clamp(
                tag.contains("BroadcastFrequencyKHz")
                        ? tag.getInt("BroadcastFrequencyKHz")
                        : BroadcastFrequency.DEFAULT_KHZ
        );
    }
}
