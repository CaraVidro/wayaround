package net.caravidro.wayaround.justice;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Persistent "Senso de Justiça".
 *
 * It does not assign morality to every possible Minecraft action. It records a
 * small set of high-confidence situations requested by the design and keeps
 * enough context for a later court domain to reason about them.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class JusticeSenseManager {

    private JusticeSenseManager() {
    }

    private static final double OWNER_NEAR_RADIUS =
            24.0;

    private static final int CHEST_WATCH_TICKS =
            12 * 20;

    private static final Map<UUID, ChestWatch> CHEST_WATCHES =
            new HashMap<>();

    private static final Map<String, Long> ARSON_COOLDOWN =
            new HashMap<>();

    @SubscribeEvent
    public static void onPlace(
            BlockEvent.EntityPlaceEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)
                || !(player.level()
                instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos =
                event.getPos();

        BlockState state =
                event.getPlacedBlock();

        JusticeSenseData data =
                JusticeSenseData.get(
                        player.server
                );

        if (state.getBlock()
                instanceof ChestBlock) {

            data.claimChest(
                    level,
                    pos,
                    player.getUUID()
            );
        }

        if (isStructureMaterial(
                state
        )) {
            data.claimStructure(
                    level,
                    pos,
                    player.getUUID()
            );
        }
    }

    @SubscribeEvent
    public static void onBreak(
            BlockEvent.BreakEvent event
    ) {
        if (!(event.getLevel()
                instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer player =
                event.getPlayer() instanceof ServerPlayer serverPlayer
                        ? serverPlayer
                        : null;

        if (player == null) {
            return;
        }

        JusticeSenseData.get(
                player.server
        ).forgetPosition(
                level,
                event.getPos()
        );
    }

    @SubscribeEvent
    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)
                || !(player.level()
                instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos =
                event.getPos();

        JusticeSenseData data =
                JusticeSenseData.get(
                        player.server
                );

        if (level.getBlockEntity(
                pos
        ) instanceof ChestBlockEntity chest) {

            UUID owner =
                    data.chestOwner(
                            level,
                            pos
                    );

            if (owner != null
                    && !owner.equals(
                    player.getUUID()
            )
                    && !ownerNearby(
                    player.server,
                    level,
                    pos,
                    owner
            )) {

                CHEST_WATCHES.put(
                        player.getUUID(),
                        new ChestWatch(
                                player.getUUID(),
                                owner,
                                pos.immutable(),
                                inventoryCount(
                                        chest
                                ),
                                player.server
                                        .getTickCount()
                        )
                );
            }
        }

        ItemStack held =
                event.getItemStack();

        if (held.is(
                Items.FLINT_AND_STEEL
        )
                || held.is(
                Items.FIRE_CHARGE
        )) {

            inspectArson(
                    player,
                    level,
                    pos,
                    data
            );
        }
    }

    @SubscribeEvent
    public static void onDeath(
            LivingDeathEvent event
    ) {
        JusticeRewardManager.onEntityDeath(
                event.getEntity()
        );

        Entity attacker =
                event.getSource()
                        .getEntity();

        if (!(attacker
                instanceof ServerPlayer offender)) {
            return;
        }

        if (event.getEntity()
                instanceof ServerPlayer victim) {

            record(
                    offender,
                    victim.getUUID(),
                    victim.getGameProfile()
                            .getName(),
                    JusticeIncident.Type.PLAYER_KILL,
                    victim.blockPosition(),
                    1.0F,
                    "morte confirmada pelo servidor"
            );

            return;
        }

        if (event.getEntity()
                instanceof Wolf wolf
                && wolf.isTame()) {

            UUID ownerId =
                    wolf.getOwnerUUID();

            String ownerName =
                    "";

            if (ownerId != null) {
                ServerPlayer owner =
                        offender.server
                                .getPlayerList()
                                .getPlayer(
                                        ownerId
                                );

                if (owner != null) {
                    ownerName =
                            owner.getGameProfile()
                                    .getName();
                }
            }

            record(
                    offender,
                    ownerId,
                    ownerName,
                    JusticeIncident.Type.DOG_KILL,
                    wolf.blockPosition(),
                    0.96F,
                    "lobo domesticado morto"
            );
        }
    }

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        if (tick % 5L == 0L) {
            Iterator<Map.Entry<UUID, ChestWatch>> iterator =
                    CHEST_WATCHES.entrySet()
                            .iterator();

            while (iterator.hasNext()) {
                ChestWatch watch =
                        iterator.next()
                                .getValue();

                ServerPlayer thief =
                        server.getPlayerList()
                                .getPlayer(
                                        watch.thief
                                );

                if (thief == null
                        || !(thief.level()
                        instanceof ServerLevel level)
                        || tick - watch.startedAt
                        > CHEST_WATCH_TICKS) {

                    iterator.remove();
                    continue;
                }

                if (!(level.getBlockEntity(
                        watch.pos
                ) instanceof ChestBlockEntity chest)) {

                    iterator.remove();
                    continue;
                }

                int current =
                        inventoryCount(
                                chest
                        );

                if (current < watch.initialCount) {
                    int removed =
                            watch.initialCount
                                    - current;

                    record(
                            thief,
                            watch.owner,
                            playerName(
                                    server,
                                    watch.owner
                            ),
                            JusticeIncident.Type.CHEST_THEFT,
                            watch.pos,
                            0.90F,
                            removed
                                    + " item(ns) removido(s) enquanto o dono estava ausente"
                    );

                    iterator.remove();
                    continue;
                }

                if (thief.distanceToSqr(
                        watch.pos.getX() + 0.5,
                        watch.pos.getY() + 0.5,
                        watch.pos.getZ() + 0.5
                ) > 10.0 * 10.0) {

                    iterator.remove();
                }
            }
        }

        ARSON_COOLDOWN.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue()
                                        <= tick
                );
    }

    public static void clearTransient() {
        CHEST_WATCHES.clear();
        ARSON_COOLDOWN.clear();
    }

    private static void inspectArson(
            ServerPlayer offender,
            ServerLevel level,
            BlockPos clicked,
            JusticeSenseData data
    ) {
        long tick =
                offender.server
                        .getTickCount();

        BlockPos[] candidates =
                new BlockPos[] {
                        clicked,
                        clicked.relative(Direction.DOWN),
                        clicked.relative(Direction.UP),
                        clicked.relative(Direction.NORTH),
                        clicked.relative(Direction.SOUTH),
                        clicked.relative(Direction.WEST),
                        clicked.relative(Direction.EAST)
                };

        for (BlockPos candidate :
                candidates) {

            BlockState state =
                    level.getBlockState(
                            candidate
                    );

            if (!isStructureMaterial(
                    state
            )) {
                continue;
            }

            UUID owner =
                    data.structureOwner(
                            level,
                            candidate
                    );

            if (owner == null
                    || owner.equals(
                    offender.getUUID()
            )
                    || ownerNearby(
                    offender.server,
                    level,
                    candidate,
                    owner
            )) {
                continue;
            }

            String cooldownKey =
                    offender.getUUID()
                            + "|"
                            + candidate.asLong();

            if (ARSON_COOLDOWN.getOrDefault(
                    cooldownKey,
                    0L
            ) > tick) {
                return;
            }

            ARSON_COOLDOWN.put(
                    cooldownKey,
                    tick + 20L * 30L
            );

            record(
                    offender,
                    owner,
                    playerName(
                            offender.server,
                            owner
                    ),
                    JusticeIncident.Type.ARSON,
                    candidate,
                    0.82F,
                    "fogo iniciado junto a estrutura pertencente a outro jogador ausente"
            );

            return;
        }
    }

    private static boolean isStructureMaterial(
            BlockState state
    ) {
        return state.is(
                BlockTags.PLANKS
        )
                || state.is(
                BlockTags.LOGS
        )
                || state.is(
                BlockTags.WOOL
        )
                || state.getBlock()
                instanceof ChestBlock;
    }

    private static boolean ownerNearby(
            MinecraftServer server,
            ServerLevel level,
            BlockPos pos,
            UUID ownerId
    ) {
        ServerPlayer owner =
                server.getPlayerList()
                        .getPlayer(
                                ownerId
                        );

        return owner != null
                && owner.serverLevel()
                        == level
                && owner.distanceToSqr(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5
                ) <= OWNER_NEAR_RADIUS
                * OWNER_NEAR_RADIUS;
    }

    private static int inventoryCount(
            ChestBlockEntity chest
    ) {
        int total =
                0;

        for (int slot = 0;
             slot < chest.getContainerSize();
             slot++) {

            total +=
                    chest.getItem(
                            slot
                    ).getCount();
        }

        return total;
    }

    private static String playerName(
            MinecraftServer server,
            UUID playerId
    ) {
        ServerPlayer player =
                server.getPlayerList()
                        .getPlayer(
                                playerId
                        );

        return player == null
                ? playerId.toString()
                : player.getGameProfile()
                        .getName();
    }

    private static void record(
            ServerPlayer offender,
            UUID victim,
            String victimName,
            JusticeIncident.Type type,
            BlockPos pos,
            float confidence,
            String detail
    ) {
        JusticeSenseData.get(
                offender.server
        ).record(
                new JusticeIncident(
                        type,
                        offender.getUUID(),
                        offender.getGameProfile()
                                .getName(),
                        victim,
                        victimName == null
                                ? ""
                                : victimName,
                        offender.level()
                                .dimension()
                                .location()
                                .toString(),
                        pos.asLong(),
                        offender.serverLevel()
                                .getGameTime(),
                        confidence,
                        detail
                )
        );

        WayAround.LOGGER.info(
                "[JusticeSense] {} -> {} ({})",
                offender.getGameProfile()
                        .getName(),
                type,
                detail
        );
    }

    private record ChestWatch(
            UUID thief,
            UUID owner,
            BlockPos pos,
            int initialCount,
            long startedAt
    ) {
    }
}
