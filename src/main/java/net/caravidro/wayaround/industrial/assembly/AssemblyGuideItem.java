package net.caravidro.wayaround.industrial.assembly;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class AssemblyGuideItem
        extends Item {

    public AssemblyGuideItem(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack =
                player.getItemInHand(
                        hand
                );

        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                showDiagnostics(
                        player
                );
            } else {
                showAssembly(
                        player
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }

    private static void showAssembly(
            Player player
    ) {
        player.sendSystemMessage(
                Component.translatable(
                        "guide.wayaround.assembly.header"
                ).withStyle(
                        ChatFormatting.GOLD,
                        ChatFormatting.BOLD
                )
        );

        send(
                player,
                "guide.wayaround.assembly.1"
        );

        send(
                player,
                "guide.wayaround.assembly.2"
        );

        send(
                player,
                "guide.wayaround.assembly.3"
        );

        send(
                player,
                "guide.wayaround.assembly.4"
        );

        send(
                player,
                "guide.wayaround.assembly.5"
        );

        send(
                player,
                "guide.wayaround.assembly.6"
        );

        send(
                player,
                "guide.wayaround.assembly.7"
        );

        player.sendSystemMessage(
                Component.translatable(
                        "guide.wayaround.assembly.more"
                ).withStyle(
                        ChatFormatting.DARK_GRAY
                )
        );
    }

    private static void showDiagnostics(
            Player player
    ) {
        player.sendSystemMessage(
                Component.translatable(
                        "guide.wayaround.assembly.diagnostics_header"
                ).withStyle(
                        ChatFormatting.AQUA,
                        ChatFormatting.BOLD
                )
        );

        send(
                player,
                "guide.wayaround.assembly.diagnostics.1"
        );

        send(
                player,
                "guide.wayaround.assembly.diagnostics.2"
        );

        send(
                player,
                "guide.wayaround.assembly.diagnostics.3"
        );

        send(
                player,
                "guide.wayaround.assembly.diagnostics.4"
        );

        send(
                player,
                "guide.wayaround.assembly.diagnostics.5"
        );

        send(
                player,
                "guide.wayaround.assembly.diagnostics.6"
        );

        player.sendSystemMessage(
                Component.translatable(
                        "guide.wayaround.assembly.v1_header"
                ).withStyle(
                        ChatFormatting.GOLD,
                        ChatFormatting.BOLD
                )
        );

        send(
                player,
                "guide.wayaround.assembly.v1.1"
        );

        send(
                player,
                "guide.wayaround.assembly.v1.2"
        );

        send(
                player,
                "guide.wayaround.assembly.v1.3"
        );

        send(
                player,
                "guide.wayaround.assembly.v1.4"
        );
    }

    private static void send(
            Player player,
            String key
    ) {
        player.sendSystemMessage(
                Component.literal(
                        "• "
                ).withStyle(
                        ChatFormatting.GRAY
                ).append(
                        Component.translatable(
                                key
                        ).withStyle(
                                ChatFormatting.WHITE
                        )
                )
        );
    }
}
