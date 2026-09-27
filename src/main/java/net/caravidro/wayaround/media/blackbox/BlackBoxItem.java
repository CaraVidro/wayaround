package net.caravidro.wayaround.media.blackbox;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class BlackBoxItem
        extends BlockItem {

    public BlackBoxItem(
            Block block,
            Properties properties
    ) {
        super(
                block,
                properties
        );
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

        var info =
                BlackBoxData.read(
                        stack
                );

        if (info.isPresent()
                && info.get()
                .sealed()) {
            if (!level.isClientSide
                    && player
                    instanceof ServerPlayer serverPlayer) {
                BlackBoxManager.rewind(
                        serverPlayer,
                        info.get()
                                .recordingId()
                );
            }

            return InteractionResultHolder.success(
                    stack
            );
        }

        return super.use(
                level,
                player,
                hand
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        var info =
                BlackBoxData.read(
                        stack
                );

        if (info.isEmpty()) {
            tooltip.add(
                    Component.literal(
                                    "Pronta para instalação."
                            )
                            .withStyle(
                                    ChatFormatting.DARK_GRAY
                            )
            );

            tooltip.add(
                    Component.literal(
                                    "Redstone + clique inicia a gravação."
                            )
                            .withStyle(
                                    ChatFormatting.GRAY
                            )
            );

            return;
        }

        BlackBoxData.Info data =
                info.get();

        tooltip.add(
                Component.literal(
                                data.sealed()
                                        ? "SELADA — somente leitura"
                                        : "Arquivo ativo"
                        )
                        .withStyle(
                                data.sealed()
                                        ? ChatFormatting.GOLD
                                        : ChatFormatting.RED
                        )
        );

        tooltip.add(
                Component.literal(
                                String.format(
                                        Locale.ROOT,
                                        "Duração: %.1fs",
                                        data.durationTicks()
                                                / 20.0
                                )
                        )
                        .withStyle(
                                ChatFormatting.GRAY
                        )
        );

        tooltip.add(
                Component.literal(
                                "Origem: "
                                        + data.x()
                                        + " "
                                        + data.y()
                                        + " "
                                        + data.z()
                        )
                        .withStyle(
                                ChatFormatting.DARK_GRAY
                        )
        );

        tooltip.add(
                Component.literal(
                                "Clique no ar para rebobinar."
                        )
                        .withStyle(
                                ChatFormatting.DARK_GRAY
                        )
        );
    }
}
