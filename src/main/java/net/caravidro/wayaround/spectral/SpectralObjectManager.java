package net.caravidro.wayaround.spectral;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.cursed.TukunaManager;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = WayAround.MODID)
public final class SpectralObjectManager {

    private SpectralObjectManager() {}

    private static final Map<UUID, Vec3> LAST_POSITION =
            new HashMap<>();

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        for (ServerPlayer player :
                server.getPlayerList()
                        .getPlayers()) {
            Vec3 previous =
                    LAST_POSITION.put(
                            player.getUUID(),
                            player.position()
                    );

            Vec3 movement =
                    previous == null
                            ? Vec3.ZERO
                            : player.position()
                            .subtract(
                                    previous
                            );

            handleHeld(
                    player,
                    player.getMainHandItem(),
                    movement,
                    tick
            );

            handleHeld(
                    player,
                    player.getOffhandItem(),
                    movement,
                    tick
            );

            ItemStack chest =
                    player.getItemBySlot(
                            EquipmentSlot.CHEST
                    );

            if (chest.getItem()
                    instanceof SpectralArmorItem) {
                handleSpectral(
                        player,
                        chest,
                        movement
                );

                int brightness =
                        player.serverLevel()
                                .getMaxLocalRawBrightness(
                                        player.blockPosition()
                                );

                if (brightness <= 6) {
                    SpectralTraitData.ascendArmor(
                            chest
                    );

                    player.addEffect(
                            new MobEffectInstance(
                                    MobEffects.NIGHT_VISION,
                                    240,
                                    0,
                                    true,
                                    false,
                                    false
                            )
                    );

                    if (tick % 10L == 0L) {
                        player.serverLevel()
                                .sendParticles(
                                        ParticleTypes.END_ROD,
                                        player.getX(),
                                        player.getY()
                                                + 1.0,
                                        player.getZ(),
                                        3,
                                        0.28,
                                        0.48,
                                        0.28,
                                        0.012
                                );
                    }
                }
            }
        }
    }

    private static void handleHeld(
            ServerPlayer player,
            ItemStack stack,
            Vec3 movement,
            long tick
    ) {
        if (!(stack.getItem()
                instanceof SpectralObject)) {
            return;
        }

        handleSpectral(
                player,
                stack,
                movement
        );

        if (stack.getItem()
                instanceof SpectralCompassItem
                && tick % 10L == 0L) {
            updateCompass(
                    player,
                    stack
            );
        }
    }

    private static void handleSpectral(
            ServerPlayer player,
            ItemStack stack,
            Vec3 movement
    ) {
        int trait =
                SpectralTraitData.ensure(
                        stack,
                        player.serverLevel()
                                .random
                );

        boolean active =
                false;

        switch (trait) {
            case 0 -> {
                if (player.serverLevel()
                        .getMaxLocalRawBrightness(
                                player.blockPosition()
                        ) <= 7) {
                    player.addEffect(
                            new MobEffectInstance(
                                    MobEffects.DAMAGE_BOOST,
                                    30,
                                    0,
                                    true,
                                    false,
                                    false
                            )
                    );

                    active =
                            true;
                }
            }

            case 1 -> {
                if (movement.z < -0.035
                        || player.getLookAngle().z
                        < -0.86) {
                    player.addEffect(
                            new MobEffectInstance(
                                    MobEffects.MOVEMENT_SPEED,
                                    30,
                                    0,
                                    true,
                                    false,
                                    false
                            )
                    );

                    active =
                            true;
                }
            }

            case 2 -> {
                if (movement.lengthSqr()
                        < 0.00055) {
                    player.addEffect(
                            new MobEffectInstance(
                                    MobEffects.DAMAGE_RESISTANCE,
                                    30,
                                    0,
                                    true,
                                    false,
                                    false
                            )
                    );

                    active =
                            true;
                }
            }

            default -> {
                if (player.serverLevel()
                        .isRainingAt(
                                player.blockPosition()
                        )) {
                    player.addEffect(
                            new MobEffectInstance(
                                    MobEffects.DIG_SPEED,
                                    30,
                                    1,
                                    true,
                                    false,
                                    false
                            )
                    );

                    active =
                            true;
                }
            }
        }

        if (active) {
            SpectralTraitData.discover(
                    stack
            );
        }
    }

    private static void updateCompass(
            ServerPlayer holder,
            ItemStack stack
    ) {
        ServerLevel level =
                holder.serverLevel();

        Vec3 center =
                holder.position();

        double best =
                Double.POSITIVE_INFINITY;

        net.minecraft.core.BlockPos target =
                null;

        AABB box =
                new AABB(
                        center.x - 192.0,
                        level.getMinBuildHeight(),
                        center.z - 192.0,
                        center.x + 192.0,
                        level.getMaxBuildHeight(),
                        center.z + 192.0
                );

        for (ItemEntity item :
                level.getEntitiesOfClass(
                        ItemEntity.class,
                        box,
                        entity ->
                                entity.getItem()
                                        .is(
                                                WayAroundContent.TUKUNA_FINGER.get()
                                        )
                )) {
            double distance =
                    item.distanceToSqr(
                            holder
                    );

            if (distance < best) {
                best =
                        distance;

                target =
                        item.blockPosition();
            }
        }

        for (ServerPlayer player :
                holder.server
                        .getPlayerList()
                        .getPlayers()) {
            if (player == holder
                    || player.serverLevel()
                    != level
                    || !player.isAlive()) {
                continue;
            }

            boolean carriesFinger =
                    TukunaManager.fingerCount(
                            player
                    ) > 0;

            if (!carriesFinger) {
                for (int slot = 0;
                     slot < player.getInventory()
                             .getContainerSize();
                     slot++) {
                    if (player.getInventory()
                            .getItem(
                                    slot
                            )
                            .is(
                                    WayAroundContent.TUKUNA_FINGER.get()
                            )) {
                        carriesFinger =
                                true;
                        break;
                    }
                }
            }

            if (!carriesFinger) {
                continue;
            }

            double distance =
                    player.distanceToSqr(
                            holder
                    );

            if (distance < best) {
                best =
                        distance;

                target =
                        player.blockPosition();
            }
        }

        Optional<GlobalPos> global =
                target == null
                        ? Optional.empty()
                        : Optional.of(
                                GlobalPos.of(
                                        level.dimension(),
                                        target
                                )
                        );

        stack.set(
                DataComponents.LODESTONE_TRACKER,
                new LodestoneTracker(
                        global,
                        false
                )
        );
    }

    @SubscribeEvent
    public static void stop(
            ServerStoppedEvent event
    ) {
        LAST_POSITION.clear();
    }
}
