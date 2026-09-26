package net.caravidro.wayaround.blue;

import net.caravidro.wayaround.spectrum.SpectrumType;

import net.caravidro.wayaround.spectrum.SpectrumAccess;

import net.caravidro.wayaround.network.PlayerCinematicPayload;

import net.caravidro.wayaround.cinematic.PlayerControlLockManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.BetaTechniqueVisualPayload;
import net.caravidro.wayaround.sounds.WayAroundSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Experimental Otherworld Powers beta:
 *
 * RED is a fast destructive cube fired along the player's crosshair.
 * A RED colliding with that player's stationary BLUE starts a synchronized
 * PURPLE cinematic sequence.
 */
@net.neoforged.fml.common.EventBusSubscriber(
        modid = WayAround.MODID
)
public final class ImaginaryBetaManager {

    private ImaginaryBetaManager() {
    }

    private static final int RED_LIFE_TICKS =
            165;

    private static final int RED_HELD_TICKS =
            30 * 20;

    private static final double RED_SPEED =
            4.65;

    private static final double RED_RADIUS =
            0.68;

    private static final int RED_SUBSTEPS =
            14;

    private static final double RED_IMPACT_RADIUS =
            10.5;

    private static final int PURPLE_BLAST_TICK =
            92;

    private static final int PURPLE_END_TICK =
            188;

    private static final float PURPLE_NUKE_POWER =
            72.0F;

    private static final double PURPLE_STAR_RADIUS =
            150.0;

    private static final double PURPLE_SHOCKWAVE_RADIUS =
            176.0;

    private static final int PURPLE_CRATER_RADIUS_XZ =
            34;

    private static final int PURPLE_CRATER_RADIUS_Y =
            22;

    private static final int DUAL_WAIT_TICKS =
            100;

    private static final int PURPLE_PROJECTILE_LIFE =
            110;

    private static final double PURPLE_PROJECTILE_SPEED =
            1.35;

    private static final int PURPLE_PROJECTILE_SUBSTEPS =
            7;

    private static final double PURPLE_PROJECTILE_RADIUS =
            3.4;

    private static final double VISUAL_RANGE =
            384.0;

    private static final Map<UUID, RedProjectile>
            REDS =
            new HashMap<>();

    private static final Map<UUID, PurpleFusion>
            FUSIONS =
            new HashMap<>();

    private static final Map<UUID, PendingDual>
            DUALS =
            new HashMap<>();

    private static final Map<UUID, PurpleProjectile>
            PURPLE_PROJECTILES =
            new HashMap<>();

    private static final Map<UUID, PendingPurpleCast>
            PENDING_PURPLE_CASTS =
            new HashMap<>();

    public static boolean prepareDual(
            ServerPlayer player
    ) {
        if (!SpectrumAccess.has(
                player,
                SpectrumType.VOID
        )
                || PlayerControlLockManager.actionsLocked(
                player
        )) {
            return false;
        }

        UUID owner =
                player.getUUID();

        /*
         * If Red was physically held, preparing the pair must not leave the
         * bearer anchored forever.
         */
        PlayerControlLockManager.clearMovement(
                player
        );

        sendCinematic(
                player,
                PlayerCinematicPayload.CLEAR,
                0
        );

        if (FUSIONS.containsKey(owner)
                || PURPLE_PROJECTILES.containsKey(owner)) {

            return false;
        }

        REDS.remove(owner);

        DUALS.put(
                owner,
                new PendingDual(
                        owner,
                        player.serverLevel().dimension(),
                        DUAL_WAIT_TICKS
                )
        );

        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS,
                0.72F,
                0.92F
        );

        return true;
    }

    public static boolean launchPurpleVoid(
            ServerPlayer player
    ) {
        if (!SpectrumAccess.has(
                player,
                SpectrumType.VOID
        )
                || PlayerControlLockManager.actionsLocked(
                player
        )) {
            return false;
        }

        PurpleFusion held = FUSIONS.get(player.getUUID());
        if (held != null && held.age >= 72 && !held.blastTriggered) {
            held.blastTriggered = true;
            held.age = PURPLE_BLAST_TICK;
            sendCinematic(player, PlayerCinematicPayload.PURPLE_RELEASE, 28);
            player.serverLevel().playSound(null, player.blockPosition(),
                    SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.0F, 0.52F);
            detonatePurple(player.serverLevel(), held);
            return true;
        }

        PendingDual dual =
                DUALS.remove(
                        player.getUUID()
                );

        if (dual == null
                || PENDING_PURPLE_CASTS.containsKey(
                player.getUUID()
        )) {
            return false;
        }

        Vec3 direction =
                player.getLookAngle()
                        .normalize();

        PENDING_PURPLE_CASTS.put(
                player.getUUID(),
                new PendingPurpleCast(
                        player.getUUID(),
                        player.serverLevel()
                                .dimension(),
                        direction,
                        24
                )
        );

        sendCinematic(
                player,
                PlayerCinematicPayload.PURPLE_FUSION,
                24
        );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.AMETHYST_BLOCK_RESONATE,
                        SoundSource.PLAYERS,
                        1.45F,
                        0.54F
                );

        return true;
    }

    public static boolean prepareRed(
            ServerPlayer player
    ) {
        if (!SpectrumAccess.has(
                player,
                SpectrumType.VOID
        )
                || PlayerControlLockManager.actionsLocked(
                player
        )) {
            return false;
        }

        UUID owner =
                player.getUUID();

        if (FUSIONS.containsKey(
                owner
        )
                || PURPLE_PROJECTILES.containsKey(
                owner
        )) {
            return false;
        }

        BlueManager.FusionSeed activeBlue =
                BlueManager.consumeActiveBlueForFusion(
                        player
                );

        if (activeBlue != null) {
            beginFusion(
                    player.serverLevel(),
                    player,
                    activeBlue
            );

            player.serverLevel().sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    activeBlue.center().x,
                    activeBlue.center().y,
                    activeBlue.center().z,
                    46,
                    0.8,
                    0.8,
                    0.8,
                    0.18
            );

            player.serverLevel().playSound(
                    null,
                    BlockPos.containing(activeBlue.center()),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH,
                    SoundSource.PLAYERS,
                    1.15F,
                    0.48F
            );

            return true;
        }

        RedProjectile existing =
                REDS.get(
                        owner
                );

        if (existing != null
                && existing.held) {
            return true;
        }

        Vec3 direction =
                player.getLookAngle()
                        .normalize();

        Vec3 position =
                player.getEyePosition()
                        .add(
                                direction.scale(
                                        2.55
                                )
                        );

        RedProjectile red =
                new RedProjectile(
                        owner,
                        player.serverLevel()
                                .dimension(),
                        position,
                        Vec3.ZERO,
                        RED_HELD_TICKS
                );

        red.held =
                true;

        REDS.put(
                owner,
                red
        );

        PlayerControlLockManager.lockMovement(
                player,
                0
        );

        sendCinematic(
                player,
                PlayerCinematicPayload.RED_HOLD,
                0
        );

        player.swing(
                net.minecraft.world.InteractionHand.MAIN_HAND,
                true
        );

        player.serverLevel()
                .sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        position.x,
                        position.y,
                        position.z,
                        18,
                        0.22,
                        0.22,
                        0.22,
                        0.08
                );

        player.serverLevel()
                .sendParticles(
                        ParticleTypes.FLAME,
                        position.x,
                        position.y,
                        position.z,
                        8,
                        0.12,
                        0.12,
                        0.12,
                        0.015
                );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.AMETHYST_BLOCK_RESONATE,
                        SoundSource.PLAYERS,
                        0.72F,
                        0.58F
                );

        sendVisual(
                player.serverLevel(),
                owner,
                BetaTechniqueVisualPayload.RED_HELD,
                position,
                1.0F,
                0.0F
        );

        return true;
    }

    public static boolean chargeRedMaximum(
            ServerPlayer player
    ) {
        if (!SpectrumAccess.has(
                player,
                SpectrumType.VOID
        )) {
            return false;
        }

        RedProjectile red =
                REDS.get(
                        player.getUUID()
                );

        if (red == null
                || !red.held) {
            return false;
        }

        red.maximum =
                true;

        ServerLevel level =
                player.serverLevel();

        level.playSound(
                null,
                BlockPos.containing(
                        red.position
                ),
                SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS,
                1.25F,
                0.44F
        );

        level.playSound(
                null,
                BlockPos.containing(
                        red.position
                ),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                1.1F,
                1.62F
        );

        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                red.position.x,
                red.position.y,
                red.position.z,
                42,
                0.34,
                0.34,
                0.34,
                0.16
        );

        sendVisual(
                level,
                red.owner,
                BetaTechniqueVisualPayload.RED_HELD,
                red.position,
                2.0F,
                0.0F
        );

        return true;
    }

    public static boolean launchRed(
            ServerPlayer player
    ) {
        if (!SpectrumAccess.has(
                player,
                SpectrumType.VOID
        )
                || PlayerControlLockManager.actionsLocked(
                player
        )) {
            return false;
        }

        RedProjectile red =
                REDS.get(
                        player.getUUID()
                );

        if (red == null
                || !red.held) {
            return false;
        }

        Vec3 direction =
                player.getLookAngle()
                        .normalize();

        red.held =
                false;

        PlayerControlLockManager.clearMovement(
                player
        );

        sendCinematic(
                player,
                PlayerCinematicPayload.RED_RELEASE,
                24
        );

        red.life =
                RED_LIFE_TICKS;

        red.velocity =
                direction.scale(
                        RED_SPEED
                                * (
                                red.maximum
                                        ? 1.12
                                        : 1.0
                        )
                );

        red.position =
                player.getEyePosition()
                        .add(
                                direction.scale(
                                        2.25
                                )
                        );

        player.swing(
                net.minecraft.world.InteractionHand.MAIN_HAND,
                true
        );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.FIREWORK_ROCKET_LAUNCH,
                        SoundSource.PLAYERS,
                        red.maximum
                                ? 1.55F
                                : 1.05F,
                        red.maximum
                                ? 0.46F
                                : 0.62F
                );

        sendVisual(
                player.serverLevel(),
                red.owner,
                BetaTechniqueVisualPayload.RED,
                red.position,
                red.maximum
                        ? 2.0F
                        : 1.0F,
                0.0F
        );

        return true;
    }

    /**
     * Compatibility entry point used by older callers: prepare and launch.
     */
    public static boolean fireRed(
            ServerPlayer player
    ) {
        return prepareRed(
                player
        )
                && launchRed(
                player
        );
    }

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        tickDuals(server);
        tickPendingPurpleCasts(server);
        tickReds(server);
        tickPurpleProjectiles(server);
        tickFusions(server);
    }

    private static void tickDuals(
            MinecraftServer server
    ) {
        Iterator<Map.Entry<UUID, PendingDual>> iterator =
                DUALS.entrySet().iterator();

        while (iterator.hasNext()) {
            PendingDual dual =
                    iterator.next().getValue();

            ServerPlayer owner =
                    server.getPlayerList()
                            .getPlayer(dual.owner);

            ServerLevel level =
                    server.getLevel(dual.dimension);

            if (owner == null
                    || level == null
                    || !owner.isAlive()
                    || owner.serverLevel() != level) {

                if (owner != null) {
                    PlayerControlLockManager.clearMovement(
                            owner
                    );

                    sendCinematic(
                            owner,
                            PlayerCinematicPayload.CLEAR,
                            0
                    );
                }

                iterator.remove();
                continue;
            }

            dual.life--;

            if (dual.life <= 0) {
                iterator.remove();
                continue;
            }

            Vec3 look =
                    owner.getLookAngle().normalize();

            Vec3 right =
                    look.cross(
                            new Vec3(0.0, 1.0, 0.0)
                    );

            if (right.lengthSqr() < 0.0001) {
                right =
                        new Vec3(1.0, 0.0, 0.0);
            } else {
                right =
                        right.normalize();
            }

            Vec3 center =
                    owner.getEyePosition()
                            .add(
                                    look.scale(4.6)
                            );

            sendVisual(
                    level,
                    pairVisualId(
                            dual.owner,
                            0x42A11E5BL
                    ),
                    BetaTechniqueVisualPayload.PAIR_BLUE,
                    center.add(
                            right.scale(-1.18)
                    ),
                    1.0F,
                    0.0F
            );

            sendVisual(
                    level,
                    pairVisualId(
                            dual.owner,
                            0x7ED00D5EL
                    ),
                    BetaTechniqueVisualPayload.RED,
                    center.add(
                            right.scale(1.18)
                    ),
                    1.0F,
                    0.0F
            );
        }
    }

    private static void tickPendingPurpleCasts(
            MinecraftServer server
    ) {
        Iterator<Map.Entry<UUID, PendingPurpleCast>> iterator =
                PENDING_PURPLE_CASTS.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            PendingPurpleCast cast =
                    iterator.next()
                            .getValue();

            ServerPlayer owner =
                    server.getPlayerList()
                            .getPlayer(
                                    cast.owner
                            );

            ServerLevel level =
                    server.getLevel(
                            cast.dimension
                    );

            if (owner == null
                    || level == null
                    || !owner.isAlive()
                    || owner.serverLevel() != level
                    || !SpectrumAccess.has(
                    owner,
                    SpectrumType.VOID
            )) {

                if (owner != null) {
                    sendCinematic(
                            owner,
                            PlayerCinematicPayload.CLEAR,
                            0
                    );
                }

                iterator.remove();
                continue;
            }

            cast.delay--;

            if (cast.delay > 0) {
                continue;
            }

            Vec3 position =
                    owner.getEyePosition()
                            .add(
                                    cast.direction.scale(
                                            4.6
                                    )
                            );

            PURPLE_PROJECTILES.put(
                    cast.owner,
                    new PurpleProjectile(
                            cast.owner,
                            cast.dimension,
                            position,
                            cast.direction.scale(
                                    PURPLE_PROJECTILE_SPEED
                            ),
                            PURPLE_PROJECTILE_LIFE
                    )
            );

            sendCinematic(
                    owner,
                    PlayerCinematicPayload.PURPLE_RELEASE,
                    48
            );

            level.playSound(
                    null,
                    owner.blockPosition(),
                    SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.PLAYERS,
                    2.0F,
                    0.62F
            );

            sendVisual(
                    level,
                    cast.owner,
                    BetaTechniqueVisualPayload.PURPLE_PROJECTILE,
                    position,
                    1.35F,
                    0.0F
            );

            iterator.remove();
        }
    }

    private static void tickPurpleProjectiles(
            MinecraftServer server
    ) {
        Iterator<Map.Entry<UUID, PurpleProjectile>> iterator =
                PURPLE_PROJECTILES.entrySet().iterator();

        while (iterator.hasNext()) {
            PurpleProjectile purple =
                    iterator.next().getValue();

            ServerPlayer owner =
                    server.getPlayerList()
                            .getPlayer(purple.owner);

            ServerLevel level =
                    server.getLevel(purple.dimension);

            if (owner == null
                    || level == null
                    || owner.serverLevel() != level) {

                iterator.remove();
                continue;
            }

            purple.life--;

            if (purple.life <= 0) {
                iterator.remove();
                continue;
            }

            Vec3 step =
                    purple.velocity.scale(
                            1.0
                                    / PURPLE_PROJECTILE_SUBSTEPS
                    );

            for (int sub = 0;
                 sub < PURPLE_PROJECTILE_SUBSTEPS;
                 sub++) {

                purple.position =
                        purple.position.add(step);

                destroyPurplePath(
                        level,
                        purple.position
                );

                burnPurplePath(
                        level,
                        purple.position
                );

                erasePurpleEntities(
                        level,
                        owner,
                        purple.position
                );
            }

            level.sendParticles(
                    ParticleTypes.END_ROD,
                    purple.position.x,
                    purple.position.y,
                    purple.position.z,
                    14,
                    0.55,
                    0.55,
                    0.55,
                    0.08
            );

            level.sendParticles(
                    ParticleTypes.FLAME,
                    purple.position.x,
                    purple.position.y,
                    purple.position.z,
                    18,
                    0.72,
                    0.42,
                    0.72,
                    0.035
            );

            sendVisual(
                    level,
                    purple.owner,
                    BetaTechniqueVisualPayload.PURPLE_PROJECTILE,
                    purple.position,
                    1.35F,
                    1.0F
                            - purple.life
                                    / (float) PURPLE_PROJECTILE_LIFE
            );
        }
    }

    private static void destroyPurplePath(
            ServerLevel level,
            Vec3 center
    ) {
        int radius =
                4;

        BlockPos origin =
                BlockPos.containing(center);

        double radiusSquared =
                PURPLE_PROJECTILE_RADIUS
                        * PURPLE_PROJECTILE_RADIUS;

        for (BlockPos sample :
                BlockPos.betweenClosed(
                        origin.offset(-radius, -radius, -radius),
                        origin.offset(radius, radius, radius)
                )) {

            if (!level.hasChunkAt(sample)) {
                continue;
            }

            if (Vec3.atCenterOf(sample)
                    .distanceToSqr(center)
                    > radiusSquared) {

                continue;
            }

            BlockState state =
                    level.getBlockState(sample);

            if (state.isAir()
                    || state.getDestroySpeed(
                            level,
                            sample
                    ) < 0.0F) {

                continue;
            }

            level.removeBlock(
                    sample,
                    false
            );
        }
    }

    private static void burnPurplePath(
            ServerLevel level,
            Vec3 center
    ) {
        BlockPos origin =
                BlockPos.containing(center);

        for (int attempt = 0;
             attempt < 8;
             attempt++) {

            BlockPos pos =
                    origin.offset(
                            level.random.nextInt(7) - 3,
                            level.random.nextInt(5) - 2,
                            level.random.nextInt(7) - 3
                    );

            if (!level.getBlockState(pos).isAir()) {
                continue;
            }

            BlockState fire =
                    Blocks.FIRE.defaultBlockState();

            if (fire.canSurvive(level, pos)) {
                level.setBlockAndUpdate(
                        pos,
                        fire
                );
            }
        }
    }

    private static void erasePurpleEntities(
            ServerLevel level,
            ServerPlayer owner,
            Vec3 center
    ) {
        double reach =
                PURPLE_PROJECTILE_RADIUS
                        + 1.4;

        AABB area =
                new AABB(
                        center.x - reach,
                        center.y - reach,
                        center.z - reach,
                        center.x + reach,
                        center.y + reach,
                        center.z + reach
                );

        for (LivingEntity living :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        entity ->
                                entity.isAlive()
                                        && entity != owner
                )) {

            living.igniteForSeconds(10.0F);

            if (living instanceof ServerPlayer spectrum
                    && net.caravidro.wayaround.spectrum.SpectrumCombat.isBearer(spectrum)) {
                spectrum.hurt(level.damageSources().generic(),
                        Math.max(12.0F, spectrum.getMaxHealth() * 0.85F));
            } else {
                living.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            }
        }
    }

    private static UUID pairVisualId(
            UUID owner,
            long salt
    ) {
        return new UUID(
                owner.getMostSignificantBits() ^ salt,
                owner.getLeastSignificantBits()
                        ^ Long.rotateLeft(
                        salt,
                        21
                )
        );
    }

    private static void tickReds(
            MinecraftServer server
    ) {
        Iterator<Map.Entry<UUID, RedProjectile>>
                iterator =
                REDS.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            RedProjectile red =
                    iterator.next()
                            .getValue();

            ServerPlayer owner =
                    server.getPlayerList()
                            .getPlayer(
                                    red.owner
                            );

            ServerLevel level =
                    server.getLevel(
                            red.dimension
                    );

            if (owner == null
                    || level == null
                    || !owner.isAlive()
                    || owner.serverLevel() != level) {

                iterator.remove();
                continue;
            }

            red.life--;

            if (red.held) {
                if (red.life <= 0) {
                    PlayerControlLockManager.clearMovement(
                            owner
                    );

                    sendCinematic(
                            owner,
                            PlayerCinematicPayload.CLEAR,
                            0
                    );

                    iterator.remove();
                    continue;
                }

                Vec3 look =
                        owner.getLookAngle()
                                .normalize();

                red.position =
                        owner.getEyePosition()
                                .add(
                                        look.scale(
                                                2.55
                                        )
                                );

                if (red.life % 3 == 0) {
                    level.sendParticles(
                            red.maximum
                                    ? ParticleTypes.END_ROD
                                    : ParticleTypes.ELECTRIC_SPARK,
                            red.position.x,
                            red.position.y,
                            red.position.z,
                            red.maximum
                                    ? 5
                                    : 2,
                            0.12,
                            0.12,
                            0.12,
                            red.maximum
                                    ? 0.035
                                    : 0.015
                    );
                }

                sendVisual(
                        level,
                        red.owner,
                        BetaTechniqueVisualPayload.RED_HELD,
                        red.position,
                        red.maximum
                                ? 2.0F
                                : 1.0F,
                        0.0F
                );

                continue;
            }

            if (red.life <= 0) {
                detonateRedImpact(
                        level,
                        owner,
                        red
                );

                iterator.remove();
                continue;
            }

            Vec3 step =
                    red.velocity.scale(
                            1.0
                                    / RED_SUBSTEPS
                    );

            boolean fused =
                    false;

            for (int sub = 0;
                 sub < RED_SUBSTEPS;
                 sub++) {

                red.position =
                        red.position.add(
                                step
                        );

                BlueManager.FusionSeed blue =
                        BlueManager
                                .consumeHeldBlueForFusion(
                                        owner,
                                        red.position,
                                        RED_RADIUS
                                );

                if (blue != null) {
                    beginFusion(
                            level,
                            owner,
                            blue
                    );

                    fused =
                            true;

                    break;
                }

                impactRedEntities(
                        level,
                        owner,
                        red
                );

                destroyRedPath(
                        level,
                        red.position
                );

                igniteRedTrail(
                        level,
                        red.position
                );

                if (sub % 3 == 0) {
                    emitRedWake(
                            level,
                            owner,
                            red.position,
                            red.velocity
                    );
                }
            }

            if (fused) {
                iterator.remove();
                continue;
            }

            level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    red.position.x,
                    red.position.y,
                    red.position.z,
                    red.maximum
                            ? 9
                            : 4,
                    0.10,
                    0.10,
                    0.10,
                    red.maximum
                            ? 0.16
                            : 0.08
            );

            level.sendParticles(
                    ParticleTypes.FLAME,
                    red.position.x,
                    red.position.y,
                    red.position.z,
                    red.maximum
                            ? 6
                            : 2,
                    0.08,
                    0.08,
                    0.08,
                    0.02
            );

            sendVisual(
                    level,
                    red.owner,
                    BetaTechniqueVisualPayload.RED,
                    red.position,
                    red.maximum
                            ? 2.0F
                            : 1.0F,
                    1.0F
                            - red.life
                                    / (float) RED_LIFE_TICKS
            );
        }
    }

    private static void impactRedEntities(
            ServerLevel level,
            ServerPlayer owner,
            RedProjectile red
    ) {
        double reach =
                RED_RADIUS
                        + 0.72;

        AABB area =
                new AABB(
                        red.position.x - reach,
                        red.position.y - reach,
                        red.position.z - reach,
                        red.position.x + reach,
                        red.position.y + reach,
                        red.position.z + reach
                );

        Vec3 travel =
                red.velocity.lengthSqr() > 0.0001
                        ? red.velocity.normalize()
                        : owner.getLookAngle()
                                .normalize();

        for (LivingEntity living :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        entity ->
                                entity.isAlive()
                                        && entity != owner
                                        && !red.hitEntities
                                                .contains(
                                                        entity.getUUID()
                                                )
                )) {

            red.hitEntities.add(
                    living.getUUID()
            );

            Vec3 away =
                    living.position()
                            .subtract(
                                    red.position
                            );

            if (away.lengthSqr() < 0.0001) {
                away =
                        travel;
            } else {
                away =
                        away.normalize();
            }

            living.hurt(
                    level.damageSources()
                            .playerAttack(
                                    owner
                            ),
                    9.0F
            );

            living.igniteForSeconds(
                    6.0F
            );

            Vec3 knockback =
                    away.scale(
                            1.55
                    )
                            .add(
                                    travel.scale(
                                            0.72
                                    )
                            )
                            .add(
                                    0.0,
                                    0.34,
                                    0.0
                            );

            living.setDeltaMovement(
                    living.getDeltaMovement()
                            .add(
                                    knockback
                            )
            );
        }
    }

    private static void igniteRedTrail(
            ServerLevel level,
            Vec3 center
    ) {
        BlockPos origin =
                BlockPos.containing(
                        center
                );

        for (int attempt = 0;
             attempt < 3;
             attempt++) {

            BlockPos pos =
                    origin.offset(
                            level.random.nextInt(
                                    3
                            ) - 1,
                            level.random.nextInt(
                                    3
                            ) - 1,
                            level.random.nextInt(
                                    3
                            ) - 1
                    );

            if (!level.getBlockState(
                    pos
            )
                    .isAir()) {

                continue;
            }

            BlockState fire =
                    Blocks.FIRE
                            .defaultBlockState();

            if (!fire.canSurvive(
                    level,
                    pos
            )) {
                continue;
            }

            level.setBlockAndUpdate(
                    pos,
                    fire
            );
        }
    }

    private static void destroyRedPath(
            ServerLevel level,
            Vec3 center
    ) {
        int radius =
                1;

        BlockPos origin =
                BlockPos.containing(
                        center
                );

        for (BlockPos sample :
                BlockPos.betweenClosed(
                        origin.offset(
                                -radius,
                                -radius,
                                -radius
                        ),
                        origin.offset(
                                radius,
                                radius,
                                radius
                        )
                )) {

            Vec3 blockCenter =
                    Vec3.atCenterOf(
                            sample
                    );

            if (blockCenter.distanceToSqr(
                    center
            )
                    > RED_RADIUS
                            * RED_RADIUS
                            * 2.25) {

                continue;
            }

            BlockState state =
                    level.getBlockState(
                            sample
                    );

            if (state.isAir()
                    || state.getDestroySpeed(
                            level,
                            sample
                    ) < 0.0F) {

                continue;
            }

            level.removeBlock(
                    sample,
                    false
            );

            level.sendParticles(
                    new BlockParticleOption(
                            ParticleTypes.BLOCK,
                            state
                    ),
                    blockCenter.x,
                    blockCenter.y,
                    blockCenter.z,
                    5,
                    0.34,
                    0.34,
                    0.34,
                    0.26
            );
        }
    }

    private static void emitRedWake(
            ServerLevel level,
            ServerPlayer owner,
            Vec3 center,
            Vec3 velocity
    ) {
        Vec3 direction =
                velocity.lengthSqr() > 0.0001
                        ? velocity.normalize()
                        : owner.getLookAngle()
                                .normalize();

        level.sendParticles(
                ParticleTypes.POOF,
                center.x,
                center.y,
                center.z,
                0,
                direction.x,
                direction.y * 0.35 + 0.04,
                direction.z,
                0.72
        );

        int groundY =
                level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        Mth.floor(
                                center.x
                        ),
                        Mth.floor(
                                center.z
                        )
                ) - 1;

        BlockPos ground =
                new BlockPos(
                        Mth.floor(
                                center.x
                        ),
                        groundY,
                        Mth.floor(
                                center.z
                        )
                );

        BlockState groundState =
                level.getBlockState(
                        ground
                );

        if (!groundState.isAir()) {
            level.sendParticles(
                    new BlockParticleOption(
                            ParticleTypes.BLOCK,
                            groundState
                    ),
                    center.x,
                    groundY + 1.04,
                    center.z,
                    3,
                    0.55,
                    0.10,
                    0.55,
                    0.18
            );
        }

        AABB wake =
                new AABB(
                        center.x - 2.2,
                        center.y - 2.2,
                        center.z - 2.2,
                        center.x + 2.2,
                        center.y + 2.2,
                        center.z + 2.2
                );

        for (ItemEntity item :
                level.getEntitiesOfClass(
                        ItemEntity.class,
                        wake
                )) {

            item.setDeltaMovement(
                    item.getDeltaMovement()
                            .add(
                                    direction.scale(
                                            0.42
                                    )
                            )
                            .add(
                                    0.0,
                                    0.10,
                                    0.0
                            )
            );
        }
    }

    private static void detonateRedImpact(
            ServerLevel level,
            ServerPlayer owner,
            RedProjectile red
    ) {
        pulverizeEllipsoid(
                level,
                red.position,
                RED_IMPACT_RADIUS,
                RED_IMPACT_RADIUS * 0.70,
                70
        );

        level.explode(
                owner,
                red.position.x,
                red.position.y,
                red.position.z,
                red.maximum
                        ? 12.5F
                        : 9.0F,
                true,
                Level.ExplosionInteraction.TNT
        );

        double radius =
                RED_IMPACT_RADIUS
                        * 1.55;

        AABB area =
                new AABB(
                        red.position.x - radius,
                        red.position.y - radius,
                        red.position.z - radius,
                        red.position.x + radius,
                        red.position.y + radius,
                        red.position.z + radius
                );

        for (LivingEntity living :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        entity ->
                                entity.isAlive()
                                        && entity != owner
                )) {

            Vec3 away =
                    living.position()
                            .subtract(
                                    red.position
                            );

            double distance =
                    away.length();

            if (distance > radius) {
                continue;
            }

            double factor =
                    1.0
                            - distance
                                    / radius;

            Vec3 direction =
                    distance < 0.01
                            ? new Vec3(
                            0.0,
                            1.0,
                            0.0
                    )
                            : away.scale(
                            1.0 / distance
                    );

            living.hurt(
                    level.damageSources()
                            .playerAttack(
                                    owner
                            ),
                    (float) (
                            10.0
                                    + factor
                                            * (
                                            red.maximum
                                                    ? 28.0
                                                    : 18.0
                                    )
                    )
            );

            living.setDeltaMovement(
                    living.getDeltaMovement()
                            .add(
                                    direction.scale(
                                            1.2
                                                    + factor
                                                            * 3.8
                                    )
                            )
                            .add(
                                    0.0,
                                    0.28
                                            + factor
                                                    * 0.9,
                                    0.0
                            )
            );

            living.hurtMarked =
                    true;
        }

        level.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                red.position.x,
                red.position.y,
                red.position.z,
                1,
                0.0,
                0.0,
                0.0,
                0.0
        );

        for (int i = 0;
             i < 180;
             i++) {

            double theta =
                    level.random.nextDouble()
                            * Math.PI
                            * 2.0;

            double y =
                    level.random.nextDouble()
                            * 1.4
                            - 0.25;

            double speed =
                    0.7
                            + level.random.nextDouble()
                                    * 2.3;

            level.sendParticles(
                    i % 4 == 0
                            ? ParticleTypes.END_ROD
                            : ParticleTypes.ELECTRIC_SPARK,
                    red.position.x,
                    red.position.y,
                    red.position.z,
                    0,
                    Math.cos(theta) * speed,
                    y * speed,
                    Math.sin(theta) * speed,
                    1.0
            );
        }

        level.playSound(
                null,
                BlockPos.containing(
                        red.position
                ),
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS,
                3.2F,
                red.maximum
                        ? 0.42F
                        : 0.56F
        );
    }

    private static void pulverizeEllipsoid(
            ServerLevel level,
            Vec3 center,
            double radiusXZ,
            double radiusY,
            int particleBudget
    ) {
        int minX =
                Mth.floor(
                        center.x - radiusXZ
                );

        int maxX =
                Mth.floor(
                        center.x + radiusXZ
                );

        int minY =
                Math.max(
                        level.getMinBuildHeight(),
                        Mth.floor(
                                center.y - radiusY
                        )
                );

        int maxY =
                Math.min(
                        level.getMaxBuildHeight() - 1,
                        Mth.floor(
                                center.y + radiusY
                        )
                );

        int minZ =
                Mth.floor(
                        center.z - radiusXZ
                );

        int maxZ =
                Mth.floor(
                        center.z + radiusXZ
                );

        int particles =
                0;

        double invXZ =
                1.0
                        / (
                        radiusXZ
                                * radiusXZ
                );

        double invY =
                1.0
                        / (
                        radiusY
                                * radiusY
                );

        for (int x = minX;
             x <= maxX;
             x++) {

            double dx =
                    x + 0.5
                            - center.x;

            for (int y = minY;
                 y <= maxY;
                 y++) {

                double dy =
                        y + 0.5
                                - center.y;

                for (int z = minZ;
                     z <= maxZ;
                     z++) {

                    double dz =
                            z + 0.5
                                    - center.z;

                    double normalized =
                            (
                                    dx * dx
                                            + dz * dz
                            )
                                    * invXZ
                                    + dy * dy
                                            * invY;

                    if (normalized > 1.0) {
                        continue;
                    }

                    BlockPos pos =
                            new BlockPos(
                                    x,
                                    y,
                                    z
                            );

                    BlockState state =
                            level.getBlockState(
                                    pos
                            );

                    if (state.isAir()
                            || state.getDestroySpeed(
                            level,
                            pos
                    ) < 0.0F) {
                        continue;
                    }

                    level.setBlock(
                            pos,
                            Blocks.AIR
                                    .defaultBlockState(),
                            2
                    );

                    if (particles < particleBudget
                            && level.random.nextFloat()
                            < 0.055F) {

                        Vec3 blockCenter =
                                Vec3.atCenterOf(
                                        pos
                                );

                        level.sendParticles(
                                new BlockParticleOption(
                                        ParticleTypes.BLOCK,
                                        state
                                ),
                                blockCenter.x,
                                blockCenter.y,
                                blockCenter.z,
                                2,
                                0.30,
                                0.30,
                                0.30,
                                0.20
                        );

                        particles++;
                    }
                }
            }
        }
    }

    private static void beginFusion(
            ServerLevel level,
            ServerPlayer owner,
            BlueManager.FusionSeed blue
    ) {
        /*
         * IMPORTANT: tickReds() is already iterating REDS with an Iterator.
         * Removing from the backing map here invalidates that iterator and
         * caused the PURPLE fusion crash (ConcurrentModificationException).
         * The caller removes the fused RED through iterator.remove().
         */
        PurpleFusion fusion =
                new PurpleFusion(
                        owner.getUUID(),
                        blue.dimension(),
                        blue.center(),
                        Math.max(
                                0.78F,
                                blue.power()
                        ),
                        blue.spinDirection()
                );

        FUSIONS.put(
                owner.getUUID(),
                fusion
        );

        level.playSound(
                null,
                BlockPos.containing(
                        fusion.center
                ),
                SoundEvents.END_PORTAL_SPAWN,
                SoundSource.PLAYERS,
                1.55F,
                0.72F
        );

        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                fusion.center.x,
                fusion.center.y,
                fusion.center.z,
                110,
                3.2,
                3.2,
                3.2,
                0.44
        );

        level.sendParticles(
                ParticleTypes.END_ROD,
                fusion.center.x,
                fusion.center.y,
                fusion.center.z,
                36,
                1.6,
                1.6,
                1.6,
                0.18
        );

        sendVisual(
                level,
                fusion.owner,
                BetaTechniqueVisualPayload.FUSION,
                fusion.center,
                fusion.power,
                0.0F
        );
    }

    private static void tickFusions(
            MinecraftServer server
    ) {
        Iterator<Map.Entry<UUID, PurpleFusion>>
                iterator =
                FUSIONS.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            PurpleFusion fusion =
                    iterator.next()
                            .getValue();

            ServerLevel level =
                    server.getLevel(
                            fusion.dimension
                    );

            if (level == null) {
                iterator.remove();
                continue;
            }

            ServerPlayer owner = server.getPlayerList().getPlayer(fusion.owner);
            if (owner == null || owner.serverLevel() != level
                    || !owner.isAlive() || !SpectrumAccess.has(owner, SpectrumType.VOID)) {
                iterator.remove();
                continue;
            }

            // BLUE + RED is committed to the old Purple Nuke again.
            // Once fusion starts, its center is frozen and the charge grows
            // until detonation; it never becomes a held/projectile Purple.

            fusion.age++;

            if (fusion.age
                    < PURPLE_BLAST_TICK) {

                float progress =
                        Mth.clamp(
                                fusion.age
                                        / (float) PURPLE_BLAST_TICK,
                                0.0F,
                                1.0F
                        );

                /*
                 * Keep the original BLUE snapshot alive so its spatial theme
                 * does not fade while RED is being fused into it.
                 */
                BlueManager.keepFusionBlueVisible(
                        level,
                        fusion.owner,
                        fusion.center,
                        fusion.power
                );

                if (fusion.age % 2 == 0) {
                    emitFusionArcs(
                            level,
                            fusion,
                            progress
                    );
                }

                sendVisual(
                        level,
                        fusion.owner,
                        BetaTechniqueVisualPayload.FUSION,
                        fusion.center,
                        fusion.power,
                        progress
                );

                continue;
            }

            if (!fusion.blastTriggered) {
                fusion.blastTriggered =
                        true;

                detonatePurple(
                        level,
                        fusion
                );
            }

            float aftermath =
                    Mth.clamp(
                            (
                                    fusion.age
                                            - PURPLE_BLAST_TICK
                            )
                                    / (float) (
                                    PURPLE_END_TICK
                                            - PURPLE_BLAST_TICK
                            ),
                            0.0F,
                            1.0F
                    );

            if (fusion.age % 3 == 0) {
                emitAftermathStars(
                        level,
                        fusion.center,
                        54,
                        1.0F - aftermath * 0.55F
                );
            }

            sendVisual(
                    level,
                    fusion.owner,
                    BetaTechniqueVisualPayload.AFTERMATH,
                    fusion.center,
                    fusion.power,
                    aftermath
            );

            if (fusion.age
                    >= PURPLE_END_TICK) {

                BlueManager.finishFusion(
                        server,
                        fusion.owner
                );

                iterator.remove();
            }
        }
    }

    private static void emitFusionArcs(
            ServerLevel level,
            PurpleFusion fusion,
            float progress
    ) {
        int rays =
                12
                        + Math.round(
                                progress
                                        * 30.0F
                        );

        double radius =
                1.5
                        + progress
                                * 9.2;

        for (int index = 0;
             index < rays;
             index++) {

            double angle =
                    index
                            * (
                            Math.PI
                                    * 2.0
                                    / rays
                    )
                            + fusion.age
                                    * (
                                    0.10
                                            + progress
                                                    * progress
                                                    * 0.66
                            )
                                    * fusion.spinDirection;

            double y =
                    Math.sin(
                            angle * 1.73
                    )
                            * radius
                            * 0.34;

            Vec3 source =
                    fusion.center.add(
                            Math.cos(
                                    angle
                            )
                                    * radius,
                            y,
                            Math.sin(
                                    angle
                            )
                                    * radius
                    );

            Vec3 inward =
                    fusion.center.subtract(
                            source
                    );

            if (inward.lengthSqr()
                    > 0.0001) {

                inward =
                        inward.normalize();
            }

            level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    source.x,
                    source.y,
                    source.z,
                    0,
                    inward.x,
                    inward.y,
                    inward.z,
                    0.42
                            + progress
                                    * 0.74
            );
        }
    }

    private static void detonatePurple(
            ServerLevel level,
            PurpleFusion fusion
    ) {
        /*
         * Vanilla's explosion propagation stops scaling gracefully at extreme
         * magnitudes. The physical crater is therefore authored explicitly,
         * then the vanilla blast supplies sound/fire/secondary physics.
         */
        pulverizeEllipsoid(
                level,
                fusion.center,
                PURPLE_CRATER_RADIUS_XZ,
                PURPLE_CRATER_RADIUS_Y,
                190
        );

        emitPurpleSparkleField(
                level,
                fusion.center
        );

        level.explode(
                null,
                fusion.center.x,
                fusion.center.y,
                fusion.center.z,
                PURPLE_NUKE_POWER,
                true,
                Level.ExplosionInteraction.TNT
        );

        applyPurpleNukeShockwave(
                level,
                fusion
        );

        level.playSound(
                null,
                BlockPos.containing(
                        fusion.center
                ),
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS,
                4.0F,
                0.62F
        );

        level.sendParticles(
                ParticleTypes.FLASH,
                fusion.center.x,
                fusion.center.y,
                fusion.center.z,
                1,
                0.0,
                0.0,
                0.0,
                0.0
        );

        /*
         * Shockwave particles are emitted with explicit outward velocity
         * instead of lingering at the center.
         */
        for (int index = 0;
             index < 620;
             index++) {

            double y =
                    level.random.nextDouble()
                            * 2.0
                            - 1.0;

            double theta =
                    level.random.nextDouble()
                            * Math.PI
                            * 2.0;

            double horizontal =
                    Math.sqrt(
                            Math.max(
                                    0.0,
                                    1.0
                                            - y * y
                            )
                    );

            double speed =
                    1.3
                            + level.random.nextDouble()
                                    * 2.6;

            Vec3 velocity =
                    new Vec3(
                            Math.cos(
                                    theta
                            )
                                    * horizontal
                                    * speed,
                            y * speed,
                            Math.sin(
                                    theta
                            )
                                    * horizontal
                                    * speed
                    );

            level.sendParticles(
                    index % 5 == 0
                            ? ParticleTypes.END_ROD
                            : ParticleTypes.ELECTRIC_SPARK,
                    fusion.center.x,
                    fusion.center.y,
                    fusion.center.z,
                    0,
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    1.0
            );
        }

        igniteAftermath(
                level,
                fusion.center
        );

        emitAftermathStars(
                level,
                fusion.center,
                420,
                1.0F
        );

        sendVisual(
                level,
                fusion.owner,
                BetaTechniqueVisualPayload.BLAST,
                fusion.center,
                fusion.power,
                0.0F
        );
    }

    private static void emitPurpleSparkleField(
            ServerLevel level,
            Vec3 center
    ) {
        for (int index = 0;
             index < 640;
             index++) {

            double angle =
                    level.random.nextDouble()
                            * Math.PI
                            * 2.0;

            double radius =
                    6.0
                            + Math.sqrt(
                            level.random.nextDouble()
                    )
                                    * (
                                    PURPLE_CRATER_RADIUS_XZ
                                            * 1.55
                            );

            double y =
                    center.y
                            - 3.0
                            + level.random.nextDouble()
                                    * (
                                    PURPLE_CRATER_RADIUS_Y
                                            + 18.0
                            );

            double x =
                    center.x
                            + Math.cos(
                                    angle
                            )
                                    * radius;

            double z =
                    center.z
                            + Math.sin(
                                    angle
                            )
                                    * radius;

            level.sendParticles(
                    index % 4 == 0
                            ? ParticleTypes.END_ROD
                            : index % 4 == 1
                            ? ParticleTypes.FIREWORK
                            : ParticleTypes.ELECTRIC_SPARK,
                    x,
                    y,
                    z,
                    1,
                    0.12,
                    0.22,
                    0.12,
                    0.035
            );
        }
    }

    private static void applyPurpleNukeShockwave(
            ServerLevel level,
            PurpleFusion fusion
    ) {
        double radius =
                PURPLE_SHOCKWAVE_RADIUS;

        AABB area =
                new AABB(
                        fusion.center.x - radius,
                        fusion.center.y - radius,
                        fusion.center.z - radius,
                        fusion.center.x + radius,
                        fusion.center.y + radius,
                        fusion.center.z + radius
                );

        for (LivingEntity living :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        LivingEntity::isAlive
                )) {

            Vec3 away =
                    living.position()
                            .subtract(fusion.center);

            double distance =
                    away.length();

            if (distance <= 0.01
                    || distance > radius) {

                continue;
            }

            double factor =
                    1.0
                            - distance / radius;

            Vec3 direction =
                    away.scale(
                            1.0 / distance
                    );

            living.hurt(
                    level.damageSources().magic(),
                    (float) (
                            8.0
                                    + factor
                                            * factor
                                            * 54.0
                    )
            );

            living.setDeltaMovement(
                    living.getDeltaMovement()
                            .add(
                                    direction.scale(
                                            1.0
                                                    + factor
                                                            * 5.2
                                    )
                            )
                            .add(
                                    0.0,
                                    0.28
                                            + factor
                                                    * 1.2,
                                    0.0
                            )
            );

            living.hurtMarked =
                    true;
        }
    }

    private static void igniteAftermath(
            ServerLevel level,
            Vec3 center
    ) {
        for (int attempt = 0;
             attempt < 420;
             attempt++) {

            double angle =
                    level.random.nextDouble()
                            * Math.PI
                            * 2.0;

            double radius =
                    4.0
                            + Math.sqrt(
                            level.random.nextDouble()
                    )
                                    * 74.0;

            int x =
                    Mth.floor(
                            center.x
                                    + Math.cos(
                                    angle
                            )
                                    * radius
                    );

            int z =
                    Mth.floor(
                            center.z
                                    + Math.sin(
                                    angle
                            )
                                    * radius
                    );

            int y =
                    Mth.floor(
                            center.y
                                    - 7
                                    + level.random.nextInt(
                                    15
                            )
                    );

            BlockPos pos =
                    new BlockPos(
                            x,
                            y,
                            z
                    );

            if (!level.getBlockState(
                    pos
            )
                    .isAir()
                    || level.getBlockState(
                    pos.below()
            )
                    .isAir()) {

                continue;
            }

            level.setBlockAndUpdate(
                    pos,
                    Blocks.FIRE
                            .defaultBlockState()
            );
        }
    }

    private static void emitAftermathStars(
            ServerLevel level,
            Vec3 center,
            int count,
            float intensity
    ) {
        float cleanIntensity =
                Mth.clamp(
                        intensity,
                        0.15F,
                        1.0F
                );

        level.sendParticles(
                ParticleTypes.END_ROD,
                center.x,
                center.y,
                center.z,
                Math.max(
                        8,
                        Math.round(
                                count
                                        * cleanIntensity
                        )
                ),
                PURPLE_STAR_RADIUS,
                PURPLE_STAR_RADIUS * 0.55,
                PURPLE_STAR_RADIUS,
                0.003
        );

        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                center.x,
                center.y,
                center.z,
                Math.max(
                        4,
                        Math.round(
                                count
                                        * 0.22F
                                        * cleanIntensity
                        )
                ),
                PURPLE_STAR_RADIUS * 0.82,
                PURPLE_STAR_RADIUS * 0.46,
                PURPLE_STAR_RADIUS * 0.82,
                0.008
        );
    }

    public static void clearAll() {
        REDS.clear();
        FUSIONS.clear();
        DUALS.clear();
        PENDING_PURPLE_CASTS.clear();
        PURPLE_PROJECTILES.clear();
    }

    private static void sendCinematic(
            ServerPlayer player,
            byte animation,
            int durationTicks
    ) {
        PacketDistributor.sendToPlayersNear(
                player.serverLevel(),
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                VISUAL_RANGE,
                new PlayerCinematicPayload(
                        player.getUUID(),
                        animation,
                        durationTicks,
                        false,
                        0.0F
                )
        );
    }

    private static void sendVisual(
            ServerLevel level,
            UUID owner,
            byte mode,
            Vec3 position,
            float power,
            float progress
    ) {
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                position.x,
                position.y,
                position.z,
                VISUAL_RANGE,
                new BetaTechniqueVisualPayload(
                        owner,
                        mode,
                        position.x,
                        position.y,
                        position.z,
                        power,
                        progress
                )
        );
    }

    private static final class PendingPurpleCast {

        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final Vec3 direction;
        private int delay;

        private PendingPurpleCast(
                UUID owner,
                net.minecraft.resources.ResourceKey<Level> dimension,
                Vec3 direction,
                int delay
        ) {
            this.owner = owner;
            this.dimension = dimension;
            this.direction = direction;
            this.delay = delay;
        }
    }

    private static final class PendingDual {

        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level>
                dimension;
        private int life;

        private PendingDual(
                UUID owner,
                net.minecraft.resources.ResourceKey<Level> dimension,
                int life
        ) {
            this.owner = owner;
            this.dimension = dimension;
            this.life = life;
        }
    }

    private static final class PurpleProjectile {

        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level>
                dimension;
        private Vec3 position;
        private final Vec3 velocity;
        private int life;

        private PurpleProjectile(
                UUID owner,
                net.minecraft.resources.ResourceKey<Level> dimension,
                Vec3 position,
                Vec3 velocity,
                int life
        ) {
            this.owner = owner;
            this.dimension = dimension;
            this.position = position;
            this.velocity = velocity;
            this.life = life;
        }
    }

    private static final class RedProjectile {

        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level>
                dimension;

        private Vec3 position;
        private Vec3 velocity;
        private final Set<UUID> hitEntities =
                new HashSet<>();
        private int life;
        private boolean held;
        private boolean maximum;

        private RedProjectile(
                UUID owner,
                net.minecraft.resources.ResourceKey<Level> dimension,
                Vec3 position,
                Vec3 velocity,
                int life
        ) {
            this.owner =
                    owner;

            this.dimension =
                    dimension;

            this.position =
                    position;

            this.velocity =
                    velocity;

            this.life =
                    life;
        }
    }

    private static final class PurpleFusion {

        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level>
                dimension;
        private Vec3 center;
        private final float power;
        private final double spinDirection;

        private int age;
        private boolean blastTriggered;

        private PurpleFusion(
                UUID owner,
                net.minecraft.resources.ResourceKey<Level> dimension,
                Vec3 center,
                float power,
                double spinDirection
        ) {
            this.owner =
                    owner;

            this.dimension =
                    dimension;

            this.center =
                    center;

            this.power =
                    power;

            this.spinDirection =
                    spinDirection;
        }
    }
}
