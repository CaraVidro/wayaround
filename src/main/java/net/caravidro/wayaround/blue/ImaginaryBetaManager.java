package net.caravidro.wayaround.blue;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
            180;

    private static final double RED_SPEED =
            5.75;

    private static final double RED_RADIUS =
            0.82;

    private static final int RED_SUBSTEPS =
            18;

    private static final double RED_MAX_RANGE =
            168.0;

    private static final double RED_MAX_RANGE_CHARGED =
            246.0;

    private static final int PURPLE_BLAST_TICK =
            92;

    private static final int PURPLE_END_TICK =
            188;

    private static final float PURPLE_NUKE_POWER =
            72.0F;

    private static final double PURPLE_STAR_RADIUS =
            142.0;

    private static final double PURPLE_SHOCKWAVE_RADIUS =
            176.0;

    private static final int PURPLE_PULVERIZE_RADIUS =
            34;

    private static final int PURPLE_PULVERIZE_VERTICAL =
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

    private static final Map<UUID, PreparedRed>
            PREPARED_REDS =
            new HashMap<>();

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

    public static boolean prepareDual(
            ServerPlayer player
    ) {
        UUID owner =
                player.getUUID();

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
        PendingDual dual =
                DUALS.remove(
                        player.getUUID()
                );

        if (dual == null) {
            return false;
        }

        Vec3 direction =
                player.getLookAngle().normalize();

        Vec3 position =
                player.getEyePosition()
                        .add(
                                direction.scale(4.6)
                        );

        PURPLE_PROJECTILES.put(
                player.getUUID(),
                new PurpleProjectile(
                        player.getUUID(),
                        player.serverLevel().dimension(),
                        position,
                        direction.scale(
                                PURPLE_PROJECTILE_SPEED
                        ),
                        PURPLE_PROJECTILE_LIFE
                )
        );

        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.END_PORTAL_SPAWN,
                SoundSource.PLAYERS,
                2.0F,
                0.62F
        );

        sendVisual(
                player.serverLevel(),
                player.getUUID(),
                BetaTechniqueVisualPayload.PURPLE_PROJECTILE,
                position,
                1.35F,
                0.0F
        );

        return true;
    }

    public static boolean fireRed(
            ServerPlayer player
    ) {
        if (FUSIONS.containsKey(
                player.getUUID()
        )
                || REDS.containsKey(
                player.getUUID()
        )) {
            return false;
        }

        Vec3 direction =
                player.getLookAngle()
                        .normalize();

        Vec3 position =
                player.getEyePosition()
                        .add(
                                direction.scale(
                                        2.35
                                )
                        );

        PREPARED_REDS.put(
                player.getUUID(),
                new PreparedRed(
                        player.getUUID(),
                        player.serverLevel()
                                .dimension(),
                        position
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
                        SoundEvents.AMETHYST_BLOCK_RESONATE,
                        SoundSource.PLAYERS,
                        0.72F,
                        0.58F
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
                        0.04
                );

        player.serverLevel()
                .sendParticles(
                        ParticleTypes.FLAME,
                        position.x,
                        position.y,
                        position.z,
                        8,
                        0.16,
                        0.16,
                        0.16,
                        0.01
                );

        sendVisual(
                player.serverLevel(),
                player.getUUID(),
                BetaTechniqueVisualPayload.RED,
                position,
                0.82F,
                0.0F
        );

        return true;
    }

    public static boolean hasPreparedRed(
            ServerPlayer player
    ) {
        return PREPARED_REDS.containsKey(
                player.getUUID()
        );
    }

    public static boolean chargePreparedRedMaximum(
            ServerPlayer player
    ) {
        PreparedRed red =
                PREPARED_REDS.get(
                        player.getUUID()
                );

        if (red == null) {
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
                1.15F,
                1.62F
        );

        level.playSound(
                null,
                BlockPos.containing(
                        red.position
                ),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                1.0F,
                1.92F
        );

        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                red.position.x,
                red.position.y,
                red.position.z,
                68,
                0.42,
                0.42,
                0.42,
                0.26
        );

        return true;
    }

    public static boolean launchPreparedRed(
            ServerPlayer player
    ) {
        PreparedRed prepared =
                PREPARED_REDS.remove(
                        player.getUUID()
                );

        if (prepared == null) {
            return false;
        }

        Vec3 direction =
                player.getLookAngle()
                        .normalize();

        double speed =
                prepared.maximum
                        ? RED_SPEED * 1.32
                        : RED_SPEED;

        REDS.put(
                player.getUUID(),
                new RedProjectile(
                        player.getUUID(),
                        player.serverLevel()
                                .dimension(),
                        prepared.position,
                        direction.scale(
                                speed
                        ),
                        RED_LIFE_TICKS,
                        prepared.maximum
                )
        );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.FIREWORK_ROCKET_LAUNCH,
                        SoundSource.PLAYERS,
                        prepared.maximum
                                ? 1.75F
                                : 1.10F,
                        prepared.maximum
                                ? 0.46F
                                : 0.62F
                );

        return true;
    }

    private static void tickPreparedReds(
            MinecraftServer server
    ) {
        Iterator<Map.Entry<UUID, PreparedRed>> iterator =
                PREPARED_REDS.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            PreparedRed red =
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
                    || owner.serverLevel()
                            != level) {

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
                                            2.35
                                    )
                            );

            int count =
                    red.maximum
                            ? 6
                            : 2;

            level.sendParticles(
                    red.maximum
                            ? ParticleTypes.END_ROD
                            : ParticleTypes.ELECTRIC_SPARK,
                    red.position.x,
                    red.position.y,
                    red.position.z,
                    count,
                    red.maximum
                            ? 0.28
                            : 0.12,
                    red.maximum
                            ? 0.28
                            : 0.12,
                    red.maximum
                            ? 0.28
                            : 0.12,
                    red.maximum
                            ? 0.035
                            : 0.015
            );

            sendVisual(
                    level,
                    red.owner,
                    BetaTechniqueVisualPayload.RED,
                    red.position,
                    red.maximum
                            ? 1.68F
                            : 0.82F,
                    0.0F
            );
        }
    }

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        tickDuals(server);
        tickPreparedReds(server);
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

            living.hurt(
                    level.damageSources()
                            .genericKill(),
                    Float.MAX_VALUE
            );
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

            Vec3 step =
                    red.velocity.scale(
                            1.0
                                    / RED_SUBSTEPS
                    );

            boolean fused =
                    false;

            boolean stop =
                    false;

            for (int sub = 0;
                 sub < RED_SUBSTEPS;
                 sub++) {

                red.position =
                        red.position.add(
                                step
                        );

                red.travelled +=
                        step.length();

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

                boolean blocked =
                        destroyRedPath(
                                level,
                                red.position
                        );

                pushRedWake(
                        level,
                        red.position,
                        red.velocity
                );

                if (red.maximum
                        && sub % 3 == 0) {
                    igniteRedTrail(
                            level,
                            red.position
                    );
                }

                double maxRange =
                        red.maximum
                                ? RED_MAX_RANGE_CHARGED
                                : RED_MAX_RANGE;

                if (blocked
                        || red.travelled >= maxRange
                        || red.life <= 0) {

                    stop =
                            true;
                    break;
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
                            ? 12
                            : 5,
                    red.maximum
                            ? 0.24
                            : 0.10,
                    red.maximum
                            ? 0.24
                            : 0.10,
                    red.maximum
                            ? 0.24
                            : 0.10,
                    red.maximum
                            ? 0.18
                            : 0.08
            );

            level.sendParticles(
                    ParticleTypes.FLAME,
                    red.position.x,
                    red.position.y,
                    red.position.z,
                    red.maximum
                            ? 8
                            : 3,
                    0.18,
                    0.18,
                    0.18,
                    0.035
            );

            sendVisual(
                    level,
                    red.owner,
                    BetaTechniqueVisualPayload.RED,
                    red.position,
                    red.maximum
                            ? 1.75F
                            : 1.0F,
                    1.0F
                            - red.life
                                    / (float) RED_LIFE_TICKS
            );

            if (stop) {
                detonateRed(
                        level,
                        owner,
                        red
                );

                iterator.remove();
            }
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

    private static boolean destroyRedPath(
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

            if (state.isAir()) {
                continue;
            }

            if (state.getDestroySpeed(
                    level,
                    sample
            ) < 0.0F) {
                return true;
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
        return false;
    }

    private static void pushRedWake(
            ServerLevel level,
            Vec3 center,
            Vec3 velocity
    ) {
        Vec3 direction =
                velocity.lengthSqr() < 0.0001
                        ? new Vec3(0.0, 0.0, 1.0)
                        : velocity.normalize();

        AABB area =
                new AABB(
                        center.x - 4.5,
                        center.y - 3.0,
                        center.z - 4.5,
                        center.x + 4.5,
                        center.y + 3.0,
                        center.z + 4.5
                );

        for (LivingEntity living :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        LivingEntity::isAlive
                )) {

            living.setDeltaMovement(
                    living.getDeltaMovement()
                            .add(
                                    direction.scale(
                                            0.08
                                    )
                            )
            );

            living.hurtMarked =
                    true;
        }

        if (level.random.nextInt(2) == 0) {
            BlockPos ground =
                    BlockPos.containing(
                            center.x,
                            center.y - 1.0,
                            center.z
                    );

            BlockState state =
                    level.getBlockState(
                            ground
                    );

            if (!state.isAir()) {
                level.sendParticles(
                        new BlockParticleOption(
                                ParticleTypes.BLOCK,
                                state
                        ),
                        center.x,
                        center.y - 0.25,
                        center.z,
                        7,
                        1.2,
                        0.38,
                        1.2,
                        0.42
                );
            }
        }
    }

    private static void detonateRed(
            ServerLevel level,
            ServerPlayer owner,
            RedProjectile red
    ) {
        float power =
                red.maximum
                        ? 21.0F
                        : 13.0F;

        level.explode(
                owner,
                red.position.x,
                red.position.y,
                red.position.z,
                power,
                red.maximum,
                Level.ExplosionInteraction.TNT
        );

        level.playSound(
                null,
                BlockPos.containing(
                        red.position
                ),
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS,
                2.8F,
                red.maximum
                        ? 0.48F
                        : 0.68F
        );

        level.sendParticles(
                ParticleTypes.EXPLOSION,
                red.position.x,
                red.position.y,
                red.position.z,
                red.maximum
                        ? 28
                        : 14,
                red.maximum
                        ? 7.0
                        : 4.0,
                red.maximum
                        ? 5.0
                        : 3.0,
                red.maximum
                        ? 7.0
                        : 4.0,
                0.12
        );

        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                red.position.x,
                red.position.y,
                red.position.z,
                red.maximum
                        ? 180
                        : 84,
                red.maximum
                        ? 9.0
                        : 5.5,
                red.maximum
                        ? 5.0
                        : 3.0,
                red.maximum
                        ? 9.0
                        : 5.5,
                0.32
        );
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
         * One authoritative explosion. The fire flag is intentional: PURPLE
         * is the rare fusion finisher, not the ordinary BLUE lifecycle.
         */
        level.explode(
                null,
                fusion.center.x,
                fusion.center.y,
                fusion.center.z,
                PURPLE_NUKE_POWER,
                true,
                Level.ExplosionInteraction.TNT
        );

        pulverizePurpleCore(
                level,
                fusion.center
        );

        applyPurpleNukeShockwave(
                level,
                fusion
        );

        detonatePurpleSecondaryRings(
                level,
                fusion.center
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
                260,
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

    private static void pulverizePurpleCore(
            ServerLevel level,
            Vec3 center
    ) {
        BlockPos origin =
                BlockPos.containing(
                        center
                );

        double rx =
                PURPLE_PULVERIZE_RADIUS;

        double ry =
                PURPLE_PULVERIZE_VERTICAL;

        double rz =
                PURPLE_PULVERIZE_RADIUS;

        for (int x = -PURPLE_PULVERIZE_RADIUS;
             x <= PURPLE_PULVERIZE_RADIUS;
             x++) {

            for (int y = -PURPLE_PULVERIZE_VERTICAL;
                 y <= PURPLE_PULVERIZE_VERTICAL;
                 y++) {

                for (int z = -PURPLE_PULVERIZE_RADIUS;
                     z <= PURPLE_PULVERIZE_RADIUS;
                     z++) {

                    double normalized =
                            x * x / (rx * rx)
                                    + y * y / (ry * ry)
                                    + z * z / (rz * rz);

                    if (normalized > 1.0) {
                        continue;
                    }

                    BlockPos pos =
                            origin.offset(
                                    x,
                                    y,
                                    z
                            );

                    if (!level.hasChunkAt(
                            pos
                    )) {
                        continue;
                    }

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

                    /*
                     * Dense center is guaranteed pulverization. The outer
                     * ellipsoid becomes noisy so the crater is not a perfect
                     * mathematical sphere.
                     */
                    if (normalized < 0.72
                            || level.random.nextDouble()
                            > (
                            normalized - 0.72
                    ) * 2.2) {

                        level.removeBlock(
                                pos,
                                false
                        );
                    }
                }
            }
        }
    }

    private static void detonatePurpleSecondaryRings(
            ServerLevel level,
            Vec3 center
    ) {
        for (int ring = 0;
             ring < 3;
             ring++) {

            int points =
                    8 + ring * 6;

            double radius =
                    18.0 + ring * 11.0;

            for (int point = 0;
                 point < points;
                 point++) {

                double angle =
                        point
                                * Math.PI
                                * 2.0
                                / points;

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

                level.explode(
                        null,
                        x,
                        center.y
                                + (
                                ring - 1
                        ) * 2.2,
                        z,
                        7.5F
                                + ring * 1.8F,
                        false,
                        Level.ExplosionInteraction.TNT
                );
            }
        }

        for (int index = 0;
             index < 900;
             index++) {

            double angle =
                    level.random.nextDouble()
                            * Math.PI
                            * 2.0;

            double radius =
                    Math.sqrt(
                            level.random.nextDouble()
                    )
                            * PURPLE_STAR_RADIUS;

            double y =
                    (
                            level.random.nextDouble()
                                    - 0.5
                    )
                            * 52.0;

            Vec3 point =
                    center.add(
                            Math.cos(angle)
                                    * radius,
                            y,
                            Math.sin(angle)
                                    * radius
                    );

            level.sendParticles(
                    index % 4 == 0
                            ? ParticleTypes.FIREWORK
                            : ParticleTypes.END_ROD,
                    point.x,
                    point.y,
                    point.z,
                    0,
                    0.0,
                    0.012
                            + level.random.nextDouble()
                                    * 0.035,
                    0.0,
                    1.0
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
        PREPARED_REDS.clear();
        REDS.clear();
        FUSIONS.clear();
        DUALS.clear();
        PURPLE_PROJECTILES.clear();
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

    private static final class PreparedRed {

        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level>
                dimension;

        private Vec3 position;
        private boolean maximum;

        private PreparedRed(
                UUID owner,
                net.minecraft.resources.ResourceKey<Level> dimension,
                Vec3 position
        ) {
            this.owner = owner;
            this.dimension = dimension;
            this.position = position;
        }
    }

    private static final class RedProjectile {

        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level>
                dimension;

        private Vec3 position;
        private final Vec3 velocity;
        private final Set<UUID> hitEntities =
                new HashSet<>();

        private int life;
        private double travelled;
        private final boolean maximum;

        private RedProjectile(
                UUID owner,
                net.minecraft.resources.ResourceKey<Level> dimension,
                Vec3 position,
                Vec3 velocity,
                int life,
                boolean maximum
        ) {
            this.owner = owner;
            this.dimension = dimension;
            this.position = position;
            this.velocity = velocity;
            this.life = life;
            this.maximum = maximum;
        }
    }

    private static final class PurpleFusion {

        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level>
                dimension;
        private final Vec3 center;
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
