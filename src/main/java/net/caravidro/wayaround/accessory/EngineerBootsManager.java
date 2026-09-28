package net.caravidro.wayaround.accessory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Physical utility for the engineer boots.
 *
 * The boots compress deep snow under alternating feet and provide a deliberately
 * small swimming assist. The boost is intentionally capped close to vanilla
 * swimming speed. Neither effect changes the vanilla equipment slots;
 * they only apply while ENGINEER_BOOTS are equipped in Way Around's FEET slot.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class EngineerBootsManager {

    private static final Map<UUID, StepState> STEPS =
            new HashMap<>();

    private static final double STEP_DISTANCE_SQR =
            0.115 * 0.115;

    private static final double FOOT_OFFSET =
            0.135;

    private static final double SWIM_MULTIPLIER =
            1.035;

    private static final double MAX_SWIM_HORIZONTAL =
            0.120;

    private EngineerBootsManager() {
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

            if (AccessoryManager.equipped(
                    player,
                    AccessorySlot.FEET
            ) != AccessoryKind.ENGINEER_BOOTS) {
                STEPS.remove(
                        player.getUUID()
                );
                continue;
            }

            assistSwimming(
                    player
            );

            markSnow(
                    player
            );
        }
    }

    private static void assistSwimming(
            ServerPlayer player
    ) {
        if (!player.isInWater()
                || player.isPassenger()
                || player.isFallFlying()) {
            return;
        }

        Vec3 velocity =
                player.getDeltaMovement();

        double horizontal =
                Math.sqrt(
                        velocity.x * velocity.x
                                + velocity.z * velocity.z
                );

        if (horizontal < 0.008) {
            return;
        }

        double boosted =
                Math.min(
                        MAX_SWIM_HORIZONTAL,
                        horizontal
                                * SWIM_MULTIPLIER
                );

        if (boosted <= horizontal) {
            return;
        }

        double scale =
                boosted
                        / horizontal;

        player.setDeltaMovement(
                velocity.x * scale,
                velocity.y,
                velocity.z * scale
        );

        player.hurtMarked =
                true;
    }

    private static void markSnow(
            ServerPlayer player
    ) {
        UUID id =
                player.getUUID();

        Vec3 now =
                player.position();

        StepState previous =
                STEPS.get(
                        id
                );

        if (!player.onGround()) {
            STEPS.put(
                    id,
                    new StepState(
                            now.x,
                            now.z,
                            previous != null
                                    && previous.left
                    )
            );
            return;
        }

        if (previous == null) {
            STEPS.put(
                    id,
                    new StepState(
                            now.x,
                            now.z,
                            false
                    )
            );
            return;
        }

        double dx =
                now.x
                        - previous.x;

        double dz =
                now.z
                        - previous.z;

        double movedSqr =
                dx * dx
                        + dz * dz;

        if (movedSqr
                < STEP_DISTANCE_SQR) {
            return;
        }

        double length =
                Math.sqrt(
                        movedSqr
                );

        double sideX =
                -dz
                        / length;

        double sideZ =
                dx
                        / length;

        boolean left =
                !previous.left;

        double sign =
                left
                        ? 1.0
                        : -1.0;

        Vec3 foot =
                now.add(
                        sideX
                                * FOOT_OFFSET
                                * sign,
                        0.0,
                        sideZ
                                * FOOT_OFFSET
                                * sign
                );

        ServerLevel level =
                player.serverLevel();

        BlockPos snowPos =
                findSnow(
                        level,
                        foot,
                        player.getY()
                );

        if (snowPos != null) {
            stamp(
                    level,
                    foot,
                    snowPos
            );
        }

        STEPS.put(
                id,
                new StepState(
                        now.x,
                        now.z,
                        left
                )
        );
    }

    private static BlockPos findSnow(
            ServerLevel level,
            Vec3 foot,
            double playerY
    ) {
        BlockPos feet =
                BlockPos.containing(
                        foot.x,
                        playerY + 0.05,
                        foot.z
                );

        for (int y = 0;
             y >= -2;
             y--) {
            BlockPos candidate =
                    feet.offset(
                            0,
                            y,
                            0
                    );

            BlockState state =
                    level.getBlockState(
                            candidate
                    );

            if (state.is(
                    Blocks.SNOW
            )
                    || state.is(
                    Blocks.SNOW_BLOCK
            )
                    || state.is(
                    Blocks.POWDER_SNOW
            )) {
                return candidate;
            }
        }

        return null;
    }

    private static void stamp(
            ServerLevel level,
            Vec3 foot,
            BlockPos snowPos
    ) {
        BlockState state =
                level.getBlockState(
                        snowPos
                );

        /*
         * Deep snow keeps a real depression. A single layer is left intact so
         * normal paths do not become ugly full-block holes.
         */
        if (state.is(
                Blocks.SNOW
        )
                && state.hasProperty(
                SnowLayerBlock.LAYERS
        )) {
            int layers =
                    state.getValue(
                            SnowLayerBlock.LAYERS
                    );

            if (layers > 1) {
                level.setBlock(
                        snowPos,
                        state.setValue(
                                SnowLayerBlock.LAYERS,
                                layers - 1
                        ),
                        3
                );
            }
        }

        BlockState particleState =
                state.is(
                        Blocks.POWDER_SNOW
                )
                        ? Blocks.POWDER_SNOW.defaultBlockState()
                        : Blocks.SNOW_BLOCK.defaultBlockState();

        level.sendParticles(
                new BlockParticleOption(
                        ParticleTypes.BLOCK,
                        particleState
                ),
                foot.x,
                snowPos.getY()
                        + 0.08,
                foot.z,
                4,
                0.045,
                0.015,
                0.065,
                0.012
        );
    }

    @SubscribeEvent
    public static void logout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        STEPS.remove(
                event.getEntity()
                        .getUUID()
        );
    }

    private record StepState(
            double x,
            double z,
            boolean left
    ) {
    }
}
