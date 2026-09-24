package net.caravidro.wayaround.infinity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.network.InfinityVisualPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Experimental "Infinity" field.
 *
 * Speech never flips it from 0 -> 100 in one keyword. Every semantically
 * relevant sentence adds confidence. Confidence directly drives the radius,
 * visual distortion and how aggressively motion converges toward zero.
 */
public final class InfinityManager {

    private InfinityManager() {
    }

    private static final Map<UUID, InfinityState> ACTIVE =
            new HashMap<>();

    private static final double VISUAL_RANGE =
            128.0;

    private static final float MIN_ACTIVE_CONFIDENCE =
            0.08F;

    public static boolean reinforce(
            ServerPlayer player,
            float evidence,
            float urgency
    ) {
        if (!hasSpectrum(
                player
        )) {
            return false;
        }

        float cleanEvidence =
                Mth.clamp(
                        evidence,
                        0.0F,
                        0.72F
                );

        float cleanUrgency =
                Mth.clamp(
                        urgency,
                        0.0F,
                        1.0F
                );

        if (cleanEvidence
                <= 0.001F) {

            return false;
        }

        InfinityState state =
                ACTIVE.computeIfAbsent(
                        player.getUUID(),
                        ignored ->
                                new InfinityState(
                                        player.getUUID(),
                                        player.serverLevel()
                                                .dimension()
                                )
                );

        state.dimension =
                player.serverLevel()
                        .dimension();

        state.confidence =
                Mth.clamp(
                        state.confidence
                                + cleanEvidence
                                + cleanUrgency * 0.035F,
                        0.0F,
                        1.0F
                );

        state.lastReinforcedTick =
                player.server
                        .getTickCount();

        WayAround.LOGGER.info(
                "[Infinity] owner={} +evidence={} urgency={} -> confidence={}",
                player.getGameProfile()
                        .getName(),
                String.format(
                        java.util.Locale.ROOT,
                        "%.2f",
                        cleanEvidence
                ),
                String.format(
                        java.util.Locale.ROOT,
                        "%.2f",
                        cleanUrgency
                ),
                String.format(
                        java.util.Locale.ROOT,
                        "%.2f",
                        state.confidence
                )
        );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.AMETHYST_BLOCK_RESONATE,
                        SoundSource.PLAYERS,
                        0.22F
                                + state.confidence
                                        * 0.34F,
                        1.82F
                                - state.confidence
                                        * 0.28F
                );

        return true;
    }

    public static boolean deactivate(
            ServerPlayer player
    ) {
        InfinityState removed =
                ACTIVE.remove(
                        player.getUUID()
                );

        if (removed == null) {
            return false;
        }

        sendVisual(
                player.serverLevel(),
                removed,
                player.position(),
                0.0F,
                0.0F
        );

        WayAround.LOGGER.info(
                "[Infinity] owner={} desativada",
                player.getGameProfile()
                        .getName()
        );

        return true;
    }

    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        Iterator<Map.Entry<UUID, InfinityState>>
                iterator =
                ACTIVE.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            InfinityState state =
                    iterator.next()
                            .getValue();

            ServerPlayer owner =
                    server.getPlayerList()
                            .getPlayer(
                                    state.owner
                            );

            ServerLevel level =
                    server.getLevel(
                            state.dimension
                    );

            if (owner == null
                    || level == null
                    || !owner.isAlive()
                    || owner.serverLevel() != level
                    || !hasSpectrum(
                            owner
                    )) {

                iterator.remove();
                continue;
            }

            if (state.confidence
                    < MIN_ACTIVE_CONFIDENCE) {

                continue;
            }

            Vec3 center =
                    owner.getEyePosition();

            float radius =
                    radiusFor(
                            state.confidence
                    );

            applyField(
                    level,
                    owner,
                    center,
                    radius,
                    state.confidence
            );

            if ((server.getTickCount()
                    & 1) == 0) {

                sendVisual(
                        level,
                        state,
                        center,
                        state.confidence,
                        radius
                );
            }
        }
    }

    private static void applyField(
            ServerLevel level,
            ServerPlayer owner,
            Vec3 center,
            float radius,
            float confidence
    ) {
        AABB area =
                new AABB(
                        center.x - radius,
                        center.y - radius,
                        center.z - radius,
                        center.x + radius,
                        center.y + radius,
                        center.z + radius
                );

        Vec3 ownerMotion =
                owner.getDeltaMovement();

        for (Entity entity :
                level.getEntities(
                        owner,
                        area,
                        candidate ->
                                candidate.isAlive()
                                        && candidate != owner
                )) {

            if (entity instanceof Projectile projectile
                    && projectile.getOwner()
                    == owner) {

                continue;
            }

            Vec3 relative =
                    entity.position()
                            .subtract(
                                    center
                            );

            double distance =
                    relative.length();

            if (distance < 0.08
                    || distance >= radius) {

                continue;
            }

            Vec3 outward =
                    relative.scale(
                            1.0
                                    / distance
                    );

            double proximity =
                    1.0
                            - distance
                                    / radius;

            double influence =
                    Mth.clamp(
                            confidence
                                    * (
                                    0.22
                                            + proximity
                                                    * proximity
                                                    * 1.12
                            ),
                            0.0,
                            1.0
                    );

            Vec3 velocity =
                    entity.getDeltaMovement();

            double approaching =
                    -velocity.dot(
                            outward
                    );

            double damping =
                    Math.max(
                            0.0,
                            1.0
                                    - influence
                                            * influence
                                            * 0.985
                    );

            Vec3 next =
                    velocity.scale(
                            damping
                    );

            /*
             * Walking toward an incoming object effectively makes the
             * remaining distance disappear faster. At high confidence this
             * becomes a visible recoil: arrows start travelling backwards.
             */
            double ownerClosing =
                    ownerMotion.dot(
                            outward
                    );

            if (entity instanceof Projectile
                    && ownerClosing > 0.0) {

                next =
                        next.add(
                                outward.scale(
                                        ownerClosing
                                                * influence
                                                * (
                                                0.85
                                                        + proximity
                                                                * 2.40
                                        )
                                )
                        );
            }

            if (entity instanceof Projectile
                    && approaching > 0.0
                    && influence > 0.74) {

                next =
                        next.add(
                                outward.scale(
                                        approaching
                                                * (
                                                influence
                                                        - 0.70
                                        )
                                                * 1.55
                                )
                        );
            }

            if (influence > 0.965) {
                next =
                        Vec3.ZERO;
            }

            entity.setDeltaMovement(
                    next
            );

            entity.fallDistance =
                    0.0F;
        }
    }

    private static float radiusFor(
            float confidence
    ) {
        return 2.75F
                + confidence
                        * 7.75F;
    }

    private static void sendVisual(
            ServerLevel level,
            InfinityState state,
            Vec3 center,
            float confidence,
            float radius
    ) {
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.x,
                center.y,
                center.z,
                VISUAL_RANGE,
                new InfinityVisualPayload(
                        state.owner,
                        center.x,
                        center.y,
                        center.z,
                        confidence,
                        radius
                )
        );
    }

    private static boolean hasSpectrum(
            ServerPlayer player
    ) {
        for (int slot = 0;
             slot < player.getInventory()
                     .getContainerSize();
             slot++) {

            if (player.getInventory()
                    .getItem(
                            slot
                    )
                    .is(
                            WayAroundContent.GOJO_SPECTRUM.get()
                    )) {

                return true;
            }
        }

        return false;
    }

    public static void clearAll() {
        ACTIVE.clear();
    }

    private static final class InfinityState {

        private final UUID owner;
        private ResourceKey<Level> dimension;
        private float confidence;
        private long lastReinforcedTick;

        private InfinityState(
                UUID owner,
                ResourceKey<Level> dimension
        ) {
            this.owner =
                    owner;

            this.dimension =
                    dimension;
        }
    }
}
