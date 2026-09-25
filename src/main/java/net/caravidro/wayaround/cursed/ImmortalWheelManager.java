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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Cursed artifact state machine.
 *
 * While a player carries the Immortal Wheel, damage families adapt exactly as
 * before. Death is different: the artifact is removed before vanilla inventory
 * drops are created and becomes an immortal remnant entity in the world. Its
 * adaptation book is serialized into that remnant, so the memory belongs to
 * the wheel itself and can survive death, chunk unloads, server restarts and a
 * change of holder.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class ImmortalWheelManager {

    private ImmortalWheelManager() {
    }

    private static final int HITS_PER_TURN = 4;
    private static final int MAX_STEPS = 5;
    private static final int COMBO_WINDOW_TICKS = 120;
    private static final double VISUAL_RANGE = 128.0;

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

        if (adaptation.progress < HITS_PER_TURN) {
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
     * The wheel leaves the inventory before vanilla death-drop processing.
     * This also deliberately ignores keepInventory: the cursed artifact always
     * leaves a corpse and becomes a persistent dormant object in the world.
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

        CompoundTag memory =
                detachMemory(
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

        ImmortalWheelRemnantEntity remnant =
                new ImmortalWheelRemnantEntity(
                        WayAroundContent.IMMORTAL_WHEEL_REMNANT.get(),
                        player.serverLevel()
                );

        remnant.setPos(
                player.getX(),
                player.getY() + 0.34,
                player.getZ()
        );

        remnant.setDeltaMovement(
                player.getDeltaMovement()
                        .scale(0.28)
                        .add(
                                0.0,
                                0.16,
                                0.0
                        )
        );

        remnant.setMemory(
                memory
        );

        player.serverLevel()
                .addFreshEntity(
                        remnant
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
                "[ImmortalWheel] {} morreu: a roda entrou em estado dormente.",
                player.getGameProfile().getName()
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

                    /*
                     * Normal inventory loss still forgets player-bound state.
                     * Death is special because onDeath detached that state into
                     * the remnant first.
                     */
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
            CompoundTag memory,
            Vec3 origin
    ) {
        restoreMemory(
                newOwner.getUUID(),
                memory,
                newOwner.server.getTickCount()
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
                0.82F
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
                        highestStep(
                                newOwner.getUUID()
                        )
                )
        );

        send(
                newOwner,
                ImmortalWheelVisualPayload.PRESENCE,
                "",
                0,
                highestStep(
                        newOwner.getUUID()
                )
        );

        WayAround.LOGGER.info(
                "[ImmortalWheel] {} reativou a roda dormente.",
                newOwner.getGameProfile().getName()
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

    private static ItemStack removeOneWheel(ServerPlayer player) {
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

    private static CompoundTag detachMemory(UUID owner) {
        Map<String, Adaptation> book =
                ADAPTATIONS.remove(owner);

        CompoundTag root =
                new CompoundTag();

        if (book == null
                || book.isEmpty()) {
            return root;
        }

        ListTag entries =
                new ListTag();

        for (Map.Entry<String, Adaptation> entry :
                book.entrySet()) {

            CompoundTag tag =
                    new CompoundTag();

            tag.putString(
                    "Family",
                    entry.getKey()
            );

            tag.putInt(
                    "Progress",
                    entry.getValue().progress
            );

            tag.putInt(
                    "Steps",
                    entry.getValue().steps
            );

            entries.add(
                    tag
            );
        }

        root.put(
                "Adaptations",
                entries
        );

        return root;
    }

    private static void restoreMemory(
            UUID owner,
            CompoundTag root,
            long currentTick
    ) {
        Map<String, Adaptation> book =
                new HashMap<>();

        ListTag entries =
                root.getList(
                        "Adaptations",
                        Tag.TAG_COMPOUND
                );

        for (int i = 0;
             i < entries.size();
             i++) {

            CompoundTag tag =
                    entries.getCompound(i);

            String family =
                    tag.getString(
                            "Family"
                    );

            if (family.isBlank()) {
                continue;
            }

            Adaptation adaptation =
                    new Adaptation();

            adaptation.progress =
                    Mth.clamp(
                            tag.getInt("Progress"),
                            0,
                            HITS_PER_TURN - 1
                    );

            adaptation.steps =
                    Mth.clamp(
                            tag.getInt("Steps"),
                            0,
                            MAX_STEPS
                    );

            adaptation.lastHitTick =
                    currentTick;

            book.put(
                    family,
                    adaptation
            );
        }

        if (book.isEmpty()) {
            ADAPTATIONS.remove(owner);
        } else {
            ADAPTATIONS.put(
                    owner,
                    book
            );
        }
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
        for (int slot = 0;
             slot < player.getInventory().getContainerSize();
             slot++) {

            ItemStack stack =
                    player.getInventory().getItem(slot);

            if (stack.is(
                    WayAroundContent.IMMORTAL_WHEEL.get()
            )) {
                return true;
            }
        }

        return false;
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
