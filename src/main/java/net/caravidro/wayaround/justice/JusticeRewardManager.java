package net.caravidro.wayaround.justice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.caravidro.wayaround.content.WayAroundContent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class JusticeRewardManager {

    private JusticeRewardManager() {
    }

    public static final String OWNER_KEY =
            "WayAroundJusticeOwner";

    public static final String TARGET_KEY =
            "WayAroundJusticeTarget";

    public static final String TARGET_NAME_KEY =
            "WayAroundJusticeTargetName";

    private static final long LOST_LIMIT_TICKS =
            30L * 60L * 20L;

    private static final double LOST_DISTANCE =
            1000.0;

    private static final Map<UUID, Pursuit> ACTIVE =
            new HashMap<>();

    public static void grant(
            ServerPlayer owner,
            LivingEntity target
    ) {
        revoke(
                owner.getUUID(),
                owner.server
        );

        ItemStack blade =
                new ItemStack(
                        WayAroundContent.JUSTICE_EXECUTION_BLADE.get()
                );

        String targetName =
                target.getDisplayName()
                        .getString();

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                blade,
                tag -> {
                    tag.putUUID(
                            OWNER_KEY,
                            owner.getUUID()
                    );

                    tag.putUUID(
                            TARGET_KEY,
                            target.getUUID()
                    );

                    tag.putString(
                            TARGET_NAME_KEY,
                            targetName
                    );
                }
        );

        if (!owner.addItem(
                blade
        )) {
            owner.drop(
                    blade,
                    false
            );
        }

        ACTIVE.put(
                owner.getUUID(),
                new Pursuit(
                        owner.getUUID(),
                        target.getUUID(),
                        targetName
                )
        );

        owner.addEffect(
                new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED,
                        80,
                        1,
                        true,
                        true
                )
        );
    }

    public static boolean isAuthorized(
            UUID owner,
            UUID target
    ) {
        Pursuit pursuit =
                ACTIVE.get(
                        owner
                );

        return pursuit != null
                && pursuit.target.equals(
                        target
                );
    }

    public static boolean bladeValid(
            UUID holder,
            ItemStack stack
    ) {
        CompoundTag tag =
                stack.getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY
                ).copyTag();

        if (!tag.hasUUID(
                OWNER_KEY
        )
                || !tag.hasUUID(
                TARGET_KEY
        )) {
            return false;
        }

        UUID owner =
                tag.getUUID(
                        OWNER_KEY
                );

        UUID target =
                tag.getUUID(
                        TARGET_KEY
                );

        return holder.equals(
                owner
        )
                && isAuthorized(
                owner,
                target
        );
    }

    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        Iterator<Map.Entry<UUID, Pursuit>> iterator =
                ACTIVE.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Pursuit pursuit =
                    iterator.next()
                            .getValue();

            ServerPlayer owner =
                    server.getPlayerList()
                            .getPlayer(
                                    pursuit.owner
                            );

            if (owner == null
                    || !owner.isAlive()) {

                iterator.remove();
                continue;
            }

            LivingEntity target =
                    findLiving(
                            server,
                            pursuit.target
                    );

            boolean visibleEnough =
                    target != null
                            && target.isAlive()
                            && owner.level()
                            == target.level()
                            && owner.distanceToSqr(
                            target
                    ) <= LOST_DISTANCE
                    * LOST_DISTANCE;

            if (visibleEnough) {
                pursuit.lostSince =
                        -1L;

            } else if (pursuit.lostSince < 0L) {
                pursuit.lostSince =
                        tick;

            } else if (tick - pursuit.lostSince
                    >= LOST_LIMIT_TICKS) {

                removeBlades(
                        owner
                );

                iterator.remove();
                continue;
            }

            if (tick % 20L == 0L) {
                owner.addEffect(
                        new MobEffectInstance(
                                MobEffects.MOVEMENT_SPEED,
                                45,
                                1,
                                true,
                                false
                        )
                );
            }
        }
    }

    public static void onEntityDeath(
            LivingEntity entity
    ) {
        MinecraftServer server =
                entity.getServer();

        if (server == null) {
            return;
        }

        UUID id =
                entity.getUUID();

        if (entity instanceof ServerPlayer) {
            revoke(
                    id,
                    server
            );
        }

        for (Map.Entry<UUID, Pursuit> entry :
                new ArrayList<>(
                        ACTIVE.entrySet()
                )) {

            if (entry.getValue()
                    .target.equals(
                            id
                    )) {

                revoke(
                        entry.getKey(),
                        server
                );
            }
        }
    }

    public static void clearAll(
            MinecraftServer server
    ) {
        for (UUID owner :
                new ArrayList<>(
                        ACTIVE.keySet()
                )) {
            revoke(
                    owner,
                    server
            );
        }

        ACTIVE.clear();
    }

    @Nullable
    private static LivingEntity findLiving(
            MinecraftServer server,
            UUID id
    ) {
        ServerPlayer player =
                server.getPlayerList()
                        .getPlayer(
                                id
                        );

        if (player != null) {
            return player;
        }

        for (ServerLevel level :
                server.getAllLevels()) {

            var entity =
                    level.getEntity(
                            id
                    );

            if (entity instanceof LivingEntity living) {
                return living;
            }
        }

        return null;
    }

    private static void revoke(
            UUID ownerId,
            MinecraftServer server
    ) {
        ACTIVE.remove(
                ownerId
        );

        ServerPlayer owner =
                server.getPlayerList()
                        .getPlayer(
                                ownerId
                        );

        if (owner != null) {
            removeBlades(
                    owner
            );
        }
    }

    private static void removeBlades(
            ServerPlayer player
    ) {
        for (int slot = 0;
             slot < player.getInventory()
                     .getContainerSize();
             slot++) {

            ItemStack stack =
                    player.getInventory()
                            .getItem(
                                    slot
                            );

            if (stack.is(
                    WayAroundContent.JUSTICE_EXECUTION_BLADE.get()
            )) {
                stack.setCount(
                        0
                );
            }
        }

        player.getInventory()
                .setChanged();
    }

    private static final class Pursuit {
        private final UUID owner;
        private final UUID target;
        @SuppressWarnings("unused")
        private final String targetName;
        private long lostSince =
                -1L;

        private Pursuit(
                UUID owner,
                UUID target,
                String targetName
        ) {
            this.owner =
                    owner;
            this.target =
                    target;
            this.targetName =
                    targetName;
        }
    }
}
