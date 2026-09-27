package net.caravidro.wayaround.industrial.ship;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Physical hauling tool for an installed ship anchor.
 *
 * <p>The item can be held continuously after the ship starts the hauling
 * interaction. AssemblyShipEntity checks that use state every tick.</p>
 */
public final class AnchorChainItem
        extends Item {

    public AnchorChainItem(
            Properties properties
    ) {
        super(
                properties
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        player.startUsingItem(
                hand
        );

        return InteractionResultHolder.consume(
                player.getItemInHand(
                        hand
                )
        );
    }

    @Override
    public int getUseDuration(
            ItemStack stack,
            LivingEntity entity
    ) {
        return 72_000;
    }

    @Override
    public UseAnim getUseAnimation(
            ItemStack stack
    ) {
        return UseAnim.BLOCK;
    }
}
