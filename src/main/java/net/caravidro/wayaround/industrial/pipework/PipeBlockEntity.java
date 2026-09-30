package net.caravidro.wayaround.industrial.pipework;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PipeBlockEntity
        extends BlockEntity {

    private PipeMedium medium =
            PipeMedium.EMPTY;

    private int amount;

    private int lastFlow;

    private long lastSyncTick;

    public PipeBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PipeworkContent.PIPE_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            PipeBlockEntity pipe
    ) {
        if (!(level instanceof ServerLevel server)
                || !(state.getBlock()
                instanceof PipeBlock block)) {
            return;
        }

        pipe.lastFlow =
                0;

        for (Direction direction :
                Direction.values()) {

            BlockPos otherPos =
                    pos.relative(
                            direction
                    );

            /*
             * Each pair is solved once per tick, otherwise A->B and B->A can
             * oscillate in the same server step.
             */
            if (pos.asLong()
                    >= otherPos.asLong()) {
                continue;
            }

            if (!(server.getBlockEntity(
                    otherPos
            ) instanceof PipeBlockEntity other)
                    || !(other.getBlockState()
                    .getBlock()
                    instanceof PipeBlock)) {
                continue;
            }

            int moved =
                    equalize(
                            pipe,
                            other
                    );

            pipe.lastFlow =
                    Math.max(
                            pipe.lastFlow,
                            moved
                    );

            other.lastFlow =
                    Math.max(
                            other.lastFlow,
                            moved
                    );
        }

        if (pipe.amount <= 0) {
            pipe.amount =
                    0;

            pipe.medium =
                    PipeMedium.EMPTY;
        }

        if (level.getGameTime()
                - pipe.lastSyncTick >= 5) {
            pipe.sync();
            pipe.lastSyncTick =
                    level.getGameTime();
        } else {
            pipe.setChanged();
        }
    }

    private static int equalize(
            PipeBlockEntity first,
            PipeBlockEntity second
    ) {
        PipeProfile firstProfile =
                first.profile();

        PipeProfile secondProfile =
                second.profile();

        if (firstProfile == null
                || secondProfile == null
                || firstProfile.pipeClass()
                        != secondProfile.pipeClass()) {
            return 0;
        }

        PipeMedium medium =
                first.medium != PipeMedium.EMPTY
                        ? first.medium
                        : second.medium;

        if (medium == PipeMedium.EMPTY) {
            return 0;
        }

        if (first.medium != PipeMedium.EMPTY
                && second.medium != PipeMedium.EMPTY
                && first.medium != second.medium) {
            return 0;
        }

        if (!first.canAccept(
                medium
        )
                || !second.canAccept(
                medium
        )) {
            return 0;
        }

        int totalCapacity =
                firstProfile.capacity()
                        + secondProfile.capacity();

        int totalAmount =
                first.amount
                        + second.amount;

        if (totalAmount <= 0
                || totalCapacity <= 0) {
            return 0;
        }

        int desiredFirst =
                Math.round(
                        totalAmount
                                * (
                                firstProfile.capacity()
                                        / (float) totalCapacity
                        )
                );

        int delta =
                first.amount
                        - desiredFirst;

        if (Math.abs(
                delta
        ) <= 1) {
            return 0;
        }

        int rate =
                Math.min(
                        firstProfile.throughputPerTick(),
                        secondProfile.throughputPerTick()
                );

        if (delta > 0) {
            return transfer(
                    first,
                    second,
                    medium,
                    Math.min(
                            delta,
                            rate
                    )
            );
        }

        return transfer(
                second,
                first,
                medium,
                Math.min(
                        -delta,
                        rate
                )
        );
    }

    private static int transfer(
            PipeBlockEntity source,
            PipeBlockEntity target,
            PipeMedium medium,
            int requested
    ) {
        int room =
                target.profile()
                        .capacity()
                        - target.amount;

        int moved =
                Math.min(
                        requested,
                        Math.min(
                                source.amount,
                                Math.max(
                                        0,
                                        room
                                )
                        )
                );

        if (moved <= 0
                || !target.canAccept(
                medium
        )) {
            return 0;
        }

        source.amount -=
                moved;

        target.amount +=
                moved;

        target.medium =
                medium;

        if (source.amount <= 0) {
            source.amount =
                    0;

            source.medium =
                    PipeMedium.EMPTY;
        }

        source.setChanged();
        target.setChanged();

        return moved;
    }

    public PipeProfile profile() {
        if (getBlockState()
                .getBlock()
                instanceof PipeBlock block) {
            return block.profile();
        }

        return null;
    }

    public PipeMedium medium() {
        return medium;
    }

    public int amount() {
        return amount;
    }

    public int lastFlow() {
        return lastFlow;
    }

    public int insert(
            PipeMedium requestedMedium,
            int requestedAmount
    ) {
        PipeProfile profile =
                profile();

        if (profile == null
                || requestedAmount <= 0
                || !canAccept(
                requestedMedium
        )
                || (
                medium != PipeMedium.EMPTY
                        && medium != requestedMedium
        )) {
            return 0;
        }

        int accepted =
                Math.min(
                        requestedAmount,
                        profile.capacity()
                                - amount
                );

        if (accepted <= 0) {
            return 0;
        }

        medium =
                requestedMedium;

        amount +=
                accepted;

        sync();

        return accepted;
    }

    public int extract(
            int requestedAmount
    ) {
        int extracted =
                Math.min(
                        Math.max(
                                0,
                                requestedAmount
                        ),
                        amount
                );

        if (extracted <= 0) {
            return 0;
        }

        amount -=
                extracted;

        if (amount <= 0) {
            amount =
                    0;

            medium =
                    PipeMedium.EMPTY;
        }

        sync();

        return extracted;
    }

    public boolean canAccept(
            PipeMedium requestedMedium
    ) {
        PipeProfile profile =
                profile();

        if (profile == null
                || !profile.accepts(
                requestedMedium
        )) {
            return false;
        }

        return requestedMedium != PipeMedium.STEAM
                || profile.hotSteamRated();
    }

    public float fillFraction() {
        PipeProfile profile =
                profile();

        if (profile == null
                || profile.capacity() <= 0) {
            return 0.0F;
        }

        return Mth.clamp(
                amount
                        / (float) profile.capacity(),
                0.0F,
                1.0F
        );
    }

    public float pressureKpa() {
        PipeProfile profile =
                profile();

        if (profile == null
                || medium == PipeMedium.EMPTY) {
            return 0.0F;
        }

        float fill =
                fillFraction();

        if (medium.gas()) {
            return 101.3F
                    + fill
                            * (
                            profile.maxPressureKpa()
                                    - 101.3F
                    );
        }

        return 35.0F
                + fill
                        * Math.min(
                        profile.maxPressureKpa(),
                        650.0F
                );
    }

    public Component status() {
        PipeProfile profile =
                profile();

        if (profile == null) {
            return Component.literal(
                    "Pipe"
            );
        }

        return Component.translatable(
                "message.wayaround.pipe.status",
                Component.translatable(
                        "block.wayaround."
                                + profile.id()
                ),
                medium.name(),
                amount,
                profile.capacity(),
                Math.round(
                        pressureKpa()
                ),
                lastFlow
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

        tag.putString(
                "Medium",
                medium.name()
        );

        tag.putInt(
                "Amount",
                amount
        );

        tag.putInt(
                "LastFlow",
                lastFlow
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

        medium =
                PipeMedium.fromName(
                        tag.getString(
                                "Medium"
                        )
                );

        PipeProfile profile =
                profile();

        amount =
                Mth.clamp(
                        tag.getInt(
                                "Amount"
                        ),
                        0,
                        profile == null
                                ? 0
                                : profile.capacity()
                );

        lastFlow =
                Math.max(
                        0,
                        tag.getInt(
                                "LastFlow"
                        )
                );

        if (amount <= 0) {
            medium =
                    PipeMedium.EMPTY;
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
                    3
            );
        }
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
