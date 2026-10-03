package net.caravidro.wayaround.ecology;

import java.util.ArrayList;
import java.util.List;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.accessory.AccessoryManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Moves death inventory into one persistent corpse before vanilla item drops
 * run. This is deliberately server-owned so disconnects and multiplayer deaths
 * cannot duplicate the stored stacks.
 */
@EventBusSubscriber(
        modid = WayAround.MODID
)
public final class PlayerCorpseManager {

    private PlayerCorpseManager() {
    }

    @SubscribeEvent
    public static void death(
            LivingDeathEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        if (player.level()
                .getGameRules()
                .getBoolean(
                        GameRules.RULE_KEEPINVENTORY
                )) {
            return;
        }

        List<PlayerCorpseEntity.StoredStack> stored =
                new ArrayList<>();

        int size =
                player.getInventory()
                        .getContainerSize();

        for (int slot = 0;
             slot < size;
             slot++) {

            ItemStack stack =
                    player.getInventory()
                            .getItem(
                                    slot
                            );

            if (stack.isEmpty()) {
                continue;
            }

            stored.add(
                    new PlayerCorpseEntity.StoredStack(
                            slot,
                            stack.copy()
                    )
            );

            player.getInventory()
                    .setItem(
                            slot,
                            ItemStack.EMPTY
                    );
        }

        ItemStack carried =
                player.containerMenu
                        .getCarried();

        if (!carried.isEmpty()) {
            stored.add(
                    new PlayerCorpseEntity.StoredStack(
                            -1,
                            carried.copy()
                    )
            );

            player.containerMenu
                    .setCarried(
                            ItemStack.EMPTY
                    );
        }

        for (ItemStack accessory :
                AccessoryManager.takeAllEquipped(
                        player
                )) {

            if (!accessory.isEmpty()) {
                stored.add(
                        new PlayerCorpseEntity.StoredStack(
                                -1,
                                accessory.copy()
                        )
                );
            }
        }

        PlayerCorpseEntity corpse =
                EcologyContent.PLAYER_CORPSE.get()
                        .create(
                                player.serverLevel()
                        );

        if (corpse == null) {
            /*
             * Registry failure should never delete inventory. If entity
             * creation somehow fails, immediately return everything.
             */
            for (PlayerCorpseEntity.StoredStack entry :
                    stored) {
                ItemStack fallback =
                        entry.stack()
                                .copy();

                if (!player.getInventory()
                        .add(
                                fallback
                        )
                        && !fallback.isEmpty()) {
                    player.drop(
                            fallback,
                            false
                    );
                }
            }

            return;
        }

        corpse.initialize(
                player.getUUID(),
                player.getGameProfile()
                        .getName(),
                stored
        );

        corpse.copySkin(player.getGameProfile());

        corpse.setPos(
                player.getX(),
                player.getY()
                        + 0.05,
                player.getZ()
        );

        corpse.setYRot(
                player.getYRot()
        );

        corpse.setDeltaMovement(
                player.getDeltaMovement()
                        .scale(
                                0.18
                        )
        );

        player.serverLevel()
                .addFreshEntity(
                        corpse
                );
    }
}
