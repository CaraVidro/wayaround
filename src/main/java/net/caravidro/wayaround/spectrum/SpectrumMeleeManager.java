package net.caravidro.wayaround.spectrum;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.cinematic.PlayerControlLockManager;
import net.caravidro.wayaround.cursed.TukunaManager;
import net.caravidro.wayaround.network.PlayerCinematicPayload;
import net.caravidro.wayaround.network.SpectrumMeleeInputPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Native-feeling unarmed combat for Void/Tukuna.
 *
 * The client only sends intent. The server owns target raycasts, combo state,
 * F-block catches and finishers.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class SpectrumMeleeManager {

    private SpectrumMeleeManager() {}

    private static final int ATTACK_COOLDOWN_TICKS =
            5;

    private static final int COMBO_TIMEOUT_TICKS =
            34;

    private static final int FINISHER_HIT =
            5;

    private static final double REACH =
            3.35;

    private static final Map<UUID, Long> NEXT_ATTACK =
            new HashMap<>();

    private static final Map<UUID, ComboState> COMBOS =
            new HashMap<>();

    private static final Map<UUID, Boolean> BLOCKING =
            new HashMap<>();

    private static final Map<UUID, Long> BLOCK_RETURN =
            new HashMap<>();

    public static void handleInput(
            ServerPlayer player,
            byte action
    ) {
        if (action
                == SpectrumMeleeInputPayload.BLOCK_END) {
            endBlock(
                    player
            );
            return;
        }

        if (!eligible(
                player
        )) {
            endBlock(
                    player
            );
            return;
        }

        if (action
                == SpectrumMeleeInputPayload.BLOCK_START) {
            startBlock(
                    player
            );
            return;
        }

        if (action
                == SpectrumMeleeInputPayload.ATTACK) {
            attack(
                    player
            );
        }
    }

    public static boolean isBlocking(
            ServerPlayer player
    ) {
        return BLOCKING.getOrDefault(
                player.getUUID(),
                false
        );
    }

    private static boolean eligible(
            ServerPlayer player
    ) {
        if (!player.isAlive()
                || player.isSpectator()
                || !SpectrumActions.combatMode(player)
                || !player.getMainHandItem()
                        .isEmpty()
                || !player.getOffhandItem()
                        .isEmpty()
                || TukunaManager.isSilencedHost(
                        player
                )
                || TukunaManager.isDraftingPact(
                        player
                )
                || TukunaManager.isPacifistPossession(
                        player
                )
                || PlayerControlLockManager.actionsLocked(
                        player
                )) {
            return false;
        }

        return SpectrumAccess.has(
                player,
                SpectrumType.VOID
        )
                || SpectrumAccess.has(
                player,
                SpectrumType.TUKUNA
        );
    }

    private static void startBlock(
            ServerPlayer player
    ) {
        BLOCKING.put(
                player.getUUID(),
                true
        );

        pose(
                player,
                PlayerCinematicPayload.MELEE_BLOCK,
                72_000,
                false,
                0.0F
        );
    }

    private static void endBlock(
            ServerPlayer player
    ) {
        if (!Boolean.TRUE.equals(
                BLOCKING.remove(
                        player.getUUID()
                )
        )) {
            return;
        }

        BLOCK_RETURN.remove(
                player.getUUID()
        );

        pose(
                player,
                PlayerCinematicPayload.CLEAR,
                0,
                false,
                0.0F
        );
    }

    private static void attack(
            ServerPlayer attacker
    ) {
        if (isBlocking(
                attacker
        )) {
            return;
        }

        long now =
                attacker.server
                        .getTickCount();

        if (now
                < NEXT_ATTACK.getOrDefault(
                        attacker.getUUID(),
                        0L
                )) {
            return;
        }

        NEXT_ATTACK.put(
                attacker.getUUID(),
                now
                        + ATTACK_COOLDOWN_TICKS
        );

        LivingEntity target =
                findTarget(
                        attacker
                );

        ComboState combo =
                COMBOS.computeIfAbsent(
                        attacker.getUUID(),
                        ignored ->
                                new ComboState()
                );

        if (target == null) {
            combo.miss(
                    now
            );

            pose(
                    attacker,
                    PlayerCinematicPayload.MELEE_PUNCH,
                    9,
                    false,
                    0.0F
            );

            return;
        }

        if (target instanceof ServerPlayer defender
                && isBlocking(
                        defender
                )) {
            catchPunch(
                    attacker,
                    defender,
                    now
            );
            return;
        }

        UUID targetId =
                target.getUUID();

        if (!targetId.equals(
                combo.target
        )
                || now - combo.lastHit
                        > COMBO_TIMEOUT_TICKS) {
            combo.reset(
                    targetId
            );
        }

        combo.lastHit =
                now;

        combo.hits++;

        if (combo.hits
                < FINISHER_HIT) {
            pose(
                    attacker,
                    PlayerCinematicPayload.MELEE_PUNCH,
                    9,
                    false,
                    0.0F
            );

            hit(
                    attacker,
                    target,
                    5.8F
            );

            return;
        }

        combo.hits =
                0;

        if (!attacker.onGround()
                && !attacker.isInWaterOrBubble()) {
            downslam(
                    attacker,
                    target
            );
        } else if (attacker.getXRot()
                < -34.0F) {
            uppercut(
                    attacker,
                    target
            );
        } else {
            launch(
                    attacker,
                    target
            );
        }
    }

    private static void catchPunch(
            ServerPlayer attacker,
            ServerPlayer defender,
            long now
    ) {
        pose(
                attacker,
                PlayerCinematicPayload.MELEE_CATCH_ATTACKER,
                9,
                false,
                0.0F
        );

        pose(
                defender,
                PlayerCinematicPayload.MELEE_CATCH_DEFENDER,
                9,
                false,
                0.0F
        );

        BLOCK_RETURN.put(
                defender.getUUID(),
                now + 9L
        );

        COMBOS.remove(
                attacker.getUUID()
        );

        Vec3 middle =
                attacker.position()
                        .add(
                                defender.position()
                        )
                        .scale(
                                0.5
                        )
                        .add(
                                0.0,
                                1.25,
                                0.0
                        );

        ServerLevel level =
                attacker.serverLevel();

        level.sendParticles(
                ParticleTypes.POOF,
                middle.x,
                middle.y,
                middle.z,
                12,
                0.12,
                0.12,
                0.12,
                0.04
        );

        level.playSound(
                null,
                defender.blockPosition(),
                SoundEvents.PLAYER_ATTACK_NODAMAGE,
                SoundSource.PLAYERS,
                0.9F,
                0.72F
        );
    }

    private static void launch(
            ServerPlayer attacker,
            LivingEntity target
    ) {
        if (!hit(
                attacker,
                target,
                8.0F
        )) {
            return;
        }

        pose(
                attacker,
                PlayerCinematicPayload.MELEE_LAUNCH,
                14,
                false,
                0.12F
        );

        Vec3 forward =
                horizontalForward(
                        attacker
                );

        SpectrumImpact.launchAndBreak(
                attacker,
                target,
                forward.scale(
                        2.35
                ).add(
                        0.0,
                        0.34,
                        0.0
                ),
                11.0,
                1
        );
    }

    private static void downslam(
            ServerPlayer attacker,
            LivingEntity target
    ) {
        if (!hit(
                attacker,
                target,
                8.5F
        )) {
            return;
        }

        pose(
                attacker,
                PlayerCinematicPayload.MELEE_DOWNSLAM,
                16,
                false,
                0.16F
        );

        Vec3 forward =
                horizontalForward(
                        attacker
                );

        SpectrumImpact.launchAndBreak(
                attacker,
                target,
                forward.scale(
                        0.52
                ).add(
                        0.0,
                        -2.25,
                        0.0
                ),
                7.0,
                1
        );
    }

    private static void uppercut(
            ServerPlayer attacker,
            LivingEntity target
    ) {
        if (!hit(
                attacker,
                target,
                8.0F
        )) {
            return;
        }

        pose(
                attacker,
                PlayerCinematicPayload.MELEE_UPPERCUT,
                15,
                false,
                0.14F
        );

        Vec3 forward =
                horizontalForward(
                        attacker
                );

        SpectrumImpact.launchAndBreak(
                attacker,
                target,
                forward.scale(
                        0.30
                ).add(
                        0.0,
                        1.85,
                        0.0
                ),
                9.0,
                1
        );
    }

    private static boolean hit(
            ServerPlayer attacker,
            LivingEntity target,
            float damage
    ) {
        return target.hurt(
                attacker.serverLevel()
                        .damageSources()
                        .playerAttack(
                                attacker
                        ),
                damage
        );
    }

    private static LivingEntity findTarget(
            ServerPlayer attacker
    ) {
        ServerLevel level =
                attacker.serverLevel();

        Vec3 start =
                attacker.getEyePosition();

        Vec3 end =
                start.add(
                        attacker.getLookAngle()
                                .scale(
                                        REACH
                                )
                );

        BlockHitResult block =
                level.clip(
                        new ClipContext(
                                start,
                                end,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                attacker
                        )
                );

        double blockDistance =
                block.getType()
                        == HitResult.Type.MISS
                        ? Double.POSITIVE_INFINITY
                        : start.distanceToSqr(
                                block.getLocation()
                        );

        AABB search =
                new AABB(
                        start,
                        end
                ).inflate(
                        0.85
                );

        LivingEntity best =
                null;

        double bestDistance =
                blockDistance;

        for (Entity entity :
                level.getEntities(
                        attacker,
                        search,
                        candidate ->
                                candidate instanceof LivingEntity
                                        && candidate.isAlive()
                                        && candidate.isPickable()
                )) {
            Optional<Vec3> hit =
                    entity.getBoundingBox()
                            .inflate(
                                    0.28
                            )
                            .clip(
                                    start,
                                    end
                            );

            if (hit.isEmpty()) {
                continue;
            }

            double distance =
                    start.distanceToSqr(
                            hit.get()
                    );

            if (distance
                    >= bestDistance) {
                continue;
            }

            bestDistance =
                    distance;

            best =
                    (LivingEntity) entity;
        }

        return best;
    }

    private static Vec3 horizontalForward(
            ServerPlayer player
    ) {
        Vec3 look =
                player.getLookAngle();

        Vec3 flat =
                new Vec3(
                        look.x,
                        0.0,
                        look.z
                );

        return flat.lengthSqr()
                < 1.0E-6
                ? new Vec3(
                0.0,
                0.0,
                1.0
        )
                : flat.normalize();
    }

    private static void pose(
            ServerPlayer player,
            byte animation,
            int duration,
            boolean lockCamera,
            float shake
    ) {
        PacketDistributor.sendToPlayersNear(
                player.serverLevel(),
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                128.0,
                new PlayerCinematicPayload(
                        player.getUUID(),
                        animation,
                        duration,
                        lockCamera,
                        shake
                )
        );

        PacketDistributor.sendToPlayer(
                player,
                new PlayerCinematicPayload(
                        player.getUUID(),
                        animation,
                        duration,
                        lockCamera,
                        shake
                )
        );
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long now =
                server.getTickCount();

        BLOCK_RETURN.entrySet()
                .removeIf(
                        entry -> {
                            if (now
                                    < entry.getValue()) {
                                return false;
                            }

                            ServerPlayer player =
                                    server.getPlayerList()
                                            .getPlayer(
                                                    entry.getKey()
                                            );

                            if (player != null
                                    && isBlocking(
                                    player
                            )
                                    && eligible(
                                    player
                            )) {
                                pose(
                                        player,
                                        PlayerCinematicPayload.MELEE_BLOCK,
                                        72_000,
                                        false,
                                        0.0F
                                );
                            }

                            return true;
                        }
                );

        BLOCKING.entrySet()
                .removeIf(
                        entry -> {
                            ServerPlayer player =
                                    server.getPlayerList()
                                            .getPlayer(
                                                    entry.getKey()
                                            );

                            if (player == null) {
                                return true;
                            }

                            if (!eligible(
                                    player
                            )) {
                                pose(
                                        player,
                                        PlayerCinematicPayload.CLEAR,
                                        0,
                                        false,
                                        0.0F
                                );
                                return true;
                            }

                            return false;
                        }
                );

        COMBOS.entrySet()
                .removeIf(
                        entry ->
                                now - entry.getValue().lastHit
                                        > COMBO_TIMEOUT_TICKS * 2L
                );

        if (now % 200L == 0L) {
            NEXT_ATTACK.entrySet()
                    .removeIf(
                            entry ->
                                    entry.getValue()
                                            < now
                    );
        }
    }

    @SubscribeEvent
    public static void stop(
            ServerStoppedEvent event
    ) {
        NEXT_ATTACK.clear();
        COMBOS.clear();
        BLOCKING.clear();
        BLOCK_RETURN.clear();
    }

    private static final class ComboState {

        private UUID target;
        private int hits;
        private long lastHit;

        private void reset(
                UUID target
        ) {
            this.target =
                    target;
            this.hits =
                    0;
        }

        private void miss(
                long tick
        ) {
            if (tick - lastHit
                    > COMBO_TIMEOUT_TICKS) {
                hits =
                        0;
                target =
                        null;
            }
        }
    }
}
