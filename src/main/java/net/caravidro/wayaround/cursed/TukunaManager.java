package net.caravidro.wayaround.cursed;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.WayAroundContent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * First foundation for Tukuna possession.
 *
 * A Spectrum holder drops a finger carrying their UUID when they die and loses
 * the Spectrum. Whoever eats fingers from that SAME source permanently stores
 * that source UUID as the inner Tukuna spirit and increases a persistent finger
 * count. Actual body-swap / inner voice control can build on these two values.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class TukunaManager {

    private TukunaManager() {
    }

    public static final int MAX_FINGERS = 20;

    private static final String FINGER_OWNER_KEY =
            "WayAroundTukunaFingerOwner";

    private static final String HOST_FINGER_COUNT_KEY =
            "WayAroundTukunaFingerCount";

    private static final String HOST_SPIRIT_KEY =
            "WayAroundTukunaSpirit";

    @SubscribeEvent
    public static void onDeath(
            LivingDeathEvent event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack spectrum =
                removeSpectrum(
                        player
                );

        if (spectrum.isEmpty()) {
            return;
        }

        ItemStack finger =
                createFinger(
                        player.getUUID()
                );

        ItemEntity entity =
                new ItemEntity(
                        player.serverLevel(),
                        player.getX(),
                        player.getY() + 0.45,
                        player.getZ(),
                        finger
                );

        entity.setDeltaMovement(
                player.getDeltaMovement()
                        .scale(0.25)
                        .add(
                                0.0,
                                0.22,
                                0.0
                        )
        );

        player.serverLevel()
                .addFreshEntity(
                        entity
                );

        player.displayClientMessage(
                Component.translatable(
                                "message.wayaround.tukuna.spectrum_lost"
                        )
                        .withStyle(
                                ChatFormatting.DARK_RED
                        ),
                false
        );
    }

    @SubscribeEvent
    public static void onClone(
            PlayerEvent.Clone event
    ) {
        if (!(event.getOriginal()
                instanceof ServerPlayer original)
                || !(event.getEntity()
                        instanceof ServerPlayer replacement)) {
            return;
        }

        int fingers =
                original.getPersistentData()
                        .getInt(
                                HOST_FINGER_COUNT_KEY
                        );

        if (fingers > 0) {
            replacement.getPersistentData()
                    .putInt(
                            HOST_FINGER_COUNT_KEY,
                            fingers
                    );
        }

        if (original.getPersistentData()
                .hasUUID(
                        HOST_SPIRIT_KEY
                )) {

            replacement.getPersistentData()
                    .putUUID(
                            HOST_SPIRIT_KEY,
                            original.getPersistentData()
                                    .getUUID(
                                            HOST_SPIRIT_KEY
                                    )
                    );
        }
    }

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        if (event.getServer()
                .getTickCount() % 20L != 0L) {
            return;
        }

        for (ServerPlayer player :
                event.getServer()
                        .getPlayerList()
                        .getPlayers()) {

            int fingers =
                    fingerCount(
                            player
                    );

            if (fingers <= 0) {
                continue;
            }

            /*
             * Permanent in player-state terms, not an absurdly huge potion
             * duration. Refreshing short hidden effects makes reconnect /
             * respawn / dimension transfer robust.
             */
            player.addEffect(
                    new MobEffectInstance(
                            MobEffects.DAMAGE_RESISTANCE,
                            60,
                            0,
                            true,
                            false,
                            false
                    )
            );

            player.addEffect(
                    new MobEffectInstance(
                            MobEffects.DAMAGE_BOOST,
                            60,
                            0,
                            true,
                            false,
                            false
                    )
            );

            player.addEffect(
                    new MobEffectInstance(
                            MobEffects.MOVEMENT_SPEED,
                            60,
                            0,
                            true,
                            false,
                            false
                    )
            );
        }
    }

    public static boolean consumeFinger(
            ServerPlayer host,
            ItemStack stack
    ) {
        UUID owner =
                fingerOwner(
                        stack
                );

        if (owner == null) {
            host.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.tukuna.finger_empty"
                    ),
                    true
            );
            return false;
        }

        UUID existing =
                spiritOwner(
                        host
                );

        if (existing != null
                && !existing.equals(
                        owner
                )) {

            host.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.tukuna.other_spirit"
                    ),
                    true
            );
            return false;
        }

        int current =
                fingerCount(
                        host
                );

        if (current >= MAX_FINGERS) {
            host.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.tukuna.max_fingers"
                    ),
                    true
            );
            return false;
        }

        host.getPersistentData()
                .putUUID(
                        HOST_SPIRIT_KEY,
                        owner
                );

        int next =
                Math.min(
                        MAX_FINGERS,
                        current + 1
                );

        host.getPersistentData()
                .putInt(
                        HOST_FINGER_COUNT_KEY,
                        next
                );

        host.displayClientMessage(
                Component.translatable(
                                "message.wayaround.tukuna.finger_eaten",
                                next,
                                MAX_FINGERS
                        )
                        .withStyle(
                                ChatFormatting.DARK_RED
                        ),
                false
        );

        return true;
    }

    public static ItemStack createFinger(
            UUID owner
    ) {
        ItemStack stack =
                new ItemStack(
                        WayAroundContent.TUKUNA_FINGER.get()
                );

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag ->
                        tag.putUUID(
                                FINGER_OWNER_KEY,
                                owner
                        )
        );

        return stack;
    }

    public static UUID fingerOwner(
            ItemStack stack
    ) {
        CustomData data =
                stack.get(
                        DataComponents.CUSTOM_DATA
                );

        if (data == null) {
            return null;
        }

        CompoundTag tag =
                data.copyTag();

        if (!tag.hasUUID(
                FINGER_OWNER_KEY
        )) {
            return null;
        }

        return tag.getUUID(
                FINGER_OWNER_KEY
        );
    }

    public static int fingerCount(
            ServerPlayer player
    ) {
        return Math.max(
                0,
                player.getPersistentData()
                        .getInt(
                                HOST_FINGER_COUNT_KEY
                        )
        );
    }

    public static UUID spiritOwner(
            ServerPlayer player
    ) {
        if (!player.getPersistentData()
                .hasUUID(
                        HOST_SPIRIT_KEY
                )) {
            return null;
        }

        return player.getPersistentData()
                .getUUID(
                        HOST_SPIRIT_KEY
                );
    }

    private static ItemStack removeSpectrum(
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

            if (!stack.is(
                    WayAroundContent.TUKUNA_SPECTRUM.get()
            )) {
                continue;
            }

            ItemStack result =
                    stack.copyWithCount(
                            1
                    );

            stack.shrink(
                    1
            );

            player.getInventory()
                    .setChanged();

            return result;
        }

        return ItemStack.EMPTY;
    }
}
