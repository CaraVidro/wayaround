package net.caravidro.wayaround.ecology.ai;

import java.util.Comparator;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.ecology.FishCarcassEntity;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Ecological fishing: the bobber attracts actual fish entities and reeling
 * applies physical force to the fish instead of materializing a fish item.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class EcologyFishingManager {

    private static final String TARGET =
            "WayAroundFishingTarget";

    private static final String BITE_READY =
            "WayAroundFishingBiteReady";

    private static final String HOOKED =
            "WayAroundFishingHooked";

    private static final String LAST_TUG =
            "WayAroundFishingLastTug";

    private static final String TUG_STREAK =
            "WayAroundFishingTugStreak";

    private EcologyFishingManager() {
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.LIVING_VEGETATION
        )) {
            return;
        }

        boolean attractionTick =
                event.getServer()
                        .getTickCount()
                        % 5
                        == 0;

        for (ServerLevel level :
                event.getServer()
                        .getAllLevels()) {

            for (ServerPlayer player :
                    level.players()) {

                FishingHook hook =
                        player.fishing;

                if (hook == null
                        || !hook.isAlive()) {
                    continue;
                }

                boolean hooked =
                        hook.getPersistentData()
                                .getBoolean(
                                        HOOKED
                                );

                if (hooked) {
                    tickHook(
                            level,
                            player,
                            hook
                    );

                    continue;
                }

                if (!attractionTick
                        || !level.getFluidState(
                        hook.blockPosition()
                ).is(
                        FluidTags.WATER
                )) {
                    continue;
                }

                tickHook(
                        level,
                        player,
                        hook
                );
            }
        }
    }

    private static void tickHook(
            ServerLevel level,
            ServerPlayer player,
            FishingHook hook
    ) {
        CompoundTag data =
                hook.getPersistentData();

        long now =
                level.getGameTime();

        if (!data.contains(
                BITE_READY
        )) {
            data.putLong(
                    BITE_READY,
                    now
                            + 35L
                            + level.random.nextInt(
                            90
                    )
            );
        }

        boolean wasHooked =
                data.getBoolean(
                        HOOKED
                );

        AbstractFish target =
                targetFish(
                        level,
                        hook
                );

        if (target == null
                && wasHooked) {
            return;
        }

        if (target == null) {
            target =
                    nearestFish(
                            level,
                            hook.position(),
                            14.0
                    );

            if (target == null) {
                return;
            }

            setTarget(
                    hook,
                    target
            );
        }

        CompoundTag fishData =
                target.getPersistentData();

        if (data.getBoolean(
                HOOKED
        )) {
            attachHookToFish(
                    hook,
                    target
            );

            struggleAgainstLine(
                    level,
                    player,
                    target
            );

            return;
        }

        if (fishData.getLong(
                LivingFaunaManager.FISH_SCARED_UNTIL
        )
                > now) {
            clearTarget(
                    hook
            );

            return;
        }

        double distance =
                target.distanceTo(
                        hook
                );

        long lureUntil =
                fishData.getLong(
                        LivingFaunaManager.FISH_LURE_UNTIL
                );

        double speed =
                lureUntil > now
                        ? 1.32
                        : 1.04;

        speed /=
                Math.max(
                        0.70,
                        Math.sqrt(
                                LivingFaunaManager.fishSize(
                                        target
                                )
                        )
                                * 0.72
                );

        target.getNavigation()
                .moveTo(
                        hook.getX(),
                        hook.getY(),
                        hook.getZ(),
                        speed
                );

        if (distance <= 0.90
                && now >= data.getLong(
                BITE_READY
        )) {
            data.putBoolean(
                    HOOKED,
                    true
            );

            fishData.putLong(
                    LivingFaunaManager.FISH_LURE_UNTIL,
                    now + 200L
            );

            target.getNavigation()
                    .stop();

            attachHookToFish(
                    hook,
                    target
            );

            level.playSound(
                    null,
                    hook.blockPosition(),
                    SoundEvents.FISHING_BOBBER_SPLASH,
                    SoundSource.NEUTRAL,
                    0.75F,
                    0.82F
                            + level.random.nextFloat()
                                    * 0.35F
            );

            level.sendParticles(
                    ParticleTypes.BUBBLE,
                    hook.getX(),
                    hook.getY(),
                    hook.getZ(),
                    12,
                    0.28,
                    0.16,
                    0.28,
                    0.05
            );

            level.sendParticles(
                    ParticleTypes.SPLASH,
                    hook.getX(),
                    hook.getY(),
                    hook.getZ(),
                    8,
                    0.32,
                    0.08,
                    0.32,
                    0.08
            );
        }
    }

    @SubscribeEvent
    public static void tug(
            PlayerInteractEvent.RightClickItem event
    ) {
        if (!event.getItemStack()
                .is(
                        Items.FISHING_ROD
                )
                || event.getEntity()
                        .fishing
                        == null
                || event.getEntity()
                        .isShiftKeyDown()
                || !WorldFeatureRuntime.enabled(
                        event.getLevel(),
                        WorldFeature.LIVING_VEGETATION
                )) {
            return;
        }

        /*
         * Right click becomes "tug/reel" while a line is already cast.
         * Sneak + right click intentionally falls through to vanilla so the
         * player can fully retrieve/cancel the line.
         */
        event.setCanceled(
                true
        );

        event.setCancellationResult(
                InteractionResult.SUCCESS
        );

        if (!(event.getEntity()
                instanceof ServerPlayer player)
                || !(event.getLevel()
                instanceof ServerLevel level)) {
            return;
        }

        FishingHook hook =
                player.fishing;

        if (hook == null
                || !hook.isAlive()) {
            return;
        }

        pullLine(
                level,
                player,
                hook
        );
    }

    private static void pullLine(
            ServerLevel level,
            ServerPlayer player,
            FishingHook hook
    ) {
        CompoundTag data =
                hook.getPersistentData();

        long now =
                level.getGameTime();

        long last =
                data.getLong(
                        LAST_TUG
                );

        int streak =
                now - last < 14L
                        ? data.getInt(
                                TUG_STREAK
                        )
                                + 1
                        : 1;

        data.putLong(
                LAST_TUG,
                now
        );

        data.putInt(
                TUG_STREAK,
                streak
        );

        AbstractFish target =
                targetFish(
                        level,
                        hook
                );

        if (target == null) {
            target =
                    nearestFish(
                            level,
                            hook.position(),
                            15.0
                    );

            if (target == null) {
                pullLooseLine(
                        player,
                        hook
                );

                splashTug(
                        level,
                        hook
                );

                return;
            }

            setTarget(
                    hook,
                    target
            );
        }

        double biteDistance =
                target.distanceTo(
                        hook
                );

        boolean biting =
                data.getBoolean(
                        HOOKED
                )
                        || (
                        biteDistance <= 1.35
                                && now >= data.getLong(
                                BITE_READY
                        )
                );

        if (!biting) {
            if (streak >= 3) {
                frighten(
                        level,
                        target,
                        hook.position(),
                        now
                );

                clearTarget(
                        hook
                );

                data.putInt(
                        TUG_STREAK,
                        0
                );

            } else {
                target.getPersistentData()
                        .putLong(
                                LivingFaunaManager.FISH_LURE_UNTIL,
                                now + 120L
                        );

                target.getNavigation()
                        .moveTo(
                                hook.getX(),
                                hook.getY(),
                                hook.getZ(),
                                1.34
                        );
            }

            splashTug(
                    level,
                    hook
            );

            return;
        }

        if (!data.getBoolean(
                HOOKED
        )) {
            data.putBoolean(
                    HOOKED,
                    true
            );

            target.getNavigation()
                    .stop();
        }

        Vec3 pull =
                player.getEyePosition()
                        .subtract(
                                target.position()
                                        .add(
                                                0.0,
                                                target.getBbHeight()
                                                        * 0.45,
                                                0.0
                                        )
                        );

        double distance =
                Math.max(
                        0.001,
                        pull.length()
                );

        float size =
                LivingFaunaManager.fishSize(
                        target
                );

        double strength =
                Mth.clamp(
                        0.62
                                / Math.sqrt(
                                Math.max(
                                        0.35F,
                                        size
                                )
                        ),
                        0.13,
                        0.64
                );

        Vec3 impulse =
                pull.scale(
                        1.0 / distance
                )
                        .scale(
                                strength
                        );

        if (distance < 3.0) {
            impulse =
                    impulse.add(
                            0.0,
                            0.24
                                    / Math.max(
                                    0.65F,
                                    size
                            ),
                            0.0
                    );
        }

        target.setDeltaMovement(
                target.getDeltaMovement()
                        .scale(
                                0.46
                        )
                        .add(
                                impulse
                        )
        );

        target.hurtMarked =
                true;

        attachHookToFish(
                hook,
                target
        );

        target.getPersistentData()
                .putLong(
                        LivingFaunaManager.FISH_LURE_UNTIL,
                        now + 160L
                );

        level.playSound(
                null,
                target.blockPosition(),
                SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.NEUTRAL,
                0.50F,
                Mth.clamp(
                        1.35F
                                - size
                                        * 0.12F,
                        0.65F,
                        1.35F
                )
        );

        splashTug(
                level,
                hook
        );
    }

    private static void attachHookToFish(
            FishingHook hook,
            AbstractFish fish
    ) {
        Vec3 mouth =
                fish.position()
                        .add(
                                0.0,
                                fish.getBbHeight()
                                        * 0.48,
                                0.0
                        );

        hook.setPos(
                mouth.x,
                mouth.y,
                mouth.z
        );

        hook.setDeltaMovement(
                fish.getDeltaMovement()
        );
    }

    private static void pullLooseLine(
            ServerPlayer player,
            FishingHook hook
    ) {
        Vec3 pull =
                player.getEyePosition()
                        .subtract(
                                hook.position()
                        );

        double distance =
                pull.length();

        if (distance <= 0.001) {
            return;
        }

        Vec3 impulse =
                pull.scale(
                        1.0 / distance
                )
                        .scale(
                                Mth.clamp(
                                        0.18
                                                + distance
                                                        * 0.018,
                                        0.18,
                                        0.62
                                )
                        );

        hook.setDeltaMovement(
                hook.getDeltaMovement()
                        .scale(
                                0.42
                        )
                        .add(
                                impulse
                        )
        );

        hook.hasImpulse =
                true;
    }

    private static void struggleAgainstLine(
            ServerLevel level,
            ServerPlayer player,
            AbstractFish fish
    ) {
        if (!fish.isInWater()) {
            return;
        }

        float size =
                LivingFaunaManager.fishSize(
                        fish
                );

        if (size < 1.15F
                || level.random.nextFloat()
                        > Math.min(
                        0.32F,
                        0.035F
                                + size
                                        * 0.052F
                )) {
            return;
        }

        Vec3 away =
                fish.position()
                        .subtract(
                                player.position()
                        )
                        .multiply(
                                1.0,
                                0.25,
                                1.0
                        );

        if (away.lengthSqr()
                < 0.001) {
            away =
                    new Vec3(
                            level.random.nextDouble()
                                    - 0.5,
                            0.15,
                            level.random.nextDouble()
                                    - 0.5
                    );
        }

        Vec3 sideways =
                new Vec3(
                        -away.z,
                        0.0,
                        away.x
                );

        away =
                away.normalize();

        if (sideways.lengthSqr()
                > 0.001) {
            sideways =
                    sideways.normalize()
                            .scale(
                                    (
                                    level.random.nextBoolean()
                                            ? 1.0
                                            : -1.0
                            )
                                            * 0.22
                                            * Math.min(
                                            2.0F,
                                            size
                                    )
                            );
        }

        double resistance =
                Mth.clamp(
                        0.045
                                + size
                                        * 0.050,
                        0.06,
                        0.28
                );

        fish.setDeltaMovement(
                fish.getDeltaMovement()
                        .add(
                                away.scale(
                                        resistance
                                )
                        )
                        .add(
                                sideways
                        )
        );

        fish.hurtMarked =
                true;

        level.sendParticles(
                ParticleTypes.SPLASH,
                fish.getX(),
                fish.getY()
                        + fish.getBbHeight()
                                * 0.55,
                fish.getZ(),
                Mth.clamp(
                        Math.round(
                                3.0F
                                        + size
                                                * 3.0F
                        ),
                        4,
                        14
                ),
                0.18
                        + fish.getBbWidth()
                                * 0.16,
                0.10,
                0.18
                        + fish.getBbWidth()
                                * 0.16,
                0.07
        );
    }

    private static void frighten(
            ServerLevel level,
            AbstractFish fish,
            Vec3 source,
            long now
    ) {
        Vec3 away =
                fish.position()
                        .subtract(
                                source
                        );

        if (away.lengthSqr()
                < 0.001) {
            away =
                    new Vec3(
                            level.random.nextDouble()
                                    - 0.5,
                            0.0,
                            level.random.nextDouble()
                                    - 0.5
                    );
        }

        away =
                away.normalize();

        fish.getPersistentData()
                .putLong(
                        LivingFaunaManager.FISH_SCARED_UNTIL,
                        now + 100L
                );

        fish.getNavigation()
                .moveTo(
                        fish.getX()
                                + away.x
                                        * 9.0,
                        fish.getY()
                                + (
                                level.random.nextDouble()
                                        - 0.5
                        )
                                        * 2.0,
                        fish.getZ()
                                + away.z
                                        * 9.0,
                        1.55
                );

        level.sendParticles(
                ParticleTypes.SPLASH,
                fish.getX(),
                fish.getY()
                        + fish.getBbHeight()
                                * 0.5,
                fish.getZ(),
                10,
                0.25,
                0.16,
                0.25,
                0.10
        );
    }

    private static void splashTug(
            ServerLevel level,
            FishingHook hook
    ) {
        level.sendParticles(
                ParticleTypes.SPLASH,
                hook.getX(),
                hook.getY(),
                hook.getZ(),
                5,
                0.18,
                0.06,
                0.18,
                0.04
        );
    }

    private static AbstractFish nearestFish(
            ServerLevel level,
            Vec3 center,
            double radius
    ) {
        long now =
                level.getGameTime();

        return level.getEntitiesOfClass(
                        AbstractFish.class,
                        new AABB(
                                center,
                                center
                        ).inflate(
                                radius,
                                Math.min(
                                        8.0,
                                        radius
                                ),
                                radius
                        ),
                        fish ->
                                fish.isAlive()
                                        && !(fish instanceof FishCarcassEntity)
                                        && fish.getPersistentData()
                                                .getLong(
                                                        LivingFaunaManager.FISH_SCARED_UNTIL
                                                )
                                                <= now
                )
                .stream()
                .min(
                        Comparator.comparingDouble(
                                fish ->
                                        fish.position()
                                                .distanceToSqr(
                                                        center
                                                )
                        )
                )
                .orElse(null);
    }

    private static AbstractFish targetFish(
            ServerLevel level,
            FishingHook hook
    ) {
        CompoundTag data =
                hook.getPersistentData();

        if (!data.hasUUID(
                TARGET
        )) {
            return null;
        }

        UUID id =
                data.getUUID(
                        TARGET
                );

        Entity entity =
                level.getEntity(
                        id
                );

        if (entity
                instanceof AbstractFish fish
                && fish.isAlive()
                && !(fish instanceof FishCarcassEntity)) {
            return fish;
        }

        data.remove(
                TARGET
        );

        data.putBoolean(
                HOOKED,
                false
        );

        return null;
    }

    private static void setTarget(
            FishingHook hook,
            AbstractFish fish
    ) {
        hook.getPersistentData()
                .putUUID(
                        TARGET,
                        fish.getUUID()
                );
    }

    private static void clearTarget(
            FishingHook hook
    ) {
        hook.getPersistentData()
                .remove(
                        TARGET
                );
    }

    @SubscribeEvent
    public static void suppressMaterializedFish(
            ItemFishedEvent event
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.LIVING_VEGETATION
        )) {
            return;
        }

        /*
         * Ecological fishing owns the catch completely. Vanilla fish,
         * treasure and junk may no longer materialize from the bobber.
         */
        event.getDrops()
                .clear();

        event.setCanceled(
                true
        );

        event.damageRodBy(
                0
        );
    }
}
