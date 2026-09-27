package net.caravidro.wayaround.content.item;

import java.util.List;

import net.caravidro.wayaround.jujutsu.JujutsuManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * The orb does not roll a technique. It only awakens the identity that was
 * assigned to the player on this server before they ever consumed an orb.
 */
public final class JujutsuOrbItem extends Item {
    public JujutsuOrbItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Orb Jujutsu");
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (JujutsuManager.isAwakened(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.literal("O Orb fica inerte. Seu Jujutsu já despertou.")
                                .withStyle(ChatFormatting.DARK_GRAY),
                        true
                );
            }
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide && livingEntity instanceof ServerPlayer player) {
            boolean awakened = JujutsuManager.awaken(player);
            if (awakened && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("Não pode ser craftado.")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("Ele não cria sua técnica. Só acorda o que já veio com você.")
                .withStyle(ChatFormatting.GRAY));
    }
}
