package net.caravidro.wayaround.industrial.electronics;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.industrial.assembly.AssemblyItemData;
import net.caravidro.wayaround.industrial.power.PowerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ElectronicsWorkbenchBlockEntity
        extends BlockEntity {

    private final Map<UUID, Integer> traceStarts =
            new HashMap<>();

    private ItemStack circuitBoard =
            ItemStack.EMPTY;

    public ElectronicsWorkbenchBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ElectronicsContent.ELECTRONICS_WORKBENCH_ENTITY.get(),
                pos,
                state
        );
    }

    public boolean hasCircuitBoard() {
        return !circuitBoard.isEmpty();
    }

    public boolean handleCircuitItem(
            Player player,
            ItemStack held,
            BlockHitResult hit
    ) {
        if (held.is(
                ElectronicsContent.CIRCUIT_BOARD.get()
        )) {
            return installCircuitBoard(
                    player,
                    held
            );
        }

        if (!hasCircuitBoard()
                || hit.getDirection() != Direction.UP) {
            return false;
        }

        int cell =
                cellFromHit(
                        hit
                );

        if (cell < 0) {
            return false;
        }

        if (held.is(
                ElectronicsContent.COPPER_TRACE.get()
        )) {
            return routeTrace(
                    player,
                    held,
                    cell
            );
        }

        CircuitBoardData.ComponentType component =
                ElectronicsContent.componentType(
                        held
                );

        if (component != null) {
            return installComponent(
                    player,
                    held,
                    cell,
                    component
            );
        }

        if (held.is(
                PowerContent.ASSEMBLY_HAMMER.get()
        )) {
            return removeAt(
                    player,
                    cell
            );
        }

        return false;
    }

    private boolean installCircuitBoard(
            Player player,
            ItemStack held
    ) {
        if (hasCircuitBoard()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.circuit_workbench.board_present"
                    ),
                    true
            );
            return true;
        }

        circuitBoard =
                held.copyWithCount(
                        1
                );

        CircuitBoardData.read(
                circuitBoard
        ).write(
                circuitBoard
        );

        AssemblyItemData.materialMemoryOrCreate(
                circuitBoard,
                level == null
                        ? 0L
                        : level.getGameTime()
        );

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.circuit_workbench.board_inserted"
                ),
                true
        );

        setChanged();
        return true;
    }

    private boolean installComponent(
            Player player,
            ItemStack held,
            int cell,
            CircuitBoardData.ComponentType component
    ) {
        CircuitBoardData board =
                CircuitBoardData.read(
                        circuitBoard
                );

        if (!board.place(
                cell,
                component
        )) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.circuit_workbench.cell_busy",
                            CircuitBoardData.cellX(cell) + 1,
                            CircuitBoardData.cellY(cell) + 1
                    ),
                    true
            );
            return true;
        }

        board.write(
                circuitBoard
        );

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.circuit_workbench.component_installed",
                        component.name(),
                        CircuitBoardData.cellX(cell) + 1,
                        CircuitBoardData.cellY(cell) + 1
                ),
                true
        );

        setChanged();
        return true;
    }

    private boolean routeTrace(
            Player player,
            ItemStack held,
            int cell
    ) {
        UUID id =
                player.getUUID();

        Integer start =
                traceStarts.remove(
                        id
                );

        if (start == null) {
            traceStarts.put(
                    id,
                    cell
            );

            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.circuit_workbench.trace_start",
                            CircuitBoardData.cellX(cell) + 1,
                            CircuitBoardData.cellY(cell) + 1
                    ),
                    true
            );
            return true;
        }

        CircuitBoardData board =
                CircuitBoardData.read(
                        circuitBoard
                );

        if (!board.addTrace(
                start,
                cell
        )) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.circuit_workbench.trace_failed"
                    ),
                    true
            );
            return true;
        }

        board.write(
                circuitBoard
        );

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.circuit_workbench.trace_added",
                        CircuitBoardData.cellX(start) + 1,
                        CircuitBoardData.cellY(start) + 1,
                        CircuitBoardData.cellX(cell) + 1,
                        CircuitBoardData.cellY(cell) + 1
                ),
                true
        );

        setChanged();
        return true;
    }

    private boolean removeAt(
            Player player,
            int cell
    ) {
        CircuitBoardData board =
                CircuitBoardData.read(
                        circuitBoard
                );

        CircuitBoardData.ComponentType removed =
                board.component(
                        cell
                );

        int traces =
                board.removeTracesAt(
                        cell
                );

        if (removed != CircuitBoardData.ComponentType.EMPTY) {
            board.remove(
                    cell
            );

            ItemStack recovered =
                    new ItemStack(
                            ElectronicsContent.itemFor(
                                    removed
                            )
                    );

            if (!player.getInventory().add(recovered)) {
                player.drop(recovered, false);
            }
        }

        if (traces > 0) {
            ItemStack wire =
                    new ItemStack(
                            ElectronicsContent.COPPER_TRACE.get(),
                            traces
                    );

            if (!player.getInventory().add(wire)) {
                player.drop(wire, false);
            }
        }

        if (removed == CircuitBoardData.ComponentType.EMPTY
                && traces == 0) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.circuit_workbench.nothing_here"
                    ),
                    true
            );
            return true;
        }

        board.write(
                circuitBoard
        );

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.circuit_workbench.removed",
                        CircuitBoardData.cellX(cell) + 1,
                        CircuitBoardData.cellY(cell) + 1
                ),
                true
        );

        setChanged();
        return true;
    }

    public void removeCircuitBoard(
            Player player
    ) {
        if (!hasCircuitBoard()) {
            return;
        }

        ItemStack removed =
                circuitBoard;

        circuitBoard =
                ItemStack.EMPTY;

        traceStarts.clear();

        if (!player.getInventory().add(removed)) {
            player.drop(removed, false);
        }

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.circuit_workbench.board_removed"
                ),
                true
        );

        setChanged();
    }

    public void describeCircuit(
            Player player
    ) {
        if (!hasCircuitBoard()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.circuit_workbench.no_board"
                    ),
                    true
            );
            return;
        }

        CircuitBoardData board =
                CircuitBoardData.read(
                        circuitBoard
                );

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.circuit_workbench.status",
                        board.componentCount(),
                        CircuitBoardData.CELL_COUNT,
                        board.traceCount(),
                        board.powerDraw(),
                        board.hasSignalPath()
                                ? Component.translatable(
                                "message.wayaround.circuit_workbench.closed_path"
                        )
                                : Component.translatable(
                                "message.wayaround.circuit_workbench.open_path"
                        )
                ),
                true
        );
    }

    public void dropCircuitBoard() {
        if (level == null
                || circuitBoard.isEmpty()) {
            return;
        }

        Block.popResource(
                level,
                worldPosition,
                circuitBoard
        );

        circuitBoard =
                ItemStack.EMPTY;

        traceStarts.clear();
    }

    private int cellFromHit(
            BlockHitResult hit
    ) {
        double localX =
                hit.getLocation().x
                        - worldPosition.getX();

        double localZ =
                hit.getLocation().z
                        - worldPosition.getZ();

        int x =
                Math.clamp(
                        (int) Math.floor(
                                localX * CircuitBoardData.WIDTH
                        ),
                        0,
                        CircuitBoardData.WIDTH - 1
                );

        int y =
                Math.clamp(
                        (int) Math.floor(
                                localZ * CircuitBoardData.HEIGHT
                        ),
                        0,
                        CircuitBoardData.HEIGHT - 1
                );

        return CircuitBoardData.cell(
                x,
                y
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

        if (!circuitBoard.isEmpty()) {
            tag.put(
                    "CircuitBoard",
                    circuitBoard.save(
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

        circuitBoard =
                tag.contains(
                        "CircuitBoard",
                        net.minecraft.nbt.Tag.TAG_COMPOUND
                )
                        ? ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "CircuitBoard"
                        )
                )
                        : ItemStack.EMPTY;

        if (!circuitBoard.is(
                ElectronicsContent.CIRCUIT_BOARD.get()
        )) {
            circuitBoard =
                    ItemStack.EMPTY;
        }

        traceStarts.clear();
    }
}
