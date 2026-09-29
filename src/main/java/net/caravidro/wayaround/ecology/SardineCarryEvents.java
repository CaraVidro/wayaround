package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.WayAround;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Large whole sardines occupy both hands logically while held. */
@EventBusSubscriber(modid = WayAround.MODID)
public final class SardineCarryEvents {

    private SardineCarryEvents() {}

    private static boolean isLargeWhole(ItemStack stack) {
        return stack.getItem() instanceof WholeSardineItem sardine
                && sardine.large();
    }

    private static boolean carryingLarge(Player player) {
        return isLargeWhole(player.getMainHandItem())
                || isLargeWhole(player.getOffhandItem());
    }

    private static boolean shouldBlock(Player player, InteractionHand hand) {
        return carryingLarge(player)
                && !isLargeWhole(player.getItemInHand(hand));
    }

    @SubscribeEvent
    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (shouldBlock(event.getEntity(), event.getHand())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (shouldBlock(event.getEntity(), event.getHand())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void rightClickEntity(PlayerInteractEvent.EntityInteract event) {
        if (shouldBlock(event.getEntity(), event.getHand())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }
}
