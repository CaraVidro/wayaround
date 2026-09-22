package net.caravidro.wayaround.industrial.ship;

import net.caravidro.wayaround.WayAround;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;

@EventBusSubscriber(modid = WayAround.MODID)
public final class ShipSleeping {
    private ShipSleeping() {}
    @SubscribeEvent
    public static void continueSleeping(CanContinueSleepingEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)
                || !player.getPersistentData().hasUUID("WayAroundShipBerth")) return;
        var entity = level.getEntity(player.getPersistentData().getUUID("WayAroundShipBerth"));
        boolean valid = entity instanceof GreatShipEntity ship && ship.isAlive() && ship.isAnchored()
                && ship.ownsSleeper(player) && ship.canUse(player) && !level.isDay();
        if (event.getProblem() == Player.BedSleepingProblem.NOT_POSSIBLE_HERE) event.setContinueSleeping(valid);
    }
}
