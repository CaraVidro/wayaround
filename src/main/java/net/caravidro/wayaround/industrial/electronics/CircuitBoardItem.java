package net.caravidro.wayaround.industrial.electronics;

import java.util.List;

import net.caravidro.wayaround.industrial.assembly.AssemblyItemData;
import net.caravidro.wayaround.industrial.material.MaterialMemory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class CircuitBoardItem
        extends Item {

    public CircuitBoardItem(
            Properties properties
    ) {
        super(
                properties
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        CircuitBoardData board =
                CircuitBoardData.read(
                        stack
                );

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.circuit_board.layout",
                        board.componentCount(),
                        CircuitBoardData.CELL_COUNT,
                        board.traceCount()
                ).withStyle(
                        ChatFormatting.GRAY
                )
        );

        tooltip.add(
                Component.translatable(
                        board.hasSignalPath()
                                ? "tooltip.wayaround.circuit_board.path_ready"
                                : "tooltip.wayaround.circuit_board.path_open"
                ).withStyle(
                        board.hasSignalPath()
                                ? ChatFormatting.GREEN
                                : ChatFormatting.DARK_GRAY
                )
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.circuit_board.workflow"
                ).withStyle(
                        ChatFormatting.DARK_AQUA
                )
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.circuit_board.signal_flow"
                ).withStyle(
                        ChatFormatting.DARK_GRAY
                )
        );

        MaterialMemory memory =
                AssemblyItemData.readMaterialMemory(
                        stack
                );

        if (memory != null) {
            tooltip.add(
                    Component.translatable(
                            "tooltip.wayaround.material_memory",
                            memory.loadCycles(),
                            memory.thermalCycles(),
                            Math.round(
                                    memory.corrosion()
                                            * 100.0F
                            ),
                            Math.round(
                                    memory.deformation()
                                            * 100.0F
                            )
                    ).withStyle(
                            ChatFormatting.DARK_GRAY
                    )
            );
        }
    }
}
