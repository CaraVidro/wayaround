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
                            "Right-click the head slot: eyes / forehead.";
                    case WORK_GLOVES ->
                            "A pair of reinforced decorative gloves.";
                    case ENGINEER_CAPE ->
                            "Its cloth reacts to your movement.";
                    case WIND_BOOTS ->
                            "Light boots. Decorative, for now.";
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
