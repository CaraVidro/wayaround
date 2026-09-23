package net.caravidro.wayaround.blue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joml.Vector3f;

import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.sounds.WayAroundSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Deliberately over-the-top prototype ability.
 *
 * Blue is not a physical projectile with collision. Its center is a pure
 * server-side point, which means it can be aimed through terrain and can keep
 * carving/pulling from underground.
 */
public final class BlueManager {

    private static final double MIN_DISTANCE = 2.0;
    private static final double MAX_DISTANCE = 34.0;
    private static final double SCROLL_STEP = 2.2;

    private static final Map<UUID, HeldBlue> HELD =
            new HashMap<>();

    private static final List<LaunchedBlue> LAUNCHED =
            new ArrayList<>();

    private BlueManager() {
    }

    public static void begin(
            ServerPlayer player
    ) {
        long tick =
                player.server.getTickCount();

        HELD.put(
                player.getUUID(),
                new HeldBlue(
                        tick,
                        player.serverLevel()
                                .random
                                .nextBoolean()
                                ? 1.0
                                : -1.0
                )
        );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        WayAroundSounds.BLUE_THEME.get(),
                        SoundSource.PLAYERS,
                        1.25F,
                        1.0F
                );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.PLAYERS,
                        0.55F,
                        0.52F
                );
    }

    public static void tickHeld(
            ServerPlayer player
    ) {
        if (!isUsingBlue(player)) {
            return;
        }

        HeldBlue state =
                HELD.computeIfAbsent(
                        player.getUUID(),
                        id ->
                                new HeldBlue(
                                        player.server.getTickCount(),
                                        player.serverLevel()
                                                .random
                                                .nextBoolean()
                                                ? 1.0
                                                : -1.0
                                )
                );

        state.chargeTicks++;

        Vec3 look =
                player.getLookAngle()
                        .normalize();

        Vec3 center =
                player.getEyePosition()
                        .add(
                                look.scale(
                                        state.distance
                                )
                        );

        state.lastCenter = center;
        state.lastLook = look;

        float charge =
                Mth.clamp(
                        state.chargeTicks
                                / 42.0F,
                        0.18F,
                        1.0F
                );

        double radius =
                4.2
                        + charge
                        * 5.8;

        applyBlue(
                player.serverLevel(),
                player,
                center,
                radius,
                charge,
                state.spinDirection,
                2
                        + Math.round(
                                charge
                                        * 6.0F
                        ),
                true
        );

        renderBlue(
                player.serverLevel(),
                center,
                0.75
                        + charge
                        * 0.72,
                charge,
                state.spinDirection,
                player.server.getTickCount()
        );

        environmentTrail(
                player.serverLevel(),
                center,
                charge,
                false
        );
    }

    public static void scroll(
            ServerPlayer player,
            double amount
    ) {
        if (!isUsingBlue(player)
                || !Double.isFinite(amount)
                || Math.abs(amount) > 16.0) {
            return;
        }

        HeldBlue state =
                HELD.get(
                        player.getUUID()
                );

        if (state == null) {
            begin(player);
            state =
                    HELD.get(
                            player.getUUID()
                    );
        }

        long tick =
                player.server.getTickCount();

        double previous =
                state.distance;

        state.distance =
                Mth.clamp(
                        state.distance
                                + amount
                                * SCROLL_STEP,
                        MIN_DISTANCE,
                        MAX_DISTANCE
                );

        state.lastScrollDelta =
                state.distance
                        - previous;

        state.lastScrollTick =
                tick;

        if (state.distance <= 3.6) {
            state.pulledBack = true;
            state.pulledBackTick = tick;
            state.slingReady = false;
        }

        if (state.pulledBack
                && tick - state.pulledBackTick <= 16L
                && state.distance >= 8.5
                && state.distance > previous) {
            state.slingReady = true;
            state.slingReadyTick = tick;
        }
    }

    public static void release(
            ServerPlayer player
    ) {
        HeldBlue state =
                HELD.remove(
                        player.getUUID()
                );

        if (state == null) {
            return;
        }

        long tick =
                player.server.getTickCount();

        long heldTicks =
                tick
                        - state.startedAt;

        if (state.slingReady
                && tick - state.slingReadyTick <= 12L) {
            launch(
                    player,
                    state
            );
            return;
        }

        if (heldTicks <= 14L) {
            scatterRelease(
                    player.serverLevel(),
                    player,
                    state.lastCenter,
                    8.5,
                    state.spinDirection,
                    1.0F
            );
            return;
        }

        implosionRelease(
                player.serverLevel(),
                player,
                state.lastCenter,
                state.spinDirection
        );
    }

    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        cleanupHeld(server);

        Iterator<LaunchedBlue> iterator =
                LAUNCHED.iterator();

        while (iterator.hasNext()) {
            LaunchedBlue blue =
                    iterator.next();

            ServerLevel level =
                    server.getLevel(
                            blue.dimension
                    );

            if (level == null) {
                iterator.remove();
                continue;
            }

            ServerPlayer owner =
                    server.getPlayerList()
                            .getPlayer(
                                    blue.owner
                            );

            blue.life--;

            if (blue.life <= 0) {
                collapseLaunched(
                        level,
                        owner,
                        blue
                );

                iterator.remove();
                continue;
            }

            blue.position =
                    blue.position.add(
                            blue.velocity
                    );

            blue.velocity =
                    blue.velocity.scale(
                            0.994
                    );

            if (!level.hasChunkAt(
                    BlockPos.containing(
                            blue.position
                    )
            )) {
                continue;
            }

            float fade =
                    Mth.clamp(
                            blue.life
                                    / (float) blue.maxLife,
                            0.0F,
                            1.0F
                    );

            float power =
                    fade
                            * blue.initialPower;

            double radius =
                    3.6
                            + 6.4
                            * power;

            applyBlue(
                    level,
                    owner,
                    blue.position,
                    radius,
                    power,
                    blue.spinDirection,
                    Math.max(
                            1,
                            Math.round(
                                    6.0F
                                            * power
                            )
                    ),
                    true
            );

            renderBlue(
                    level,
                    blue.position,
                    0.35
                            + power
                            * 1.25,
                    power,
                    blue.spinDirection,
                    server.getTickCount()
            );

            environmentTrail(
                    level,
                    blue.position,
                    power,
                    true
            );
        }
    }

    public static void clearAll() {
        HELD.clear();
        LAUNCHED.clear();
    }

    private static void cleanupHeld(
            MinecraftServer server
    ) {
        Iterator<Map.Entry<UUID, HeldBlue>> iterator =
                HELD.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, HeldBlue> entry =
                    iterator.next();

            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(
                                    entry.getKey()
                            );

            if (player == null
                    || !player.isAlive()
                    || !isUsingBlue(player)) {
                iterator.remove();
            }
        }
    }

    private static boolean isUsingBlue(
            ServerPlayer player
    ) {
        return player.isUsingItem()
                && player.getUseItem()
                        .is(
                                WayAroundContent.BLUE.get()
                        );
    }

    private static void launch(
            ServerPlayer player,
            HeldBlue state
    ) {
        Vec3 look =
                state.lastLook.lengthSqr() < 0.001
                        ? player.getLookAngle()
                                .normalize()
                        : state.lastLook
                                .normalize();

        double outwardKick =
                Math.abs(
                        state.lastScrollDelta
                );

        double speed =
                0.42
                        + Math.min(
                                0.28,
                                outwardKick
                                        * 0.035
                        );

        float power =
                Mth.clamp(
                        0.82F
                                + state.chargeTicks
                                / 90.0F,
                        0.9F,
                        1.45F
                );

        LAUNCHED.add(
                new LaunchedBlue(
                        player.getUUID(),
                        player.serverLevel()
                                .dimension(),
                        state.lastCenter,
                        look.scale(
                                speed
                        ),
                        power,
                        state.spinDirection,
                        100
                )
        );

        player.serverLevel()
                .playSound(
                        null,
                        BlockPos.containing(
                                state.lastCenter
                        ),
                        SoundEvents.END_PORTAL_SPAWN,
                        SoundSource.PLAYERS,
                        1.25F,
                        1.65F
                );

        burst(
                player.serverLevel(),
                state.lastCenter,
                44,
                1.0F
        );
    }

    private static void applyBlue(
            ServerLevel level,
            Player owner,
            Vec3 center,
            double radius,
            float power,
            double spinDirection,
            int blockBudget,
            boolean eraseCore
    ) {
        pullEntities(
                level,
                owner,
                center,
                radius,
                power,
                spinDirection,
                eraseCore
        );

        consumeBlocks(
                level,
                owner,
                center,
                power,
                blockBudget
        );

        drawIncomingMatter(
                level,
                center,
                radius,
                power,
                spinDirection
        );
    }

    private static void pullEntities(
            ServerLevel level,
            Player owner,
            Vec3 center,
            double radius,
            float power,
            double spinDirection,
            boolean eraseCore
    ) {
        AABB box =
                new AABB(
                        center.x - radius,
                        center.y - radius,
                        center.z - radius,
                        center.x + radius,
                        center.y + radius,
                        center.z + radius
                );

        List<Entity> entities =
                level.getEntities(
                        owner,
                        box,
                        entity ->
                                entity.isAlive()
                                        && entity != owner
                                        && !entity.isSpectator()
                );

        double radiusSquared =
                radius
                        * radius;

        for (Entity entity : entities) {
            Vec3 entityCenter =
                    entity.getBoundingBox()
                            .getCenter();

            Vec3 toCenter =
                    center.subtract(
                            entityCenter
                    );

            double distanceSquared =
                    toCenter.lengthSqr();

            if (distanceSquared > radiusSquared
                    || distanceSquared < 0.0001) {
                continue;
            }

            double distance =
                    Math.sqrt(
                            distanceSquared
                    );

            if (eraseCore
                    && distance
                            < 0.78
                                    + power
                                    * 0.62) {
                eraseEntity(
                        level,
                        entity
                );

                continue;
            }

            Vec3 radial =
                    toCenter.scale(
                            1.0
                                    / distance
                    );

            Vec3 tangent =
                    new Vec3(
                            -radial.z,
                            0.18
                                    * Math.sin(
                                            entity.getId()
                                                    * 0.71
                                                    + level.getGameTime()
                                                            * 0.18
                                    ),
                            radial.x
                    )
                    .normalize()
                    .scale(
                            spinDirection
                    );

            double proximity =
                    1.0
                            - distance
                                    / radius;

            double pull =
                    0.075
                            + power
                                    * (
                                            0.10
                                                    + proximity
                                                    * 0.24
                                    );

            double orbit =
                    0.035
                            + power
                                    * (
                                            0.035
                                                    + proximity
                                                    * 0.10
                                    );

            Vec3 next =
                    entity.getDeltaMovement()
                            .scale(
                                    0.74
                            )
                            .add(
                                    radial.scale(
                                            pull
                                    )
                            )
                            .add(
                                    tangent.scale(
                                            orbit
                                    )
                            );

            entity.setDeltaMovement(
                    next
            );

            entity.hurtMarked = true;
        }
    }

    private static void eraseEntity(
            ServerLevel level,
            Entity entity
    ) {
        if (entity instanceof ServerPlayer player) {
            player.hurt(
                    level.damageSources()
                            .genericKill(),
                    Float.MAX_VALUE
            );
            return;
        }

        entity.discard();

        level.sendParticles(
                ParticleTypes.PORTAL,
                entity.getX(),
                entity.getY()
                        + entity.getBbHeight()
                                * 0.5,
                entity.getZ(),
                18,
                0.22,
                0.22,
                0.22,
                0.5
        );
    }

    private static void consumeBlocks(
            ServerLevel level,
            Player owner,
            Vec3 center,
            float power,
            int blockBudget
    ) {
        if (blockBudget <= 0) {
            return;
        }

        double digRadius =
                1.9
                        + power
                                * 2.25;

        int removed = 0;
        int attempts =
                blockBudget
                        * 5;

        for (int i = 0;
                i < attempts
                        && removed < blockBudget;
                i++) {

            double dx =
                    (
                            level.random.nextDouble()
                                    * 2.0
                            - 1.0
                    )
                    * digRadius;

            double dy =
                    (
                            level.random.nextDouble()
                                    * 2.0
                            - 1.0
                    )
                    * digRadius;

            double dz =
                    (
                            level.random.nextDouble()
                                    * 2.0
                            - 1.0
                    )
                    * digRadius;

            if (dx * dx
                    + dy * dy
                    + dz * dz
                    > digRadius
                            * digRadius) {
                continue;
            }

            BlockPos pos =
                    BlockPos.containing(
                            center.x + dx,
                            center.y + dy,
                            center.z + dz
                    );

            BlockState state =
                    level.getBlockState(
                            pos
                    );

            if (state.isAir()) {
                continue;
            }

            if (!state.getFluidState()
                    .isEmpty()) {
                if (state.getFluidState()
                        .is(
                                FluidTags.WATER
                        )) {
                    waterBurst(
                            level,
                            Vec3.atCenterOf(
                                    pos
                            ),
                            Math.max(
                                    0.4F,
                                    power
                            )
                    );
                }
            }

            float hardness =
                    state.getDestroySpeed(
                            level,
                            pos
                    );

            if (hardness < 0.0F) {
                continue;
            }

            level.removeBlock(
                    pos,
                    false
            );

            removed++;

            Vec3 source =
                    Vec3.atCenterOf(
                            pos
                    );

            Vec3 velocity =
                    center.subtract(
                            source
                    );

            if (velocity.lengthSqr()
                    > 0.0001) {
                velocity =
                        velocity.normalize()
                                .scale(
                                        0.32
                                                + power
                                                        * 0.34
                                );
            }

            level.sendParticles(
                    new BlockParticleOption(
                            ParticleTypes.BLOCK,
                            state
                    ),
                    source.x,
                    source.y,
                    source.z,
                    0,
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    1.0
            );
        }
    }

    private static void drawIncomingMatter(
            ServerLevel level,
            Vec3 center,
            double radius,
            float power,
            double spinDirection
    ) {
        int count =
                6
                        + Math.round(
                                power
                                        * 9.0F
                        );

        for (int i = 0;
                i < count;
                i++) {

            double angle =
                    level.random.nextDouble()
                            * Math.PI
                            * 2.0;

            double distance =
                    radius
                            * (
                                    0.45
                                            + level.random.nextDouble()
                                                    * 0.75
                            );

            double y =
                    (
                            level.random.nextDouble()
                                    - 0.5
                    )
                    * radius
                    * 0.75;

            Vec3 source =
                    center.add(
                            Math.cos(angle)
                                    * distance,
                            y,
                            Math.sin(angle)
                                    * distance
                    );

            Vec3 inward =
                    center.subtract(
                            source
                    );

            if (inward.lengthSqr()
                    < 0.001) {
                continue;
            }

            inward =
                    inward.normalize();

            Vec3 tangent =
                    new Vec3(
                            -inward.z,
                            0.0,
                            inward.x
                    )
                    .scale(
                            spinDirection
                                    * 0.12
                    );

            Vec3 velocity =
                    inward.scale(
                            0.16
                                    + power
                                            * 0.16
                    )
                    .add(
                            tangent
                    );

            level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    source.x,
                    source.y,
                    source.z,
                    0,
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    1.0
            );
        }
    }

    private static void renderBlue(
            ServerLevel level,
            Vec3 center,
            double size,
            float power,
            double spinDirection,
            long tick
    ) {
        for (int layer = 0;
                layer < 3;
                layer++) {

            double layerSize =
                    size
                            * (
                                    1.0
                                            + layer
                                                    * 0.46
                            );

            double rotation =
                    tick
                            * (
                                    0.075
                                            + layer
                                                    * 0.018
                            )
                            * spinDirection
                            + layer
                                    * 0.72;

            float phase =
                    (float) (
                            tick
                                    * 0.13
                                    + layer
                                            * 1.7
                    );

            float red =
                    Mth.clamp(
                            0.04F
                                    + 0.09F
                                            * (
                                                    0.5F
                                                            + 0.5F
                                                                    * Mth.sin(
                                                                            phase
                                                                    )
                                            ),
                            0.0F,
                            1.0F
                    );

            float green =
                    Mth.clamp(
                            0.28F
                                    + 0.48F
                                            * (
                                                    0.5F
                                                            + 0.5F
                                                                    * Mth.sin(
                                                                            phase
                                                                                    + 1.4F
                                                                    )
                                            ),
                            0.0F,
                            1.0F
                    );

            float blue =
                    Mth.clamp(
                            0.78F
                                    + 0.22F
                                            * (
                                                    0.5F
                                                            + 0.5F
                                                                    * Mth.sin(
                                                                            phase
                                                                                    + 2.8F
                                                                    )
                                            ),
                            0.0F,
                            1.0F
                    );

            DustParticleOptions dust =
                    new DustParticleOptions(
                            new Vector3f(
                                    red,
                                    green,
                                    blue
                            ),
                            0.82F
                                    + layer
                                            * 0.12F
                    );

            int points =
                    18
                            + layer
                                    * 6;

            for (int i = 0;
                    i < points;
                    i++) {

                double t =
                        (
                                i
                                        % 6
                        )
                                / 5.0
                                * 2.0
                                - 1.0;

                int edge =
                        (
                                i
                                        / 6
                                + layer
                        )
                                % 3;

                double sx =
                        (
                                (
                                        i
                                                & 1
                                )
                                        == 0
                        )
                                ? -1.0
                                : 1.0;

                double sy =
                        (
                                (
                                        i
                                                & 2
                                )
                                        == 0
                        )
                                ? -1.0
                                : 1.0;

                double sz =
                        (
                                (
                                        i
                                                & 4
                                )
                                        == 0
                        )
                                ? -1.0
                                : 1.0;

                Vec3 local =
                        switch (edge) {
                            case 0 ->
                                    new Vec3(
                                            t,
                                            sy,
                                            sz
                                    );
                            case 1 ->
                                    new Vec3(
                                            sx,
                                            t,
                                            sz
                                    );
                            default ->
                                    new Vec3(
                                            sx,
                                            sy,
                                            t
                                    );
                        };

                local =
                        local.scale(
                                layerSize
                        );

                double cos =
                        Math.cos(
                                rotation
                        );

                double sin =
                        Math.sin(
                                rotation
                        );

                Vec3 rotated =
                        new Vec3(
                                local.x
                                        * cos
                                        - local.z
                                                * sin,
                                local.y
                                        + Math.sin(
                                                rotation
                                                        * 0.7
                                                        + local.x
                                        )
                                                * 0.08
                                                * layer,
                                local.x
                                        * sin
                                        + local.z
                                                * cos
                        );

                Vec3 point =
                        center.add(
                                rotated
                        );

                level.sendParticles(
                        dust,
                        point.x,
                        point.y,
                        point.z,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0
                );
            }
        }

        level.sendParticles(
                ParticleTypes.PORTAL,
                center.x,
                center.y,
                center.z,
                6
                        + Math.round(
                                power
                                        * 8.0F
                        ),
                size
                        * 0.32,
                size
                        * 0.32,
                size
                        * 0.32,
                0.34
        );
    }

    private static void environmentTrail(
            ServerLevel level,
            Vec3 center,
            float power,
            boolean launched
    ) {
        BlockPos pos =
                BlockPos.containing(
                        center
                );

        if (level.getFluidState(
                pos
        ).is(
                FluidTags.WATER
        )) {
            waterBurst(
                    level,
                    center,
                    power
                            + (
                                    launched
                                            ? 0.55F
                                            : 0.15F
                            )
            );

            return;
        }

        int amount =
                launched
                        ? 7
                        : 3;

        level.sendParticles(
                ParticleTypes.CLOUD,
                center.x,
                center.y,
                center.z,
                amount,
                0.35
                        + power
                                * 0.4,
                0.24
                        + power
                                * 0.25,
                0.35
                        + power
                                * 0.4,
                0.018
        );

        if (launched) {
            level.sendParticles(
                    ParticleTypes.SMOKE,
                    center.x,
                    center.y,
                    center.z,
                    6,
                    0.38,
                    0.30,
                    0.38,
                    0.025
            );
        }
    }

    private static void waterBurst(
            ServerLevel level,
            Vec3 center,
            float power
    ) {
        int amount =
                12
                        + Math.round(
                                power
                                        * 22.0F
                        );

        level.sendParticles(
                ParticleTypes.SPLASH,
                center.x,
                center.y,
                center.z,
                amount,
                1.0
                        + power
                                * 0.9,
                0.7
                        + power
                                * 0.5,
                1.0
                        + power
                                * 0.9,
                0.18
        );

        level.sendParticles(
                ParticleTypes.BUBBLE,
                center.x,
                center.y,
                center.z,
                amount,
                0.8,
                0.8,
                0.8,
                0.24
        );
    }

    private static void scatterRelease(
            ServerLevel level,
            Player owner,
            Vec3 center,
            double radius,
            double spinDirection,
            float power
    ) {
        AABB box =
                new AABB(
                        center.x - radius,
                        center.y - radius,
                        center.z - radius,
                        center.x + radius,
                        center.y + radius,
                        center.z + radius
                );

        for (Entity entity :
                level.getEntities(
                        owner,
                        box,
                        other ->
                                other.isAlive()
                                        && other != owner
                                        && !other.isSpectator()
                )) {

            Vec3 relative =
                    entity.getBoundingBox()
                            .getCenter()
                            .subtract(
                                    center
                            );

            if (relative.lengthSqr()
                    > radius
                            * radius) {
                continue;
            }

            Vec3 radial =
                    relative.lengthSqr()
                                    < 0.001
                            ? new Vec3(
                                    level.random.nextDouble()
                                            - 0.5,
                                    0.25,
                                    level.random.nextDouble()
                                            - 0.5
                            )
                            .normalize()
                            : relative.normalize();

            Vec3 tangent =
                    new Vec3(
                            -radial.z,
                            0.0,
                            radial.x
                    )
                    .scale(
                            spinDirection
                    );

            Vec3 random =
                    new Vec3(
                            (
                                    level.random.nextDouble()
                                            - 0.5
                            )
                                    * 0.72,
                            level.random.nextDouble()
                                    * 0.62,
                            (
                                    level.random.nextDouble()
                                            - 0.5
                            )
                                    * 0.72
                    );

            Vec3 launch =
                    radial.scale(
                            0.95
                                    + level.random.nextDouble()
                                            * 0.85
                    )
                    .add(
                            tangent.scale(
                                    0.7
                                            + level.random.nextDouble()
                                                    * 0.8
                            )
                    )
                    .add(
                            random
                    )
                    .add(
                            0.0,
                            0.32
                                    + level.random.nextDouble()
                                            * 0.5,
                            0.0
                    )
                    .scale(
                            power
                    );

            entity.setDeltaMovement(
                    launch
            );

            entity.hurtMarked = true;
        }

        burst(
                level,
                center,
                74,
                1.35F
        );

        level.playSound(
                null,
                BlockPos.containing(
                        center
                ),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                1.0F,
                1.45F
        );
    }

    private static void implosionRelease(
            ServerLevel level,
            Player owner,
            Vec3 center,
            double spinDirection
    ) {
        applyBlue(
                level,
                owner,
                center,
                7.0,
                1.25F,
                spinDirection,
                10,
                true
        );

        burst(
                level,
                center,
                58,
                1.0F
        );
    }

    private static void collapseLaunched(
            ServerLevel level,
            Player owner,
            LaunchedBlue blue
    ) {
        AABB box =
                new AABB(
                        blue.position.x - 3.6,
                        blue.position.y - 3.6,
                        blue.position.z - 3.6,
                        blue.position.x + 3.6,
                        blue.position.y + 3.6,
                        blue.position.z + 3.6
                );

        for (Entity entity :
                level.getEntities(
                        owner,
                        box,
                        other ->
                                other.isAlive()
                                        && other != owner
                                        && !other.isSpectator()
                )) {
            eraseEntity(
                    level,
                    entity
            );
        }

        burst(
                level,
                blue.position,
                82,
                1.55F
        );

        level.playSound(
                null,
                BlockPos.containing(
                        blue.position
                ),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.35F,
                0.38F
        );
    }

    private static void burst(
            ServerLevel level,
            Vec3 center,
            int count,
            float scale
    ) {
        DustParticleOptions dust =
                new DustParticleOptions(
                        new Vector3f(
                                0.05F,
                                0.52F,
                                1.0F
                        ),
                        1.35F
                );

        level.sendParticles(
                dust,
                center.x,
                center.y,
                center.z,
                count,
                1.0
                        * scale,
                1.0
                        * scale,
                1.0
                        * scale,
                0.26
        );

        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                center.x,
                center.y,
                center.z,
                Math.max(
                        12,
                        count
                                / 2
                ),
                1.5
                        * scale,
                1.5
                        * scale,
                1.5
                        * scale,
                0.48
        );
    }

    private static final class HeldBlue {

        private final long startedAt;
        private final double spinDirection;

        private double distance = 7.0;
        private int chargeTicks;

        private Vec3 lastCenter = Vec3.ZERO;
        private Vec3 lastLook = new Vec3(0.0, 0.0, 1.0);

        private double lastScrollDelta;
        private long lastScrollTick = Long.MIN_VALUE;

        private boolean pulledBack;
        private long pulledBackTick = Long.MIN_VALUE;

        private boolean slingReady;
        private long slingReadyTick = Long.MIN_VALUE;

        private HeldBlue(
                long startedAt,
                double spinDirection
        ) {
            this.startedAt = startedAt;
            this.spinDirection = spinDirection;
        }
    }

    private static final class LaunchedBlue {

        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final float initialPower;
        private final double spinDirection;
        private final int maxLife;

        private Vec3 position;
        private Vec3 velocity;
        private int life;

        private LaunchedBlue(
                UUID owner,
                net.minecraft.resources.ResourceKey<Level> dimension,
                Vec3 position,
                Vec3 velocity,
                float initialPower,
                double spinDirection,
                int life
        ) {
            this.owner = owner;
            this.dimension = dimension;
            this.position = position;
            this.velocity = velocity;
            this.initialPower = initialPower;
            this.spinDirection = spinDirection;
            this.life = life;
            this.maxLife = life;
        }
    }
}
