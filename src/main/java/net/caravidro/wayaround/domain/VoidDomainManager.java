package net.caravidro.wayaround.domain;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.network.VoidDomainVisualPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Special domain tied to the Void Spectrum.
 *
 * This deliberately does NOT use the generic procedural DomainManager. The
 * outside world sees a compact geometric shell, while participants receive a
 * much larger / effectively infinite black-star interior on the client. That
 * mismatch is intentional: the inside is not represented by normal Euclidean
 * world geometry.
 */
public final class VoidDomainManager {

    private VoidDomainManager() {
    }

    private static final double RADIUS =
            16.0;

    private static final int DURATION_TICKS =
            12 * 20;

    private static final int COOLDOWN_TICKS =
            30 * 20;

    private static final double VISUAL_RANGE =
            192.0;

    private static final Map<UUID, ActiveVoidDomain> ACTIVE =
            new HashMap<>();

    private static final Map<UUID, Long> COOLDOWN =
            new HashMap<>();

    public static boolean expand(
            ServerPlayer owner
    ) {
        if (!hasVoidSpectrum(owner)) {
            return false;
        }

        UUID ownerId =
                owner.getUUID();

        long tick =
                owner.server
                        .getTickCount();

        if (ACTIVE.containsKey(
                ownerId
        )) {
            return false;
        }

        long readyAt =
                COOLDOWN.getOrDefault(
                        ownerId,
                        0L
                );

        if (tick < readyAt) {
            owner.displayClientMessage(
                    Component.literal(
                            "Void Domain: "
                                    + Math.max(
                                    1L,
                                    (readyAt - tick + 19L)
                                            / 20L
                            )
                                    + "s para estabilizar."
                    ),
                    true
            );

            return false;
        }

        ServerLevel level =
                owner.serverLevel();

        Vec3 center =
                owner.position();

        Map<UUID, LockedTarget> trapped =
                new LinkedHashMap<>();

        AABB box =
                new AABB(
                        center.x - RADIUS,
                        center.y - RADIUS,
                        center.z - RADIUS,
                        center.x + RADIUS,
                        center.y + RADIUS,
                        center.z + RADIUS
                );

        for (ServerPlayer target :
                level.getEntitiesOfClass(
                        ServerPlayer.class,
                        box,
                        player ->
                                player.isAlive()
                                        && player != owner
                )) {

            if (target.position()
                    .distanceToSqr(
                            center
                    )
                    > RADIUS * RADIUS) {
                continue;
            }

            trapped.put(
                    target.getUUID(),
                    LockedTarget.capture(
                            target
                    )
            );
        }

        ActiveVoidDomain domain =
                new ActiveVoidDomain(
                        ownerId,
                        level.dimension(),
                        center,
                        LockedTarget.capture(
                                owner
                        ),
                        trapped,
                        tick + DURATION_TICKS
                );

        ACTIVE.put(
                ownerId,
                domain
        );

        COOLDOWN.put(
                ownerId,
                domain.endsAt
                        + COOLDOWN_TICKS
        );

        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.x,
                center.y,
                center.z,
                VISUAL_RANGE,
                VoidDomainVisualPayload.open(
                        ownerId,
                        center,
                        (float) RADIUS,
                        DURATION_TICKS
                )
        );

        PacketDistributor.sendToPlayer(
                owner,
                VoidDomainVisualPayload.enter(
                        ownerId,
                        center,
                        (float) RADIUS,
                        DURATION_TICKS,
                        false
                )
        );

        for (UUID targetId :
                trapped.keySet()) {

            ServerPlayer target =
                    owner.server
                            .getPlayerList()
                            .getPlayer(
                                    targetId
                            );

            if (target == null) {
                continue;
            }

            PacketDistributor.sendToPlayer(
                    target,
                    VoidDomainVisualPayload.enter(
                            ownerId,
                            center,
                            (float) RADIUS,
                            DURATION_TICKS,
                            true
                    )
            );
        }

        level.playSound(
                null,
                owner.blockPosition(),
                SoundEvents.END_PORTAL_SPAWN,
                SoundSource.PLAYERS,
                2.0F,
                0.44F
        );

        level.sendParticles(
                ParticleTypes.FLASH,
                center.x,
                center.y + 1.0,
                center.z,
                7,
                3.8,
                2.0,
                3.8,
                0.0
        );

        level.sendParticles(
                ParticleTypes.END_ROD,
                center.x,
                center.y + 1.0,
                center.z,
                90,
                RADIUS * 0.42,
                3.2,
                RADIUS * 0.42,
                0.08
        );

        owner.displayClientMessage(
                Component.literal(
                        "EXPANSÃO DE DOMÍNIO — VOID"
                ),
                false
        );

        WayAround.LOGGER.info(
                "[VoidDomain] owner={} trapped={} duration={}t",
                owner.getGameProfile()
                        .getName(),
                trapped.size(),
                DURATION_TICKS
        );

        return true;
    }

    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        Iterator<Map.Entry<UUID, ActiveVoidDomain>> iterator =
                ACTIVE.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            ActiveVoidDomain domain =
                    iterator.next()
                            .getValue();

            ServerPlayer owner =
                    server.getPlayerList()
                            .getPlayer(
                                    domain.owner
                            );

            ServerLevel level =
                    server.getLevel(
                            domain.dimension
                    );

            if (owner == null
                    || level == null
                    || !owner.isAlive()
                    || owner.serverLevel() != level
                    || tick >= domain.endsAt) {

                closeDomain(
                        server,
                        domain
                );

                iterator.remove();
                continue;
            }

            domain.tick(
                    server,
                    level
            );
        }

        COOLDOWN.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue()
                                        <= tick
                );
    }

    public static void clearAll() {
        ACTIVE.clear();
        COOLDOWN.clear();
    }

    private static void closeDomain(
            MinecraftServer server,
            ActiveVoidDomain domain
    ) {
        ServerLevel originalLevel =
                server.getLevel(
                        domain.dimension
                );

        ServerPlayer owner =
                server.getPlayerList()
                        .getPlayer(
                                domain.owner
                        );

        if (owner != null
                && originalLevel != null) {

            domain.ownerReturn.restore(
                    owner,
                    originalLevel
            );

            PacketDistributor.sendToPlayer(
                    owner,
                    VoidDomainVisualPayload.close(
                            domain.owner
                    )
            );
        }

        for (Map.Entry<UUID, LockedTarget> entry :
                domain.trapped.entrySet()) {

            ServerPlayer target =
                    server.getPlayerList()
                            .getPlayer(
                                    entry.getKey()
                            );

            if (target == null
                    || originalLevel == null) {
                continue;
            }

            entry.getValue()
                    .restore(
                            target,
                            originalLevel
                    );

            PacketDistributor.sendToPlayer(
                    target,
                    VoidDomainVisualPayload.close(
                            domain.owner
                    )
            );
        }

        if (originalLevel != null) {
            PacketDistributor.sendToPlayersNear(
                    originalLevel,
                    null,
                    domain.center.x,
                    domain.center.y,
                    domain.center.z,
                    VISUAL_RANGE,
                    VoidDomainVisualPayload.close(
                            domain.owner
                    )
            );

            originalLevel.playSound(
                    null,
                    domain.center.x,
                    domain.center.y,
                    domain.center.z,
                    SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS,
                    1.3F,
                    0.58F
            );
        }
    }

    private static boolean hasVoidSpectrum(
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
                    WayAroundContent.GOJO_SPECTRUM.get()
            )) {
                return true;
            }
        }

        return false;
    }

    private static final class ActiveVoidDomain {

        private final UUID owner;
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final LockedTarget ownerReturn;
        private final Map<UUID, LockedTarget> trapped;
        private final long endsAt;

        private ActiveVoidDomain(
                UUID owner,
                ResourceKey<Level> dimension,
                Vec3 center,
                LockedTarget ownerReturn,
                Map<UUID, LockedTarget> trapped,
                long endsAt
        ) {
            this.owner =
                    owner;

            this.dimension =
                    dimension;

            this.center =
                    center;

            this.ownerReturn =
                    ownerReturn;

            this.trapped =
                    trapped;

            this.endsAt =
                    endsAt;
        }

        private void tick(
                MinecraftServer server,
                ServerLevel level
        ) {
            for (Map.Entry<UUID, LockedTarget> entry :
                    trapped.entrySet()) {

                ServerPlayer target =
                        server.getPlayerList()
                                .getPlayer(
                                        entry.getKey()
                                );

                if (target == null
                        || !target.isAlive()
                        || target.serverLevel() != level) {
                    continue;
                }

                LockedTarget lock =
                        entry.getValue();

                target.setDeltaMovement(
                        Vec3.ZERO
                );

                target.fallDistance =
                        0.0F;

                if (target.position()
                        .distanceToSqr(
                                lock.position
                        )
                        > 0.0004) {

                    target.teleportTo(
                            level,
                            lock.position.x,
                            lock.position.y,
                            lock.position.z,
                            Set.<RelativeMovement>of(),
                            target.getYRot(),
                            target.getXRot()
                    );
                }
            }
        }
    }

    private static final class LockedTarget {

        private final Vec3 position;
        private final float yaw;
        private final float pitch;

        private LockedTarget(
                Vec3 position,
                float yaw,
                float pitch
        ) {
            this.position =
                    position;

            this.yaw =
                    yaw;

            this.pitch =
                    pitch;
        }

        private static LockedTarget capture(
                ServerPlayer player
        ) {
            return new LockedTarget(
                    player.position(),
                    player.getYRot(),
                    player.getXRot()
            );
        }

        private void restore(
                ServerPlayer player,
                ServerLevel level
        ) {
            player.setDeltaMovement(
                    Vec3.ZERO
            );

            player.fallDistance =
                    0.0F;

            player.teleportTo(
                    level,
                    position.x,
                    position.y,
                    position.z,
                    Set.<RelativeMovement>of(),
                    yaw,
                    pitch
            );
        }
    }
}
