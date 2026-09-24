package net.caravidro.wayaround.blue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.network.BlueGestureS2CPayload;
import net.caravidro.wayaround.network.BlueVisualPayload;
import net.caravidro.wayaround.particle.WayAroundParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
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
    private static final int RELEASE_TICKS = 120;
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

    private static final List<AccretionSmoke> ACCRETION_SMOKE =
            new ArrayList<>();

    private static final int MAX_ACCRETION_SMOKE =
            360;

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

        sendGesture(
                player,
                BlueGestureS2CPayload.SUMMON
        );

        return true;
    }

    public static boolean invokeFromVoice(
            ServerPlayer player
    ) {
        return invokeFromVoice(
                player,
                -1.0F
        );
    }

    public static boolean invokeFromVoice(
            ServerPlayer player,
            float output
    ) {
        return invokeFromVoice(
                player,
                output,
                0.42F
        );
    }

    public static boolean invokeFromVoice(
            ServerPlayer player,
            float output,
            float urgency
    ) {
        if (!beginCharge(
                player
        )) {
            return false;
        }

        ChargeState charge =
                CHARGING.get(
                        player.getUUID()
                );

        if (charge != null) {
            if (output >= 0.0F) {
                float normalized =
                        Mth.clamp(
                                output,
                                0.0F,
                                1.0F
                        );

                float targetPower =
                        Mth.lerp(
                                normalized,
                                0.04F,
                                1.35F
                        );

                charge.ticks =
                        Math.max(
                                1,
                                Math.round(
                                        (
                                                targetPower
                                                        - 0.025F
                                        )
                                                * 62.0F
                                )
                        );

            } else {
                charge.ticks =
                        Math.max(
                                charge.ticks,
                                18
                        );
            }
        }

        float normalizedUrgency =
                Mth.clamp(
                        urgency,
                        0.0F,
                        1.0F
                );

        int emergenceTicks =
                normalizedUrgency >= 0.72F
                        ? 2
                        : normalizedUrgency >= 0.52F
                                ? 9
                                : normalizedUrgency >= 0.34F
                                        ? 17
                                        : 30;

        finishCharge(
                player,
                emergenceTicks
        );

        return ACTIVE.containsKey(
                player.getUUID()
        );
    }

    public static boolean setActiveOutput(
            ServerPlayer player,
            float output
    ) {
        ActiveBlue blue =
                ACTIVE.get(
                        player.getUUID()
                );

        if (blue == null
                || !Float.isFinite(
                        output
                )) {

            return false;
        }

        float normalized =
                Mth.clamp(
                        output,
                        0.0F,
                        1.0F
                );

        blue.targetPower =
                Mth.lerp(
                        normalized,
                        0.04F,
                        1.35F
                );

        player.serverLevel()
                .playSound(
                        null,
                        BlockPos.containing(
                                blue.center
                        ),
                        SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.PLAYERS,
                        0.50F,
                        normalized >= 0.5F
                                ? 1.55F
                                : 0.72F
                );

        return true;
    }

    public static boolean holdActive(
            ServerPlayer player
    ) {
        ActiveBlue blue =
                ACTIVE.get(
                        player.getUUID()
                );

        if (blue == null) {
            return false;
        }

        blue.orbiting =
                false;

        blue.held =
                true;

        sendGesture(
                player,
                BlueGestureS2CPayload.HOLD
        );

        player.serverLevel()
                .playSound(
                        null,
                        BlockPos.containing(
                                blue.center
                        ),
                        SoundEvents.AMETHYST_BLOCK_RESONATE,
                        SoundSource.PLAYERS,
                        0.48F,
                        0.82F
                );

        return true;
    }

    public static boolean orbitActive(
            ServerPlayer player
    ) {
        ActiveBlue blue =
                ACTIVE.get(
                        player.getUUID()
                );

        if (blue == null) {
            return false;
        }

        Vec3 eye =
                player.getEyePosition();

        Vec3 relative =
                blue.center.subtract(
                        eye
                );

        blue.orbitAngle =
                Math.atan2(
                        relative.z,
                        relative.x
                );

        blue.held =
                false;

        blue.orbiting =
                true;

        sendGesture(
                player,
                BlueGestureS2CPayload.ORBIT
        );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.PLAYERS,
                        0.55F,
                        1.65F
                );

        return true;
    }

    public static boolean launchActive(
            ServerPlayer player
    ) {
        ActiveBlue blue =
                ACTIVE.get(
                        player.getUUID()
                );

        if (blue == null) {
            return false;
        }

        launchActive(
                player,
                blue
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
        finishCharge(
                player,
                0
        );
    }

    private static void finishCharge(
            ServerPlayer player,
            int emergenceTicks
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

        /*
         * A tap can create a genuinely tiny Blue. It still exerts gravity,
         * but destructive capacity climbs on a steep curve with charge.
         */
        float targetPower =
                Mth.clamp(
                        0.025F
                                + charge.ticks
                                        / 62.0F,
                        0.04F,
                        1.35F
                );

        double distance =
                8.0;

        Vec3 look =
                player.getLookAngle()
                        .normalize();

        int spawnTicks =
                Math.max(
                        0,
                        emergenceTicks
                );

        float initialPower =
                spawnTicks > 3
                        ? Math.max(
                        0.04F,
                        targetPower
                                * 0.08F
                )
                        : targetPower;

        double initialDistance =
                spawnTicks > 3
                        ? 0.72
                        : distance;

        Vec3 center =
                player.getEyePosition()
                        .add(
                                look.scale(
                                        initialDistance
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
                        initialPower,
                        targetPower,
                        spawnTicks,
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

        if (spawnTicks > 3) {
            player.serverLevel()
                    .playSound(
                            null,
                            player.blockPosition(),
                            SoundEvents.AMETHYST_BLOCK_RESONATE,
                            SoundSource.PLAYERS,
                            0.42F,
                            0.72F
                    );

            player.serverLevel()
                    .sendParticles(
                            ParticleTypes.PORTAL,
                            center.x,
                            center.y,
                            center.z,
                            24,
                            0.24,
                            0.34,
                            0.24,
                            0.025
                    );

        } else {
            player.serverLevel()
                    .playSound(
                            null,
                            player.blockPosition(),
                            SoundEvents.ENDERMAN_TELEPORT,
                            SoundSource.PLAYERS,
                            0.72F,
                            0.58F
                    );

            player.serverLevel()
                    .playSound(
                            null,
                            BlockPos.containing(
                                    center
                            ),
                            SoundEvents.GENERIC_EXPLODE.value(),
                            SoundSource.PLAYERS,
                            1.15F,
                            1.28F
                    );

            burst(
                    player.serverLevel(),
                    center,
                    82,
                    1.55F
            );

            player.serverLevel()
                    .sendParticles(
                            ParticleTypes.EXPLOSION,
                            center.x,
                            center.y,
                            center.z,
                            7,
                            1.0
                                    + targetPower
                                            * 1.6,
                            0.8
                                    + targetPower,
                            1.0
                                    + targetPower
                                            * 1.6,
                            0.06
                    );
        }

        sendVisual(
                player.serverLevel(),
                blue.owner,
                center,
                initialPower,
                attractionRadius(
                        initialPower
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

        sendGesture(
                player,
                BlueGestureS2CPayload.STOP
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
        tickAccretionSmoke(server);

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

            if (blue.emergenceAge
                    < blue.emergenceDuration) {

                blue.emergenceAge++;

                float raw =
                        blue.emergenceAge
                                / (float) Math.max(
                                1,
                                blue.emergenceDuration
                        );

                float progress =
                        raw
                                * raw
                                * (
                                3.0F
                                        - 2.0F
                                                * raw
                        );

                blue.power =
                        Mth.lerp(
                                progress,
                                Math.max(
                                        0.04F,
                                        blue.targetPower
                                                * 0.08F
                                ),
                                blue.targetPower
                        );

                double emergingDistance =
                        0.72
                                + (
                                blue.distance
                                        - 0.72
                        )
                                        * progress;

                blue.center =
                        owner.getEyePosition()
                                .add(
                                        blue.look.scale(
                                                emergingDistance
                                        )
                                );

                if (tick % 2L == 0L) {
                    level.sendParticles(
                            ParticleTypes.PORTAL,
                            blue.center.x,
                            blue.center.y,
                            blue.center.z,
                            5,
                            0.16
                                    + progress * 0.20,
                            0.16
                                    + progress * 0.20,
                            0.16
                                    + progress * 0.20,
                            0.02
                    );
                }

                sendVisual(
                        level,
                        blue.owner,
                        blue.center,
                        blue.power,
                        attractionRadius(
                                blue.power
                        ),
                        BlueVisualPayload.ACTIVE
                );

                continue;
            }

            blue.power =
                    Mth.lerp(
                            0.18F,
                            blue.power,
                            blue.targetPower
                    );

            if (Math.abs(
                    blue.power
                            - blue.targetPower
            )
                    < 0.004F) {

                blue.power =
                        blue.targetPower;
            }

            if (blue.held) {
                /*
                 * Voice command "pare/fique parado": keep the exact world
                 * position while the field remains fully active.
                 */
            } else if (blue.orbiting) {
                blue.orbitAngle +=
                        0.42
                                * blue.spinDirection;

                double orbitRadius =
                        Mth.clamp(
                                3.25
                                        + blue.power
                                                * 1.35,
                                3.25,
                                5.10
                        );

                double bob =
                        Math.sin(
                                tick * 0.28
                                        + blue.owner.hashCode()
                                                * 0.01
                        )
                                * 0.38;

                Vec3 eye =
                        owner.getEyePosition();

                blue.center =
                        eye.add(
                                Math.cos(
                                        blue.orbitAngle
                                )
                                        * orbitRadius,
                                -0.18
                                        + bob,
                                Math.sin(
                                        blue.orbitAngle
                                )
                                        * orbitRadius
                        );

            } else {
                blue.center =
                        owner.getEyePosition()
                                .add(
                                        blue.look.scale(
                                                blue.distance
                                        )
                                );
            }

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
                    destructionBudget(
                            blue.power,
                            false
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
                float explosionPower =
                        Mth.clamp(
                                2.5F
                                        + blue.power
                                                * 3.0F,
                                2.5F,
                                6.5F
                        );

                level.explode(
                        null,
                        blue.position.x,
                        blue.position.y,
                        blue.position.z,
                        explosionPower,
                        Level.ExplosionInteraction.TNT
                );

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
                        destructionBudget(
                                power,
                                true
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

            releaseSmokeSequence(
                    level,
                    blue
            );

            if (blue.life % 4 == 0) {
                drawIncomingMatter(
                        level,
                        blue.position,
                        5.0
                                + visualPower
                                        * 6.0,
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
        ACCRETION_SMOKE.clear();
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

        sendGesture(
                player,
                BlueGestureS2CPayload.LAUNCH
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

    public static FusionSeed consumeHeldBlueForFusion(
            ServerPlayer player,
            Vec3 redPosition,
            double redRadius
    ) {
        ActiveBlue blue =
                ACTIVE.get(
                        player.getUUID()
                );

        if (blue == null
                || !blue.held
                || blue.dimension
                != player.serverLevel()
                        .dimension()) {

            return null;
        }

        double collision =
                physicalOuterHalf(
                        blue.power
                )
                        + Math.max(
                                0.20,
                                redRadius
                        );

        if (blue.center
                .distanceToSqr(
                        redPosition
                )
                > collision * collision) {

            return null;
        }

        if (!ACTIVE.remove(
                player.getUUID(),
                blue
        )) {
            return null;
        }

        sendGesture(
                player,
                BlueGestureS2CPayload.FUSION
        );

        return new FusionSeed(
                blue.owner,
                blue.dimension,
                blue.center,
                blue.power,
                blue.spinDirection
        );
    }

    public static void keepFusionBlueVisible(
            ServerLevel level,
            UUID owner,
            Vec3 center,
            float power
    ) {
        sendVisual(
                level,
                owner,
                center,
                power,
                attractionRadius(
                        power
                ),
                BlueVisualPayload.ACTIVE
        );
    }

    public static void finishFusion(
            MinecraftServer server,
            UUID owner
    ) {
        finishAbility(
                server,
                owner
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
        double normalized =
                Mth.clamp(
                        power / 1.35F,
                        0.0F,
                        1.0F
                );

        /*
         * Tiny Blues still tug from a useful distance, while fully charged
         * ones influence an enormous volume.
         */
        return 6.0
                + Math.pow(
                        normalized,
                        0.85
                )
                        * 68.0;
    }

    private static int destructionBudget(
            float power,
            boolean launched
    ) {
        double normalized =
                Mth.clamp(
                        power / 1.35F,
                        0.0F,
                        1.0F
                );

        double curve =
                normalized
                        * normalized
                        * normalized;

        return Math.round(
                (float) (
                        curve
                                * (
                                        launched
                                                ? 48.0
                                                : 40.0
                                )
                )
        );
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

        UUID ownerId =
                owner == null
                        ? null
                        : owner.getUUID();

        consumeBlueBody(
                level,
                center,
                power,
                ownerId,
                spinDirection
        );

        consumeBlocks(
                level,
                center,
                power,
                blockBudget,
                ownerId,
                spinDirection
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

    private static void consumeBlueBody(
            ServerLevel level,
            Vec3 center,
            float power,
            UUID ownerId,
            double spinDirection
    ) {
        double half =
                physicalOuterHalf(
                        power
                );

        BlockPos min =
                BlockPos.containing(
                        center.x - half,
                        center.y - half,
                        center.z - half
                );

        BlockPos max =
                BlockPos.containing(
                        center.x + half,
                        center.y + half,
                        center.z + half
                );

        for (BlockPos pos :
                BlockPos.betweenClosed(
                        min,
                        max
                )) {

            if (!level.hasChunkAt(
                    pos
            )) {
                continue;
            }

            /*
             * The renderer rotates the cube, while this server volume uses a
             * slightly conservative AABB around the translucent outer shell.
             * Result: if the visible Blue touches a breakable block, that block
             * cannot survive the pass.
             */
            destroyBlueBlock(
                    level,
                    pos,
                    center,
                    power,
                    true,
                    ownerId,
                    spinDirection
            );
        }
    }

    private static double physicalOuterHalf(
            float power
    ) {
        double normalized =
                Mth.clamp(
                        power / 1.35F,
                        0.0F,
                        1.0F
                );

        double coreHalf =
                0.14
                        + Math.pow(
                                normalized,
                                1.50
                        )
                                * 2.31;

        /*
         * Matches the renderer's two translucent shells at approximately
         * their widest pulse.
         */
        return coreHalf
                * 1.90;
    }

    private static void consumeBlocks(
            ServerLevel level,
            Vec3 center,
            float power,
            int blockBudget,
            UUID ownerId,
            double spinDirection
    ) {
        if (blockBudget <= 0) {
            return;
        }

        double normalized =
                Mth.clamp(
                        power / 1.35F,
                        0.0F,
                        1.0F
                );

        double bodyHalf =
                physicalOuterHalf(
                        power
                );

        double digRadius =
                bodyHalf
                        + 1.5
                        + Math.pow(
                                normalized,
                                1.35
                        )
                                * 13.0;

        int removed =
                0;

        int attempts =
                Math.max(
                        18,
                        blockBudget
                                * 8
                );

        for (int i = 0;
                i < attempts
                        && removed < blockBudget;
                i++) {

            /*
             * Bias destruction heavily toward the visible cube. pow(random,2.5)
             * makes close samples far more common while still allowing the
             * larger destructive field to chew irregular chunks farther out.
             */
            double distance =
                    bodyHalf
                            + Math.pow(
                                    level.random.nextDouble(),
                                    2.5
                            )
                                    * Math.max(
                                            0.0,
                                            digRadius
                                                    - bodyHalf
                                    );

            double azimuth =
                    level.random.nextDouble()
                            * Math.PI
                            * 2.0;

            double yDirection =
                    level.random.nextDouble()
                            * 2.0
                            - 1.0;

            double horizontal =
                    Math.sqrt(
                            Math.max(
                                    0.0,
                                    1.0
                                            - yDirection
                                                    * yDirection
                            )
                    );

            double dx =
                    Math.cos(
                            azimuth
                    )
                            * horizontal
                            * distance;

            double dy =
                    yDirection
                            * distance;

            double dz =
                    Math.sin(
                            azimuth
                    )
                            * horizontal
                            * distance;

            BlockPos pos =
                    BlockPos.containing(
                            center.x + dx,
                            center.y + dy,
                            center.z + dz
                    );

            if (destroyBlueBlock(
                    level,
                    pos,
                    center,
                    power,
                    false,
                    ownerId,
                    spinDirection
            )) {
                removed++;
            }
        }
    }

    private static boolean destroyBlueBlock(
            ServerLevel level,
            BlockPos pos,
            Vec3 center,
            float power,
            boolean bodyContact,
            UUID ownerId,
            double spinDirection
    ) {
        BlockState state =
                level.getBlockState(
                        pos
                );

        if (state.isAir()) {
            return false;
        }

        float hardness =
                state.getDestroySpeed(
                        level,
                        pos
                );

        if (hardness < 0.0F) {
            return false;
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

        level.removeBlock(
                pos,
                false
        );

        Vec3 source =
                Vec3.atCenterOf(
                        pos
                );

        Vec3 inward =
                center.subtract(
                        source
                );

        if (inward.lengthSqr()
                > 0.0001) {
            inward =
                    inward.normalize();
        } else {
            inward =
                    Vec3.ZERO;
        }

        level.sendParticles(
                new BlockParticleOption(
                        ParticleTypes.BLOCK,
                        state
                ),
                source.x,
                source.y,
                source.z,
                bodyContact
                        ? 3
                        : 1,
                bodyContact
                        ? 0.34
                        : 0.18,
                bodyContact
                        ? 0.34
                        : 0.18,
                bodyContact
                        ? 0.34
                        : 0.18,
                0.18
        );

        boolean vegetation =
                isVegetationBlock(
                        state
                );

        if (vegetation) {
            int leaves =
                    bodyContact
                            ? 9
                            : 5;

            level.sendParticles(
                    WayAroundParticles.WIND_LEAF.get(),
                    source.x,
                    source.y,
                    source.z,
                    leaves,
                    bodyContact
                            ? 0.72
                            : 0.42,
                    bodyContact
                            ? 0.54
                            : 0.30,
                    bodyContact
                            ? 0.72
                            : 0.42,
                    0.11
            );
        }

        /*
         * Smoke now belongs to mineral destruction instead of every erased
         * block. Leaves/grass use green wind debris, while wood/glass/etc.
         * mainly expose their own BLOCK fragments.
         */
        if (isSmokyMineralBlock(
                state
        )) {
            int smokeCount =
                    7
                            + level.random.nextInt(
                                    7
                            )
                            + (
                                    bodyContact
                                            ? 3
                                            : 0
                            );

            int accretionCount =
                    ownerId == null
                            ? 0
                            : Math.min(
                                    bodyContact
                                            ? 4
                                            : 3,
                                    Math.max(
                                            1,
                                            smokeCount / 4
                                    )
                            );

            int hanging =
                    smokeCount
                            - accretionCount;

            level.sendParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    source.x,
                    source.y,
                    source.z,
                    hanging,
                    bodyContact
                            ? 1.05
                            : 0.82,
                    bodyContact
                            ? 0.58
                            : 0.42,
                    bodyContact
                            ? 1.05
                            : 0.82,
                    0.008
            );

            if (bodyContact
                    && level.random.nextFloat()
                            < 0.45F) {

                level.sendParticles(
                        ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                        source.x,
                        source.y,
                        source.z,
                        1,
                        0.42,
                        0.34,
                        0.42,
                        0.004
                );
            }

            for (int i = 0;
                 i < accretionCount;
                 i++) {

                addAccretionSmoke(
                        level,
                        ownerId,
                        source,
                        center,
                        power,
                        spinDirection
                );
            }
        }

        return true;
    }

    private static boolean isVegetationBlock(
            BlockState state
    ) {
        if (state.is(
                BlockTags.LEAVES
        )) {
            return true;
        }

        String path =
                BuiltInRegistries.BLOCK
                        .getKey(
                                state.getBlock()
                        )
                        .getPath();

        return path.contains("grass")
                || path.contains("fern")
                || path.contains("leaves")
                || path.contains("leaf")
                || path.contains("vine")
                || path.contains("moss")
                || path.contains("flower")
                || path.contains("sapling")
                || path.contains("bush");
    }

    private static boolean isSmokyMineralBlock(
            BlockState state
    ) {
        String path =
                BuiltInRegistries.BLOCK
                        .getKey(
                                state.getBlock()
                        )
                        .getPath();

        return path.contains("stone")
                || path.contains("deepslate")
                || path.contains("cobble")
                || path.contains("ore")
                || path.contains("brick")
                || path.contains("concrete")
                || path.contains("terracotta")
                || path.contains("sandstone")
                || path.contains("netherrack")
                || path.contains("basalt")
                || path.contains("blackstone")
                || path.contains("tuff")
                || path.contains("calcite")
                || path.contains("granite")
                || path.contains("diorite")
                || path.contains("andesite")
                || path.contains("obsidian");
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
                28
                        + Math.round(
                                power
                                        * 42.0F
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
                                    0.18
                                            + level.random.nextDouble()
                                                    * 0.95
                            );

            double y =
                    (
                            level.random.nextDouble()
                                    - 0.5
                    )
                            * Math.min(
                                    radius
                                            * 0.82,
                                    26.0
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
                2.20
                        + power
                                * 1.80,
                2.20
                        + power
                                * 1.80,
                2.20
                        + power
                                * 1.80,
                0.38
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

        /*
         * Normal travel no longer invents smoke in empty air. Dust now comes
         * from blocks actually destroyed by Blue. The exception is the
         * collapse itself, where the Blue body disintegrates into smoke.
         */
        if (!releasing) {
            return;
        }

        double spread =
                6.6
                        + power
                                * 2.2;

        level.sendParticles(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                center.x,
                center.y,
                center.z,
                22,
                spread,
                1.8
                        + power,
                spread,
                0.007
        );

        level.sendParticles(
                ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                center.x,
                center.y,
                center.z,
                10,
                spread
                        * 0.82,
                1.4
                        + power
                                * 0.8,
                spread
                        * 0.82,
                0.004
        );

        level.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                center.x,
                center.y,
                center.z,
                30,
                spread,
                1.1
                        + power
                                * 0.7,
                spread,
                0.014
        );
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

    private static void addAccretionSmoke(
            ServerLevel level,
            UUID owner,
            Vec3 source,
            Vec3 center,
            float power,
            double spinDirection
    ) {
        if (owner == null) {
            return;
        }

        while (ACCRETION_SMOKE.size()
                >= MAX_ACCRETION_SMOKE) {
            ACCRETION_SMOKE.remove(0);
        }

        Vec3 relative =
                source.subtract(
                        center
                );

        double horizontalRadius =
                Math.sqrt(
                        relative.x * relative.x
                                + relative.z * relative.z
                );

        double angle =
                Math.atan2(
                        relative.z,
                        relative.x
                );

        double targetRadius =
                Math.max(
                        0.55,
                        physicalOuterHalf(
                                power
                        )
                                * (
                                        0.88
                                                + level.random.nextDouble()
                                                        * 0.34
                                )
                );

        int life =
                54
                        + level.random.nextInt(
                                58
                        );

        ACCRETION_SMOKE.add(
                new AccretionSmoke(
                        owner,
                        level.dimension(),
                        Math.max(
                                targetRadius + 0.4,
                                Math.min(
                                        horizontalRadius,
                                        targetRadius + 13.0
                                )
                        ),
                        targetRadius,
                        angle,
                        relative.y,
                        spinDirection,
                        life
                )
        );
    }

    private static void tickAccretionSmoke(
            MinecraftServer server
    ) {
        Iterator<AccretionSmoke> iterator =
                ACCRETION_SMOKE.iterator();

        while (iterator.hasNext()) {
            AccretionSmoke smoke =
                    iterator.next();

            BlueAnchor anchor =
                    findBlueAnchor(
                            server,
                            smoke.owner,
                            smoke.dimension
                    );

            if (anchor == null) {
                iterator.remove();
                continue;
            }

            ServerLevel level =
                    server.getLevel(
                            smoke.dimension
                    );

            if (level == null) {
                iterator.remove();
                continue;
            }

            smoke.age++;

            double progress =
                    Mth.clamp(
                            smoke.age
                                    / (double) smoke.life,
                            0.0,
                            1.0
                    );

            double approach =
                    1.0
                            - Math.pow(
                                    1.0
                                            - progress,
                                    2.35
                            );

            double radius =
                    Mth.lerp(
                            approach,
                            smoke.startRadius,
                            smoke.targetRadius
                    );

            double angularSpeed =
                    0.16
                            + progress
                                    * 0.58;

            smoke.angle +=
                    smoke.spinDirection
                            * angularSpeed;

            smoke.verticalOffset *=
                    0.91;

            double wobble =
                    Math.sin(
                            smoke.angle
                                    * 2.2
                                    + smoke.age
                                            * 0.12
                    )
                            * (
                                    0.10
                                            + (
                                                    1.0
                                                            - progress
                                            )
                                                    * 0.22
                            );

            Vec3 position =
                    anchor.position.add(
                            Math.cos(
                                    smoke.angle
                            )
                                    * radius,
                            smoke.verticalOffset
                                    + wobble,
                            Math.sin(
                                    smoke.angle
                            )
                                    * radius
                    );

            level.sendParticles(
                    smoke.age % 7 == 0
                            ? ParticleTypes.LARGE_SMOKE
                            : ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    position.x,
                    position.y,
                    position.z,
                    1,
                    0.035,
                    0.025,
                    0.035,
                    0.001
            );

            if (progress > 0.82
                    && smoke.age % 3 == 0) {
                level.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        position.x,
                        position.y,
                        position.z,
                        1,
                        0.03,
                        0.03,
                        0.03,
                        0.015
                );
            }

            if (smoke.age
                    >= smoke.life) {
                iterator.remove();
            }
        }
    }

    private static BlueAnchor findBlueAnchor(
            MinecraftServer server,
            UUID owner,
            net.minecraft.resources.ResourceKey<Level> dimension
    ) {
        ActiveBlue active =
                ACTIVE.get(
                        owner
                );

        if (active != null
                && active.dimension.equals(
                        dimension
                )) {
            return new BlueAnchor(
                    active.center
            );
        }

        for (LaunchedBlue blue : LAUNCHED) {
            if (blue.owner.equals(
                    owner
            )
                    && blue.dimension.equals(
                            dimension
                    )) {
                return new BlueAnchor(
                        blue.position
                );
            }
        }

        for (ReleasingBlue blue : RELEASING) {
            if (blue.owner.equals(
                    owner
            )
                    && blue.dimension.equals(
                            dimension
                    )) {
                return new BlueAnchor(
                        blue.position
                );
            }
        }

        return null;
    }

    private static void releaseSmokeSequence(
            ServerLevel level,
            ReleasingBlue blue
    ) {
        int age =
                blue.maxLife
                        - blue.life;

        Vec3 center =
                blue.position;

        /*
         * Phase 1: violent implosion. Smoke spawns around the old Blue body
         * and receives a high inward velocity with a little tangent, so the
         * whole cloud appears to collapse into the cube.
         */
        if (age <= 30) {
            int strands =
                    16
                            + Math.round(
                                    blue.power
                                            * 9.0F
                            );

            for (int i = 0;
                    i < strands;
                    i++) {

                double angle =
                        level.random.nextDouble()
                                * Math.PI
                                * 2.0;

                double radius =
                        4.5
                                + level.random.nextDouble()
                                        * (
                                                6.0
                                                        + blue.power
                                                                * 4.0
                                        );

                double y =
                        (
                                level.random.nextDouble()
                                        - 0.5
                        )
                                * (
                                        5.0
                                                + blue.power
                                                        * 4.0
                                );

                Vec3 source =
                        center.add(
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
                        center.subtract(
                                source
                        )
                                .normalize();

                Vec3 tangent =
                        new Vec3(
                                -inward.z,
                                (
                                        level.random.nextDouble()
                                                - 0.5
                                )
                                        * 0.08,
                                inward.x
                        )
                                .scale(
                                        blue.spinDirection
                                                * 0.20
                                );

                Vec3 velocity =
                        inward.scale(
                                0.62
                                        + blue.power
                                                * 0.18
                        )
                                .add(
                                        tangent
                                );

                level.sendParticles(
                        i % 4 == 0
                                ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE
                                : ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        source.x,
                        source.y,
                        source.z,
                        0,
                        velocity.x,
                        velocity.y,
                        velocity.z,
                        0.34
                );
            }

            return;
        }

        /*
         * Phase 2: the compressed cloud detonates. This happens once after
         * the inward rush, with smoke and sparks thrown outward.
         */
        if (age == 31) {
            level.playSound(
                    null,
                    BlockPos.containing(
                            center
                    ),
                    SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS,
                    1.75F,
                    0.82F
            );

            level.sendParticles(
                    ParticleTypes.EXPLOSION,
                    center.x,
                    center.y,
                    center.z,
                    12,
                    2.2
                            + blue.power
                                    * 1.8,
                    1.8
                            + blue.power,
                    2.2
                            + blue.power
                                    * 1.8,
                    0.10
            );

            int blastSmoke =
                    72
                            + Math.round(
                                    blue.power
                                            * 48.0F
                            );

            for (int i = 0;
                    i < blastSmoke;
                    i++) {

                Vec3 outward =
                        new Vec3(
                                level.random.nextDouble()
                                        * 2.0
                                        - 1.0,
                                level.random.nextDouble()
                                        * 1.6
                                        - 0.45,
                                level.random.nextDouble()
                                        * 2.0
                                        - 1.0
                        );

                if (outward.lengthSqr()
                        < 0.001) {
                    continue;
                }

                outward =
                        outward.normalize();

                Vec3 tangent =
                        new Vec3(
                                -outward.z,
                                0.0,
                                outward.x
                        )
                                .scale(
                                        blue.spinDirection
                                                * (
                                                        0.18
                                                                + level.random.nextDouble()
                                                                        * 0.24
                                                )
                                );

                Vec3 velocity =
                        outward.scale(
                                0.48
                                        + level.random.nextDouble()
                                                * 0.58
                        )
                                .add(
                                        tangent
                                );

                level.sendParticles(
                        i % 3 == 0
                                ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE
                                : ParticleTypes.LARGE_SMOKE,
                        center.x,
                        center.y,
                        center.z,
                        0,
                        velocity.x,
                        velocity.y,
                        velocity.z,
                        0.30
                );
            }

            burst(
                    level,
                    center,
                    110,
                    2.25F
            );

            return;
        }

        /*
         * Phase 3: several smoky revolutions after the blast. New smoke is
         * placed around rotating rings and given tangent + slight outward
         * velocity, making the aftermath visibly corkscrew before fading.
         */
        if (age <= 78) {
            double turn =
                    (
                            age
                                    - 31
                    )
                            * 0.48
                            * blue.spinDirection;

            int ringPoints =
                    12
                            + Math.round(
                                    blue.power
                                            * 4.0F
                            );

            for (int i = 0;
                    i < ringPoints;
                    i++) {

                double angle =
                        turn
                                + i
                                        * (
                                                Math.PI
                                                        * 2.0
                                                        / ringPoints
                                        );

                double radius =
                        2.8
                                + (
                                        age
                                                - 31
                                )
                                        * 0.075
                                + blue.power
                                        * 1.8;

                double y =
                        Math.sin(
                                angle
                                        * 1.7
                                        + age
                                                * 0.11
                        )
                                * (
                                        0.9
                                                + blue.power
                                                        * 0.7
                                );

                Vec3 source =
                        center.add(
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

                Vec3 tangent =
                        new Vec3(
                                -Math.sin(
                                        angle
                                ),
                                0.08
                                        + Math.sin(
                                                angle
                                                        * 0.7
                                        )
                                                * 0.03,
                                Math.cos(
                                        angle
                                )
                        )
                                .scale(
                                        blue.spinDirection
                                                * 0.42
                                );

                Vec3 outward =
                        source.subtract(
                                center
                        )
                                .normalize()
                                .scale(
                                        0.10
                                );

                Vec3 velocity =
                        tangent.add(
                                outward
                        );

                level.sendParticles(
                        i % 5 == 0
                                ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE
                                : ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        source.x,
                        source.y,
                        source.z,
                        0,
                        velocity.x,
                        velocity.y,
                        velocity.z,
                        0.22
                );
            }
        }
    }

    private static void smokeCollapse(
            ServerLevel level,
            Vec3 center,
            float power
    ) {
        double spread =
                5.4
                        + power
                                * 3.4;

        level.sendParticles(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                center.x,
                center.y,
                center.z,
                92,
                spread,
                2.4
                        + power,
                spread,
                0.010
        );

        level.sendParticles(
                ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                center.x,
                center.y,
                center.z,
                44,
                spread
                        * 0.86,
                2.0
                        + power
                                * 0.8,
                spread
                        * 0.86,
                0.005
        );

        level.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                center.x,
                center.y,
                center.z,
                138,
                spread
                        * 1.10,
                2.0
                        + power,
                spread
                        * 1.10,
                0.028
        );

        burst(
                level,
                center,
                96,
                2.0F
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
                2.15
                        * scale,
                2.15
                        * scale,
                2.15
                        * scale,
                0.52
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
                1.85
                        * scale,
                1.85
                        * scale,
                1.85
                        * scale,
                0.44
        );
    }

    private static void sendGesture(
            ServerPlayer player,
            byte gesture
    ) {
        PacketDistributor.sendToPlayer(
                player,
                new BlueGestureS2CPayload(
                        gesture
                )
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

    public record FusionSeed(
            UUID owner,
            net.minecraft.resources.ResourceKey<Level> dimension,
            Vec3 center,
            float power,
            double spinDirection
    ) {
    }

    private record BlueAnchor(
            Vec3 position
    ) {
    }

    private static final class AccretionSmoke {

        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final double startRadius;
        private final double targetRadius;
        private final double spinDirection;
        private final int life;

        private double angle;
        private double verticalOffset;
        private int age;

        private AccretionSmoke(
                UUID owner,
                net.minecraft.resources.ResourceKey<Level> dimension,
                double startRadius,
                double targetRadius,
                double angle,
                double verticalOffset,
                double spinDirection,
                int life
        ) {
            this.owner = owner;
            this.dimension = dimension;
            this.startRadius = startRadius;
            this.targetRadius = targetRadius;
            this.angle = angle;
            this.verticalOffset = verticalOffset;
            this.spinDirection = spinDirection;
            this.life = life;
        }
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
        private float power;
        private float targetPower;
        private final int emergenceDuration;
        private int emergenceAge;
        private final double spinDirection;
        private final long startedAt;

        private Vec3 center;
        private Vec3 look;
        private double distance;

        private boolean held;
        private boolean orbiting;
        private double orbitAngle;

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
                float targetPower,
                int emergenceDuration,
                double spinDirection,
                long startedAt
        ) {
            this.owner = owner;
            this.dimension = dimension;
            this.center = center;
            this.look = look;
            this.distance = distance;
            this.power = power;
            this.targetPower = targetPower;
            this.emergenceDuration =
                    Math.max(
                            0,
                            emergenceDuration
                    );
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
