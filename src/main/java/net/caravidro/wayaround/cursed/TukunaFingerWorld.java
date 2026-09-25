package net.caravidro.wayaround.cursed;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.WayAroundContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Keeps the physical fingers discoverable without keeping remote chunks loaded. */
@EventBusSubscriber(modid = WayAround.MODID)
public final class TukunaFingerWorld {
    private static final String ABANDONED_SINCE = "WayAroundFingerAbandonedSince";
    private static final int SEARCH_DISTANCE = 96;
    private static final int RECALL_DISTANCE = 640;
    private static final long RECALL_DELAY = 20L * 60L * 5L;

    private TukunaFingerWorld() {}

    public static boolean isFinger(ItemStack stack) {
        return stack.is(WayAroundContent.TUKUNA_FINGER.get());
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity item && isFinger(item.getItem())) {
            protect(item);
        }
    }

    public static void protect(ItemEntity item) {
        item.setUnlimitedLifetime();
        item.setInvulnerable(true);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item)
                || !(item.level() instanceof ServerLevel level)
                || !isFinger(item.getItem()) || item.isRemoved()) return;

        // Rescue before crossing the vanilla void kill plane; never wait for
        // the less frequent abandonment sweep when the item is falling.
        boolean voided = item.getY() < level.getMinBuildHeight() + 32;
        if (!voided && level.getGameTime() % 20L != 0L) return;

        protect(item);
        ServerLevel home = level.getServer().overworld();
        BlockPos spawn = home.getSharedSpawnPos();
        boolean remote = level != home || item.blockPosition().distSqr(spawn) >
                (long) RECALL_DISTANCE * RECALL_DISTANCE;

        // A nearby player is actively searching or carrying the finger.
        boolean watched = !level.getEntitiesOfClass(
                net.minecraft.server.level.ServerPlayer.class,
                item.getBoundingBox().inflate(SEARCH_DISTANCE),
                player -> !player.isSpectator()).isEmpty();

        if (!remote || watched) {
            item.getPersistentData().remove(ABANDONED_SINCE);
        } else if (!item.getPersistentData().contains(ABANDONED_SINCE)) {
            item.getPersistentData().putLong(ABANDONED_SINCE, level.getGameTime());
        }

        long since = item.getPersistentData().getLong(ABANDONED_SINCE);
        if (!voided && (!remote || watched || since == 0L
                || level.getGameTime() - since < RECALL_DELAY)) return;

        // Recreate the entire stack in the overworld, then remove the old entity.
        // One relocation per stack; never shrink or split the finger count.
        int x = spawn.getX() + level.random.nextInt(25) - 12;
        int z = spawn.getZ() + level.random.nextInt(25) - 12;
        int y = home.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        ItemEntity recalled = new ItemEntity(home, x + .5, y + .8, z + .5,
                item.getItem().copy());
        protect(recalled);
        if (home.addFreshEntity(recalled)) item.discard();
    }
}
