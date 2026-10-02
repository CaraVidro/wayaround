package net.caravidro.wayaround.industrial.mining;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class MiningInteractionEvents {
    private MiningInteractionEvents() {}

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (level.getBlockEntity(event.getPos()) instanceof ComplexOreBlockEntity complex) {
            event.setCanceled(true);
            complex.extractByPlayer(player);
            return;
        }

        DeferredMiningManager.onBlockBroken(
                level,
                event.getPos()
        );
    }
}
