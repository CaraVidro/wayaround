package net.caravidro.wayaround.blue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.network.BlueVisualPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-authoritative Blue ability.
 *
 * Flow:
 *  1. hold use -> charge only;
 *  2. release -> summon a controllable Blue;
 *  3. mouse wheel changes its distance;
 *  4. a fast inward/outward wheel gesture launches it;
 *  5. use the item again -> dismiss it into a shrinking smoky collapse.
 *
 * The Blue center has no collision. It can move through terrain while its
 * local destruction/pull field remains fully server-side.
 */
public final class BlueManager {

    private static final double MIN_DISTANCE = 2.0;
    private static final double MAX_DISTANCE = 48.0;
    private static final double SCROLL_STEP = 2.5;

    private static final int MAX_ACTIVE_TICKS = 55 * 20;
    private static final int RELEASE_TICKS = 60;
    private static final int LAUNCHED_TICKS = 20 * 20;
    private static final int COOLDOWN_TICKS = 5 * 20;
    private static final int COLLAPSE_ENTITY_TICKS = 16;

    private static final double CORE_COLLAPSE_RADIUS = 2.65;
    private static final double VISUAL_RANGE = 96.0;

    private static final Map<UUID, ChargeState> CHARGING =
            new HashMap<>();

    private static final Map<UUID, ActiveBlue> ACTIVE =
            new HashMap<>();

    private static final List<LaunchedBlue> LAUNCHED =
            new ArrayList<>();

    private static final List<ReleasingBlue> RELEASING =
            new ArrayList<>();

    private static final List<CollapsingEntity> COLLAPSING =
            new ArrayList<>();

    private static final Map<UUID, Long> COOLDOWN =
            new HashMap<>();

    private BlueManager() {
    }

    public static boolean beginCharge(
            ServerPlayer player
    ) {
        long tick =
                player.server.getTickCount();

        if (isBusy(
                player.getUUID()
        )) {
            return false;
        }

        long ready =
                COOLDOWN.getOrDefault(
                        player.getUUID(),
                        0L
                );

        if (tick < ready) {
            return false;
        }

        CHARGING.put(
                player.getUUID(),
                new ChargeState(
                        tick
                )
        );

        player.swing(
                InteractionHand.MAIN_HAND,
                true
        );

        return true;
    }

    public static void tickCharge(
            ServerPlayer player
    ) {
        ChargeState charge =
                CHARGING.get(
                        player.getUUID()
                );

        if (charge == null) {
            return;
        }

        charge.ticks++;

        /*
         * Vanilla arm swings are intentionally used instead of a bespoke
         * player animation system for this prototype. With the item model
         * invisible, alternating swings read like hand control gestures.
         */
        if (charge.ticks % 18 == 0) {
            player.swing(
                    (charge.ticks / 18) % 2 == 0
                            ? InteractionHand.MAIN_HAND
                            : InteractionHand.OFF_HAND,
                    true
            );
        }
    }

    public static void finishCharge(
            ServerPlayer player
    ) {
        ChargeState charge =
                CHARGING.remove(
                        player.getUUID()
                );

        if (charge == null
                || isBusy(
                        player.getUUID()
                )) {
            return;
        }

        float power =
                Mth.clamp(
                        0.35F
                                + charge.ticks
                                        / 58.0F,
                        0.42F,
                        1.35F
                );

        double distance =
                8.0;

        Vec3 look =
                player.getLookAngle()
                        .normalize();

        Vec3 center =
                player.getEyePosition()
                        .add(
                                look.scale(
                                        distance
                                )
                        );

        ActiveBlue blue =
                new ActiveBlue(
                        player.getUUID(),
                        player.serverLevel()
                                .dimension(),
                        center,
                        look,
                        distance,
                        power,
                        player.serverLevel()
                                .random
                                .nextBoolean()
                                ? 1.0
                                : -1.0,
                        player.server.getTickCount()
                );

        ACTIVE.put(
                player.getUUID(),
                blue
        );

        player.swing(
                InteractionHand.OFF_HAND,
                true
        );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.PLAYERS,
                        0.65F,
                        0.48F
                );

        burst(
                player.serverLevel(),
                center,
                52,
                1.15F
        );

        sendVisual(
                player.serverLevel(),
                blue.owner,
                center,
                power,
                attractionRadius(
                        power
                ),
                BlueVisualPayload.ACTIVE
        );
    }

    public static boolean hasControllableBlue(
            ServerPlayer player
    ) {
        return ACTIVE.containsKey(
                player.getUUID()
        );
    }

    public static boolean releaseActive(
            ServerPlayer player
    ) {
        ActiveBlue blue =
                ACTIVE.remove(
                        player.getUUID()
                );

        if (blue == null) {
            return false;
        }

        beginRelease(
                player.serverLevel(),
                blue.owner,
                blue.center,
                blue.power,
                blue.spinDirection
        );

        player.swing(
                InteractionHand.MAIN_HAND,
                true
        );

        player.swing(
                InteractionHand.OFF_HAND,
                true
        );

        return true;
    }

    public static void scroll(
            ServerPlayer player,
            double amount
    ) {
        if (!Double.isFinite(
                amount
        )
                || Math.abs(
                        amount
                ) > 16.0) {
            return;
        }

        ActiveBlue blue =
                ACTIVE.get(
                        player.getUUID()
                );

        if (blue == null) {
            return;
        }

        long tick =
                player.server.getTickCount();

        double previous =
                blue.distance;

        blue.distance =
                Mth.clamp(
                        blue.distance
                                + amount
                                        * SCROLL_STEP,
                        MIN_DISTANCE,
                        MAX_DISTANCE
                );

        double delta =
                blue.distance
                        - previous;

        if (delta < -0.1) {
            blue.inwardWheel +=
                    Math.abs(
                            amount
                    );

            blue.lastInwardTick =
                    tick;

            if (tick - blue.lastOutwardTick > 5L) {
                blue.outwardWheel = 0.0;
            }
        } else if (delta > 0.1) {
            if (tick - blue.lastInwardTick <= 12L) {
                blue.outwardWheel +=
                        Math.abs(
                                amount
                        );
            } else {
                blue.inwardWheel = 0.0;
                blue.outwardWheel = 0.0;
            }

            blue.lastOutwardTick =
                    tick;
        }

        /*
         * A quick inward -> outward snap becomes the slingshot gesture.
         * It launches immediately; no extra release click is needed.
         */
        if (tick - blue.lastInwardTick <= 12L
                && blue.inwardWheel >= 2.0
                && blue.outwardWheel >= 3.0
                && blue.distance >= 10.0) {
            launchActive(
                    player,
                    blue
            );
        }
    }

    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        cleanupCharges(server);
        tickCollapsing(server);

        Iterator<Map.Entry<UUID, ActiveBlue>> activeIterator =
                ACTIVE.entrySet()
                        .iterator();

        while (activeIterator.hasNext()) {
            ActiveBlue blue =
                    activeIterator.next()
                            .getValue();

            ServerPlayer owner =
                    server.getPlayerList()
                            .getPlayer(
                                    blue.owner
                            );

            ServerLevel level =
                    server.getLevel(
                            blue.dimension
                    );

            if (owner == null
                    || level == null
                    || !owner.isAlive()
                    || owner.serverLevel() != level) {
                if (level != null) {
                    beginRelease(
                            level,
                            blue.owner,
                            blue.center,
                            blue.power,
                            blue.spinDirection
                    );
                }

                activeIterator.remove();
                continue;
            }

            if (tick - blue.startedAt
                    >= MAX_ACTIVE_TICKS) {
                beginRelease(
                        level,
                        blue.owner,
                        blue.center,
                        blue.power,
                        blue.spinDirection
                );

                owner.swing(
                        InteractionHand.MAIN_HAND,
                        true
                );

                activeIterator.remove();
                continue;
            }

            blue.look =
                    owner.getLookAngle()
                            .normalize();

            blue.center =
                    owner.getEyePosition()
                            .add(
                                    blue.look.scale(
                                            blue.distance
                                    )
                            );

            double radius =
                    attractionRadius(
                            blue.power
                    );

            applyBlue(
                    level,
                    owner,
                    blue.center,
                    radius,
                    blue.power,
                    blue.spinDirection,
                    6
                            + Math.round(
                                    blue.power
                                            * 7.0F
                            )
            );

            disturbEnvironment(
                    level,
                    blue.center,
                    blue.power,
                    tick
            );

            environmentTrail(
                    level,
                    blue.center,
                    blue.power,
                    false,
                    false
            );

            if (tick % 22L == 0L) {
                owner.swing(
                        (tick / 22L) % 2L == 0L
                                ? InteractionHand.MAIN_HAND
                                : InteractionHand.OFF_HAND,
                        true
                );
            }

            sendVisual(
                    level,
                    blue.owner,
                    blue.center,
                    blue.power,
                    radius,
                    BlueVisualPayload.ACTIVE
            );
        }

        Iterator<LaunchedBlue> launchedIterator =
                LAUNCHED.iterator();

        while (launchedIterator.hasNext()) {
            LaunchedBlue blue =
                    launchedIterator.next();

            ServerLevel level =
                    server.getLevel(
                            blue.dimension
                    );

            if (level == null) {
                launchedIterator.remove();
                continue;
            }

            blue.life--;

            if (blue.life <= 0) {
                queueCoreCollapse(
                        level,
                        server.getPlayerList()
                                .getPlayer(
                                        blue.owner
                                ),
                        blue.position,
                        3.4
                );

                smokeCollapse(
                        level,
                        blue.position,
                        blue.power
                );

                finishAbility(
                        server,
                        blue.owner
                );

                launchedIterator.remove();
                continue;
            }

            blue.position =
                    blue.position.add(
                            blue.velocity
                    );

            blue.velocity =
                    blue.velocity.scale(
                            0.997
                    );

            float fade =
                    Mth.clamp(
                            blue.life
                                    / (float) blue.maxLife,
                            0.0F,
                            1.0F
                    );

            float power =
                    Math.max(
                            0.16F,
                            blue.power
                                    * (
                                            0.30F
                                                    + fade
                                                            * 0.70F
                                    )
                    );

            double radius =
                    attractionRadius(
                            power
                    );

            if (level.hasChunkAt(
                    BlockPos.containing(
                            blue.position
                    )
            )) {
                applyBlue(
                        level,
                        server.getPlayerList()
                                .getPlayer(
                                        blue.owner
                                ),
                        blue.position,
                        radius,
                        power,
                        blue.spinDirection,
                        Math.max(
                                1,
                                Math.round(
                                        8.0F
                                                * power
                                )
                        )
                );

                disturbEnvironment(
                        level,
                        blue.position,
                        power,
                        tick
                );

                environmentTrail(
                        level,
                        blue.position,
                        power,
                        true,
                        false
                );
            }

            sendVisual(
                    level,
                    blue.owner,
                    blue.position,
                    power,
                    radius,
                    BlueVisualPayload.LAUNCHED
            );
        }

        Iterator<ReleasingBlue> releaseIterator =
                RELEASING.iterator();

        while (releaseIterator.hasNext()) {
            ReleasingBlue blue =
                    releaseIterator.next();

            ServerLevel level =
                    server.getLevel(
                            blue.dimension
                    );

            if (level == null) {
                releaseIterator.remove();
                continue;
            }

            blue.life--;

            float fade =
                    Mth.clamp(
                            blue.life
                                    / (float) blue.maxLife,
                            0.0F,
                            1.0F
                    );

            float visualPower =
                    Math.max(
                            0.05F,
                            blue.power
                                    * fade
                    );

            environmentTrail(
                    level,
                    blue.position,
                    Math.max(
                            0.7F,
                            blue.power
                    ),
                    false,
                    true
            );

            if (blue.life % 4 == 0) {
                drawIncomingMatter(
                        level,
                        blue.position,
                        4.0
                                + visualPower
                                        * 4.0,
                        visualPower,
                        blue.spinDirection
                );
            }

            sendVisual(
                    level,
                    blue.owner,
                    blue.position,
                    visualPower,
                    4.0
                            + visualPower
                                    * 6.0,
                    BlueVisualPayload.COLLAPSING
            );

            if (blue.life <= 0) {
                smokeCollapse(
                        level,
                        blue.position,
                        blue.power
                );

                finishAbility(
                        server,
                        blue.owner
                );

                releaseIterator.remove();
            }
        }
    }

    public static void clearAll() {
        CHARGING.clear();
        ACTIVE.clear();
        LAUNCHED.clear();
        RELEASING.clear();
        COLLAPSING.clear();
        COOLDOWN.clear();
    }

    private static boolean isBusy(
            UUID owner
    ) {
        if (ACTIVE.containsKey(
                owner
        )) {
            return true;
        }

        for (LaunchedBlue blue : LAUNCHED) {
            if (blue.owner.equals(
                    owner
            )) {
                return true;
            }
        }

        for (ReleasingBlue blue : RELEASING) {
            if (blue.owner.equals(
                    owner
            )) {
                return true;
            }
        }

        return false;
    }

    private static void cleanupCharges(
            MinecraftServer server
    ) {
        Iterator<Map.Entry<UUID, ChargeState>> iterator =
                CHARGING.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ChargeState> entry =
                    iterator.next();

            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(
                                    entry.getKey()
                            );

            if (player == null
                    || !player.isAlive()) {
                iterator.remove();
            }
        }
    }

    private static void launchActive(
            ServerPlayer player,
            ActiveBlue blue
    ) {
        if (ACTIVE.remove(
                player.getUUID()
        ) == null) {
            return;
        }

        Vec3 direction =
                blue.look.lengthSqr() < 0.001
                        ? player.getLookAngle()
                                .normalize()
                        : blue.look
                                .normalize();

        LAUNCHED.add(
                new LaunchedBlue(
                        blue.owner,
                        blue.dimension,
                        blue.center,
                        direction.scale(
                                0.47
                        ),
                        blue.power,
                        blue.spinDirection,
                        LAUNCHED_TICKS
                )
        );

        player.swing(
                InteractionHand.MAIN_HAND,
                true
        );

        player.swing(
                InteractionHand.OFF_HAND,
                true
        );

        player.serverLevel()
                .playSound(
                        null,
                        BlockPos.containing(
                                blue.center
                        ),
                        SoundEvents.END_PORTAL_SPAWN,
                        SoundSource.PLAYERS,
                        1.15F,
                        1.6F
                );

        burst(
                player.serverLevel(),
                blue.center,
                62,
                1.30F
        );
    }

    private static void beginRelease(
            ServerLevel level,
            UUID owner,
            Vec3 center,
            float power,
            double spinDirection
    ) {
        queueCoreCollapse(
                level,
                level.getServer()
                        .getPlayerList()
                        .getPlayer(
                                owner
                        ),
                center,
                CORE_COLLAPSE_RADIUS
        );

        RELEASING.add(
                new ReleasingBlue(
                        owner,
                        level.dimension(),
                        center,
                        power,
                        spinDirection,
                        RELEASE_TICKS
                )
        );

        burst(
                level,
                center,
                52,
                1.0F
        );
    }

    private static void finishAbility(
            MinecraftServer server,
            UUID owner
    ) {
        COOLDOWN.put(
                owner,
                (long) server.getTickCount()
                        + COOLDOWN_TICKS
        );
    }

    private static double attractionRadius(
            float power
    ) {
        return 28.0
                + power
                        * 32.0;
    }

    private static void applyBlue(
            ServerLevel level,
            Player owner,
            Vec3 center,
            double radius,
            float power,
            double spinDirection,
            int blockBudget
    ) {
        pullEntities(
                level,
                owner,
                center,
                radius,
                power,
                spinDirection
        );

        consumeBlocks(
                level,
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
            double spinDirection
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
            Vec3 body =
                    entity.getBoundingBox()
                            .getCenter();

            Vec3 toCenter =
                    center.subtract(
                            body
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

            Vec3 inward =
                    toCenter.scale(
                            1.0
                                    / distance
                    );

            Vec3 tangent =
                    new Vec3(
                            -inward.z,
                            0.10
                                    * Math.sin(
                                            entity.getId()
                                                    * 0.73
                                                    + level.getGameTime()
                                                            * 0.17
                                    ),
                            inward.x
                    )
                    .normalize()
                    .scale(
                            spinDirection
                    );

            double proximity =
                    Mth.clamp(
                            1.0
                                    - distance
                                            / radius,
                            0.0,
                            1.0
                    );

            /*
             * Wider reach than the first prototype, but deliberately softer
             * acceleration. Far-away entities feel the field long before
             * they are violently captured.
             */
            double pull =
                    0.012
                            + power
                                    * (
                                            0.018
                                                    + proximity
                                                            * proximity
                                                            * 0.095
                                    );

            double orbit =
                    0.010
                            + power
                                    * (
                                            0.018
                                                    + proximity
                                                            * 0.075
                                    );

            if (distance < 2.1) {
                /*
                 * Do not let the living target sit at mathematical zero.
                 * A tiny outward pressure plus strong tangent motion creates
                 * the visible "trapped orbit" until release.
                 */
                pull =
                        -0.018;

                orbit *=
                        1.65;
            }

            Vec3 next =
                    entity.getDeltaMovement()
                            .scale(
                                    distance < 4.0
                                            ? 0.80
                                            : 0.91
                            )
                            .add(
                                    inward.scale(
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

    private static void consumeBlocks(
            ServerLevel level,
            Vec3 center,
            float power,
            int blockBudget
    ) {
        if (blockBudget <= 0) {
            return;
        }

        double digRadius =
                3.4
                        + power
                                * 4.6;

        int removed =
                0;

        int attempts =
                blockBudget
                        * 6;

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
                                0.45F,
                                power
                        )
                );
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

            if (velocity.lengthSqr() > 0.0001) {
                velocity =
                        velocity.normalize()
                                .scale(
                                        0.20
                                                + power
                                                        * 0.24
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

            /*
             * Destruction now throws a real dust cloud, not just block chips.
             * Most of it hangs/rises, while a smaller share is visibly pulled
             * toward Blue so the smoke participates in the gravity field.
             */
            level.sendParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    source.x,
                    source.y,
                    source.z,
                    2,
                    0.45,
                    0.22,
                    0.45,
                    0.008
            );

            if (level.random.nextFloat()
                    < 0.48F) {
                Vec3 smokeInward =
                        center.subtract(
                                source
                        );

                if (smokeInward.lengthSqr()
                        > 0.001) {
                    smokeInward =
                            smokeInward.normalize();

                    level.sendParticles(
                            ParticleTypes.LARGE_SMOKE,
                            source.x,
                            source.y,
                            source.z,
                            0,
                            smokeInward.x,
                            smokeInward.y
                                    * 0.72,
                            smokeInward.z,
                            0.22
                                    + power
                                            * 0.08
                    );
                }
            }
        }
    }

    private static void disturbEnvironment(
            ServerLevel level,
            Vec3 center,
            float power,
            long tick
    ) {
        if (tick % 4L == 0L) {
            int attempts =
                    96
                            + Math.round(
                                    power
                                            * 56.0F
                            );

            for (int i = 0;
                    i < attempts;
                    i++) {

                double radius =
                        72.0;

                double dx =
                        (
                                level.random.nextDouble()
                                        * 2.0
                                - 1.0
                        )
                                * radius;

                double dy =
                        (
                                level.random.nextDouble()
                                        * 2.0
                                - 1.0
                        )
                                * 24.0;

                double dz =
                        (
                                level.random.nextDouble()
                                        * 2.0
                                - 1.0
                        )
                                * radius;

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

                double distance =
                        Vec3.atCenterOf(
                                pos
                        )
                                .distanceTo(
                                        center
                                );

                if (distance > radius) {
                    continue;
                }

                String blockPath =
                        BuiltInRegistries.BLOCK
                                .getKey(
                                        state.getBlock()
                                )
                                .getPath();

                if (blockPath.contains(
                        "glass"
                )) {
                    double chance =
                            0.10
                                    + (
                                            1.0
                                                    - distance
                                                            / radius
                                    )
                                            * 0.62;

                    if (level.random.nextDouble()
                            < chance) {
                        level.levelEvent(
                                2001,
                                pos,
                                Block.getId(
                                        state
                                )
                        );

                        level.removeBlock(
                                pos,
                                false
                        );
                    }

                    continue;
                }

                if (isTorch(
                        state
                )
                        && distance <= 52.0
                        && level.random.nextDouble()
                                < 0.22
                                        + (
                                                1.0
                                                        - distance
                                                                / 52.0
                                        )
                                                * 0.68) {
                    level.destroyBlock(
                            pos,
                            true
                    );
                }
            }
        }

        if (tick % 10L == 0L) {
            double fearRadius =
                    60.0;

            AABB fearBox =
                    new AABB(
                            center.x - fearRadius,
                            center.y - 20.0,
                            center.z - fearRadius,
                            center.x + fearRadius,
                            center.y + 20.0,
                            center.z + fearRadius
                    );

            for (Animal animal :
                    level.getEntitiesOfClass(
                            Animal.class,
                            fearBox,
                            animal ->
                                    animal.isAlive()
                    )) {

                Vec3 away =
                        animal.position()
                                .subtract(
                                        center
                                );

                double distance =
                        away.length();

                if (distance < 0.001
                        || distance > fearRadius) {
                    continue;
                }

                away =
                        away.scale(
                                1.0
                                        / distance
                        );

                double panic =
                        0.06
                                + (
                                        1.0
                                                - distance
                                                        / fearRadius
                                )
                                        * 0.24;

                animal.setDeltaMovement(
                        animal.getDeltaMovement()
                                .add(
                                        away.x
                                                * panic,
                                        0.06,
                                        away.z
                                                * panic
                                )
                );

                animal.getNavigation()
                        .moveTo(
                                animal.getX()
                                        + away.x
                                                * 14.0,
                                animal.getY(),
                                animal.getZ()
                                        + away.z
                                                * 14.0,
                                1.35
                                        + power
                                                * 0.20
                        );
            }
        }
    }

    private static boolean isTorch(
            BlockState state
    ) {
        return state.is(
                Blocks.TORCH
        )
                || state.is(
                        Blocks.WALL_TORCH
                )
                || state.is(
                        Blocks.SOUL_TORCH
                )
                || state.is(
                        Blocks.SOUL_WALL_TORCH
                )
                || state.is(
                        Blocks.REDSTONE_TORCH
                )
                || state.is(
                        Blocks.REDSTONE_WALL_TORCH
                );
    }

    private static void drawIncomingMatter(
            ServerLevel level,
            Vec3 center,
            double radius,
            float power,
            double spinDirection
    ) {
        int count =
                18
                        + Math.round(
                                power
                                        * 28.0F
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
                                    0.28
                                            + level.random.nextDouble()
                                                    * 0.72
                            );

            double y =
                    (
                            level.random.nextDouble()
                                    - 0.5
                    )
                            * Math.min(
                                    radius
                                            * 0.72,
                                    18.0
                            );

            Vec3 source =
                    center.add(
                            Math.cos(
                                    angle
                            )
                                    * distance,
                            y,
                            Math.sin(
                                    angle
                            )
                                    * distance
                    );

            Vec3 inward =
                    center.subtract(
                            source
                    );

            if (inward.lengthSqr() < 0.001) {
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
                                            * 0.11
                            );

            Vec3 velocity =
                    inward.scale(
                            0.13
                                    + power
                                            * 0.14
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

        level.sendParticles(
                ParticleTypes.PORTAL,
                center.x,
                center.y,
                center.z,
                12
                        + Math.round(
                                power
                                        * 14.0F
                        ),
                1.25
                        + power
                                * 1.10,
                1.25
                        + power
                                * 1.10,
                1.25
                        + power
                                * 1.10,
                0.34
        );
    }

    private static void environmentTrail(
            ServerLevel level,
            Vec3 center,
            float power,
            boolean launched,
            boolean releasing
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
                                            ? 0.75F
                                            : releasing
                                                    ? 1.05F
                                                    : 0.35F
                            )
            );

            return;
        }

        double spread =
                releasing
                        ? 5.8
                        : launched
                                ? 4.8
                                : 3.8;

        int cosy =
                releasing
                        ? 18
                        : launched
                                ? 10
                                : 6;

        int signal =
                releasing
                        ? 8
                        : launched
                                ? 4
                                : 2;

        int large =
                releasing
                        ? 24
                        : launched
                                ? 14
                                : 8;

        /*
         * This is meant to read as the dust of the surrounding destruction.
         * Signal smoke gives the trail very long-lived columns; cosy and large
         * smoke fill the lower volume so looking back reveals a wall of haze.
         */
        level.sendParticles(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                center.x,
                center.y,
                center.z,
                cosy,
                spread,
                1.2
                        + power
                                * 0.8,
                spread,
                0.006
        );

        level.sendParticles(
                ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                center.x,
                center.y,
                center.z,
                signal,
                spread
                        * 0.78,
                0.9
                        + power
                                * 0.6,
                spread
                        * 0.78,
                0.004
        );

        level.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                center.x,
                center.y,
                center.z,
                large,
                spread
                        * 0.86,
                0.75
                        + power
                                * 0.45,
                spread
                        * 0.86,
                0.014
        );

        /*
         * Only a fraction of nearby smoke is sucked inward. Most of the cloud
         * remains suspended as a persistent trail while these strands visibly
         * spiral/fall toward the Blue.
         */
        int sucked =
                3
                        + Math.round(
                                power
                                        * 4.0F
                        );

        for (int i = 0;
                i < sucked;
                i++) {
            double angle =
                    level.random.nextDouble()
                            * Math.PI
                            * 2.0;

            double distance =
                    3.5
                            + level.random.nextDouble()
                                    * (
                                            5.0
                                                    + power
                                                            * 4.0
                                    );

            Vec3 source =
                    center.add(
                            Math.cos(
                                    angle
                            )
                                    * distance,
                            (
                                    level.random.nextDouble()
                                            - 0.35
                            )
                                    * 4.5,
                            Math.sin(
                                    angle
                            )
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

            level.sendParticles(
                    i % 3 == 0
                            ? ParticleTypes.CAMPFIRE_COSY_SMOKE
                            : ParticleTypes.LARGE_SMOKE,
                    source.x,
                    source.y,
                    source.z,
                    0,
                    inward.x,
                    inward.y
                            * 0.65,
                    inward.z,
                    0.20
                            + power
                                    * 0.09
            );
        }
    }

    private static void waterBurst(
            ServerLevel level,
            Vec3 center,
            float power
    ) {
        int amount =
                26
                        + Math.round(
                                power
                                        * 42.0F
                        );

        level.sendParticles(
                ParticleTypes.SPLASH,
                center.x,
                center.y,
                center.z,
                amount,
                1.8
                        + power
                                * 1.35,
                1.2
                        + power
                                * 0.85,
                1.8
                        + power
                                * 1.35,
                0.24
        );

        level.sendParticles(
                ParticleTypes.BUBBLE,
                center.x,
                center.y,
                center.z,
                amount,
                0.9,
                0.9,
                0.9,
                0.28
        );
    }

    private static void queueCoreCollapse(
            ServerLevel level,
            Player owner,
            Vec3 center,
            double radius
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

        double radiusSquared =
                radius
                        * radius;

        for (Entity entity :
                level.getEntities(
                        owner,
                        box,
                        other ->
                                other.isAlive()
                                        && other != owner
                                        && !other.isSpectator()
                )) {

            if (entity.getBoundingBox()
                    .getCenter()
                    .distanceToSqr(
                            center
                    )
                    > radiusSquared) {
                continue;
            }

            boolean duplicate =
                    COLLAPSING.stream()
                            .anyMatch(
                                    collapse ->
                                            collapse.entityId.equals(
                                                    entity.getUUID()
                                            )
                            );

            if (duplicate) {
                continue;
            }

            double originalScale =
                    1.0;

            if (entity instanceof LivingEntity living) {
                var scale =
                        living.getAttribute(
                                Attributes.SCALE
                        );

                if (scale != null) {
                    originalScale =
                            scale.getBaseValue();
                }
            }

            COLLAPSING.add(
                    new CollapsingEntity(
                            entity.getUUID(),
                            level.dimension(),
                            center,
                            originalScale,
                            COLLAPSE_ENTITY_TICKS
                    )
            );
        }
    }

    private static void tickCollapsing(
            MinecraftServer server
    ) {
        Iterator<CollapsingEntity> iterator =
                COLLAPSING.iterator();

        while (iterator.hasNext()) {
            CollapsingEntity collapse =
                    iterator.next();

            ServerLevel level =
                    server.getLevel(
                            collapse.dimension
                    );

            if (level == null) {
                iterator.remove();
                continue;
            }

            Entity entity =
                    level.getEntity(
                            collapse.entityId
                    );

            if (entity == null
                    || !entity.isAlive()) {
                iterator.remove();
                continue;
            }

            collapse.age++;

            double progress =
                    Mth.clamp(
                            collapse.age
                                    / (double) collapse.duration,
                            0.0,
                            1.0
                    );

            Vec3 current =
                    entity.getBoundingBox()
                            .getCenter();

            Vec3 inward =
                    collapse.center.subtract(
                            current
                    );

            double angle =
                    server.getTickCount()
                            * 0.48
                            + entity.getId()
                                    * 0.71;

            Vec3 orbit =
                    new Vec3(
                            Math.cos(
                                    angle
                            ),
                            Math.sin(
                                    angle
                                            * 0.63
                            )
                                    * 0.16,
                            Math.sin(
                                    angle
                            )
                    )
                            .scale(
                                    (
                                            1.0
                                                    - progress
                                    )
                                            * 0.24
                            );

            entity.setDeltaMovement(
                    inward.scale(
                            0.22
                                    + progress
                                            * 0.42
                    )
                            .add(
                                    orbit
                            )
            );

            entity.hurtMarked =
                    true;

            if (entity instanceof LivingEntity living) {
                var scale =
                        living.getAttribute(
                                Attributes.SCALE
                        );

                if (scale != null) {
                    scale.setBaseValue(
                            Math.max(
                                    0.05,
                                    collapse.originalScale
                                            * (
                                                    1.0
                                                            - progress
                                                                    * 0.95
                                            )
                            )
                    );
                }
            }

            level.sendParticles(
                    ParticleTypes.PORTAL,
                    current.x,
                    current.y,
                    current.z,
                    4,
                    0.12,
                    0.12,
                    0.12,
                    0.08
            );

            if (collapse.age >= collapse.duration) {
                if (shieldSaves(
                        entity,
                        collapse
                )) {
                    iterator.remove();
                    continue;
                }

                eraseEntity(
                        level,
                        entity
                );

                iterator.remove();
            }
        }
    }

    private static boolean shieldSaves(
            Entity entity,
            CollapsingEntity collapse
    ) {
        if (!(entity instanceof LivingEntity living)
                || !living.isBlocking()) {
            return false;
        }

        ItemStack shield =
                living.getUseItem();

        if (!shield.is(
                Items.SHIELD
        )) {
            return false;
        }

        InteractionHand hand =
                living.getUsedItemHand();

        EquipmentSlot slot =
                hand == InteractionHand.OFF_HAND
                        ? EquipmentSlot.OFFHAND
                        : EquipmentSlot.MAINHAND;

        shield.hurtAndBreak(
                72,
                living,
                slot
        );

        var scale =
                living.getAttribute(
                        Attributes.SCALE
                );

        if (scale != null) {
            scale.setBaseValue(
                    collapse.originalScale
            );
        }

        Vec3 away =
                living.position()
                        .subtract(
                                collapse.center
                        );

        if (away.lengthSqr() < 0.001) {
            away =
                    new Vec3(
                            1.0,
                            0.0,
                            0.0
                    );
        } else {
            away =
                    away.normalize();
        }

        living.setDeltaMovement(
                away.scale(
                        1.05
                )
                        .add(
                                0.0,
                                0.28,
                                0.0
                        )
        );

        living.hurtMarked =
                true;

        return true;
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
                22,
                0.24,
                0.24,
                0.24,
                0.52
        );
    }

    private static void smokeCollapse(
            ServerLevel level,
            Vec3 center,
            float power
    ) {
        level.sendParticles(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                center.x,
                center.y,
                center.z,
                28,
                1.2
                        + power,
                0.65,
                1.2
                        + power,
                0.012
        );

        level.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                center.x,
                center.y,
                center.z,
                46,
                1.45
                        + power,
                0.8,
                1.45
                        + power,
                0.035
        );

        burst(
                level,
                center,
                58,
                1.2F
        );
    }

    private static void burst(
            ServerLevel level,
            Vec3 center,
            int count,
            float scale
    ) {
        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                center.x,
                center.y,
                center.z,
                count,
                1.25
                        * scale,
                1.25
                        * scale,
                1.25
                        * scale,
                0.46
        );

        level.sendParticles(
                ParticleTypes.PORTAL,
                center.x,
                center.y,
                center.z,
                Math.max(
                        12,
                        count / 2
                ),
                1.0
                        * scale,
                1.0
                        * scale,
                1.0
                        * scale,
                0.38
        );
    }

    private static void sendVisual(
            ServerLevel level,
            UUID owner,
            Vec3 center,
            float power,
            double radius,
            byte mode
    ) {
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.x,
                center.y,
                center.z,
                VISUAL_RANGE,
                new BlueVisualPayload(
                        owner,
                        center.x,
                        center.y,
                        center.z,
                        power,
                        (float) radius,
                        mode
                )
        );
    }

    private static final class ChargeState {

        private final long startedAt;
        private int ticks;

        private ChargeState(
                long startedAt
        ) {
            this.startedAt = startedAt;
        }
    }

    private static final class ActiveBlue {

        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final float power;
        private final double spinDirection;
        private final long startedAt;

        private Vec3 center;
        private Vec3 look;
        private double distance;

        private double inwardWheel;
        private double outwardWheel;
        private long lastInwardTick =
                Long.MIN_VALUE;
        private long lastOutwardTick =
                Long.MIN_VALUE;

        private ActiveBlue(
                UUID owner,
                net.minecraft.resources.ResourceKey<Level> dimension,
                Vec3 center,
                Vec3 look,
                double distance,
                float power,
                double spinDirection,
                long startedAt
        ) {
            this.owner = owner;
            this.dimension = dimension;
            this.center = center;
            this.look = look;
            this.distance = distance;
            this.power = power;
            this.spinDirection = spinDirection;
            this.startedAt = startedAt;
        }
    }

    private static final class LaunchedBlue {

        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final float power;
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
                float power,
                double spinDirection,
                int life
        ) {
            this.owner = owner;
            this.dimension = dimension;
            this.position = position;
            this.velocity = velocity;
            this.power = power;
            this.spinDirection = spinDirection;
            this.life = life;
            this.maxLife = life;
        }
    }

    private static final class ReleasingBlue {

        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final Vec3 position;
        private final float power;
        private final double spinDirection;
        private final int maxLife;

        private int life;

        private ReleasingBlue(
                UUID owner,
                net.minecraft.resources.ResourceKey<Level> dimension,
                Vec3 position,
                float power,
                double spinDirection,
                int life
        ) {
            this.owner = owner;
            this.dimension = dimension;
            this.position = position;
            this.power = power;
            this.spinDirection = spinDirection;
            this.life = life;
            this.maxLife = life;
        }
    }

    private static final class CollapsingEntity {

        private final UUID entityId;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final Vec3 center;
        private final double originalScale;
        private final int duration;

        private int age;

        private CollapsingEntity(
                UUID entityId,
                net.minecraft.resources.ResourceKey<Level> dimension,
                Vec3 center,
                double originalScale,
                int duration
        ) {
            this.entityId = entityId;
            this.dimension = dimension;
            this.center = center;
            this.originalScale = originalScale;
            this.duration = duration;
        }
    }
}
