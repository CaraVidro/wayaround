package net.caravidro.wayaround.media;

import java.util.UUID;

import net.caravidro.wayaround.network.PlacedCameraStartS2CPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlacedCameraBlockEntity
        extends BlockEntity {

    private static final int COUNTDOWN_TICKS =
            60;

    private UUID owner;
    private long startGameTime = -1L;
    private Direction facing = Direction.NORTH;
    private int lastBeep = -1;
    private boolean started;

    public PlacedCameraBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                MediaContent.PLACED_CAMERA_ENTITY.get(),
                pos,
                state
        );
    }

    public void arm(
            UUID owner,
            Direction facing
    ) {
        this.owner = owner;
        this.facing = facing;

        this.startGameTime =
                level == null
                        ? COUNTDOWN_TICKS
                        : level.getGameTime()
                                + COUNTDOWN_TICKS;

        this.lastBeep = -1;
        this.started = false;

        setChanged();
    }

    public boolean isOwner(
            UUID id
    ) {
        return owner != null
                && owner.equals(id);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            PlacedCameraBlockEntity camera
    ) {
        if (camera.owner == null
                || camera.startGameTime < 0L
                || camera.started) {

            return;
        }

        long remaining =
                camera.startGameTime
                        - level.getGameTime();

        if (remaining > 0L) {
            int number =
                    (int) Math.max(
                            1L,
                            Math.min(
                                    3L,
                                    (remaining + 19L)
                                            / 20L
                            )
                    );

            if (number != camera.lastBeep) {
                camera.lastBeep =
                        number;

                level.playSound(
                        null,
                        pos,
                        SoundEvents.NOTE_BLOCK_HAT
                                .value(),
                        SoundSource.BLOCKS,
                        0.50F,
                        1.05F
                                + (3 - number)
                                * 0.10F
                );
            }

            return;
        }

        camera.started = true;
        camera.setChanged();

        level.playSound(
                null,
                pos,
                SoundEvents.NOTE_BLOCK_PLING
                        .value(),
                SoundSource.BLOCKS,
                0.65F,
                1.65F
        );

        if (level.getServer() == null) {
            return;
        }

        ServerPlayer player =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(
                                camera.owner
                        );

        if (player != null) {
            PacketDistributor.sendToPlayer(
                    player,
                    new PlacedCameraStartS2CPayload(
                            pos,
                            camera.facing
                    )
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

        if (owner != null) {
            tag.putUUID(
                    "Owner",
                    owner
            );
        }

        tag.putLong(
                "StartGameTime",
                startGameTime
        );

        tag.putString(
                "Facing",
                facing.getName()
        );

        tag.putInt(
                "LastBeep",
                lastBeep
        );

        tag.putBoolean(
                "Started",
                started
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

        owner =
                tag.hasUUID(
                        "Owner"
                )
                        ? tag.getUUID(
                                "Owner"
                        )
                        : null;

        startGameTime =
                tag.getLong(
                        "StartGameTime"
                );

        facing =
                Direction.byName(
                        tag.getString(
                                "Facing"
                        )
                );

        if (facing == null
                || !facing.getAxis()
                .isHorizontal()) {

            facing =
                    Direction.NORTH;
        }

        lastBeep =
                tag.getInt(
                        "LastBeep"
                );

        started =
                tag.getBoolean(
                        "Started"
                );
    }
}
