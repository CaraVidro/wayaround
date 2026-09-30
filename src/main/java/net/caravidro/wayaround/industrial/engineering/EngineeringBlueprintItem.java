package net.caravidro.wayaround.industrial.engineering;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import net.caravidro.wayaround.network.BlueprintLabelOpenS2CPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public final class EngineeringBlueprintItem
        extends Item {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm"
            );

    public EngineeringBlueprintItem(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    public Component getName(
            ItemStack stack
    ) {
        return EngineeringBlueprintData.read(stack)
                .<Component>map(
                        info -> Component.literal(
                                info.title()
                        )
                )
                .orElseGet(
                        () -> super.getName(stack)
                );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack =
                player.getItemInHand(hand);

        EngineeringBlueprintData.Info info =
                EngineeringBlueprintData.read(stack)
                        .orElse(null);

        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer
                && info != null) {

            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new BlueprintLabelOpenS2CPayload(
                            info.blueprintId(),
                            info.title(),
                            info.showCoordinates(),
                            info.showDateTime()
                    )
            );
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        EngineeringBlueprintData.Info info =
                EngineeringBlueprintData.read(stack)
                        .orElse(null);

        if (info == null) {
            tooltip.add(
                    Component.translatable(
                            "tooltip.wayaround.engineering_blueprint.blank"
                    ).withStyle(
                            ChatFormatting.GRAY
                    )
            );

            return;
        }

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.engineering_blueprint.nodes",
                        info.nodeCount()
                ).withStyle(
                        ChatFormatting.GRAY
                )
        );

        if (info.showCoordinates()) {
            tooltip.add(
                    Component.translatable(
                            "tooltip.wayaround.engineering_blueprint.coords",
                            info.x(),
                            info.y(),
                            info.z()
                    ).withStyle(
                            ChatFormatting.DARK_GRAY
                    )
            );
        }

        if (info.showDateTime()
                && info.createdAtMillis() > 0L) {
            tooltip.add(
                    Component.translatable(
                            "tooltip.wayaround.engineering_blueprint.datetime",
                            DATE_FORMAT.format(
                                    Instant.ofEpochMilli(
                                            info.createdAtMillis()
                                    ).atZone(
                                            ZoneId.systemDefault()
                                    )
                            )
                    ).withStyle(
                            ChatFormatting.DARK_GRAY
                    )
            );
        }

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.engineering_blueprint.edit"
                ).withStyle(
                        ChatFormatting.DARK_AQUA
                )
        );
    }
}
