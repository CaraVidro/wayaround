package net.caravidro.wayaround.cursed;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.network.ImmortalWheelReactivationPayload;
import net.caravidro.wayaround.network.ImmortalWheelVisualPayload;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Immortal Wheel is a PLAYER BINDING, not an inventory item.
 *
 * The legacy item stays registered only so old worlds do not lose registry
 * references. If an old item is found in a player's inventory it is silently
 * absorbed and converted into the binding.
 *
 * The only normal way to lose the binding is death. Death creates a dormant
 * physical remnant and resets all adaptation. Physical Black Flash scars stay
 * with the wheel through the remnant. A third Black Flash destroys the wheel.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class ImmortalWheelManager {

    private ImmortalWheelManager() {
    }

    private static final int BASE_HITS_PER_TURN = 4;
    private static final int EXTRA_HITS_PER_BLACK_FLASH = 2;
    private static final int MAX_STEPS = 5;
    private static final int COMBO_WINDOW_TICKS = 120;
    private static final int MAX_BLACK_FLASH_DAMAGE = 3;
    private static final int MAX_REBIRTHS = 2;
    private static final int REBIRTH_TICKS = 86;
    private static final double VISUAL_RANGE = 128.0;

    private static final String BOUND_KEY =
            "WayAroundImmortalWheelBound";

    private static final String DAMAGE_KEY =
            "WayAroundImmortalWheelDamage";

    private static final String LEGACY_DAMAGE_KEY =
            "WayAroundImmortalWheelBlackFlashDamage";

    private static final String REBIRTHS_KEY =
            "WayAroundImmortalWheelRebirthsUsed";

    private static final Map<UUID, Map<String, Adaptation>> ADAPTATIONS =
            new HashMap<>();

    private static final Set<UUID> PRESENT =
            new HashSet<>();

    private static final Map<UUID, RebirthState> REBIRTHING =
            new HashMap<>();

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !hasWheel(player)
                || event.getAmount() <= 0.0F) {
            return;
        }

        String family = classify(event.getSource());

        Map<String, Adaptation> book =
                ADAPTATIONS.computeIfAbsent(
                        player.getUUID(),
                        ignored -> new HashMap<>()
                );

        Adaptation adaptation =
                book.computeIfAbsent(
                        family,
                        ignored -> new Adaptation()
                );

        float reduction =
                Mth.clamp(
                        adaptation.steps * 0.20F,
                        0.0F,
                        1.0F
                );

        if (reduction > 0.0F) {
            event.setAmount(
                    Math.max(
                            0.0F,
                            event.getAmount() * (1.0F - reduction)
                    )
            );
        }

        if (adaptation.steps >= MAX_STEPS) {
            send(
                    player,
                    ImmortalWheelVisualPayload.HIT,
                    family,
                    0,
                    adaptation.steps
            );
            return;
        }

        long tick = player.server.getTickCount();

        if (tick - adaptation.lastHitTick > COMBO_WINDOW_TICKS) {
            adaptation.progress = 0;
        }

        adaptation.lastHitTick = tick;
        adaptation.progress++;

        int requiredHits =
                BASE_HITS_PER_TURN
                        + wheelDamage(player)
                                * EXTRA_HITS_PER_BLACK_FLASH;

        if (adaptation.progress < requiredHits) {
            send(
                    player,
                    ImmortalWheelVisualPayload.HIT,
                    family,
                    adaptation.progress,
                    adaptation.steps
            );
            return;
        }

        adaptation.progress = 0;
        adaptation.steps =
                Math.min(
                        MAX_STEPS,
                        adaptation.steps + 1
                );

        spin(
                player,
                family,
                adaptation.steps
        );
    }

    @SubscribeEvent
    public static void onDeath(
            LivingDeathEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)
                || !hasWheel(
                player
        )) {
            return;
        }

        int used =
                Mth.clamp(
                        player.getPersistentData()
                                .getInt(
                                        REBIRTHS_KEY
                                ),
                        0,
                        MAX_REBIRTHS
                );

        if (used < MAX_REBIRTHS) {
            event.setCanceled(
                    true
            );

            player.getPersistentData()
                    .putInt(
                            REBIRTHS_KEY,
                            used + 1
                    );

            player.setHealth(
                    1.0F
            );

            player.setDeltaMovement(
                    Vec3.ZERO
            );

            player.fallDistance =
                    0.0F;

            player.setInvulnerable(
                    true
            );

            REBIRTHING.put(
                    player.getUUID(),
                    new RebirthState(
                            player.getUUID(),
                            player.server.getTickCount(),
                            player.position(),
                            used + 1
                    )
            );

            ADAPTATIONS.remove(
                    player.getUUID()
            );

            ServerLevel level =
                    player.serverLevel();

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.TOTEM_USE,
                    SoundSource.PLAYERS,
                    2.0F,
                    0.38F
            );

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.PLAYERS,
                    1.5F,
                    0.54F
            );

            send(
                    player,
                    ImmortalWheelVisualPayload.SPIN,
                    "rebirth",
                    0,
                    MAX_STEPS
            );

            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "IMMORTAL WHEEL — RECONSTRUÇÃO "
                                    + (used + 1)
                                    + "/"
                                    + MAX_REBIRTHS
                    ).withStyle(
                            net.minecraft.ChatFormatting.GOLD,
                            net.minecraft.ChatFormatting.BOLD
                    ),
                    false
            );

            return;
        }

        int physicalDamage =
                wheelDamage(
                        player
                );

        REBIRTHING.remove(
                player.getUUID()
        );

        player.getPersistentData()
                .remove(
                        REBIRTHS_KEY
                );

        clearBinding(
                player
        );

        ADAPTATIONS.remove(
                player.getUUID()
        );

        PRESENT.remove(
                player.getUUID()
        );

        send(
                player,
                ImmortalWheelVisualPayload.REMOVE,
                "",
                0,
                0
        );

        spawnDormantRemnant(
                player.serverLevel(),
                player.position()
                        .add(
                                0.0,
                                0.34,
                                0.0
                        ),
                player.getDeltaMovement()
                        .scale(0.28)
                        .add(
                                0.0,
                                0.16,
                                0.0
                        ),
                physicalDamage,
                false
        );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.TOTEM_USE,
                        SoundSource.PLAYERS,
                        0.85F,
                        0.48F
                );
    }

    public static void damageByBlackFlash(
            ServerPlayer bearer,
            ServerPlayer attacker,
            int charge
    ) {
        if (!hasWheel(bearer)) {
            return;
        }

        int damage =
                Mth.clamp(
                        wheelDamage(bearer) + 1,
                        0,
                        MAX_BLACK_FLASH_DAMAGE
                );

        ServerLevel level =
                bearer.serverLevel();

        Vec3 halo =
                bearer.position()
                        .add(
                                0.0,
                                bearer.getBbHeight() + 0.42,
                                0.0
                        );

        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                halo.x,
                halo.y,
                halo.z,
                34 + charge * 5,
                0.52,
                0.18,
                0.52,
                0.20
        );

        level.sendParticles(
                ParticleTypes.SMOKE,
                halo.x,
                halo.y,
                halo.z,
                12 + damage * 8,
                0.42,
                0.12,
                0.42,
                0.035
        );

        if (damage < MAX_BLACK_FLASH_DAMAGE) {
            setWheelDamage(
                    bearer,
                    damage
            );

            level.playSound(
                    null,
                    bearer.blockPosition(),
                    SoundEvents.ANVIL_LAND,
                    SoundSource.PLAYERS,
                    0.85F,
                    1.18F - damage * 0.12F
            );

            bearer.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.wayaround.immortal_wheel.black_flash_damage",
                            damage,
                            MAX_BLACK_FLASH_DAMAGE,
                            BASE_HITS_PER_TURN
                                    + damage
                                            * EXTRA_HITS_PER_BLACK_FLASH
                    ),
                    true
            );

            return;
        }

        clearBinding(
                bearer
        );

        ADAPTATIONS.remove(
                bearer.getUUID()
        );

        PRESENT.remove(
                bearer.getUUID()
        );

        send(
                bearer,
                ImmortalWheelVisualPayload.REMOVE,
                "",
                0,
                0
        );

        Vec3 knock =
                bearer.position()
                        .subtract(
                                attacker.position()
                        );

        if (knock.lengthSqr() < 0.0001) {
            knock =
                    new Vec3(
                            0.0,
                            0.0,
                            1.0
                    );
        }

        knock =
                knock.normalize()
                        .scale(
                                0.36
                        )
                        .add(
                                0.0,
                                0.34,
                                0.0
                        );

        spawnDormantRemnant(
                level,
                halo,
                knock,
                MAX_BLACK_FLASH_DAMAGE,
                true
        );

        level.playSound(
                null,
                bearer.blockPosition(),
                SoundEvents.ANVIL_BREAK,
                SoundSource.PLAYERS,
                1.35F,
                0.62F
        );

        level.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                halo.x,
                halo.y,
                halo.z,
                42,
                0.62,
                0.32,
                0.62,
                0.08
        );
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long tick = server.getTickCount();

        tickRebirths(
                server,
                tick
        );

        if (tick % 20L != 0L) {
            return;
        }

        Set<UUID> nowPresent =
                new HashSet<>();

        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {

            migrateLegacyItem(
                    player
            );

            UUID id = player.getUUID();

            if (!isBound(player)) {
                if (PRESENT.remove(id)) {
                    send(
                            player,
                            ImmortalWheelVisualPayload.REMOVE,
                            "",
                            0,
                            0
                    );

                    ADAPTATIONS.remove(id);
                }
                continue;
            }

            nowPresent.add(id);
            PRESENT.add(id);

            send(
                    player,
                    ImmortalWheelVisualPayload.PRESENCE,
                    "",
                    0,
                    highestStep(id)
            );
        }

        PRESENT.retainAll(nowPresent);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ADAPTATIONS.clear();
        PRESENT.clear();
        REBIRTHING.clear();
    }

    public static void bindFromRemnant(
            ServerPlayer newOwner,
            int physicalDamage,
            Vec3 origin
    ) {
        bind(
                newOwner,
                physicalDamage
        );

        ADAPTATIONS.remove(
                newOwner.getUUID()
        );

        PRESENT.add(
                newOwner.getUUID()
        );

        ServerLevel level =
                newOwner.serverLevel();

        Vec3 target =
                newOwner.position()
                        .add(
                                0.0,
                                newOwner.getBbHeight() + 0.42,
                                0.0
                        );

        level.playSound(
                null,
                origin.x,
                origin.y,
                origin.z,
                SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS,
                1.25F,
                0.82F - physicalDamage * 0.035F
        );

        for (int i = 0; i <= 14; i++) {
            double t = i / 14.0;

            Vec3 point =
                    origin.lerp(
                            target,
                            t
                    ).add(
                            0.0,
                            Math.sin(Math.PI * t) * 0.32,
                            0.0
                    );

            level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    point.x,
                    point.y,
                    point.z,
                    3,
                    0.06,
                    0.06,
                    0.06,
                    0.025
            );
        }

        level.sendParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                target.x,
                target.y,
                target.z,
                58,
                0.72,
                0.24,
                0.72,
                0.18
        );

        PacketDistributor.sendToPlayersNear(
                level,
                null,
                newOwner.getX(),
                newOwner.getY(),
                newOwner.getZ(),
                VISUAL_RANGE,
                new ImmortalWheelReactivationPayload(
                        newOwner.getUUID(),
                        origin.x,
                        origin.y,
                        origin.z,
                        0
                )
        );

        send(
                newOwner,
                ImmortalWheelVisualPayload.PRESENCE,
                "",
                0,
                0
        );
    }

    private static void tickRebirths(
            MinecraftServer server,
            long tick
    ) {
        java.util.Iterator<Map.Entry<UUID, RebirthState>> iterator =
                REBIRTHING.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            RebirthState state =
                    iterator.next()
                            .getValue();

            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(
                                    state.owner
                            );

            if (player == null
                    || !hasWheel(
                    player
            )) {
                iterator.remove();
                continue;
            }

            int age =
                    (int) Math.max(
                            0L,
                            tick - state.startedAt
                    );

            float progress =
                    Mth.clamp(
                            age
                                    / (float) REBIRTH_TICKS,
                            0.0F,
                            1.0F
                    );

            /*
             * The body is held in place while rebuilding. Visually, particles
             * start scattered around the old body volume and collapse inward,
             * making the regeneration read as piece-by-piece reconstruction.
             */
            player.setDeltaMovement(
                    Vec3.ZERO
            );

            player.fallDistance =
                    0.0F;

            ServerLevel level =
                    player.serverLevel();

            Vec3 body =
                    player.position()
                            .add(
                                    0.0,
                                    player.getBbHeight()
                                            * 0.52,
                                    0.0
                            );

            int fragments =
                    7
                            + Math.round(
                            progress
                                    * 15.0F
                    );

            for (int i = 0;
                 i < fragments;
                 i++) {

                double angle =
                        level.random.nextDouble()
                                * Math.PI
                                * 2.0;

                double radius =
                        2.2
                                * (
                                1.0 - progress
                        )
                                + 0.18
                                + level.random.nextDouble()
                                        * 0.45;

                double y =
                        (
                                level.random.nextDouble()
                                        - 0.5
                        )
                                * (
                                2.4
                                        - progress
                                                * 1.4
                        );

                Vec3 source =
                        body.add(
                                Math.cos(angle)
                                        * radius,
                                y,
                                Math.sin(angle)
                                        * radius
                        );

                Vec3 inward =
                        body.subtract(
                                source
                        );

                if (inward.lengthSqr()
                        > 0.0001) {
                    inward =
                            inward.normalize();
                }

                level.sendParticles(
                        i % 4 == 0
                                ? ParticleTypes.TOTEM_OF_UNDYING
                                : ParticleTypes.ELECTRIC_SPARK,
                        source.x,
                        source.y,
                        source.z,
                        0,
                        inward.x,
                        inward.y,
                        inward.z,
                        0.28
                                + progress
                                        * 0.52
                );
            }

            if (age % 6 == 0) {
                send(
                        player,
                        ImmortalWheelVisualPayload.SPIN,
                        "rebirth",
                        age,
                        MAX_STEPS
                );
            }

            float targetHealth =
                    Math.max(
                            1.0F,
                            player.getMaxHealth()
                                    * (
                                    0.06F
                                            + progress
                                                    * 0.94F
                            )
                    );

            if (player.getHealth()
                    < targetHealth) {
                player.setHealth(
                        targetHealth
                );
            }

            if (progress >= 1.0F) {
                player.setHealth(
                        player.getMaxHealth()
                );

                player.setInvulnerable(
                        false
                );

                level.playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.TOTEM_USE,
                        SoundSource.PLAYERS,
                        1.35F,
                        1.18F
                );

                level.sendParticles(
                        ParticleTypes.TOTEM_OF_UNDYING,
                        body.x,
                        body.y,
                        body.z,
                        110,
                        0.85,
                        1.05,
                        0.85,
                        0.30
                );

                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "RECONSTRUÇÃO COMPLETA — resta(m) "
                                        + Math.max(
                                        0,
                                        MAX_REBIRTHS
                                                - state.rebirthIndex
                                )
                                        + " renascimento(s)."
                        ).withStyle(
                                net.minecraft.ChatFormatting.YELLOW
                        ),
                        false
                );

                iterator.remove();
            }
        }
    }

    private static void spawnDormantRemnant(
            ServerLevel level,
            Vec3 position,
            Vec3 velocity,
            int physicalDamage,
            boolean shattered
    ) {
        ImmortalWheelRemnantEntity remnant =
                new ImmortalWheelRemnantEntity(
                        WayAroundContent.IMMORTAL_WHEEL_REMNANT.get(),
                        level
                );

        remnant.setPos(
                position.x,
                position.y,
                position.z
        );

        remnant.setDeltaMovement(
                velocity
        );

        remnant.setWheelDamage(
                physicalDamage
        );

        remnant.setShattered(
                shattered
        );

        level.addFreshEntity(
                remnant
        );
    }

    private static void bind(
            ServerPlayer player,
            int damage
    ) {
        player.getPersistentData()
                .putBoolean(
                        BOUND_KEY,
                        true
                );

        player.getPersistentData()
                .putInt(
                        DAMAGE_KEY,
                        Mth.clamp(
                                damage,
                                0,
                                MAX_BLACK_FLASH_DAMAGE - 1
                        )
                );

        player.getPersistentData()
                .putInt(
                        REBIRTHS_KEY,
                        0
                );
    }

    private static void clearBinding(
            ServerPlayer player
    ) {
        player.getPersistentData()
                .remove(
                        BOUND_KEY
                );

        player.getPersistentData()
                .remove(
                        DAMAGE_KEY
                );

        player.getPersistentData()
                .remove(
                        REBIRTHS_KEY
                );

        REBIRTHING.remove(
                player.getUUID()
        );
    }

    private static boolean isBound(
            ServerPlayer player
    ) {
        return player.getPersistentData()
                .getBoolean(
                        BOUND_KEY
                );
    }

    private static void migrateLegacyItem(
            ServerPlayer player
    ) {
        if (isBound(player)) {
            removeLegacyItems(player);
            return;
        }

        int bestDamage = 0;
        boolean found = false;

        for (int slot = 0;
             slot < player.getInventory().getContainerSize();
             slot++) {

            ItemStack stack =
                    player.getInventory().getItem(
                            slot
                    );

            if (!stack.is(
                    WayAroundContent.IMMORTAL_WHEEL.get()
            )) {
                continue;
            }

            found = true;
            bestDamage =
                    Math.max(
                            bestDamage,
                            legacyWheelDamage(stack)
                    );

            stack.setCount(
                    0
            );
        }

        if (found) {
            player.getInventory()
                    .setChanged();

            bind(
                    player,
                    bestDamage
            );

            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.wayaround.immortal_wheel.bound"
                    ),
                    true
            );
        }
    }

    private static void removeLegacyItems(
            ServerPlayer player
    ) {
        boolean changed = false;

        for (int slot = 0;
             slot < player.getInventory().getContainerSize();
             slot++) {

            ItemStack stack =
                    player.getInventory().getItem(
                            slot
                    );

            if (stack.is(
                    WayAroundContent.IMMORTAL_WHEEL.get()
            )) {
                stack.setCount(
                        0
                );
                changed = true;
            }
        }

        if (changed) {
            player.getInventory()
                    .setChanged();
        }
    }

    private static int legacyWheelDamage(
            ItemStack stack
    ) {
        CustomData data =
                stack.get(
                        DataComponents.CUSTOM_DATA
                );

        if (data == null) {
            return 0;
        }

        return Mth.clamp(
                data.copyTag()
                        .getInt(
                                LEGACY_DAMAGE_KEY
                        ),
                0,
                MAX_BLACK_FLASH_DAMAGE - 1
        );
    }

    public static boolean hasWheel(
            ServerPlayer player
    ) {
        migrateLegacyItem(
                player
        );

        return isBound(
                player
        );
    }

    public static int wheelDamage(
            ServerPlayer player
    ) {
        return Mth.clamp(
                player.getPersistentData()
                        .getInt(
                                DAMAGE_KEY
                        ),
                0,
                MAX_BLACK_FLASH_DAMAGE - 1
        );
    }

    private static void setWheelDamage(
            ServerPlayer player,
            int damage
    ) {
        player.getPersistentData()
                .putInt(
                        DAMAGE_KEY,
                        Mth.clamp(
                                damage,
                                0,
                                MAX_BLACK_FLASH_DAMAGE - 1
                        )
                );
    }

    private static void spin(
            ServerPlayer player,
            String family,
            int steps
    ) {
        ServerLevel level = player.serverLevel();

        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS,
                1.15F,
                0.72F + steps * 0.055F
        );

        level.sendParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(),
                player.getY() + player.getBbHeight() + 0.72,
                player.getZ(),
                72,
                1.35,
                0.48,
                1.35,
                0.24
        );

        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                player.getX(),
                player.getY() + player.getBbHeight() + 0.72,
                player.getZ(),
                28,
                1.1,
                0.32,
                1.1,
                0.14
        );

        send(
                player,
                ImmortalWheelVisualPayload.SPIN,
                family,
                0,
                steps
        );
    }

    private static void send(
            ServerPlayer owner,
            byte action,
            String family,
            int progress,
            int steps
    ) {
        ServerLevel level = owner.serverLevel();

        PacketDistributor.sendToPlayersNear(
                level,
                null,
                owner.getX(),
                owner.getY(),
                owner.getZ(),
                VISUAL_RANGE,
                new ImmortalWheelVisualPayload(
                        owner.getUUID(),
                        action,
                        family,
                        progress,
                        steps
                )
        );
    }

    private static int highestStep(UUID owner) {
        Map<String, Adaptation> book =
                ADAPTATIONS.get(owner);

        if (book == null
                || book.isEmpty()) {
            return 0;
        }

        int highest = 0;

        for (Adaptation adaptation :
                book.values()) {
            highest =
                    Math.max(
                            highest,
                            adaptation.steps
                    );
        }

        return highest;
    }

    private static String classify(DamageSource source) {
        Entity direct = source.getDirectEntity();

        if (direct instanceof AbstractArrow) {
            return "arrow";
        }

        if (source.is(DamageTypeTags.IS_PROJECTILE)
                || direct instanceof Projectile) {
            return "projectile";
        }

        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return "explosion";
        }

        if (source.is(DamageTypeTags.IS_FIRE)) {
            return "fire";
        }

        if (source.is(DamageTypeTags.IS_FALL)) {
            return "fall";
        }

        if (source.is(DamageTypeTags.IS_FREEZING)) {
            return "freezing";
        }

        if (source.is(DamageTypeTags.IS_LIGHTNING)) {
            return "lightning";
        }

        Entity attacker = source.getEntity();

        if (attacker instanceof Player) {
            return "player_melee";
        }

        if (attacker instanceof LivingEntity) {
            return "mob_melee";
        }

        String id = source.getMsgId();

        if (id != null) {
            String lowered =
                    id.toLowerCase(
                            java.util.Locale.ROOT
                    );

            if (lowered.contains("magic")
                    || lowered.contains("wither")) {
                return "magic";
            }

            if (lowered.contains("sonic")) {
                return "sonic";
            }

            if (lowered.contains("drown")) {
                return "drowning";
            }
        }

        return "generic";
    }

    private static final class RebirthState {
        private final UUID owner;
        private final long startedAt;
        @SuppressWarnings("unused")
        private final Vec3 deathPosition;
        private final int rebirthIndex;

        private RebirthState(
                UUID owner,
                long startedAt,
                Vec3 deathPosition,
                int rebirthIndex
        ) {
            this.owner = owner;
            this.startedAt = startedAt;
            this.deathPosition = deathPosition;
            this.rebirthIndex = rebirthIndex;
        }
    }

    private static final class Adaptation {
        private int progress;
        private int steps;
        private long lastHitTick =
                Long.MIN_VALUE / 4L;
    }
}
