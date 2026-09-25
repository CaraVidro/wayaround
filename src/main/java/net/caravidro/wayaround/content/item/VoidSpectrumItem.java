package net.caravidro.wayaround.content.item;

import java.util.List;

import net.caravidro.wayaround.blue.BlueManager;
import net.caravidro.wayaround.blue.ImaginaryBetaManager;
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

/**
 * Inventory-active Void Spectrum control surface.
 *
 * Right click is intentionally contextual:
 *  1) dismiss active Blue;
 *  2) otherwise launch a prepared Red;
 *  3) otherwise do nothing and let voice remain the primary invocation path.
 */
public final class VoidSpectrumItem extends Item {

    public VoidSpectrumItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer) {

            if (BlueManager.hasControllableBlue(serverPlayer)) {
                BlueManager.releaseActive(serverPlayer);
                return InteractionResultHolder.success(stack);
            }

            if (ImaginaryBetaManager.launchPreparedRed(serverPlayer)) {
                return InteractionResultHolder.success(stack);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(
                Component.translatable(
                                "tooltip.wayaround.gojo_spectrum"
                        )
                        .withStyle(
                                ChatFormatting.AQUA
                        )
        );

        tooltip.add(
                Component.translatable(
                                "tooltip.wayaround.gojo_spectrum.controls"
                        )
                        .withStyle(
                                ChatFormatting.GRAY
                        )
        );
    }
}
