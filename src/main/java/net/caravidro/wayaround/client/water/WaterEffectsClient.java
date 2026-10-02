package net.caravidro.wayaround.client.water;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import net.caravidro.wayaround.storage.IncrementalSquareScan;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.particle.WayAroundParticles;
import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
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

    private static final ArrayList<TurbulenceEmitter>
            TURBULENCE =
            new ArrayList<>();

    private static final ArrayList<TurbulenceEmitter> pendingTurbulence = new ArrayList<>();
    private static IncrementalSquareScan scan;
    private static int scanX, scanY, scanZ;
    private static int turbulenceCenterY = Integer.MIN_VALUE;
    private static int lastRebuildTick;
    private static Level cachedLevel;
    private static final int SCAN_BUDGET = 128;
    private static boolean wasInWater;
    private static int ticks;
    private static int turbulenceCenterX =
            Integer.MIN_VALUE;
    private static int turbulenceCenterZ =
            Integer.MIN_VALUE;

    private WaterEffectsClient() {
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (!WorldFeatureRuntime.clientEnabled(WorldFeature.WATER_DYNAMICS)
                || minecraft.level == null || minecraft.player == null
                || !minecraft.level.dimension().equals(Level.OVERWORLD)) {
            clearCache();
            return;
        }
        if (minecraft.isPaused()) return;
        if (cachedLevel != minecraft.level) {
            clearCache();
            cachedLevel = minecraft.level;
        }

        ticks++;

        playerSplash(
                minecraft
        );

        fallingBlockSplashes(
                minecraft
        );

        if (scan != null && (Math.abs((long) minecraft.player.getBlockX() - scanX) > 22
                || Math.abs((long) minecraft.player.getBlockZ() - scanZ) > 22
                || Math.abs((long) minecraft.player.getBlockY() - scanY) > 7)) {
            scan = null;
            pendingTurbulence.clear();
        }
        if (scan == null && needsTurbulenceRebuild(minecraft)) {
            scanX = minecraft.player.getBlockX();
            scanY = minecraft.player.getBlockY();
            scanZ = minecraft.player.getBlockZ();
            scan = new IncrementalSquareScan(22);
            pendingTurbulence.clear();
        }
        if (scan != null) rebuildTurbulence(minecraft);

        if (ticks % 2 == 0) {
            emitTurbulence(
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
                                28.0,
                                20.0,
                                28.0
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

            double impactSpeed =
                    movement.length();

            float volume =
                    Mth.clamp(
                            (float) (
                                    0.34
                                    + impactSpeed * 0.48
                            ),
                            0.34F,
                            1.35F
                    );

            float pitch =
                    Mth.clamp(
                            1.12F
                            - (float) impactSpeed
                            * 0.16F
                            + (
                                    minecraft.level.random.nextFloat()
                                    - 0.5F
                            ) * 0.12F,
                            0.72F,
                            1.22F
                    );

            minecraft.level.playLocalSound(
                    falling.getX(),
                    falling.getY(),
                    falling.getZ(),
                    SoundEvents.GENERIC_SPLASH,
                    SoundSource.BLOCKS,
                    volume,
                    pitch,
                    false
            );

            int splashAmount =
                    Mth.clamp(
                            12
                            + (int) Math.round(
                                    impactSpeed * 12.0
                            ),
                            12,
                            30
                    );

            for (int i = 0;
                    i < splashAmount;
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

    private static boolean needsTurbulenceRebuild(
            Minecraft minecraft
    ) {
        int x =
                minecraft.player.getBlockX();

        int z =
                minecraft.player.getBlockZ();

        if (turbulenceCenterX
                == Integer.MIN_VALUE) {
            return true;
        }

        long dx = (long) x - turbulenceCenterX;

        long dz = (long) z - turbulenceCenterZ;

        return ticks - lastRebuildTick >= 20
                || Math.abs((long) minecraft.player.getBlockY() - turbulenceCenterY) >= 4
                || dx * dx
                + dz * dz
                >= 36;
    }

    private static void rebuildTurbulence(
            Minecraft minecraft
    ) {
        int centerX = scanX;
        int centerY = scanY;
        int centerZ = scanZ;
        int radius = 22;
        for (int budget = 0; budget < SCAN_BUDGET && scan.advance(); budget++) {
                int dx = scan.x();
                int dz = scan.z();
                int x = centerX + dx;
                int z = centerZ + dz;
                if (dx * dx + dz * dz > radius * radius
                        || !minecraft.level.hasChunk(x >> 4, z >> 4)) continue;

                BlockPos water =
                        findWater(
                                minecraft,
                                x,
                                z,
                                centerY - 7,
                                centerY + 7
                        );

                if (water == null) {
                    continue;
                }

                float turbulence =
                        WaterDynamics.turbulence(
                                minecraft.level,
                                water
                        );

                if (turbulence < 0.16F) {
                    continue;
                }

                Vec3 current =
                        WaterDynamics.currentAround(
                                minecraft.level,
                                water
                        );

                boolean impact =
                        WaterDynamics.hitsObstacle(
                                minecraft.level,
                                water,
                                current
                        );

                pendingTurbulence.add(
                        new TurbulenceEmitter(
                                water,
                                turbulence,
                                current,
                                impact
                        )
                );
            }


        if (!scan.complete()) return;
        scan = null;
        TURBULENCE.clear();
        TURBULENCE.addAll(pendingTurbulence);
        pendingTurbulence.clear();
        turbulenceCenterY = centerY;
        lastRebuildTick = ticks;

        TURBULENCE.sort(
                Comparator.comparingDouble(
                        TurbulenceEmitter::intensity
                ).reversed()
        );

        if (TURBULENCE.size() > 48) {
            TURBULENCE.subList(
                    48,
                    TURBULENCE.size()
            ).clear();
        }

        turbulenceCenterX =
                centerX;

        turbulenceCenterZ =
                centerZ;
    }

    private static void emitTurbulence(
            Minecraft minecraft
    ) {
        for (TurbulenceEmitter emitter : TURBULENCE) {
            if (emitter.pos().distSqr(minecraft.player.blockPosition()) > 32 * 32
                    || !minecraft.level.hasChunkAt(emitter.pos())) continue;
            if (!minecraft.level
                    .getFluidState(
                            emitter.pos()
                    )
                    .is(FluidTags.WATER)) {
                continue;
            }

            Vec3 current =
                    WaterDynamics.currentAround(
                            minecraft.level,
                            emitter.pos()
                    );

            float liveTurbulence =
                    WaterDynamics.turbulence(
                            minecraft.level,
                            emitter.pos()
                    );

            if (liveTurbulence < 0.13F) {
                continue;
            }

            Direction direction =
                    WaterDynamics.dominantDirection(
                            current
                    );

            double offsetX =
                    0.0;

            double offsetZ =
                    0.0;

            if (emitter.impact()
                    && direction != null) {

                offsetX =
                        direction.getStepX()
                        * 0.38;

                offsetZ =
                        direction.getStepZ()
                        * 0.38;
            }

            int count =
                    liveTurbulence > 0.65F
                            ? 2
                            : 1;

            for (int p = 0;
                    p < count;
                    p++) {

                double x =
                        emitter.pos().getX()
                        + 0.5
                        + offsetX
                        + (
                                minecraft.level.random.nextDouble()
                                - 0.5
                        ) * 0.32;

                double y =
                        emitter.pos().getY()
                        + 0.77
                        + minecraft.level.random.nextDouble()
                        * 0.08;

                double z =
                        emitter.pos().getZ()
                        + 0.5
                        + offsetZ
                        + (
                                minecraft.level.random.nextDouble()
                                - 0.5
                        ) * 0.32;

                minecraft.level.addParticle(
                        ParticleTypes.SPLASH,
                        x,
                        y,
                        z,
                        -current.x * 0.045
                                + (
                                        minecraft.level.random.nextDouble()
                                        - 0.5
                                ) * 0.028,
                        0.022
                                + liveTurbulence * 0.055
                                + minecraft.level.random.nextDouble()
                                * 0.025,
                        -current.z * 0.045
                                + (
                                        minecraft.level.random.nextDouble()
                                        - 0.5
                                ) * 0.028
                );

                if (liveTurbulence > 0.48F
                        && minecraft.level.random.nextFloat()
                        < 0.28F) {

                    minecraft.level.addParticle(
                            ParticleTypes.BUBBLE_POP,
                            x,
                            y - 0.05,
                            z,
                            current.x * 0.02,
                            0.012,
                            current.z * 0.02
                    );
                }
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

    public static void clearCache() {
        FALLING_BLOCKS_IN_WATER.clear();
        TURBULENCE.clear();
        TURBULENCE.trimToSize();
        pendingTurbulence.clear();
        pendingTurbulence.trimToSize();
        scan = null;
        cachedLevel = null;
        turbulenceCenterX = Integer.MIN_VALUE;
        turbulenceCenterY = Integer.MIN_VALUE;
        turbulenceCenterZ = Integer.MIN_VALUE;
        wasInWater = false;
        ticks = 0;
    }

    private record TurbulenceEmitter(
            BlockPos pos,
            float intensity,
            Vec3 current,
            boolean impact
    ) {
    }
}
