package net.caravidro.wayaround.accessory;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.TopHatStateS2CPayload;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
        modid = WayAround.MODID
)
public final class TopHatManager {

    private static final String WARNING_UNTIL =
            "WayAroundTopHatWarningUntil";

    private static final String ADJUST_UNTIL =
            "WayAroundTopHatAdjustUntil";

    private static final String COOLDOWN_UNTIL =
            "WayAroundTopHatCooldownUntil";

    private static final int WARNING_TICKS =
            46;

    private static final int ADJUST_TICKS =
            24;

    private static final int SAFE_COOLDOWN =
            180;

    private TopHatManager() {
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.ACCESSORIES
        )) {
            return;
        }

        MinecraftServer server =
                event.getServer();

        for (ServerPlayer player :
                server.getPlayerList()
                        .getPlayers()) {
            tickPlayer(
                    player
            );
        }
    }

    private static void tickPlayer(
            ServerPlayer player
    ) {
        CompoundTag data =
                player.getPersistentData();

        if (AccessoryManager.equipped(
                player,
                AccessorySlot.HEAD
        ) != AccessoryKind.ENGINEER_CAP) {
            if (data.contains(
                    WARNING_UNTIL
            )
                    || data.contains(
                    ADJUST_UNTIL
            )
                    || data.contains(
                    COOLDOWN_UNTIL
            )) {
                clearState(
                        player
                );
            }

            return;
        }

        ServerLevel level =
                player.serverLevel();

        long now =
                level.getGameTime();

        float instability =
                instability(
                        player
                );

        long warningUntil =
                data.getLong(
                        WARNING_UNTIL
                );

        long adjustUntil =
                data.getLong(
                        ADJUST_UNTIL
                );

        if (warningUntil > 0L
                && now >= warningUntil) {
            detach(
                    player
            );
            return;
        }

        if (adjustUntil > now) {
            if (now % 4L == 0L) {
                sync(
                        player,
                        instability
                );
            }

            return;
        }

        if (adjustUntil > 0L) {
            data.remove(
                    ADJUST_UNTIL
            );

            sync(
                    player,
                    instability
            );
        }

        if (warningUntil > now) {
            if (now % 3L == 0L) {
                sync(
                        player,
                        instability
                );
            }

            return;
        }

        long cooldown =
                data.getLong(
                        COOLDOWN_UNTIL
                );

        if (now < cooldown
                || instability < 0.48F) {
            return;
        }

        float chance =
                0.0010F
                        + instability
                        * 0.0034F;

        if (player.getRandom()
                .nextFloat()
                < chance) {
            data.putLong(
                    WARNING_UNTIL,
                    now + WARNING_TICKS
            );

            sync(
                    player,
                    instability
            );
        }
    }

    public static void adjust(
            ServerPlayer player
    ) {
        if (AccessoryManager.equipped(
                player,
                AccessorySlot.HEAD
        ) != AccessoryKind.ENGINEER_CAP) {
            return;
        }

        CompoundTag data =
                player.getPersistentData();

        long now =
                player.serverLevel()
                        .getGameTime();

        long warningUntil =
                data.getLong(
                        WARNING_UNTIL
                );

        if (warningUntil <= now) {
            return;
        }

        data.remove(
                WARNING_UNTIL
        );

        data.putLong(
                ADJUST_UNTIL,
                now + ADJUST_TICKS
        );

        data.putLong(
                COOLDOWN_UNTIL,
                now + SAFE_COOLDOWN
        );

        player.swing(
                InteractionHand.MAIN_HAND
        );

        sync(
                player,
                0.0F
        );
    }

    private static void detach(
            ServerPlayer player
    ) {
        ItemStack stack =
                AccessoryManager.takeEquipped(
                        player,
                        AccessorySlot.HEAD
                );

        clearState(
                player
        );

        if (stack.isEmpty()) {
            return;
        }

        FlyingTopHatEntity hat =
                TopHatContent.FLYING_TOP_HAT.get()
                        .create(
                                player.serverLevel()
                        );

        if (hat == null) {
            player.getInventory()
                    .add(
                            stack
                    );
            return;
        }

        hat.setWear(
                stack.getDamageValue()
        );

        hat.moveTo(
                player.getX(),
                player.getEyeY()
                        + 0.28,
                player.getZ(),
                player.getYRot(),
                0.0F
        );

        float windX =
                0.0F;

        float windZ =
                0.0F;

        float strength =
                0.45F;

        if (player.level()
                .dimension()
                .equals(
                        Level.OVERWORLD
                )) {
            LocalWeatherField.Sample sample =
                    LocalWeatherField.sample(
                            player.getX(),
                            player.getZ(),
                            player.level()
                                    .getGameTime()
                    );

            windX =
                    sample.windX();

            windZ =
                    sample.windZ();

            strength +=
                    sample.warning()
                            * 0.65F;
        }

        Vec3 launch =
                player.getDeltaMovement()
                        .scale(
                                0.65
                        )
                        .add(
                                windX
                                        * strength,
                                0.34
                                        + player.getRandom()
                                        .nextDouble()
                                        * 0.12,
                                windZ
                                        * strength
                        );

        hat.setDeltaMovement(
                launch
        );

        player.serverLevel()
                .addFreshEntity(
                        hat
                );
    }

    private static float instability(
            ServerPlayer player
    ) {
        float speed =
                Mth.clamp(
                        (float) player.getDeltaMovement()
                                .horizontalDistance()
                                * 2.9F,
                        0.0F,
                        1.0F
                );

        float wind =
                0.0F;

        if (player.level()
                .dimension()
                .equals(
                        Level.OVERWORLD
                )
                && player.serverLevel()
                .canSeeSky(
                        player.blockPosition()
                                .above()
                )) {
            wind =
                    LocalWeatherField.sample(
                                    player.getX(),
                                    player.getZ(),
                                    player.level()
                                            .getGameTime()
                            )
                            .warning();
        }

        float airborne =
                player.isFallFlying()
                        ? 0.72F
                        : !player.onGround()
                        ? 0.16F
                        : 0.0F;

        return Mth.clamp(
                wind * 0.82F
                        + speed * 0.44F
                        + airborne,
                0.0F,
                1.0F
        );
    }

    private static void clearState(
            ServerPlayer player
    ) {
        CompoundTag data =
                player.getPersistentData();

        data.remove(
                WARNING_UNTIL
        );

        data.remove(
                ADJUST_UNTIL
        );

        data.remove(
                COOLDOWN_UNTIL
        );

        sync(
                player,
                0.0F
        );
    }

    private static void sync(
            ServerPlayer player,
            float instability
    ) {
        CompoundTag data =
                player.getPersistentData();

        TopHatStateS2CPayload payload =
                new TopHatStateS2CPayload(
                        player.getUUID(),
                        data.getLong(
                                WARNING_UNTIL
                        ),
                        data.getLong(
                                ADJUST_UNTIL
                        ),
                        Mth.clamp(
                                instability,
                                0.0F,
                                1.0F
                        )
                );

        PacketDistributor.sendToPlayer(
                player,
                payload
        );

        PacketDistributor.sendToPlayersNear(
                player.serverLevel(),
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                160.0,
                payload
        );
    }
}
