package net.caravidro.wayaround.cursed;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.network.ImmortalWheelVisualPayload;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Cursed-item adaptation prototype.
 *
 * Each holder learns damage families independently. Four accepted hits of the
 * same family inside the combo window turn the wheel once and add another
 * 20% reduction against that family, up to full adaptation.
 */
@EventBusSubscriber(
        modid = WayAround.MODID
)
public final class ImmortalWheelManager {

    private ImmortalWheelManager() {
    }

    private static final int HITS_PER_TURN = 4;
    private static final int MAX_STEPS = 5;
    private static final int COMBO_WINDOW_TICKS = 120;
    private static final double VISUAL_RANGE = 128.0;

    private static final Map<UUID, Map<String, Adaptation>>
            ADAPTATIONS =
            new HashMap<>();

    private static final Set<UUID> PRESENT =
            new HashSet<>();

    @SubscribeEvent
    public static void onDamage(
            LivingIncomingDamageEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)
                || !hasWheel(
                        player
                )
                || event.getAmount() <= 0.0F) {

            return;
        }

        String family =
                classify(
                        event.getSource()
                );

        Map<String, Adaptation> book =
                ADAPTATIONS.computeIfAbsent(
                        player.getUUID(),
                        ignored ->
                                new HashMap<>()
                );

        Adaptation adaptation =
                book.computeIfAbsent(
                        family,
                        ignored ->
                                new Adaptation()
                );

        /*
         * Existing turns protect against this hit. A newly completed turn
         * begins affecting the NEXT matching hit, so the spin has a clear
         * cause -> adaptation -> protection rhythm.
         */
        float reduction =
                Mth.clamp(
                        adaptation.steps
                                * 0.20F,
                        0.0F,
                        1.0F
                );

        if (reduction > 0.0F) {
            event.setAmount(
                    Math.max(
                            0.0F,
                            event.getAmount()
                                    * (
                                    1.0F
                                            - reduction
                            )
                    )
            );
        }

        if (adaptation.steps
                >= MAX_STEPS) {

            send(
                    player,
                    ImmortalWheelVisualPayload.HIT,
                    family,
                    0,
                    adaptation.steps
            );

            return;
        }

        long tick =
                player.server
                        .getTickCount();

        if (tick - adaptation.lastHitTick
                > COMBO_WINDOW_TICKS) {

            adaptation.progress =
                    0;
        }

        adaptation.lastHitTick =
                tick;

        adaptation.progress++;

        if (adaptation.progress
                < HITS_PER_TURN) {

            send(
                    player,
                    ImmortalWheelVisualPayload.HIT,
                    family,
                    adaptation.progress,
                    adaptation.steps
            );

            return;
        }

        adaptation.progress =
                0;

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
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        if (tick % 20L != 0L) {
            return;
        }

        Set<UUID> nowPresent =
                new HashSet<>();

        for (ServerPlayer player :
                server.getPlayerList()
                        .getPlayers()) {

            UUID id =
                    player.getUUID();

            if (!hasWheel(
                    player
            )) {
                if (PRESENT.remove(
                        id
                )) {

                    send(
                            player,
                            ImmortalWheelVisualPayload.REMOVE,
                            "",
                            0,
                            0
                    );

                    ADAPTATIONS.remove(
                            id
                    );
                }

                continue;
            }

            nowPresent.add(
                    id
            );

            PRESENT.add(
                    id
            );

            send(
                    player,
                    ImmortalWheelVisualPayload.PRESENCE,
                    "",
                    0,
                    highestStep(
                            id
                    )
            );
        }

        PRESENT.retainAll(
                nowPresent
        );
    }

    @SubscribeEvent
    public static void onServerStopped(
            ServerStoppedEvent event
    ) {
        ADAPTATIONS.clear();
        PRESENT.clear();
    }

    private static void spin(
            ServerPlayer player,
            String family,
            int steps
    ) {
        ServerLevel level =
                player.serverLevel();

        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS,
                1.15F,
                0.72F
                        + steps
                                * 0.055F
        );

        level.sendParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(),
                player.getY()
                        + player.getBbHeight()
                        + 0.72,
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
                player.getY()
                        + player.getBbHeight()
                        + 0.72,
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
                player.getGameProfile()
                        .getName(),
                family,
                Math.min(
                        100,
                        steps * 20
                )
        );
    }

    private static void send(
            ServerPlayer owner,
            byte action,
            String family,
            int progress,
            int steps
    ) {
        ServerLevel level =
                owner.serverLevel();

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

    private static int highestStep(
            UUID owner
    ) {
        Map<String, Adaptation> book =
                ADAPTATIONS.get(
                        owner
                );

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

    public static boolean hasWheel(
            ServerPlayer player
    ) {
        for (int slot = 0;
             slot < player.getInventory()
                     .getContainerSize();
             slot++) {

            ItemStack stack =
                    player.getInventory()
                            .getItem(
                                    slot
                            );

            if (stack.is(
                    WayAroundContent.IMMORTAL_WHEEL.get()
            )) {

                return true;
            }
        }

        return false;
    }

    private static String classify(
            DamageSource source
    ) {
        Entity direct =
                source.getDirectEntity();

        if (direct
                instanceof AbstractArrow) {

            return "arrow";
        }

        if (source.is(
                DamageTypeTags.IS_PROJECTILE
        )
                || direct
                instanceof Projectile) {

            return "projectile";
        }

        if (source.is(
                DamageTypeTags.IS_EXPLOSION
        )) {

            return "explosion";
        }

        if (source.is(
                DamageTypeTags.IS_FIRE
        )) {

            return "fire";
        }

        if (source.is(
                DamageTypeTags.IS_FALL
        )) {

            return "fall";
        }

        if (source.is(
                DamageTypeTags.IS_FREEZING
        )) {

            return "freezing";
        }

        if (source.is(
                DamageTypeTags.IS_LIGHTNING
        )) {

            return "lightning";
        }

        Entity attacker =
                source.getEntity();

        if (attacker
                instanceof Player) {

            return "player_melee";
        }

        if (attacker
                instanceof LivingEntity) {

            return "mob_melee";
        }

        String id =
                source.getMsgId();

        if (id != null) {
            String lowered =
                    id.toLowerCase(
                            java.util.Locale.ROOT
                    );

            if (lowered.contains(
                    "magic"
            )
                    || lowered.contains(
                    "wither"
            )) {

                return "magic";
            }

            if (lowered.contains(
                    "sonic"
            )) {

                return "sonic";
            }

            if (lowered.contains(
                    "drown"
            )) {

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
