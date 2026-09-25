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
 * Immortal Wheel state.
 *
 * Adaptation belongs to the CURRENT holder and is lost on death.
 * Physical Black Flash damage belongs to the WHEEL item and survives a death
 * remnant / new holder. One or two scars slow future adaptation; the third
 * Black Flash destroys the artifact completely.
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
    private static final double VISUAL_RANGE = 128.0;

    private static final String BLACK_FLASH_DAMAGE_KEY =
            "WayAroundImmortalWheelBlackFlashDamage";

    private static final Map<UUID, Map<String, Adaptation>> ADAPTATIONS =
            new HashMap<>();

    private static final Set<UUID> PRESENT =
            new HashSet<>();

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

    /**
     * Death always drops the cursed wheel as a dormant remnant, but the
     * holder's learned adaptations are intentionally erased.
     *
     * Physical Black Flash scars remain stored on the artifact itself.
     */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack wheel =
                removeOneWheel(
                        player
                );

        if (wheel.isEmpty()) {
            return;
        }

        int physicalDamage =
                wheelDamage(
                        wheel
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

        WayAround.LOGGER.info(
                "[ImmortalWheel] {} morreu: a roda caiu dormente e esqueceu todas as adaptações.",
                player.getGameProfile().getName()
        );
    }

    /**
     * Called directly by BlackFlashManager on a successful Black Flash.
     *
     * 1st/2nd impact: permanent physical scar -> slower adaptation.
     * 3rd impact: remove the active halo while the holder is still alive,
     *              throw a charred wheel into the world and ash it away.
     */
    public static void damageByBlackFlash(
            ServerPlayer bearer,
            ServerPlayer attacker,
            int charge
    ) {
        ItemStack wheel =
                findWheel(
                        bearer
                );

        if (wheel.isEmpty()) {
            return;
        }

        int damage =
                Mth.clamp(
                        wheelDamage(wheel) + 1,
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
                    wheel,
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

            attacker.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.wayaround.black_flash.wheel_cracked",
                            damage,
                            MAX_BLACK_FLASH_DAMAGE
                    ),
                    true
            );

            return;
        }

        ItemStack destroyed =
                removeOneWheel(
                        bearer
                );

        if (destroyed.isEmpty()) {
            return;
        }

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

        bearer.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        "message.wayaround.immortal_wheel.destroyed"
                ),
                true
        );

        attacker.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        "message.wayaround.black_flash.wheel_destroyed"
                ),
                true
        );

        WayAround.LOGGER.info(
                "[ImmortalWheel] {} destruiu a roda de {} com o terceiro Black Flash.",
                attacker.getGameProfile().getName(),
                bearer.getGameProfile().getName()
        );
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long tick = server.getTickCount();

        if (tick % 20L != 0L) {
            return;
        }

        Set<UUID> nowPresent =
                new HashSet<>();

        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {

            UUID id = player.getUUID();

            if (!hasWheel(player)) {
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
    }

    public static void reactivateFromRemnant(
            ServerPlayer newOwner,
            int physicalDamage,
            Vec3 origin
    ) {
        /*
         * A dormant wheel never brings adaptation memory with it.
         * Even if the previous bearer had reached 100%, the new bearer starts
         * from zero. Physical scars are already written to the item stack.
         */
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

        WayAround.LOGGER.info(
                "[ImmortalWheel] {} reativou uma roda com {} cicatriz(es) de Black Flash e adaptação zerada.",
                newOwner.getGameProfile().getName(),
                physicalDamage
        );
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

        WayAround.LOGGER.info(
                "[ImmortalWheel] {} adaptou {} -> {}%",
                player.getGameProfile().getName(),
                family,
                Math.min(100, steps * 20)
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

    private static ItemStack findWheel(
            ServerPlayer player
    ) {
        for (int slot = 0;
             slot < player.getInventory().getContainerSize();
             slot++) {

            ItemStack stack =
                    player.getInventory().getItem(slot);

            if (stack.is(
                    WayAroundContent.IMMORTAL_WHEEL.get()
            )) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack removeOneWheel(
            ServerPlayer player
    ) {
        for (int slot = 0;
             slot < player.getInventory().getContainerSize();
             slot++) {

            ItemStack stack =
                    player.getInventory().getItem(slot);

            if (!stack.is(
                    WayAroundContent.IMMORTAL_WHEEL.get()
            )) {
                continue;
            }

            ItemStack removed =
                    stack.copyWithCount(1);

            stack.shrink(1);
            player.getInventory().setChanged();

            return removed;
        }

        return ItemStack.EMPTY;
    }

    public static int wheelDamage(
            ServerPlayer player
    ) {
        return wheelDamage(
                findWheel(player)
        );
    }

    public static int wheelDamage(
            ItemStack stack
    ) {
        if (stack.isEmpty()
                || !stack.is(
                        WayAroundContent.IMMORTAL_WHEEL.get()
                )) {
            return 0;
        }

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
                                BLACK_FLASH_DAMAGE_KEY
                        ),
                0,
                MAX_BLACK_FLASH_DAMAGE
        );
    }

    public static void setWheelDamage(
            ItemStack stack,
            int damage
    ) {
        if (stack.isEmpty()) {
            return;
        }

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag ->
                        tag.putInt(
                                BLACK_FLASH_DAMAGE_KEY,
                                Mth.clamp(
                                        damage,
                                        0,
                                        MAX_BLACK_FLASH_DAMAGE
                                )
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

    public static boolean hasWheel(ServerPlayer player) {
        return !findWheel(player)
                .isEmpty();
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

    private static final class Adaptation {
        private int progress;
        private int steps;
        private long lastHitTick =
                Long.MIN_VALUE / 4L;
    }
}
