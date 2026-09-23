package net.caravidro.wayaround.domain;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import net.caravidro.wayaround.domain.DomainProfile.Consequence;
import net.caravidro.wayaround.domain.DomainProfile.Reward;
import net.caravidro.wayaround.domain.DomainProfile.Trigger;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class DomainManager {

    private static final Map<UUID, ActiveDomain> ACTIVE =
            new HashMap<>();

    private static final Map<UUID, Long> EXPAND_COOLDOWN =
            new HashMap<>();

    private static final Map<UUID, Long> QUICK_COOLDOWN =
            new HashMap<>();

    private DomainManager() {
    }

    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        Iterator<Map.Entry<UUID, ActiveDomain>> iterator =
                ACTIVE.entrySet()
                        .iterator();

        while (iterator.hasNext()) {

            ActiveDomain domain =
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

                iterator.remove();
                continue;
            }

            domain.tick(
                    level,
                    owner,
                    tick
            );
        }
    }

    public static boolean expand(
            ServerPlayer player
    ) {
        DomainProfile profile =
                DomainPlayerData.profile(
                        player
                );

        if (profile == null) {
            player.sendSystemMessage(
                    Component.literal(
                            "Seu domínio ainda não despertou. Use /domain awaken."
                    )
            );

            return false;
        }

        long tick =
                player.server.getTickCount();

        if (ACTIVE.containsKey(
                player.getUUID()
        )) {
            player.sendSystemMessage(
                    Component.literal(
                            "Seu domínio já está expandido."
                    )
            );

            return false;
        }

        long readyAt =
                EXPAND_COOLDOWN.getOrDefault(
                        player.getUUID(),
                        0L
                );

        if (tick < readyAt) {
            player.sendSystemMessage(
                    Component.literal(
                            "Seu domínio ainda está se estabilizando por "
                            + Math.max(
                                    1,
                                    (readyAt - tick + 19)
                                    / 20
                            )
                            + "s."
                    )
            );

            return false;
        }

        Vec3 center =
                player.position();

        ActiveDomain active =
                new ActiveDomain(
                        player.getUUID(),
                        player.serverLevel()
                                .dimension(),
                        center,
                        profile,
                        tick
                                + profile.durationTicks()
                );

        ACTIVE.put(
                player.getUUID(),
                active
        );

        EXPAND_COOLDOWN.put(
                player.getUUID(),
                active.endsAt
                        + 80L
        );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.PLAYERS,
                        0.75F,
                        0.68F
                );

        burst(
                player.serverLevel(),
                center,
                profile,
                42
        );

        player.sendSystemMessage(
                Component.literal(
                        "EXPANSÃO DE DOMÍNIO — "
                        + profile.name()
                )
        );

        return true;
    }

    public static boolean quick(
            ServerPlayer player
    ) {
        DomainProfile profile =
                DomainPlayerData.profile(
                        player
                );

        if (profile == null) {
            player.sendSystemMessage(
                    Component.literal(
                            "Seu domínio ainda não despertou. Use /domain awaken."
                    )
            );

            return false;
        }

        long tick =
                player.server.getTickCount();

        long readyAt =
                QUICK_COOLDOWN.getOrDefault(
                        player.getUUID(),
                        0L
                );

        if (tick < readyAt) {
            player.sendSystemMessage(
                    Component.literal(
                            "Manifestação rápida disponível em "
                            + Math.max(
                                    1,
                                    (readyAt - tick + 19)
                                    / 20
                            )
                            + "s."
                    )
            );

            return false;
        }

        QUICK_COOLDOWN.put(
                player.getUUID(),
                tick + 100L
        );

        applyQuickReward(
                player,
                profile.reward()
        );

        burst(
                player.serverLevel(),
                player.position(),
                profile,
                18
        );

        player.sendSystemMessage(
                Component.literal(
                        "Manifestação rápida: "
                        + profile.reward()
                                .description
                )
        );

        return true;
    }

    public static void clearAll() {
        ACTIVE.clear();
        EXPAND_COOLDOWN.clear();
        QUICK_COOLDOWN.clear();
    }

    private static void applyQuickReward(
            ServerPlayer owner,
            Reward reward
    ) {
        switch (reward) {
            case HEAL ->
                    owner.heal(
                            4.0F
                    );

            case SPEED ->
                    owner.addEffect(
                            new MobEffectInstance(
                                    MobEffects.MOVEMENT_SPEED,
                                    80,
                                    1,
                                    true,
                                    true
                            )
                    );

            case REGEN ->
                    owner.addEffect(
                            new MobEffectInstance(
                                    MobEffects.REGENERATION,
                                    80,
                                    1,
                                    true,
                                    true
                            )
                    );

            case JUMP ->
                    owner.addEffect(
                            new MobEffectInstance(
                                    MobEffects.JUMP,
                                    100,
                                    2,
                                    true,
                                    true
                            )
                    );

            case STRENGTH ->
                    owner.addEffect(
                            new MobEffectInstance(
                                    MobEffects.DAMAGE_BOOST,
                                    70,
                                    0,
                                    true,
                                    true
                            )
                    );

            case RESISTANCE ->
                    owner.addEffect(
                            new MobEffectInstance(
                                    MobEffects.DAMAGE_RESISTANCE,
                                    80,
                                    0,
                                    true,
                                    true
                            )
                    );
        }
    }

    private static void applyReward(
            ServerPlayer owner,
            Reward reward
    ) {
        switch (reward) {
            case HEAL ->
                    owner.heal(
                            0.45F
                    );

            case SPEED ->
                    owner.addEffect(
                            new MobEffectInstance(
                                    MobEffects.MOVEMENT_SPEED,
                                    28,
                                    0,
                                    true,
                                    false
                            )
                    );

            case REGEN ->
                    owner.addEffect(
                            new MobEffectInstance(
                                    MobEffects.REGENERATION,
                                    30,
                                    0,
                                    true,
                                    false
                            )
                    );

            case JUMP ->
                    owner.addEffect(
                            new MobEffectInstance(
                                    MobEffects.JUMP,
                                    34,
                                    1,
                                    true,
                                    false
                            )
                    );

            case STRENGTH ->
                    owner.addEffect(
                            new MobEffectInstance(
                                    MobEffects.DAMAGE_BOOST,
                                    28,
                                    0,
                                    true,
                                    false
                            )
                    );

            case RESISTANCE ->
                    owner.addEffect(
                            new MobEffectInstance(
                                    MobEffects.DAMAGE_RESISTANCE,
                                    28,
                                    0,
                                    true,
                                    false
                            )
                    );
        }
    }

    private static void applyConsequence(
            LivingEntity target,
            ServerPlayer owner,
            Consequence consequence
    ) {
        switch (consequence) {
            case IGNITE ->
                    target.igniteForSeconds(
                            2
                    );

            case SLOW ->
                    target.addEffect(
                            new MobEffectInstance(
                                    MobEffects.MOVEMENT_SLOWDOWN,
                                    36,
                                    1,
                                    true,
                                    true
                            )
                    );

            case WEAKEN ->
                    target.addEffect(
                            new MobEffectInstance(
                                    MobEffects.WEAKNESS,
                                    40,
                                    0,
                                    true,
                                    true
                            )
                    );

            case KNOCK -> {
                Vec3 away =
                        target.position()
                                .subtract(
                                        owner.position()
                                );

                if (away.lengthSqr() < 0.001) {
                    away =
                            new Vec3(
                                    1.0,
                                    0.0,
                                    0.0
                            );
                }

                away =
                        new Vec3(
                                away.x,
                                0.0,
                                away.z
                        ).normalize();

                target.push(
                        away.x * 0.62,
                        0.16,
                        away.z * 0.62
                );
            }

            case LIFT ->
                    target.push(
                            0.0,
                            0.34,
                            0.0
                    );

            case HUNGER ->
                    target.addEffect(
                            new MobEffectInstance(
                                    MobEffects.HUNGER,
                                    60,
                                    0,
                                    true,
                                    true
                            )
                    );
        }
    }

    private static void burst(
            ServerLevel level,
            Vec3 center,
            DomainProfile profile,
            int amount
    ) {
        DustParticleOptions dust =
                dust(
                        profile
                );

        level.sendParticles(
                dust,
                center.x,
                center.y + 1.0,
                center.z,
                amount,
                2.2,
                1.2,
                2.2,
                0.035
        );
    }

    private static DustParticleOptions dust(
            DomainProfile profile
    ) {
        return new DustParticleOptions(
                new Vector3f(
                        profile.color().red,
                        profile.color().green,
                        profile.color().blue
                ),
                1.15F
        );
    }

    private static final class ActiveDomain {

        private final UUID owner;

        private final ResourceKey<Level> dimension;

        private final Vec3 center;

        private final DomainProfile profile;

        private final long endsAt;

        private final Map<UUID, Vec3> lastPosition =
                new HashMap<>();

        private final Map<UUID, Float> lastHealth =
                new HashMap<>();

        private final Map<UUID, Integer> stillTicks =
                new HashMap<>();

        private final Map<UUID, Long> nextTrigger =
                new HashMap<>();

        private long nextOwnerReward;

        private ActiveDomain(
                UUID owner,
                ResourceKey<Level> dimension,
                Vec3 center,
                DomainProfile profile,
                long endsAt
        ) {
            this.owner =
                    owner;

            this.dimension =
                    dimension;

            this.center =
                    center;

            this.profile =
                    profile;

            this.endsAt =
                    endsAt;
        }

        private void tick(
                ServerLevel level,
                ServerPlayer ownerPlayer,
                long tick
        ) {
            if (tick % 4L == 0L) {
                renderBoundary(
                        level,
                        tick
                );
            }

            double radius =
                    profile.radius();

            AABB box =
                    new AABB(
                            center.x - radius,
                            center.y - 4.5,
                            center.z - radius,
                            center.x + radius,
                            center.y + 4.5,
                            center.z + radius
                    );

            List<LivingEntity> targets =
                    level.getEntitiesOfClass(
                            LivingEntity.class,
                            box,
                            entity ->
                                    entity.isAlive()
                                    && entity != ownerPlayer
                    );

            for (LivingEntity target :
                    targets) {

                double dx =
                        target.getX()
                        - center.x;

                double dz =
                        target.getZ()
                        - center.z;

                if (dx * dx + dz * dz
                        > radius * radius) {
                    continue;
                }

                evaluate(
                        target,
                        ownerPlayer,
                        tick
                );
            }
        }

        private void evaluate(
                LivingEntity target,
                ServerPlayer ownerPlayer,
                long tick
        ) {
            UUID id =
                    target.getUUID();

            Vec3 current =
                    target.position();

            Vec3 previous =
                    lastPosition.getOrDefault(
                            id,
                            current
                    );

            double moved =
                    current.distanceToSqr(
                            previous
                    );

            int still =
                    moved < 0.0016
                            ? stillTicks.getOrDefault(
                                    id,
                                    0
                            ) + 1
                            : 0;

            float health =
                    target.getHealth();

            float previousHealth =
                    lastHealth.getOrDefault(
                            id,
                            health
                    );

            boolean triggered =
                    switch (profile.trigger()) {
                        case MOVE ->
                                moved > 0.0064;

                        case STILL ->
                                still >= 16;

                        case AIRBORNE ->
                                !target.onGround();

                        case NEAR_OWNER ->
                                target.distanceToSqr(
                                        ownerPlayer
                                )
                                <= Math.pow(
                                        profile.radius()
                                        * 0.46,
                                        2.0
                                );

                        case HURT ->
                                health
                                < previousHealth
                                - 0.01F;
                    };

            lastPosition.put(
                    id,
                    current
            );

            stillTicks.put(
                    id,
                    still
            );

            lastHealth.put(
                    id,
                    health
            );

            if (!triggered) {
                return;
            }

            long readyAt =
                    nextTrigger.getOrDefault(
                            id,
                            0L
                    );

            if (tick < readyAt) {
                return;
            }

            nextTrigger.put(
                    id,
                    tick
                    + (
                            profile.trigger()
                                    == Trigger.HURT
                                    ? 4L
                                    : 12L
                    )
            );

            applyConsequence(
                    target,
                    ownerPlayer,
                    profile.consequence()
            );

            if (tick >= nextOwnerReward) {
                applyReward(
                        ownerPlayer,
                        profile.reward()
                );

                nextOwnerReward =
                        tick + 8L;
            }
        }

        private void renderBoundary(
                ServerLevel level,
                long tick
        ) {
            DustParticleOptions dust =
                    dust(
                            profile
                    );

            int points =
                    18;

            double radius =
                    profile.radius();

            double phase =
                    tick * 0.025;

            for (int i = 0;
                    i < points;
                    i++) {

                double angle =
                        phase
                        + i
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

                level.sendParticles(
                        dust,
                        x,
                        center.y + 0.20,
                        z,
                        1,
                        0.02,
                        0.02,
                        0.02,
                        0.0
                );

                if (i % 3 == 0) {
                    level.sendParticles(
                            dust,
                            x,
                            center.y + 2.8,
                            z,
                            1,
                            0.02,
                            0.02,
                            0.02,
                            0.0
                    );
                }
            }

            if (tick % 12L == 0L) {
                level.sendParticles(
                        ParticleTypes.PORTAL,
                        center.x,
                        center.y + 1.0,
                        center.z,
                        5,
                        radius * 0.35,
                        1.5,
                        radius * 0.35,
                        0.015
                );
            }
        }
    }
}
