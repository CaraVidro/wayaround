package net.caravidro.wayaround.ecology.ai;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Absolutely essential simulation: cats can beef with other cats.
 *
 * Tamed cats only play-fight; wild cats may deal tiny, non-lethal scratches.
 * Fights are intentionally short and self-terminating so the ecology does not
 * turn every village into permanent feline warfare.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class CatFightManager {

    private static final Map<UUID, Fight> FIGHTS =
            new HashMap<>();

    private static final Map<UUID, Long> COOLDOWN_UNTIL =
            new HashMap<>();

    private CatFightManager() {
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long now =
                server.getTickCount();

        tickFights(
                server,
                now
        );

        if ((now % 20L) != 0L) {
            return;
        }

        tryStartFights(
                server,
                now
        );

        if ((now % 400L) == 0L) {
            COOLDOWN_UNTIL.entrySet()
                    .removeIf(
                            entry ->
                                    entry.getValue()
                                            < now
                    );
        }
    }

    private static void tryStartFights(
            MinecraftServer server,
            long now
    ) {
        Set<UUID> seen =
                new HashSet<>();

        for (ServerPlayer player :
                server.getPlayerList()
                        .getPlayers()) {

            ServerLevel level =
                    player.serverLevel();

            if (!WorldFeatureRuntime.enabled(
                    level,
                    WorldFeature.LIVING_VEGETATION
            )) {
                continue;
            }

            AABB area =
                    player.getBoundingBox()
                            .inflate(
                                    24.0,
                                    12.0,
                                    24.0
                            );

            for (Cat cat :
                    level.getEntitiesOfClass(
                            Cat.class,
                            area,
                            CatFightManager::eligible
                    )) {

                if (!seen.add(
                        cat.getUUID()
                )
                        || inFight(
                        cat.getUUID()
                )
                        || now
                        < COOLDOWN_UNTIL.getOrDefault(
                        cat.getUUID(),
                        0L
                )) {
                    continue;
                }

                if (cat.getRandom()
                        .nextInt(
                                300
                        ) != 0) {
                    continue;
                }

                Cat rival =
                        level.getEntitiesOfClass(
                                        Cat.class,
                                        cat.getBoundingBox()
                                                .inflate(
                                                        7.0,
                                                        3.0,
                                                        7.0
                                                ),
                                        other ->
                                                other != cat
                                                        && eligible(
                                                        other
                                                )
                                                        && !inFight(
                                                        other.getUUID()
                                                )
                                                        && now
                                                        >= COOLDOWN_UNTIL
                                                        .getOrDefault(
                                                                other.getUUID(),
                                                                0L
                                                        )
                                )
                                .stream()
                                .min(
                                        java.util.Comparator
                                                .comparingDouble(
                                                        cat::distanceToSqr
                                                )
                                )
                                .orElse(
                                        null
                                );

                if (rival == null) {
                    continue;
                }

                start(
                        cat,
                        rival,
                        now
                );
            }
        }
    }

    private static boolean eligible(
            Cat cat
    ) {
        return cat.isAlive()
                && !cat.isBaby()
                && !cat.isInSittingPose()
                && cat.getHealth()
                > 2.5F;
    }

    private static boolean inFight(
            UUID cat
    ) {
        for (Fight fight :
                FIGHTS.values()) {
            if (fight.first.equals(
                    cat
            )
                    || fight.second.equals(
                    cat
            )) {
                return true;
            }
        }

        return false;
    }

    private static void start(
            Cat first,
            Cat second,
            long now
    ) {
        UUID id =
                UUID.randomUUID();

        boolean playful =
                first.isTame()
                        || second.isTame();

        Fight fight =
                new Fight(
                        id,
                        first.getUUID(),
                        second.getUUID(),
                        ((ServerLevel) first.level())
                                .dimension()
                                .location()
                                .toString(),
                        now
                                + 140L
                                + first.getRandom()
                                .nextInt(
                                90
                        ),
                        now + 8L,
                        playful
                );

        FIGHTS.put(
                id,
                fight
        );

        ((ServerLevel) first.level())
                .playSound(
                        null,
                        first.blockPosition(),
                        SoundEvents.CAT_HISS,
                        SoundSource.NEUTRAL,
                        0.65F,
                        0.92F
                );

        ((ServerLevel) second.level())
                .playSound(
                        null,
                        second.blockPosition(),
                        SoundEvents.CAT_HISS,
                        SoundSource.NEUTRAL,
                        0.65F,
                        1.08F
                );
    }

    private static void tickFights(
            MinecraftServer server,
            long now
    ) {
        Iterator<Map.Entry<UUID, Fight>> iterator =
                FIGHTS.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Fight fight =
                    iterator.next()
                            .getValue();

            Cat first =
                    findCat(
                            server,
                            fight.first
                    );

            Cat second =
                    findCat(
                            server,
                            fight.second
                    );

            if (first == null
                    || second == null
                    || !eligible(
                    first
            )
                    || !eligible(
                    second
            )
                    || ((ServerLevel) first.level())
                    != ((ServerLevel) second.level())
                    || now >= fight.endsAt
                    || first.distanceToSqr(
                    second
            ) > 11.0 * 11.0) {

                finish(
                        fight,
                        now
                );

                iterator.remove();
                continue;
            }

            first.getLookControl()
                    .setLookAt(
                            second,
                            30.0F,
                            30.0F
                    );

            second.getLookControl()
                    .setLookAt(
                            first,
                            30.0F,
                            30.0F
                    );

            double distance =
                    first.distanceTo(
                            second
                    );

            if (distance > 1.45) {
                first.getNavigation()
                        .moveTo(
                                second,
                                1.18
                        );

                second.getNavigation()
                        .moveTo(
                                first,
                                1.12
                        );

                continue;
            }

            first.getNavigation()
                    .stop();

            second.getNavigation()
                    .stop();

            if (now < fight.nextSwipe) {
                continue;
            }

            fight.nextSwipe =
                    now
                            + 9L
                            + first.getRandom()
                            .nextInt(
                            10
                    );

            boolean firstAttacks =
                    first.getRandom()
                            .nextBoolean();

            swipe(
                    firstAttacks
                            ? first
                            : second,
                    firstAttacks
                            ? second
                            : first,
                    fight.playful
            );
        }
    }

    private static void swipe(
            Cat attacker,
            Cat victim,
            boolean playful
    ) {
        ServerLevel level =
                ((ServerLevel) attacker.level());

        Vec3 away =
                victim.position()
                        .subtract(
                                attacker.position()
                        );

        if (away.lengthSqr()
                < 0.001) {
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

        victim.push(
                away.x * 0.18,
                0.10,
                away.z * 0.18
        );

        if (!playful
                && victim.getHealth()
                        > 3.0F) {
            victim.hurt(
                    level.damageSources()
                            .mobAttack(
                                    attacker
                            ),
                    0.5F
            );
        }

        level.sendParticles(
                ParticleTypes.CRIT,
                victim.getX(),
                victim.getY()
                        + victim.getBbHeight()
                                * 0.55,
                victim.getZ(),
                4,
                0.18,
                0.14,
                0.18,
                0.03
        );

        level.sendParticles(
                ParticleTypes.POOF,
                victim.getX(),
                victim.getY()
                        + 0.25,
                victim.getZ(),
                3,
                0.16,
                0.08,
                0.16,
                0.02
        );

        level.playSound(
                null,
                victim.blockPosition(),
                SoundEvents.CAT_HURT,
                SoundSource.NEUTRAL,
                0.38F,
                1.25F
                        + level.random.nextFloat()
                                * 0.35F
        );
    }

    private static void finish(
            Fight fight,
            long now
    ) {
        long cooldown =
                now
                        + 20L * 30L
                        + (long) (
                        Math.random()
                                * 20L
                                * 60L
                );

        COOLDOWN_UNTIL.put(
                fight.first,
                cooldown
        );

        COOLDOWN_UNTIL.put(
                fight.second,
                cooldown
        );
    }

    private static Cat findCat(
            MinecraftServer server,
            UUID id
    ) {
        for (ServerLevel level :
                server.getAllLevels()) {
            if (level.getEntity(
                    id
            ) instanceof Cat cat) {
                return cat;
            }
        }

        return null;
    }

    private static final class Fight {
        final UUID id;
        final UUID first;
        final UUID second;
        final String dimension;
        final long endsAt;
        final boolean playful;
        long nextSwipe;

        Fight(
                UUID id,
                UUID first,
                UUID second,
                String dimension,
                long endsAt,
                long nextSwipe,
                boolean playful
        ) {
            this.id =
                    id;
            this.first =
                    first;
            this.second =
                    second;
            this.dimension =
                    dimension;
            this.endsAt =
                    endsAt;
            this.nextSwipe =
                    nextSwipe;
            this.playful =
                    playful;
        }
    }
}
