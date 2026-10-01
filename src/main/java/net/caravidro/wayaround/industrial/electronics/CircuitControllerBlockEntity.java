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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
    private int detectorSignal;
    private int outputSignal;
    private int lastDraw;
    private boolean ledActive;
    private boolean buzzerActive;

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

    public boolean hasBoard() {
        return !board.isEmpty();
    }

    public boolean ledActive() {
        return ledActive;
    }

    public int detectorSignal() {
        return detectorSignal;
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

        boolean oldLed =
                ledActive;

        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.POWER_NETWORKS
        )
                || board.isEmpty()) {

            inputSignal = 0;
            detectorSignal = 0;
            outputSignal = 0;
            lastDraw = 0;
            ledActive = false;
            buzzerActive = false;

            updateRuntimeState(
                    oldOutput,
                    oldLed
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

        detectorSignal =
                circuit.hasComponent(
                        CircuitBoardData.ComponentType.DISTANCE_DETECTOR
                )
                        ? distanceSignal(
                        level,
                        facing
                )
                        : 0;

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

        int rawOutput =
                Math.max(
                        routedSignal(
                                circuit,
                                CircuitBoardData.ComponentType.INPUT_TERMINAL,
                                CircuitBoardData.ComponentType.OUTPUT_TERMINAL,
                                inputSignal
                        ),
                        routedSignal(
                                circuit,
                                CircuitBoardData.ComponentType.DISTANCE_DETECTOR,
                                CircuitBoardData.ComponentType.OUTPUT_TERMINAL,
                                detectorSignal
                        )
                );

        int rawLed =
                Math.max(
                        routedSignal(
                                circuit,
                                CircuitBoardData.ComponentType.INPUT_TERMINAL,
                                CircuitBoardData.ComponentType.LED,
                                inputSignal
                        ),
                        routedSignal(
                                circuit,
                                CircuitBoardData.ComponentType.DISTANCE_DETECTOR,
                                CircuitBoardData.ComponentType.LED,
                                detectorSignal
                        )
                );

        int rawBuzzer =
                Math.max(
                        routedSignal(
                                circuit,
                                CircuitBoardData.ComponentType.INPUT_TERMINAL,
                                CircuitBoardData.ComponentType.BUZZER,
                                inputSignal
                        ),
                        routedSignal(
                                circuit,
                                CircuitBoardData.ComponentType.DISTANCE_DETECTOR,
                                CircuitBoardData.ComponentType.BUZZER,
                                detectorSignal
                        )
                );

        outputSignal =
                powered
                        ? attenuatedSignal(
                        rawOutput,
                        conductivity
                )
                        : 0;

        ledActive =
                powered
                        && attenuatedSignal(
                        rawLed,
                        conductivity
                ) > 0;

        buzzerActive =
                powered
                        && attenuatedSignal(
                        rawBuzzer,
                        conductivity
                ) > 0;

        if (buzzerActive
                && Math.floorMod(
                level.getGameTime()
                        + worldPosition.asLong(),
                12
        ) == 0) {

            int buzzerSignal =
                    Math.max(
                            rawBuzzer,
                            1
                    );

            float pitch =
                    0.72F
                            + buzzerSignal
                            / 15.0F
                            * 0.58F;

            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.NOTE_BLOCK_BELL.value(),
                    SoundSource.BLOCKS,
                    0.62F,
                    pitch
            );
        }

        updateRuntimeState(
                oldOutput,
                oldLed
        );

        setChanged();
    }

    private static int routedSignal(
            CircuitBoardData circuit,
            CircuitBoardData.ComponentType source,
            CircuitBoardData.ComponentType target,
            int strength
    ) {
        return strength > 0
                && circuit.connected(
                source,
                target
        )
                ? strength
                : 0;
    }

    private static int attenuatedSignal(
            int signal,
            float conductivity
    ) {
        return Mth.clamp(
                Math.round(
                        signal
                                * conductivity
                ),
                0,
                15
        );
    }

    private int distanceSignal(
            ServerLevel level,
            Direction facing
    ) {
        final double range =
                16.0;

        Vec3 origin =
                Vec3.atCenterOf(
                        worldPosition
                );

        Vec3 forward =
                new Vec3(
                        facing.getStepX(),
                        0.0,
                        facing.getStepZ()
                ).normalize();

        AABB scan =
                new AABB(
                        worldPosition
                ).inflate(
                        range
                );

        double nearest =
                range + 1.0;

        for (LivingEntity entity :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        scan,
                        LivingEntity::isAlive
                )) {

            Vec3 delta =
                    entity.getBoundingBox()
                            .getCenter()
                            .subtract(
                                    origin
                            );

            double distance =
                    delta.length();

            if (distance < 0.25
                    || distance > range) {
                continue;
            }

            double facingDot =
                    delta.normalize()
                            .dot(
                                    forward
                            );

            if (facingDot < 0.30) {
                continue;
            }

            nearest =
                    Math.min(
                            nearest,
                            distance
                    );
        }

        if (nearest > range) {
            return 0;
        }

        return Mth.clamp(
                15
                        - (int) Math.floor(
                        nearest
                                / range
                                * 14.0
                ),
                1,
                15
        );
    }

    private void updateRuntimeState(
            int oldOutput,
            boolean oldLed
    ) {
        if (level == null) {
            return;
        }

        BlockState state =
                getBlockState();

        BlockState updated =
                state;

        if (state.getValue(
                CircuitControllerBlock.POWER
        ) != outputSignal) {
            updated =
                    updated.setValue(
                            CircuitControllerBlock.POWER,
                            outputSignal
                    );
        }

        if (state.getValue(
                CircuitControllerBlock.LED_ACTIVE
        ) != ledActive) {
            updated =
                    updated.setValue(
                            CircuitControllerBlock.LED_ACTIVE,
                            ledActive
                    );
        }

        boolean hasBoard =
                !board.isEmpty();

        if (state.getValue(
                CircuitControllerBlock.HAS_BOARD
        ) != hasBoard) {
            updated =
                    updated.setValue(
                            CircuitControllerBlock.HAS_BOARD,
                            hasBoard
                    );
        }

        if (!updated.equals(
                state
        )) {
            level.setBlock(
                    worldPosition,
                    updated,
                    3
            );
        }

        if (oldOutput != outputSignal) {
            Direction facing =
                    updated.getValue(
                            CircuitControllerBlock.FACING
                    );

            level.updateNeighborsAt(
                    worldPosition.relative(
                            facing
                    ),
                    updated.getBlock()
            );
        }

        if (oldLed != ledActive) {
            level.sendBlockUpdated(
                    worldPosition,
                    updated,
                    updated,
                    3
            );
        }
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

        if (level != null) {
            BlockState state =
                    getBlockState();

            if (!state.getValue(
                    CircuitControllerBlock.HAS_BOARD
            )) {
                level.setBlock(
                        worldPosition,
                        state.setValue(
                                CircuitControllerBlock.HAS_BOARD,
                                true
                        ),
                        3
                );
            }
        }

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

        inputSignal = 0;
        detectorSignal = 0;
        outputSignal = 0;
        ledActive = false;
        buzzerActive = false;

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

        if (level != null) {
            BlockState state =
                    getBlockState();

            level.setBlock(
                    worldPosition,
                    state.setValue(
                            CircuitControllerBlock.HAS_BOARD,
                            false
                    ).setValue(
                            CircuitControllerBlock.LED_ACTIVE,
                            false
                    ).setValue(
                            CircuitControllerBlock.POWER,
                            0
                    ),
                    3
            );
        }

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
                        detectorSignal,
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
                "DetectorSignal",
                detectorSignal
        );

        tag.putInt(
                "OutputSignal",
                outputSignal
        );

        tag.putBoolean(
                "LedActive",
                ledActive
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

        detectorSignal =
                Mth.clamp(
                        tag.getInt(
                                "DetectorSignal"
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

        ledActive =
                tag.getBoolean(
                        "LedActive"
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
