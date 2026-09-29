package net.caravidro.wayaround.war;

import net.caravidro.wayaround.WayAround;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * Rocket jumps are movement tech, not delayed self-damage.
 *
 * Protection exists only while WarBallistics tracks a live rocket-jump chain;
 * normal falls remain completely vanilla.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class RocketJumpEvents {

    private RocketJumpEvents() {
    }

    @SubscribeEvent
    public static void fall(
            LivingFallEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)
                || !WarBallistics.protectsRocketFall(
                player
        )) {
            return;
        }

        event.setDistance(
                0.0F
        );

        event.setDamageMultiplier(
                0.0F
        );
    }
}
