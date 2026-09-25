package net.caravidro.wayaround.content.item;

import java.util.List;
import net.caravidro.wayaround.blue.BlueManager;
import net.caravidro.wayaround.blue.ImaginaryBetaManager;
import net.caravidro.wayaround.spectrum.SpectrumItem;
import net.caravidro.wayaround.spectrum.SpectrumType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Context-sensitive physical controls for the Void Spectrum. */
public final class VoidSpectrumItem extends SpectrumItem {
    public VoidSpectrumItem(Properties properties) {
        super(SpectrumType.VOID, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Active Blue: dismiss immediately, with no chat/status line.
            if (BlueManager.hasControllableBlue(serverPlayer)) {
                BlueManager.releaseActive(serverPlayer);
                return InteractionResultHolder.success(stack);
            }

            // Prepared Red: physical equivalent of saying "lançar".
            if (ImaginaryBetaManager.launchRed(serverPlayer)) {
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
        tooltip.add(Component.translatable(
                "tooltip.wayaround.gojo_spectrum"
        ).withStyle(ChatFormatting.AQUA));

        tooltip.add(Component.translatable(
                "tooltip.wayaround.gojo_spectrum.controls"
        ).withStyle(ChatFormatting.GRAY));
    }
}
