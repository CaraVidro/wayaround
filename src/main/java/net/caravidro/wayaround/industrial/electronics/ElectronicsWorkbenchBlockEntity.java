package net.caravidro.wayaround.industrial.electronics;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.assembly.AssemblyItemData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ElectronicsWorkbenchBlockEntity
        extends BlockEntity
        implements MenuProvider {

    private ItemStack circuitBoard =
            ItemStack.EMPTY;

    private final ContainerData menuData =
            new ContainerData() {
                @Override
                public int get(
                        int index
                ) {
                    return switch (index) {
                        case 0 -> worldPosition.getX();
                        case 1 -> worldPosition.getY();
                        case 2 -> worldPosition.getZ();
                        default -> 0;
                    };
                }

                @Override
                public void set(
                        int index,
                        int value
                ) {
                }

                @Override
                public int getCount() {
                    return 3;
                }
            };

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

    public ItemStack circuitBoardCopy() {
        return circuitBoard.copy();
    }

    public CircuitBoardData boardData() {
        return CircuitBoardData.read(
                circuitBoard
        );
    }

    public boolean installCircuitBoard(
            Player player,
            ItemStack held
    ) {
        if (hasCircuitBoard()
                || !held.is(
                ElectronicsContent.CIRCUIT_BOARD.get()
        )) {
            return false;
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
            held.shrink(
                    1
            );
        }

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.circuit_workbench.board_inserted"
                ),
                true
        );

        sync();
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

        give(
                player,
                removed
        );

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.circuit_workbench.board_removed"
                ),
                true
        );

        sync();
    }

    public boolean placeComponent(
            ServerPlayer player,
            CircuitBoardData.ComponentType type,
            int cell
    ) {
        if (!hasCircuitBoard()
                || type == null
                || type == CircuitBoardData.ComponentType.EMPTY) {
            return false;
        }

        CircuitBoardData board =
                boardData();

        if (board.component(
                cell
        ) != CircuitBoardData.ComponentType.EMPTY) {
            return false;
        }

        Item item =
                ElectronicsContent.itemFor(
                        type
                );

        if (!consume(
                player,
                item,
                1
        )) {
            return false;
        }

        if (!board.place(
                cell,
                type
        )) {
            give(
                    player,
                    new ItemStack(
                            item
                    )
            );
            return false;
        }

        board.write(
                circuitBoard
        );

        sync();
        return true;
    }

    public boolean connect(
            ServerPlayer player,
            int a,
            int b
    ) {
        if (!hasCircuitBoard()
                || a == b) {
            return false;
        }

        CircuitBoardData board =
                boardData();

        if (board.component(
                a
        ) == CircuitBoardData.ComponentType.EMPTY
                || board.component(
                b
        ) == CircuitBoardData.ComponentType.EMPTY
                || board.hasTrace(
                a,
                b
        )) {
            return false;
        }

        int copperCost =
                CircuitBoardData.traceCopperCost(
                        a,
                        b
                );

        if (copperCost <= 0
                || !consume(
                player,
                ElectronicsContent.COPPER_TRACE.get(),
                copperCost
        )) {
            return false;
        }

        if (!board.addTrace(
                a,
                b
        )) {
            give(
                    player,
                    new ItemStack(
                            ElectronicsContent.COPPER_TRACE.get(),
                            copperCost
                    )
            );
            return false;
        }

        board.write(
                circuitBoard
        );

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.circuit_workbench.trace_cost",
                        copperCost
                ),
                true
        );

        sync();
        return true;
    }

    public boolean removeCell(
            ServerPlayer player,
            int cell
    ) {
        if (!hasCircuitBoard()) {
            return false;
        }

        CircuitBoardData board =
                boardData();

        CircuitBoardData.ComponentType type =
                board.component(
                        cell
                );

        int copper =
                board.copperCostAt(
                        cell
                );

        if (type == CircuitBoardData.ComponentType.EMPTY
                && copper <= 0) {
            return false;
        }

        if (type != CircuitBoardData.ComponentType.EMPTY) {
            give(
                    player,
                    new ItemStack(
                            ElectronicsContent.itemFor(
                                    type
                            )
                    )
            );
        }

        if (copper > 0) {
            give(
                    player,
                    new ItemStack(
                            ElectronicsContent.COPPER_TRACE.get(),
                            copper
                    )
            );
        }

        board.remove(
                cell
        );

        board.write(
                circuitBoard
        );

        sync();
        return true;
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

        sync();
    }

    private static boolean consume(
            ServerPlayer player,
            Item item,
            int count
    ) {
        if (count <= 0
                || player.getAbilities().instabuild) {
            return true;
        }

        int available =
                0;

        for (ItemStack stack :
                player.getInventory().items) {
            if (stack.is(
                    item
            )) {
                available +=
                        stack.getCount();

                if (available >= count) {
                    break;
                }
            }
        }

        if (available < count) {
            return false;
        }

        int remaining =
                count;

        for (ItemStack stack :
                player.getInventory().items) {
            if (!stack.is(
                    item
            )) {
                continue;
            }

            int taken =
                    Math.min(
                            remaining,
                            stack.getCount()
                    );

            stack.shrink(
                    taken
            );

            remaining -=
                    taken;

            if (remaining <= 0) {
                break;
            }
        }

        player.getInventory()
                .setChanged();

        return true;
    }

    private static void give(
            Player player,
            ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return;
        }

        if (!player.getInventory()
                .add(
                        stack
                )) {
            player.drop(
                    stack,
                    false
            );
        }
    }

    private void sync() {
        setChanged();

        if (level != null) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    3
            );
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "container.wayaround.electronics_workbench"
        );
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int id,
            Inventory inventory,
            Player player
    ) {
        return new ElectronicsWorkbenchMenu(
                id,
                inventory,
                this,
                menuData
        );
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        return saveWithoutMetadata(
                registries
        );
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(
                this
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
    }
}
