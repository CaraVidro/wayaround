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
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Void Spectrum domain.
 *
 * The exterior shell stays at the original cast position, but captured players
 * are teleported to a real temporary pocket-space far from normal gameplay.
 * The pocket has an invisible Barrier floor, real Minecraft collision and real
 * block placement. The Void user is completely free inside it; victims are
 * position-locked while still remaining real attackable players.
 *
 * Anything a participant places in the pocket is temporary and is erased when
 * the domain closes. The client renders the pocket as actual 3D black space
 * with world-space stars instead of a fullscreen background.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class VoidDomainManager {

    private VoidDomainManager() {
    }

    private static final double CAPTURE_RADIUS =
            16.0;

    private static final int DURATION_TICKS =
            12 * 20;

    private static final int COOLDOWN_TICKS =
            30 * 20;

    private static final double VISUAL_RANGE =
            192.0;

    private static final int POCKET_HALF_SIZE =
            48;

    private static final int POCKET_EDGE_GUARD =
            4;

    private static final int POCKET_SPACING =
            160;

    private static final int POCKET_BASE_X =
            -8_000_000;

    private static final int POCKET_BASE_Z =
            8_000_000;

    private static final Map<UUID, ActiveVoidDomain> ACTIVE =
            new HashMap<>();

    private static final Map<UUID, UUID> PARTICIPANT_TO_OWNER =
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

        Vec3 exteriorCenter =
                owner.position();

        int pocketFloorY =
                Math.max(
                        level.getMinBuildHeight() + 16,
                        level.getMaxBuildHeight() - 48
                );

        Vec3 pocketCenter =
                pocketCenter(
                        ownerId,
                        pocketFloorY + 1
                );

        ParticipantState ownerState =
                ParticipantState.capture(
                        owner,
                        pocketCenter
                );

        Map<UUID, ParticipantState> trapped =
                new LinkedHashMap<>();

        AABB captureBox =
                new AABB(
                        exteriorCenter.x - CAPTURE_RADIUS,
                        exteriorCenter.y - CAPTURE_RADIUS,
                        exteriorCenter.z - CAPTURE_RADIUS,
                        exteriorCenter.x + CAPTURE_RADIUS,
                        exteriorCenter.y + CAPTURE_RADIUS,
                        exteriorCenter.z + CAPTURE_RADIUS
                );

        int targetIndex =
                0;

        for (ServerPlayer target :
                level.getEntitiesOfClass(
                        ServerPlayer.class,
                        captureBox,
                        player ->
                                player.isAlive()
                                        && player != owner
                )) {

            if (target.position()
                    .distanceToSqr(
                            exteriorCenter
                    )
                    > CAPTURE_RADIUS
                    * CAPTURE_RADIUS) {
                continue;
            }

            double angle =
                    targetIndex
                            * (
                            Math.PI
                                    * 2.0
                                    / Math.max(
                                    1,
                                    level.players()
                                            .size()
                            )
                    );

            double ring =
                    5.0
                            + (
                            targetIndex % 3
                    )
                            * 1.8;

            Vec3 lock =
                    pocketCenter.add(
                            Math.cos(angle)
                                    * ring,
                            0.0,
                            Math.sin(angle)
                                    * ring
                    );

            trapped.put(
                    target.getUUID(),
                    ParticipantState.capture(
                            target,
                            lock
                    )
            );

            targetIndex++;
        }

        ActiveVoidDomain domain =
                new ActiveVoidDomain(
                        ownerId,
                        level.dimension(),
                        exteriorCenter,
                        pocketCenter,
                        pocketFloorY,
                        ownerState,
                        trapped,
                        tick
                                + DURATION_TICKS
                );

        ACTIVE.put(
                ownerId,
                domain
        );

        PARTICIPANT_TO_OWNER.put(
                ownerId,
                ownerId
        );

        for (UUID targetId :
                trapped.keySet()) {

            PARTICIPANT_TO_OWNER.put(
                    targetId,
                    ownerId
            );
        }

        COOLDOWN.put(
                ownerId,
                domain.endsAt
                        + COOLDOWN_TICKS
        );

        buildPocketFloor(
                level,
                domain
        );

        /*
         * Outside observers keep the original compact bubble.
         */
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                exteriorCenter.x,
                exteriorCenter.y,
                exteriorCenter.z,
                VISUAL_RANGE,
                VoidDomainVisualPayload.open(
                        ownerId,
                        exteriorCenter,
                        (float) CAPTURE_RADIUS,
                        DURATION_TICKS
                )
        );

        /*
         * Participants physically enter the pocket. The ENTER payload carries
         * the POCKET center, so the client builds its 3D star-space there.
         */
        teleportToPocket(
                owner,
                level,
                pocketCenter
        );

        PacketDistributor.sendToPlayer(
                owner,
                VoidDomainVisualPayload.enter(
                        ownerId,
                        pocketCenter,
                        POCKET_HALF_SIZE,
                        DURATION_TICKS,
                        false
                )
        );

        for (Map.Entry<UUID, ParticipantState> entry :
                trapped.entrySet()) {

            ServerPlayer target =
                    owner.server
                            .getPlayerList()
                            .getPlayer(
                                    entry.getKey()
                            );

            if (target == null) {
                continue;
            }

            teleportToPocket(
                    target,
                    level,
                    entry.getValue()
                            .pocketPosition
            );

            PacketDistributor.sendToPlayer(
                    target,
                    VoidDomainVisualPayload.enter(
                            ownerId,
                            pocketCenter,
                            POCKET_HALF_SIZE,
                            DURATION_TICKS,
                            true
                    )
            );
        }

        level.playSound(
                null,
                BlockPos.containing(
                        exteriorCenter
                ),
                SoundEvents.END_PORTAL_SPAWN,
                SoundSource.PLAYERS,
                2.0F,
                0.44F
        );

        level.sendParticles(
                ParticleTypes.FLASH,
                exteriorCenter.x,
                exteriorCenter.y + 1.0,
                exteriorCenter.z,
                7,
                3.8,
                2.0,
                3.8,
                0.0
        );

        level.sendParticles(
                ParticleTypes.END_ROD,
                exteriorCenter.x,
                exteriorCenter.y + 1.0,
                exteriorCenter.z,
                90,
                CAPTURE_RADIUS * 0.42,
                3.2,
                CAPTURE_RADIUS * 0.42,
                0.08
        );

        owner.displayClientMessage(
                Component.literal(
                        "EXPANSÃO DE DOMÍNIO — VOID"
                ),
                false
        );

        WayAround.LOGGER.info(
                "[VoidDomain] owner={} trapped={} pocket={} duration={}t",
                owner.getGameProfile()
                        .getName(),
                trapped.size(),
                pocketCenter,
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

    /**
     * Player-placed blocks inside the pocket are explicitly ephemeral.
     * EntityMultiPlaceEvent is a subclass of EntityPlaceEvent, so one handler
     * also catches multi-place operations such as beds.
     */
    @SubscribeEvent
    public static void onBlockPlace(
            BlockEvent.EntityPlaceEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        UUID ownerId =
                PARTICIPANT_TO_OWNER.get(
                        player.getUUID()
                );

        if (ownerId == null) {
            return;
        }

        ActiveVoidDomain domain =
                ACTIVE.get(
                        ownerId
                );

        if (domain == null
                || !domain.isInsidePocket(
                        event.getPos()
                )) {
            return;
        }

        /*
         * Victims are supposed to be information-locked, not building while
         * frozen. The Void user, however, can construct normally.
         */
        if (!player.getUUID()
                .equals(
                        domain.owner
                )) {

            event.setCanceled(
                    true
            );

            return;
        }

        domain.temporaryBlocks.putIfAbsent(
                event.getPos()
                        .asLong(),
                Boolean.TRUE
        );
    }

    @SubscribeEvent
    public static void onBlockBreak(
            BlockEvent.BreakEvent event
    ) {
        ServerPlayer player =
                event.getPlayer();

        UUID ownerId =
                PARTICIPANT_TO_OWNER.get(
                        player.getUUID()
                );

        if (ownerId == null
                || player.getUUID()
                        .equals(
                                ownerId
                        )) {
            return;
        }

        ActiveVoidDomain domain =
                ACTIVE.get(
                        ownerId
                );

        if (domain != null
                && domain.isInsidePocket(
                        event.getPos()
                )) {

            event.setCanceled(
                    true
            );
        }
    }

    public static boolean isTrapped(
            ServerPlayer player
    ) {
        UUID ownerId =
                PARTICIPANT_TO_OWNER.get(
                        player.getUUID()
                );

        return ownerId != null
                && !ownerId.equals(
                        player.getUUID()
                );
    }

    public static void clearAll() {
        ACTIVE.clear();
        PARTICIPANT_TO_OWNER.clear();
        COOLDOWN.clear();
    }

    private static void closeDomain(
            MinecraftServer server,
            ActiveVoidDomain domain
    ) {
        ServerLevel level =
                server.getLevel(
                        domain.dimension
                );

        ServerPlayer owner =
                server.getPlayerList()
                        .getPlayer(
                                domain.owner
                        );

        /*
         * Remove temporary construction before returning players. This means
         * even if the caster builds a little tower, none of it survives the
         * domain lifecycle.
         */
        if (level != null) {
            clearTemporaryConstruction(
                    level,
                    domain
            );

            removePocketFloor(
                    level,
                    domain
            );
        }

        if (owner != null
                && level != null) {

            domain.ownerReturn.restore(
                    owner,
                    level
            );

            PacketDistributor.sendToPlayer(
                    owner,
                    VoidDomainVisualPayload.close(
                            domain.owner
                    )
            );
        }

        for (Map.Entry<UUID, ParticipantState> entry :
                domain.trapped.entrySet()) {

            ServerPlayer target =
                    server.getPlayerList()
                            .getPlayer(
                                    entry.getKey()
                            );

            if (target == null
                    || level == null) {
                continue;
            }

            entry.getValue()
                    .restore(
                            target,
                            level
                    );

            PacketDistributor.sendToPlayer(
                    target,
                    VoidDomainVisualPayload.close(
                            domain.owner
                    )
            );
        }

        PARTICIPANT_TO_OWNER.remove(
                domain.owner
        );

        for (UUID targetId :
                domain.trapped.keySet()) {

            PARTICIPANT_TO_OWNER.remove(
                    targetId
            );
        }

        if (level != null) {
            PacketDistributor.sendToPlayersNear(
                    level,
                    null,
                    domain.exteriorCenter.x,
                    domain.exteriorCenter.y,
                    domain.exteriorCenter.z,
                    VISUAL_RANGE,
                    VoidDomainVisualPayload.close(
                            domain.owner
                    )
            );

            level.playSound(
                    null,
                    domain.exteriorCenter.x,
                    domain.exteriorCenter.y,
                    domain.exteriorCenter.z,
                    SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS,
                    1.3F,
                    0.58F
            );
        }
    }

    private static void buildPocketFloor(
            ServerLevel level,
            ActiveVoidDomain domain
    ) {
        BlockPos.MutableBlockPos cursor =
                new BlockPos.MutableBlockPos();

        int centerX =
                (int) Math.floor(
                        domain.pocketCenter.x
                );

        int centerZ =
                (int) Math.floor(
                        domain.pocketCenter.z
                );

        for (int x =
                     centerX
                             - POCKET_HALF_SIZE;
             x <= centerX
                     + POCKET_HALF_SIZE;
             x++) {

            for (int z =
                         centerZ
                                 - POCKET_HALF_SIZE;
                 z <= centerZ
                         + POCKET_HALF_SIZE;
                 z++) {

                cursor.set(
                        x,
                        domain.pocketFloorY,
                        z
                );

                level.setBlock(
                        cursor,
                        Blocks.BARRIER
                                .defaultBlockState(),
                        2
                );
            }
        }
    }

    private static void removePocketFloor(
            ServerLevel level,
            ActiveVoidDomain domain
    ) {
        BlockPos.MutableBlockPos cursor =
                new BlockPos.MutableBlockPos();

        int centerX =
                (int) Math.floor(
                        domain.pocketCenter.x
                );

        int centerZ =
                (int) Math.floor(
                        domain.pocketCenter.z
                );

        for (int x =
                     centerX
                             - POCKET_HALF_SIZE;
             x <= centerX
                     + POCKET_HALF_SIZE;
             x++) {

            for (int z =
                         centerZ
                                 - POCKET_HALF_SIZE;
                 z <= centerZ
                         + POCKET_HALF_SIZE;
                 z++) {

                cursor.set(
                        x,
                        domain.pocketFloorY,
                        z
                );

                if (level.getBlockState(
                        cursor
                ).is(
                        Blocks.BARRIER
                )) {

                    level.setBlock(
                            cursor,
                            Blocks.AIR
                                    .defaultBlockState(),
                            2
                    );
                }
            }
        }
    }

    private static void clearTemporaryConstruction(
            ServerLevel level,
            ActiveVoidDomain domain
    ) {
        for (Long packed :
                domain.temporaryBlocks
                        .keySet()) {

            BlockPos pos =
                    BlockPos.of(
                            packed
                    );

            level.setBlock(
                    pos,
                    Blocks.AIR
                            .defaultBlockState(),
                    3
            );
        }

        domain.temporaryBlocks.clear();
    }

    private static void teleportToPocket(
            ServerPlayer player,
            ServerLevel level,
            Vec3 position
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
                player.getYRot(),
                player.getXRot()
        );
    }

    private static Vec3 pocketCenter(
            UUID owner,
            int y
    ) {
        long mixed =
                owner.getMostSignificantBits()
                        ^ Long.rotateLeft(
                        owner.getLeastSignificantBits(),
                        21
                );

        int gridX =
                (int) (
                        mixed
                                & 0x3FFL
                );

        int gridZ =
                (int) (
                        mixed >>> 10
                                & 0x3FFL
                );

        return new Vec3(
                POCKET_BASE_X
                        + gridX
                                * POCKET_SPACING,
                y + 0.05,
                POCKET_BASE_Z
                        + gridZ
                                * POCKET_SPACING
        );
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
        private final Vec3 exteriorCenter;
        private final Vec3 pocketCenter;
        private final int pocketFloorY;
        private final ParticipantState ownerReturn;
        private final Map<UUID, ParticipantState> trapped;
        private final Map<Long, Boolean> temporaryBlocks =
                new HashMap<>();
        private final long endsAt;

        private ActiveVoidDomain(
                UUID owner,
                ResourceKey<Level> dimension,
                Vec3 exteriorCenter,
                Vec3 pocketCenter,
                int pocketFloorY,
                ParticipantState ownerReturn,
                Map<UUID, ParticipantState> trapped,
                long endsAt
        ) {
            this.owner =
                    owner;

            this.dimension =
                    dimension;

            this.exteriorCenter =
                    exteriorCenter;

            this.pocketCenter =
                    pocketCenter;

            this.pocketFloorY =
                    pocketFloorY;

            this.ownerReturn =
                    ownerReturn;

            this.trapped =
                    trapped;

            this.endsAt =
                    endsAt;
        }

        private boolean isInsidePocket(
                BlockPos pos
        ) {
            int centerX =
                    (int) Math.floor(
                            pocketCenter.x
                    );

            int centerZ =
                    (int) Math.floor(
                            pocketCenter.z
                    );

            return Math.abs(
                    pos.getX()
                            - centerX
            ) <= POCKET_HALF_SIZE + 4
                    && Math.abs(
                    pos.getZ()
                            - centerZ
            ) <= POCKET_HALF_SIZE + 4
                    && pos.getY()
                            >= pocketFloorY
                    && pos.getY()
                            < pocketFloorY
                                    + 48;
        }

        private void tick(
                MinecraftServer server,
                ServerLevel level
        ) {
            ServerPlayer ownerPlayer =
                    server.getPlayerList()
                            .getPlayer(
                                    owner
                            );

            if (ownerPlayer != null) {
                double dx =
                        ownerPlayer.getX()
                                - pocketCenter.x;

                double dz =
                        ownerPlayer.getZ()
                                - pocketCenter.z;

                double limit =
                        POCKET_HALF_SIZE
                                - POCKET_EDGE_GUARD;

                if (Math.abs(dx) > limit
                        || Math.abs(dz) > limit
                        || ownerPlayer.getY()
                                < pocketFloorY - 4) {

                    teleportToPocket(
                            ownerPlayer,
                            level,
                            pocketCenter
                    );
                }
            }

            for (Map.Entry<UUID, ParticipantState> entry :
                    trapped.entrySet()) {

                ServerPlayer target =
                        server.getPlayerList()
                                .getPlayer(
                                        entry.getKey()
                                );

                if (target == null
                        || !target.isAlive()) {
                    continue;
                }

                ParticipantState state =
                        entry.getValue();

                target.setDeltaMovement(
                        Vec3.ZERO
                );

                target.fallDistance =
                        0.0F;

                if (target.serverLevel() != level
                        || target.position()
                                .distanceToSqr(
                                        state.pocketPosition
                                )
                                > 0.0004) {

                    teleportToPocket(
                            target,
                            level,
                            state.pocketPosition
                    );
                }
            }
        }
    }

    private static final class ParticipantState {

        private final Vec3 returnPosition;
        private final float returnYaw;
        private final float returnPitch;
        private final Vec3 pocketPosition;

        private ParticipantState(
                Vec3 returnPosition,
                float returnYaw,
                float returnPitch,
                Vec3 pocketPosition
        ) {
            this.returnPosition =
                    returnPosition;

            this.returnYaw =
                    returnYaw;

            this.returnPitch =
                    returnPitch;

            this.pocketPosition =
                    pocketPosition;
        }

        private static ParticipantState capture(
                ServerPlayer player,
                Vec3 pocketPosition
        ) {
            return new ParticipantState(
                    player.position(),
                    player.getYRot(),
                    player.getXRot(),
                    pocketPosition
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
                    returnPosition.x,
                    returnPosition.y,
                    returnPosition.z,
                    Set.<RelativeMovement>of(),
                    returnYaw,
                    returnPitch
            );
        }
    }
}
