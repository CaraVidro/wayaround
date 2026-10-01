package net.caravidro.wayaround.industrial.electronics;

import net.caravidro.wayaround.industrial.assembly.AssemblyItemData;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.caravidro.wayaround.industrial.material.MaterialMemory;
import net.caravidro.wayaround.industrial.material.MaterialProperties;
import net.caravidro.wayaround.industrial.power.EnergyBudget;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class CircuitControllerBlockEntity
        extends BlockEntity {

    public static final int CAPACITY = 4096;
    public static final int RECEIVE_LIMIT = 256;

    private final EnergyBudget energy =
            new EnergyBudget(
                    CAPACITY
            );

    private ItemStack board =
            ItemStack.EMPTY;

    private int inputSignal;
    private int outputSignal;
    private int lastDraw;

    private final IEnergyStorage energyInput =
            new IEnergyStorage() {

                @Override
                public int receiveEnergy(
                        int amount,
                        boolean simulate
                ) {
                    int accepted =
                            Math.min(
                                    RECEIVE_LIMIT,
                                    Math.min(
                                            Math.max(
                                                    0,
                                                    amount
                                            ),
                                            energy.capacity()
                                                    - energy.stored()
                                    )
                            );

                    if (!simulate
                            && accepted > 0) {
                        energy.add(
                                accepted
                        );

                        setChanged();
                    }

                    return accepted;
                }

                @Override
                public int extractEnergy(
                        int amount,
                        boolean simulate
                ) {
                    return 0;
                }

                @Override
                public int getEnergyStored() {
                    return energy.stored();
                }

                @Override
                public int getMaxEnergyStored() {
                    return energy.capacity();
                }

                @Override
                public boolean canExtract() {
                    return false;
                }

                @Override
                public boolean canReceive() {
                    return true;
                }
            };

    public CircuitControllerBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ElectronicsContent.CIRCUIT_CONTROLLER_ENTITY.get(),
                pos,
                state
        );
    }

    public IEnergyStorage energyInput() {
        return energyInput;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CircuitControllerBlockEntity controller
    ) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        controller.tick(
                server
        );
    }

    private void tick(
            ServerLevel level
    ) {
        int oldOutput =
                outputSignal;

        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.POWER_NETWORKS
        )
                || board.isEmpty()) {

            inputSignal = 0;
            outputSignal = 0;
            lastDraw = 0;
            updateOutput(
                    oldOutput
            );
            return;
        }

        CircuitBoardData circuit =
                CircuitBoardData.read(
                        board
                );

        Direction facing =
                getBlockState()
                        .getValue(
                                CircuitControllerBlock.FACING
                        );

        Direction inputSide =
                facing.getOpposite();

        BlockPos inputPos =
                worldPosition.relative(
                        inputSide
                );

        inputSignal =
                Mth.clamp(
                        level.getSignal(
                                inputPos,
                                facing
                        ),
                        0,
                        15
                );

        int wanted =
                circuit.powerDraw();

        int supplied =
                energy.extract(
                        wanted,
                        false
                );

        lastDraw =
                supplied;

        boolean powered =
                supplied >= wanted;

        AssemblyItemData.observeMaterialUse(
                board,
                AssemblyPartProfile.Material.COPPER,
                level.getGameTime(),
                wanted / 16.0F,
                0.0F,
                circuit.complexity()
                        * 0.42F
        );

        if (level.isRainingAt(
                worldPosition.above()
        )
                && Math.floorMod(
                level.getGameTime()
                        + worldPosition.asLong(),
                20
        ) == 0) {

            AssemblyItemData.exposeMaterialWet(
                    board,
                    AssemblyPartProfile.Material.COPPER,
                    level.getGameTime(),
                    0.22F,
                    false
            );
        }

        MaterialMemory memory =
                AssemblyItemData.readMaterialMemory(
                        board
                );

        float conductivity =
                MaterialProperties.of(
                        AssemblyPartProfile.Material.COPPER
                )
                        .conductivityAfter(
                                memory
                        );

        outputSignal =
                powered
                        && circuit.hasSignalPath()
                        ? Mth.clamp(
                        Math.round(
                                inputSignal
                                        * conductivity
                        ),
                        0,
                        15
                )
                        : 0;

        updateOutput(
                oldOutput
        );

        setChanged();
    }

    private void updateOutput(
            int oldOutput
    ) {
        if (level == null
                || oldOutput == outputSignal) {
            return;
        }

        BlockState state =
                getBlockState();

        if (state.hasProperty(
                CircuitControllerBlock.POWER
        )
                && state.getValue(
                CircuitControllerBlock.POWER
        ) != outputSignal) {

            level.setBlock(
                    worldPosition,
                    state.setValue(
                            CircuitControllerBlock.POWER,
                            outputSignal
                    ),
                    3
            );
        }

        Direction facing =
                state.getValue(
                        CircuitControllerBlock.FACING
                );

        level.updateNeighborsAt(
                worldPosition.relative(
                        facing
                ),
                state.getBlock()
        );
    }

    public void installBoard(
            Player player,
            ItemStack held
    ) {
        if (!board.isEmpty()
                || held.isEmpty()
                || !held.is(
                ElectronicsContent.CIRCUIT_BOARD.get()
        )) {
            describe(
                    player
            );
            return;
        }

        board =
                held.copyWithCount(
                        1
                );

        CircuitBoardData circuit =
                CircuitBoardData.read(
                        board
                );

        circuit.write(
                board
        );

        AssemblyItemData.materialMemoryOrCreate(
                board,
                level == null
                        ? 0L
                        : level.getGameTime()
        );

        if (!player.getAbilities()
                .instabuild) {
            held.shrink(
                    1
            );
        }

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.circuit_controller.board_installed"
                ),
                true
        );

        setChanged();
    }

    public void removeBoard(
            Player player
    ) {
        if (board.isEmpty()) {
            return;
        }

        ItemStack removed =
                board;

        board =
                ItemStack.EMPTY;

        outputSignal = 0;

        if (!player.getInventory()
                .add(
                        removed
                )) {
            player.drop(
                    removed,
                    false
            );
        }

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.circuit_controller.board_removed"
                ),
                true
        );

        setChanged();
    }

    public void describe(
            Player player
    ) {
        CircuitBoardData circuit =
                CircuitBoardData.read(
                        board
                );

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.circuit_controller.status",
                        board.isEmpty()
                                ? Component.translatable(
                                "message.wayaround.circuit_controller.no_board"
                        )
                                : Component.translatable(
                                "message.wayaround.circuit_controller.has_board"
                        ),
                        energy.stored(),
                        CAPACITY,
                        lastDraw,
                        inputSignal,
                        outputSignal,
                        circuit.componentCount(),
                        circuit.traceCount()
                ),
                true
        );
    }

    public void dropBoard() {
        if (level == null
                || board.isEmpty()) {
            return;
        }

        Block.popResource(
                level,
                worldPosition,
                board
        );

        board =
                ItemStack.EMPTY;
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

        tag.putInt(
                "Energy",
                energy.stored()
        );

        tag.putInt(
                "InputSignal",
                inputSignal
        );

        tag.putInt(
                "OutputSignal",
                outputSignal
        );

        tag.putInt(
                "LastDraw",
                lastDraw
        );

        if (!board.isEmpty()) {
            tag.put(
                    "Board",
                    board.save(
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

        energy.load(
                tag.getInt(
                        "Energy"
                )
        );

        inputSignal =
                Mth.clamp(
                        tag.getInt(
                                "InputSignal"
                        ),
                        0,
                        15
                );

        outputSignal =
                Mth.clamp(
                        tag.getInt(
                                "OutputSignal"
                        ),
                        0,
                        15
                );

        lastDraw =
                Math.max(
                        0,
                        tag.getInt(
                                "LastDraw"
                        )
                );

        board =
                tag.contains(
                        "Board",
                        net.minecraft.nbt.Tag.TAG_COMPOUND
                )
                        ? ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "Board"
                        )
                )
                        : ItemStack.EMPTY;

        if (!board.is(
                ElectronicsContent.CIRCUIT_BOARD.get()
        )) {
            board =
                    ItemStack.EMPTY;
        }
    }
}
