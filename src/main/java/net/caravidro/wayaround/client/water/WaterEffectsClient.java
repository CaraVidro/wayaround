package net.caravidro.wayaround.client.water;

import java.util.HashSet;
import java.util.Set;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.particle.WayAroundParticles;
import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class WaterEffectsClient {

    private static final Set<Integer>
            FALLING_BLOCKS_IN_WATER =
            new HashSet<>();

    private static boolean wasInWater;
    private static int ticks;

    private WaterEffectsClient() {
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null
                || minecraft.isPaused()) {
            FALLING_BLOCKS_IN_WATER.clear();
            wasInWater = false;
            return;
        }

        if (!minecraft.level.dimension()
                .equals(Level.OVERWORLD)) {
            return;
        }

        ticks++;

        playerSplash(
                minecraft
        );

        fallingBlockSplashes(
                minecraft
        );

        if (ticks % 2 == 0) {
            currentImpacts(
                    minecraft
            );
        }

        if (ticks % 8 == 0
                && minecraft.player.isUnderWater()) {
            breathe(
                    minecraft
            );
        }
    }

    private static void playerSplash(
            Minecraft minecraft
    ) {
        boolean inWater =
                minecraft.player.isInWater();

        if (inWater != wasInWater) {
            Vec3 movement =
                    minecraft.player
                            .getDeltaMovement();

            int amount =
                    inWater
                            ? 12
                            : 8;

            for (int i = 0;
                    i < amount;
                    i++) {

                minecraft.level.addParticle(
                        ParticleTypes.SPLASH,
                        minecraft.player.getX()
                                + (
                                        minecraft.level.random.nextDouble()
                                        - 0.5
                                ) * 0.9,
                        minecraft.player.getY()
                                + 0.15
                                + minecraft.level.random.nextDouble()
                                * 0.35,
                        minecraft.player.getZ()
                                + (
                                        minecraft.level.random.nextDouble()
                                        - 0.5
                                ) * 0.9,
                        movement.x * 0.22
                                + (
                                        minecraft.level.random.nextDouble()
                                        - 0.5
                                ) * 0.08,
                        0.04
                                + minecraft.level.random.nextDouble()
                                * 0.10,
                        movement.z * 0.22
                                + (
                                        minecraft.level.random.nextDouble()
                                        - 0.5
                                ) * 0.08
                );
            }
        }

        wasInWater =
                inWater;
    }

    private static void fallingBlockSplashes(
            Minecraft minecraft
    ) {
        AABB area =
                minecraft.player
                        .getBoundingBox()
                        .inflate(
                                24.0,
                                16.0,
                                24.0
                        );

        Set<Integer> active =
                new HashSet<>();

        for (FallingBlockEntity falling :
                minecraft.level.getEntitiesOfClass(
                        FallingBlockEntity.class,
                        area
                )) {

            BlockPos pos =
                    falling.blockPosition();

            if (!minecraft.level
                    .getFluidState(pos)
                    .is(FluidTags.WATER)) {
                continue;
            }

            active.add(
                    falling.getId()
            );

            if (!FALLING_BLOCKS_IN_WATER.add(
                    falling.getId()
            )) {
                continue;
            }

            Vec3 movement =
                    falling.getDeltaMovement();

            for (int i = 0;
                    i < 18;
                    i++) {

                minecraft.level.addParticle(
                        ParticleTypes.SPLASH,
                        falling.getX()
                                + (
                                        minecraft.level.random.nextDouble()
                                        - 0.5
                                ) * 0.75,
                        falling.getY()
                                + 0.18,
                        falling.getZ()
                                + (
                                        minecraft.level.random.nextDouble()
                                        - 0.5
                                ) * 0.75,
                        movement.x * 0.16
                                + (
                                        minecraft.level.random.nextDouble()
                                        - 0.5
                                ) * 0.12,
                        0.06
                                + minecraft.level.random.nextDouble()
                                * 0.14,
                        movement.z * 0.16
                                + (
                                        minecraft.level.random.nextDouble()
                                        - 0.5
                                ) * 0.12
                );
            }

            for (int i = 0;
                    i < 7;
                    i++) {

                minecraft.level.addParticle(
                        WayAroundParticles.AIR_BUBBLE.get(),
                        falling.getX()
                                + (
                                        minecraft.level.random.nextDouble()
                                        - 0.5
                                ) * 0.55,
                        falling.getY(),
                        falling.getZ()
                                + (
                                        minecraft.level.random.nextDouble()
                                        - 0.5
                                ) * 0.55,
                        0.0,
                        0.01,
                        0.0
                );
            }
        }

        FALLING_BLOCKS_IN_WATER
                .retainAll(active);
    }

    private static void currentImpacts(
            Minecraft minecraft
    ) {
        int attempts =
                8;

        for (int i = 0;
                i < attempts;
                i++) {

            int x =
                    minecraft.player.getBlockX()
                    + minecraft.level.random.nextInt(29)
                    - 14;

            int z =
                    minecraft.player.getBlockZ()
                    + minecraft.level.random.nextInt(29)
                    - 14;

            int centerY =
                    minecraft.player.getBlockY();

            BlockPos water =
                    findWater(
                            minecraft,
                            x,
                            z,
                            centerY - 5,
                            centerY + 5
                    );

            if (water == null) {
                continue;
            }

            Vec3 current =
                    WaterDynamics.current(
                            minecraft.level,
                            water
                    );

            if (!WaterDynamics.hitsObstacle(
                    minecraft.level,
                    water,
                    current
            )) {
                continue;
            }

            Direction direction =
                    WaterDynamics.dominantDirection(
                            current
                    );

            if (direction == null) {
                continue;
            }

            double speed =
                    WaterDynamics.speed(
                            current
                    );

            double px =
                    water.getX()
                    + 0.5
                    + direction.getStepX()
                    * 0.43;

            double pz =
                    water.getZ()
                    + 0.5
                    + direction.getStepZ()
                    * 0.43;

            double py =
                    water.getY()
                    + 0.78;

            int count =
                    speed > 0.18
                            ? 3
                            : 1;

            for (int p = 0;
                    p < count;
                    p++) {

                minecraft.level.addParticle(
                        ParticleTypes.SPLASH,
                        px
                                + (
                                        minecraft.level.random.nextDouble()
                                        - 0.5
                                ) * 0.35,
                        py,
                        pz
                                + (
                                        minecraft.level.random.nextDouble()
                                        - 0.5
                                ) * 0.35,
                        -current.x * 0.05
                                + (
                                        minecraft.level.random.nextDouble()
                                        - 0.5
                                ) * 0.035,
                        0.025
                                + minecraft.level.random.nextDouble()
                                * 0.055,
                        -current.z * 0.05
                                + (
                                        minecraft.level.random.nextDouble()
                                        - 0.5
                                ) * 0.035
                );
            }
        }
    }

    private static void breathe(
            Minecraft minecraft
    ) {
        int amount =
                1
                + minecraft.level.random.nextInt(2);

        for (int i = 0;
                i < amount;
                i++) {

            minecraft.level.addParticle(
                    WayAroundParticles.AIR_BUBBLE.get(),
                    minecraft.player.getX()
                            + (
                                    minecraft.level.random.nextDouble()
                                    - 0.5
                            ) * 0.25,
                    minecraft.player.getEyeY()
                            - 0.06,
                    minecraft.player.getZ()
                            + (
                                    minecraft.level.random.nextDouble()
                                    - 0.5
                            ) * 0.25,
                    minecraft.player.getDeltaMovement().x
                            * 0.10,
                    0.015,
                    minecraft.player.getDeltaMovement().z
                            * 0.10
            );
        }
    }

    private static BlockPos findWater(
            Minecraft minecraft,
            int x,
            int z,
            int minY,
            int maxY
    ) {
        BlockPos.MutableBlockPos mutable =
                new BlockPos.MutableBlockPos();

        for (int y = maxY;
                y >= minY;
                y--) {

            mutable.set(
                    x,
                    y,
                    z
            );

            if (minecraft.level
                    .getFluidState(mutable)
                    .is(FluidTags.WATER)) {
                return mutable.immutable();
            }
        }

        return null;
    }
}
