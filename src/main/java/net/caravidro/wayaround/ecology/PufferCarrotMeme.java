package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.advancement.WayAroundAdvancements;
import net.caravidro.wayaround.sounds.WayAroundSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Extremely important marine biology.
 *
 * Carrot + pufferfish plays the supplied meme sound and awards a tiny VISTA.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class PufferCarrotMeme {

    private PufferCarrotMeme() {
    }

    @SubscribeEvent
    public static void interact(
            PlayerInteractEvent.EntityInteractSpecific event
    ) {
        if (!(event.getTarget()
                instanceof Pufferfish puffer)
                || !event.getItemStack()
                .is(
                        Items.CARROT
                )) {
            return;
        }

        /*
         * Cancel on both logical sides so vanilla does not continue through a
         * second entity-interaction path after we have claimed the click.
         */
        event.setCancellationResult(
                InteractionResult.SUCCESS
        );

        event.setCanceled(
                true
        );

        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        if (!player.getAbilities()
                .instabuild) {
            event.getItemStack()
                    .shrink(
                            1
                    );
        }

        player.serverLevel()
                .playSound(
                        null,
                        puffer.blockPosition(),
                        WayAroundSounds.PUFFER_CARROT_MEME.get(),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F
                );

        WayAroundAdvancements.vistaPufferCarrot(
                player
        );
    }
}
