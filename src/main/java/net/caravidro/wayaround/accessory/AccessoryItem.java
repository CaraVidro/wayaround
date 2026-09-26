package net.caravidro.wayaround.accessory;

import java.util.List;

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

public final class AccessoryItem
        extends Item {

    private final AccessoryKind kind;

    public AccessoryItem(
            AccessoryKind kind,
            Properties properties
    ) {
        super(
                properties
        );

        this.kind =
                kind;
    }

    public AccessoryKind kind() {
        return kind;
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

        if (player
                instanceof ServerPlayer serverPlayer) {
            AccessoryManager.equipFromHand(
                    serverPlayer,
                    hand,
                    this
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
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        String hint =
                switch (kind) {
                    case SPECTRAL_GLASSES ->
                            "Right-click its slot: eyes / crown.";
                    case WORK_GLOVES ->
                            "Some work is remembered by the hands.";
                    case ENGINEER_CAPE ->
                            "It dislikes standing still.";
                    case WIND_BOOTS ->
                            "The ground should arrive later.";
                };

        tooltip.add(
                Component.literal(
                        hint
                ).withStyle(
                        ChatFormatting.GRAY
                )
        );
    }
}
