package net.caravidro.wayaround.cinematic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Server-authoritative movement/action lock shared by cinematic abilities.
 *
 * Camera rotation is deliberately never touched here: an ability can pin the
 * body while the player still freely looks up/down/sideways.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class PlayerControlLockManager {

    private PlayerControlLockManager() {
    }

    private static final Map<UUID, LockState> LOCKS =
            new HashMap<>();

    public static void lockMovement(
            ServerPlayer player,
            int ticks
    ) {
        LockState state =
                LOCKS.computeIfAbsent(
                        player.getUUID(),
                        ignored ->
                                new LockState()
                );

        state.anchor =
                player.position();

        state.movementUntil =
                deadline(
                        player,
                        ticks
                );
    }

    public static void clearMovement(
            ServerPlayer player
    ) {
        LockState state =
                LOCKS.get(
                        player.getUUID()
                );

        if (state == null) {
            return;
        }

        state.movementUntil =
                0L;

        state.anchor =
                null;

        cleanup(
                player.getUUID(),
                state
        );
    }

    public static void lockActions(
            ServerPlayer player,
            int ticks
    ) {
        LockState state =
                LOCKS.computeIfAbsent(
                        player.getUUID(),
                        ignored ->
                                new LockState()
                );

        state.actionsUntil =
                deadline(
                        player,
                        ticks
                );
    }

    public static void clearActions(
            ServerPlayer player
    ) {
        LockState state =
                LOCKS.get(
                        player.getUUID()
                );

        if (state == null) {
            return;
        }

        state.actionsUntil =
                0L;

        cleanup(
                player.getUUID(),
                state
        );
    }

    public static boolean actionsLocked(
            ServerPlayer player
    ) {
        LockState state =
                LOCKS.get(
                        player.getUUID()
                );

        return state != null
                && active(
                        state.actionsUntil,
                        player.server
                                .getTickCount()
                );
    }

    private static long deadline(
            ServerPlayer player,
            int ticks
    ) {
        return ticks <= 0
                ? Long.MAX_VALUE
                : player.server
                        .getTickCount()
                        + ticks;
    }

    private static boolean active(
            long until,
            long tick
    ) {
        return until == Long.MAX_VALUE
                || until > tick;
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        Iterator<Map.Entry<UUID, LockState>> iterator =
                LOCKS.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, LockState> entry =
                    iterator.next();

            LockState state =
                    entry.getValue();

            if (state.movementUntil != Long.MAX_VALUE
                    && state.movementUntil <= tick) {
                state.movementUntil =
                        0L;

                state.anchor =
                        null;
            }

            if (state.actionsUntil != Long.MAX_VALUE
                    && state.actionsUntil <= tick) {
                state.actionsUntil =
                        0L;
            }

            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(
                                    entry.getKey()
                            );

            if (player == null) {
                iterator.remove();
                continue;
            }

            if (active(
                    state.movementUntil,
                    tick
            )
                    && state.anchor != null) {

                /*
                 * Freeze translation only. setPos does not overwrite yaw/pitch,
                 * so the player can still look around during Fuga/Red.
                 */
                player.setDeltaMovement(
                        Vec3.ZERO
                );

                player.setPos(
                        state.anchor.x,
                        state.anchor.y,
                        state.anchor.z
                );

                player.fallDistance =
                        0.0F;
            }

            if (!active(
                    state.movementUntil,
                    tick
            )
                    && !active(
                    state.actionsUntil,
                    tick
            )) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void blockAttack(
            AttackEntityEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer player
                && actionsLocked(
                player
        )) {
            event.setCanceled(
                    true
            );
        }
    }

    @SubscribeEvent
    public static void blockRightClickItem(
            PlayerInteractEvent.RightClickItem event
    ) {
        cancelInteraction(
                event
        );
    }

    @SubscribeEvent
    public static void blockRightClickBlock(
            PlayerInteractEvent.RightClickBlock event
    ) {
        cancelInteraction(
                event
        );
    }

    @SubscribeEvent
    public static void blockEntityInteract(
            PlayerInteractEvent.EntityInteract event
    ) {
        cancelInteraction(
                event
        );
    }

    @SubscribeEvent
    public static void blockLeftClickBlock(
            PlayerInteractEvent.LeftClickBlock event
    ) {
        cancelInteraction(
                event
        );
    }

    private static void cancelInteraction(
            PlayerInteractEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)
                || !actionsLocked(
                player
        )) {
            return;
        }

        if (event instanceof PlayerInteractEvent.RightClickItem right) {
            right.setCancellationResult(
                    InteractionResult.FAIL
            );
            right.setCanceled(
                    true
            );

        } else if (event instanceof PlayerInteractEvent.RightClickBlock right) {
            right.setCancellationResult(
                    InteractionResult.FAIL
            );
            right.setCanceled(
                    true
            );

        } else if (event instanceof PlayerInteractEvent.EntityInteract entity) {
            entity.setCancellationResult(
                    InteractionResult.FAIL
            );
            entity.setCanceled(
                    true
            );

        } else if (event instanceof PlayerInteractEvent.LeftClickBlock left) {
            left.setCanceled(
                    true
            );
        }
    }

    @SubscribeEvent
    public static void stopped(
            ServerStoppedEvent event
    ) {
        LOCKS.clear();
    }

    private static void cleanup(
            UUID player,
            LockState state
    ) {
        if (state.movementUntil == 0L
                && state.actionsUntil == 0L) {
            LOCKS.remove(
                    player
            );
        }
    }

    private static final class LockState {
        private Vec3 anchor;
        private long movementUntil;
        private long actionsUntil;
    }
}
