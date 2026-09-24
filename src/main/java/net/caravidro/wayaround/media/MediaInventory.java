package net.caravidro.wayaround.media;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class MediaInventory {

    private MediaInventory() {
    }

    private static final String TAPE_SERIAL =
            "WayAroundTapeSerial";

    public static int count(
            Player player,
            Item item
    ) {
        int total = 0;

        for (int slot = 0;
             slot < player.getInventory()
                     .getContainerSize();
             slot++) {

            ItemStack stack =
                    player.getInventory()
                            .getItem(slot);

            if (stack.is(item)) {
                total += stack.getCount();
            }
        }

        return total;
    }

    public static boolean consumeOne(
            Player player,
            Item item
    ) {
        if (player.getAbilities()
                .instabuild) {

            return true;
        }

        for (int slot = 0;
             slot < player.getInventory()
                     .getContainerSize();
             slot++) {

            ItemStack stack =
                    player.getInventory()
                            .getItem(slot);

            if (!stack.is(item)) {
                continue;
            }

            stack.shrink(1);
            return true;
        }

        return false;
    }

    public static int nextTapeSerial(
            ServerPlayer player
    ) {
        CompoundTag root =
                player.getPersistentData();

        CompoundTag persisted =
                root.getCompound(
                        Player.PERSISTED_NBT_TAG
                );

        int next =
                Math.max(
                        1,
                        persisted.getInt(
                                TAPE_SERIAL
                        )
                                + 1
                );

        persisted.putInt(
                TAPE_SERIAL,
                next
        );

        root.put(
                Player.PERSISTED_NBT_TAG,
                persisted
        );

        return next;
    }

    public static void giveOrDrop(
            Player player,
            ItemStack stack
    ) {
        if (!player.addItem(stack)) {
            player.drop(
                    stack,
                    false
            );
        }
    }
}
