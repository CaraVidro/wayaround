package net.caravidro.wayaround.industrial.power;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.assembly.AssemblyAdvancements;
import net.caravidro.wayaround.industrial.assembly.AssemblyConnection;
import net.caravidro.wayaround.industrial.assembly.AssemblyEngine;
import net.caravidro.wayaround.industrial.assembly.AssemblyHistory;
import net.caravidro.wayaround.industrial.assembly.AssemblyItemData;
import net.caravidro.wayaround.industrial.assembly.AssemblyMachine;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartNode;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.caravidro.wayaround.interaction.StructuralDamage;
import net.caravidro.wayaround.interaction.StructuralReceiver;
import net.caravidro.wayaround.interaction.WorldForce;
import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class WaterWheelHubBlockEntity
        extends BlockEntity
        implements AssemblyMachine, StructuralReceiver {

    public static final int MAX_PLATES =
            32;

    public static final double FRAME_RADIUS =
            2.42;

    public static final double PADDLE_RADIUS =
            2.28;

    public static final float PLATE_ROTATION_STEP =
            5.0F;

    private float rpm;
    private float torque;
    private float efficiency;
    private float mechanicalPower;
    private float rotationDegrees;

    private int frameWear;
    private int wetContacts;

    private boolean unstableFlow;
    private boolean jammed;

    private float pendingMechanicalLoad;
    private float lastMechanicalLoad;
    private float availableMechanicalBudget;
    private float previousRpm;
    private float rpmDelta;

    private int failureCountdown =
            -1;

    private int collisionSoundCooldown;
    private int looseSoundCooldown;

    private int burnTicks;
    private float burnIntensity;

    private final List<Plate> plates =
            new ArrayList<>();

    private final IRotationalPower rotationOutput =
            new IRotationalPower() {

        @Override
        public float rpm() {
            return WaterWheelHubBlockEntity.this.rpm;
        }

        @Override
        public float torque() {
            return WaterWheelHubBlockEntity.this.torque;
        }

        @Override
        public float power() {
            return WaterWheelHubBlockEntity.this.mechanicalPower;
        }

        @Override
        public Direction.Axis axis() {
            return axleAxis();
        }

        @Override
        public float consumePower(
                float requestedPower
        ) {
            float granted =
                    Math.min(
                            Math.max(
                                    0.0F,
                                    requestedPower
                            ),
                            Math.max(
                                    0.0F,
                                    availableMechanicalBudget
                            )
                    );

            availableMechanicalBudget -=
                    granted;

            pendingMechanicalLoad +=
                    granted;

            return granted;
        }

        @Override
        public int rotationDirection() {
            if (Math.abs(rpm) < 0.01F) {
                return 0;
            }

            return rpm > 0.0F
                    ? 1
                    : -1;
        }
    };

    public WaterWheelHubBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.WATER_WHEEL_HUB_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            WaterWheelHubBlockEntity hub
    ) {
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.POWER_NETWORKS
        )) {
            return;
        }

        if (!(level instanceof ServerLevel server)) {
            return;
        }

        if (!WaterWheelHubBlock.hasSupport(
                server,
                pos,
                state
        )) {
            server.destroyBlock(
                    pos,
                    false
            );
            return;
        }

        long time =
                level.getGameTime();

        if (hub.collisionSoundCooldown > 0) {
            hub.collisionSoundCooldown--;
        }

        if (hub.looseSoundCooldown > 0) {
            hub.looseSoundCooldown--;
        }

        hub.simulate(
                server,
                state
        );

        if (hub.burnTicks <= 0
                && Math.floorMod(
                time + pos.asLong(),
                20
        ) == 0
                && hub.touchesFire(
                server
        )) {
            hub.ignite(
                    null
            );
        }

        if (hub.tickBurning(
                server
        )) {
            return;
        }

        if (Math.floorMod(
                time + pos.asLong(),
                8
        ) == 0) {
            hub.emitWaterFeedback(
                    server
            );
        }

        hub.emitLooseBoardRattle(
                server
        );

        int creakInterval =
                hub.creakInterval();

        if (creakInterval > 0
                && Math.floorMod(
                        time + pos.asLong(),
                        creakInterval
                ) == 0) {
            hub.emitWoodCreak(
                    server
            );
        }

        if (Math.floorMod(
                time + pos.asLong(),
                20
        ) == 0) {
            hub.applyWear(
                    server
            );
        }

        hub.updateCriticalFailure(
                server
        );

        if (Math.floorMod(
                time + pos.asLong(),
                40
        ) == 0) {
            hub.checkLavaSecret(
                    server
            );
        }

        if (Math.floorMod(
                time + pos.asLong(),
                4
        ) == 0
                && (
                        Math.abs(hub.rpm) > 0.01F
                        || hub.jammed
                        || hub.unstableFlow
                        || hub.burnTicks > 0
                )) {
            hub.sync();
        }
    }

    public void ignite(
            Player player
    ) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        burnTicks =
                Math.max(
                        burnTicks,
                        20 * 45
                );

        burnIntensity =
                Math.max(
                        burnIntensity,
                        0.28F
                );

        server.playSound(
                null,
                worldPosition,
                SoundEvents.FLINTANDSTEEL_USE,
                SoundSource.BLOCKS,
                0.75F,
                0.92F
        );

        server.playSound(
                null,
                worldPosition,
                SoundEvents.FIRE_AMBIENT,
                SoundSource.BLOCKS,
                0.35F,
                0.85F
        );

        if (player != null) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.water_wheel.ignited"
                    ),
                    true
            );
        }

        sync();
    }

    private boolean touchesFire(
            ServerLevel level
    ) {
        BlockPos min =
                worldPosition.offset(
                        -3,
                        -3,
                        -3
                );

        BlockPos max =
                worldPosition.offset(
                        3,
                        3,
                        3
                );

        for (BlockPos check :
                BlockPos.betweenClosed(
                        min,
                        max
                )) {

            if (!(level.getBlockState(
                    check
            ).getBlock()
                    instanceof BaseFireBlock)) {
                continue;
            }

            double dx =
                    check.getX()
                            + 0.5
                            - (
                            worldPosition.getX()
                                    + 0.5
                    );

            double dy =
                    check.getY()
                            + 0.5
                            - (
                            worldPosition.getY()
                                    + 0.5
                    );

            double dz =
                    check.getZ()
                            + 0.5
                            - (
                            worldPosition.getZ()
                                    + 0.5
                    );

            Direction.Axis axis =
                    axleAxis();

            double axial =
                    axis == Direction.Axis.X
                            ? Math.abs(
                            dx
                    )
                            : Math.abs(
                            dz
                    );

            double radialA =
                    dy;

            double radialB =
                    axis == Direction.Axis.X
                            ? dz
                            : dx;

            double radius =
                    Math.sqrt(
                            radialA
                                    * radialA
                                    + radialB
                                    * radialB
                    );

            if (axial <= (
                    doubleBody()
                            ? 1.15
                            : 0.72
            )
                    && radius <= FRAME_RADIUS
                            + 0.55) {
                return true;
            }
        }

        return false;
    }

    private boolean tickBurning(
            ServerLevel server
    ) {
        if (burnTicks <= 0
                && burnIntensity <= 0.01F) {
            burnTicks =
                    0;
            burnIntensity =
                    0.0F;
            return false;
        }

        int totalBoards =
                Math.max(
                        1,
                        plates.size()
                );

        float wetRatio =
                Mth.clamp(
                        wetContacts
                                / (float) totalBoards,
                        0.0F,
                        1.0F
                );

        boolean raining =
                server.isRainingAt(
                        worldPosition.above()
                );

        if (raining) {
            burnTicks =
                    Math.max(
                            0,
                            burnTicks - 14
                    );

            burnIntensity =
                    Math.max(
                            0.0F,
                            burnIntensity - 0.065F
                    );
        }

        if (wetRatio > 0.82F) {
            burnTicks =
                    Math.max(
                            0,
                            burnTicks - 22
                    );

            burnIntensity =
                    Math.max(
                            0.0F,
                            burnIntensity - 0.095F
                    );
        }

        if (burnTicks > 0) {
            burnTicks--;
        }

        /*
         * A dry wheel rapidly opens into multiple flame fronts. Water does not
         * instantly cancel fire; it fights the intensity while wet paddles pass
         * through the burning area.
         */
        float targetIntensity =
                burnTicks > 0
                        ? Mth.clamp(
                        0.38F
                                + plates.size()
                                        / 32.0F
                                        * 0.48F
                                - wetRatio
                                        * 0.62F,
                        0.05F,
                        1.0F
                )
                        : 0.0F;

        burnIntensity +=
                (
                        targetIntensity
                                - burnIntensity
                )
                        * (
                        targetIntensity > burnIntensity
                                ? 0.035F
                                : 0.075F
                );

        if (wetRatio > 0.72F
                && burnIntensity < 0.16F) {
            burnTicks =
                    Math.max(
                            0,
                            burnTicks - 6
                    );
        }

        if (Math.floorMod(
                server.getGameTime()
                        + worldPosition.asLong(),
                5
        ) == 0) {
            emitBurningFeedback(
                    server
            );
        }

        /*
         * Fire consumes the actual wooden assembly, not just a wear number.
         * Once the blaze is established, paddles disappear from the renderer
         * every few ticks. Nothing is dropped: the wood was burned.
         */
        if (burnIntensity > 0.34F
                && !plates.isEmpty()
                && Math.floorMod(
                server.getGameTime()
                        + worldPosition.asLong(),
                burnIntensity > 0.72F
                        ? 6
                        : 10
        ) == 0) {

            int removeCount =
                    burnIntensity > 0.82F
                            ? 2
                            : 1;

            for (int i = 0;
                 i < removeCount
                         && !plates.isEmpty();
                 i++) {

                int index =
                        server.random.nextInt(
                                plates.size()
                        );

                Plate removed =
                        plates.remove(
                                index
                        );

                double angle =
                        removed.anchorAngle
                                + Math.toRadians(
                                rotationDegrees
                        );

                Vec3 point =
                        Vec3.atCenterOf(
                                worldPosition
                        ).add(
                                radialVector(
                                        axleAxis(),
                                        angle
                                ).scale(
                                        removed.anchorRadius
                                )
                        );

                server.sendParticles(
                        ParticleTypes.FLAME,
                        point.x,
                        point.y,
                        point.z,
                        5,
                        0.28,
                        0.24,
                        0.28,
                        0.025
                );

                server.sendParticles(
                        ParticleTypes.LARGE_SMOKE,
                        point.x,
                        point.y + 0.18,
                        point.z,
                        4,
                        0.24,
                        0.20,
                        0.24,
                        0.022
                );
            }

            setChanged();
            sync();
        }

        if (Math.floorMod(
                server.getGameTime()
                        + worldPosition.asLong(),
                20
        ) == 0
                && burnIntensity > 0.04F) {

            int frameDamage =
                    Math.max(
                            1,
                            Math.round(
                                    260.0F
                                            + burnIntensity
                                                    * 640.0F
                            )
                    );

            frameWear =
                    Math.min(
                            AssemblyItemData.MAX_COMPONENT_WEAR,
                            frameWear
                                    + frameDamage
                    );

            for (Plate plate :
                    plates) {
                if (server.random.nextFloat()
                        > 0.34F
                                + burnIntensity
                                        * 0.38F) {
                    continue;
                }

                int plateDamage =
                        Math.max(
                                1,
                                Math.round(
                                        380.0F
                                                + burnIntensity
                                                        * 820.0F
                                )
                        );

                plate.wear =
                        Math.min(
                                AssemblyItemData.MAX_COMPONENT_WEAR,
                                plate.wear
                                        + plateDamage
                        );

                plate.profile.applyWear(
                        plateDamage
                                / (float) AssemblyItemData.MAX_COMPONENT_WEAR
                );
            }

            setChanged();
        }

        boolean frameGone =
                frameWear
                        >= Math.round(
                        AssemblyItemData.MAX_COMPONENT_WEAR
                                * 0.64F
                );

        boolean paddlesGone =
                plates.isEmpty()
                        && burnIntensity > 0.42F;

        if ((frameGone
                && burnIntensity > 0.58F)
                || paddlesGone) {

            Vec3 center =
                    Vec3.atCenterOf(
                            worldPosition
                    );

            server.sendParticles(
                    ParticleTypes.FLAME,
                    center.x,
                    center.y,
                    center.z,
                    18,
                    1.15,
                    1.15,
                    1.15,
                    0.045
            );

            server.sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    center.x,
                    center.y + 0.45,
                    center.z,
                    24,
                    1.35,
                    1.05,
                    1.35,
                    0.035
            );

            server.playSound(
                    null,
                    worldPosition,
                    SoundEvents.WOOD_BREAK,
                    SoundSource.BLOCKS,
                    1.15F,
                    0.68F
            );

            /*
             * No drops: the assembly has been consumed by the fire. Supports
             * remain, but the wheel body and all remaining wooden parts vanish.
             */
            server.destroyBlock(
                    worldPosition,
                    false
            );

            return true;
        }

        if (burnTicks <= 0
                && burnIntensity < 0.02F) {
            burnIntensity =
                    0.0F;

            server.playSound(
                    null,
                    worldPosition,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    0.35F,
                    1.10F
            );
        }

        return false;
    }

    private void emitBurningFeedback(
            ServerLevel server
    ) {
        int flamePoints =
                2
                        + Math.round(
                        burnIntensity
                                * 7.0F
                );

        Direction.Axis axis =
                axleAxis();

        double rotation =
                Math.toRadians(
                        rotationDegrees
                );

        for (int i = 0;
             i < flamePoints;
             i++) {

            double angle =
                    rotation
                            + server.random.nextDouble()
                                    * Math.PI
                                    * 2.0;

            double radius =
                    0.45
                            + server.random.nextDouble()
                                    * (
                                    FRAME_RADIUS
                                            - 0.20
                            );

            Vec3 radial =
                    radialVector(
                            axis,
                            angle
                    );

            Vec3 point =
                    Vec3.atCenterOf(
                            worldPosition
                    ).add(
                            radial.scale(
                                    radius
                            )
                    );

            double axleJitter =
                    (
                            server.random.nextDouble()
                                    - 0.5
                    )
                            * (
                            doubleBody()
                                    ? 1.30
                                    : 0.62
                    );

            if (axis == Direction.Axis.X) {
                point =
                        point.add(
                                axleJitter,
                                0.0,
                                0.0
                        );
            } else {
                point =
                        point.add(
                                0.0,
                                0.0,
                                axleJitter
                        );
            }

            server.sendParticles(
                    i % 3 == 0
                            && burnIntensity > 0.58F
                            ? ParticleTypes.FLAME
                            : ParticleTypes.SMALL_FLAME,
                    point.x,
                    point.y,
                    point.z,
                    burnIntensity > 0.72F
                            ? 2
                            : 1,
                    0.10
                            + burnIntensity
                                    * 0.16,
                    0.08,
                    0.10
                            + burnIntensity
                                    * 0.16,
                    0.008
            );
        }

        /*
         * Close smoke is dense and messy. A thinner signal-smoke stream is
         * deliberately emitted much higher so a burning machine can be located
         * from far away.
         */
        server.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                worldPosition.getX()
                        + 0.5,
                worldPosition.getY()
                        + 1.0,
                worldPosition.getZ()
                        + 0.5,
                2
                        + Math.round(
                        burnIntensity
                                * 5.0F
                ),
                0.65,
                0.50,
                0.65,
                0.025
        );

        if (burnIntensity > 0.42F
                && Math.floorMod(
                server.getGameTime()
                        + worldPosition.asLong(),
                15
        ) < 5) {
            Vec3 smokeOrigin =
                    Vec3.atCenterOf(
                            worldPosition
                    ).add(
                            0.0,
                            1.6,
                            0.0
                    );

            for (ServerPlayer viewer :
                    server.getPlayers(
                            candidate ->
                                    candidate.distanceToSqr(
                                            smokeOrigin
                                    )
                                            <= 128.0
                                            * 128.0
                    )) {
                server.sendParticles(
                        viewer,
                        ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                        true,
                        smokeOrigin.x,
                        smokeOrigin.y,
                        smokeOrigin.z,
                        1,
                        0.16,
                        0.10,
                        0.16,
                        0.008
                );
            }
        }
    }

    public boolean burning() {
        return burnTicks > 0
                || burnIntensity > 0.02F;
    }

    public float burnIntensity() {
        return burnIntensity;
    }

    private void simulate(
            ServerLevel level,
            BlockState state
    ) {
        float appliedMechanicalLoad =
                pendingMechanicalLoad;

        pendingMechanicalLoad =
                0.0F;

        lastMechanicalLoad =
                appliedMechanicalLoad;

        rpmDelta =
                rpm
                - previousRpm;

        previousRpm =
                rpm;

        if (plates.isEmpty()) {
            rpm *=
                    0.92F;

            if (Math.abs(rpm) < 0.01F) {
                rpm =
                        0.0F;
            }

            torque =
                    0.0F;

            efficiency =
                    0.0F;

            mechanicalPower =
                    0.0F;

            availableMechanicalBudget =
                    0.0F;

            wetContacts =
                    0;

            unstableFlow =
                    false;

            jammed =
                    false;

            return;
        }

        Direction.Axis axis =
                axleAxis();

        Vec3 axle =
                axleVector(
                        axis
                );

        double totalTorque =
                0.0;

        double waterTorque =
                0.0;

        double waterAlignment =
                0.0;

        double waterSpeed =
                0.0;

        int nailedCount =
                0;

        int directedContacts =
                0;

        int wet =
                0;

        double rotation =
                Math.toRadians(
                        rotationDegrees
                );

        for (int i = 0;
                i < plates.size();
                i++) {

            Plate plate =
                    plates.get(i);

            plate.lastWaterTorque =
                    0.0F;

            double worldAngle =
                    plate.anchorAngle
                    + rotation;

            Vec3 radial =
                    radialVector(
                            axis,
                            worldAngle
                    );

            Vec3 radiusVector =
                    radial.scale(
                            plate.anchorRadius
                    );

            Vec3 contact =
                    Vec3.atCenterOf(
                            worldPosition
                    ).add(
                            radiusVector
                    );

            BlockPos samplePos =
                    BlockPos.containing(
                            contact
                    );

            boolean wetNow =
                    hasWaterAround(
                            level,
                            samplePos
                    );

            plate.wet =
                    wetNow;

            if (wetNow) {
                wet++;
            }

            updateLoosePlate(
                    plate,
                    worldAngle
            );

            /*
             * Even a loose board is still hanging from its mounting hole, so
             * its mass remains part of the assembly and can unbalance the
             * wheel. What the nail changes is whether hydraulic force is
             * transmitted rigidly into the shaft.
             */
            double mass =
                    plate.mass();

            Vec3 gravityForce =
                    new Vec3(
                            0.0,
                            -0.22 * mass,
                            0.0
                    );

            totalTorque +=
                    radiusVector.cross(
                            gravityForce
                    ).dot(
                            axle
                    );

            if (!plate.nailed) {
                if (wetNow) {
                    WaterDynamics.MechanicalFlow looseFlow =
                            WaterDynamics.mechanicalFlow(
                                    level,
                                    samplePos
                            );

                    if (looseFlow.stable()
                            && looseFlow.vector()
                                    .lengthSqr() >= 0.0001) {

                        Vec3 tangent =
                                tangentVector(
                                        axis,
                                        worldAngle
                                );

                        double looseTilt =
                                Math.toRadians(
                                        plate.effectiveTilt()
                                );

                        Vec3 looseNormal =
                                tangent.scale(
                                        Math.cos(
                                                looseTilt
                                        )
                                ).add(
                                        radial.scale(
                                                Math.sin(
                                                        looseTilt
                                                )
                                        )
                                ).normalize();

                        double flowHit =
                                looseFlow.vector()
                                        .scale(
                                                4.0
                                        ).dot(
                                                looseNormal
                                        );

                        plate.looseSwingVelocity +=
                                Mth.clamp(
                                        (float) (
                                                flowHit
                                                * 0.42
                                        ),
                                        -3.2F,
                                        3.2F
                                );
                    }
                }

                continue;
            }

            nailedCount++;

            if (!wetNow) {
                continue;
            }

            WaterDynamics.MechanicalFlow flow =
                    WaterDynamics.mechanicalFlow(
                            level,
                            samplePos
                    );

            if (!flow.stable()
                    || flow.vector()
                            .lengthSqr() < 0.0001) {

                /*
                 * Still/chaotic water never drives the wheel. It only damps a
                 * wheel that is already moving.
                 */
                totalTorque +=
                        -rpm
                        * 0.0045
                        * mass;

                continue;
            }

            directedContacts++;

            Vec3 tangent =
                    tangentVector(
                            axis,
                            worldAngle
                    );

            double tilt =
                    Math.toRadians(
                            plate.effectiveTilt()
                    );

            Vec3 normal =
                    tangent.scale(
                            Math.cos(
                                    tilt
                            )
                    ).add(
                            radial.scale(
                                    Math.sin(
                                            tilt
                                    )
                            )
                    ).normalize();

            double omega =
                    rpm
                    * Math.PI
                    * 2.0
                    / 60.0;

            Vec3 wheelVelocity =
                    axle.scale(
                            omega
                    ).cross(
                            radiusVector
                    );

            Vec3 relativeFlow =
                    flow.vector()
                            .scale(
                                    4.0
                            )
                            .subtract(
                                    wheelVelocity
                            );

            double normalSpeed =
                    relativeFlow.dot(
                            normal
                    );

            double area =
                    plate.width
                    * plate.depth;

            Vec3 force =
                    normal.scale(
                            normalSpeed
                            * Math.abs(
                                    normalSpeed
                            )
                            * area
                            * 0.46
                            * flow.coherence()
                            * plate.profile.performanceFactor()
                    );

            double plateTorque =
                    radiusVector.cross(
                            force
                    ).dot(
                            axle
                    );

            plate.lastWaterTorque =
                    (float) plateTorque;

            totalTorque +=
                    plateTorque;

            waterTorque +=
                    Math.abs(
                            plateTorque
                    );

            waterAlignment +=
                    Math.min(
                            1.0,
                            Math.abs(
                                    normalSpeed
                            )
                            / 2.5
                    );

            waterSpeed +=
                    flow.vector()
                            .length();
        }

        wetContacts =
                wet;

        unstableFlow =
                wet > 0
                && directedContacts == 0;

        double inertia =
                (
                        state.getValue(
                                WaterWheelHubBlock.DOUBLE
                        )
                                ? 27.0
                                : 18.0
                );

        for (Plate plate :
                plates) {
            inertia +=
                    plate.mass()
                    * plate.anchorRadius
                    * plate.anchorRadius
                    * (
                            plate.nailed
                                    ? 1.0
                                    : 0.72
                    );
        }

        /*
         * Bearing friction grows with age. Old frames still work, but they
         * lose motion faster and complain loudly about it.
         */
        double wearRatio =
                frameWear
                / (double) AssemblyItemData.MAX_COMPONENT_WEAR;

        totalTorque +=
                -rpm
                * (
                        0.007
                        + wearRatio
                        * 0.012
                );

        if (appliedMechanicalLoad > 0.001F) {
            double direction =
                    Math.abs(rpm) > 0.05F
                            ? Math.signum(
                                    rpm
                            )
                            : Math.signum(
                                    totalTorque
                            );

            if (direction == 0.0) {
                direction =
                        1.0;
            }

            double omega =
                    Math.max(
                            0.40,
                            Math.abs(rpm)
                            * Math.PI
                            * 2.0
                            / 60.0
                    );

            double loadTorque =
                    Math.min(
                            7.5,
                            appliedMechanicalLoad
                            / omega
                    );

            totalTorque -=
                    direction
                    * loadTorque;
        }

        torque =
                (float) totalTorque;

        double angularAcceleration =
                totalTorque
                / Math.max(
                        1.0,
                        inertia
                );

        rpm +=
                (float) (
                        angularAcceleration
                        * 1.40
                );

        rpm =
                Mth.clamp(
                        rpm,
                        -58.0F,
                        58.0F
                );

        rpm *=
                (float) (
                        0.997
                        - wearRatio
                        * 0.0015
                );

        if (Math.abs(rpm) < 0.008F
                && Math.abs(totalTorque) < 0.025) {
            rpm =
                    0.0F;
        }

        float proposedRotation =
                wrapDegrees(
                        rotationDegrees
                        + rpm
                        * 0.30F
                );

        if (Math.abs(rpm) > 0.001F
                && wouldCollide(
                        level,
                        proposedRotation
                )) {

            emitBoardCollision(
                    level,
                    Math.abs(
                            rpm
                    )
            );

            for (Plate plate :
                    plates) {
                if (!plate.nailed) {
                    plate.looseSwingVelocity +=
                            Mth.clamp(
                                    rpm * 0.55F,
                                    -9.0F,
                                    9.0F
                            );
                }
            }

            rpm =
                    0.0F;

            mechanicalPower =
                    0.0F;

            availableMechanicalBudget =
                    0.0F;

            jammed =
                    true;
        } else {
            rotationDegrees =
                    proposedRotation;

            jammed =
                    false;
        }

        if (directedContacts <= 0
                || nailedCount <= 0
                || jammed) {

            efficiency =
                    0.0F;

            mechanicalPower =
                    0.0F;

            availableMechanicalBudget =
                    0.0F;

            return;
        }

        float targetEfficiency =
                (float) Mth.clamp(
                        (
                                waterAlignment
                                / directedContacts
                        )
                        * Math.min(
                                1.0,
                                directedContacts
                                / 6.0
                        ),
                        0.0,
                        1.0
                );

        efficiency =
                Mth.lerp(
                        0.12F,
                        efficiency,
                        targetEfficiency
                );

        mechanicalPower =
                (float) (
                        Math.abs(
                                rpm
                        )
                        * waterTorque
                        * 0.0174533
                        * efficiency
                );

        availableMechanicalBudget =
                mechanicalPower;
    }

    private boolean wouldCollide(
            ServerLevel level,
            float nextRotationDegrees
    ) {
        Direction.Axis axis =
                axleAxis();

        double rotation =
                Math.toRadians(
                        nextRotationDegrees
                );

        Vec3 axleDepth =
                axis == Direction.Axis.X
                        ? new Vec3(
                                1.0,
                                0.0,
                                0.0
                        )
                        : new Vec3(
                                0.0,
                                0.0,
                                1.0
                        );

        for (Plate plate :
                plates) {

            double angle =
                    plate.anchorAngle
                    + rotation;

            Vec3 radial =
                    radialVector(
                            axis,
                            angle
                    );

            Vec3 tangent =
                    tangentVector(
                            axis,
                            angle
                    );

            double tilt =
                    Math.toRadians(
                            plate.effectiveTilt()
                    );

            Vec3 boardAxis =
                    radial.scale(
                            Math.cos(
                                    tilt
                            )
                    ).add(
                            tangent.scale(
                                    Math.sin(
                                            tilt
                                    )
                            )
                    ).normalize();

            Vec3 center =
                    Vec3.atCenterOf(
                            worldPosition
                    ).add(
                            radial.scale(
                                    plate.anchorRadius
                            )
                    );

            double depth =
                    getBlockState()
                            .getValue(
                                    WaterWheelHubBlock.DOUBLE
                            )
                                    ? Math.max(
                                            1.34,
                                            plate.depth
                                    )
                                    : plate.depth;

            double[] along =
                    new double[] {
                            -0.50,
                            -0.25,
                            0.0,
                            0.25,
                            0.50
                    };

            double[] across =
                    new double[] {
                            -0.50,
                            0.0,
                            0.50
                    };

            for (double u :
                    along) {

                for (double v :
                        across) {

                    Vec3 check =
                            center.add(
                                    boardAxis.scale(
                                            plate.width
                                            * u
                                    )
                            ).add(
                                    axleDepth.scale(
                                            depth
                                            * v
                                    )
                            );

                    if (solidAssemblyObstacle(
                            level,
                            BlockPos.containing(
                                    check
                            )
                    )) {
                        return true;
                    }
                }
            }
        }

        /*
         * Only the boards are operational collision pieces. The body/frame
         * may overlap decorative construction around the axle; if a paddle
         * hits something, however, the entire assembly jams.
         */
        return false;
    }

    private void emitBoardCollision(
            ServerLevel level,
            float impactRpm
    ) {
        if (collisionSoundCooldown > 0) {
            return;
        }

        collisionSoundCooldown =
                7;

        level.playSound(
                null,
                worldPosition,
                impactRpm > 8.0F
                        ? SoundEvents.WOOD_BREAK
                        : SoundEvents.WOOD_HIT,
                SoundSource.BLOCKS,
                Mth.clamp(
                        0.35F
                        + impactRpm / 30.0F,
                        0.35F,
                        1.0F
                ),
                Mth.clamp(
                        1.15F
                        - impactRpm / 55.0F,
                        0.58F,
                        1.12F
                )
        );
    }

    private void updateLoosePlate(
            Plate plate,
            double worldAngle
    ) {
        if (plate.nailed) {
            plate.looseSwingDegrees =
                    0.0F;

            plate.looseSwingVelocity =
                    0.0F;

            return;
        }

        float speed =
                Math.abs(
                        rpm
                );

        float target =
                speed < 0.08F
                        ? 0.0F
                        : Mth.clamp(
                                (float) (
                                        -Math.sin(
                                                worldAngle
                                        )
                                        * Math.min(
                                                58.0,
                                                10.0
                                                + speed * 1.9
                                        )
                                ),
                                -65.0F,
                                65.0F
                        );

        plate.looseSwingVelocity +=
                (
                        target
                        - plate.looseSwingDegrees
                )
                * (
                        speed < 0.08F
                                ? 0.055F
                                : 0.082F
                );

        plate.looseSwingVelocity +=
                Mth.clamp(
                        (
                                rpmDelta
                        )
                        * 0.42F,
                        -4.5F,
                        4.5F
                );

        plate.looseSwingVelocity *=
                speed < 0.08F
                        ? 0.72F
                        : 0.91F;

        plate.looseSwingDegrees +=
                plate.looseSwingVelocity;

        plate.looseSwingDegrees =
                Mth.clamp(
                        plate.looseSwingDegrees,
                        -72.0F,
                        72.0F
                );
    }

    private boolean solidAssemblyObstacle(
            ServerLevel level,
            BlockPos pos
    ) {
        if (pos.equals(
                worldPosition
        )) {
            return false;
        }

        BlockState state =
                level.getBlockState(
                        pos
                );

        if (state.getBlock()
                instanceof WaterWheelSupportBlock
                || state.getBlock()
                instanceof WaterWheelHubBlock) {
            return false;
        }

        return !state.getCollisionShape(
                level,
                pos
        ).isEmpty();
    }

    public boolean addPlateAt(
            double anchorAngle,
            ItemStack source,
            @Nullable ServerPlayer player
    ) {
        if (plates.size() >= MAX_PLATES) {
            return false;
        }

        double normalized =
                normalizeAngle(
                        anchorAngle
                );

        for (Plate existing :
                plates) {
            if (angleDistance(
                    existing.anchorAngle,
                    normalized
            ) < Math.toRadians(
                    3.0
            )) {
                return false;
            }
        }

        Plate plate =
                new Plate();

        plate.anchorAngle =
                normalized;

        plate.anchorRadius =
                Math.max(
                        0.35,
                        hexRadiusAt(
                                normalized
                        )
                        - 0.08
                );

        plate.tiltDegrees =
                0.0F;

        RandomSource assemblyRandom =
                level == null
                        ? RandomSource.create()
                        : level.getRandom();

        plate.profile =
                AssemblyItemData.profileOrCreate(
                        source,
                        AssemblyPartProfile.Kind.BOARD,
                        AssemblyPartProfile.Material.WOOD,
                        (int) Math.round(
                                Math.toDegrees(
                                        normalized
                                )
                        ),
                        assemblyRandom
                );

        plate.wear =
                Math.max(
                        AssemblyItemData.wear(
                                source
                        ),
                        Math.round(
                                plate.profile.wear()
                                * AssemblyItemData.MAX_COMPONENT_WEAR
                        )
                );

        plate.profile.setWearFraction(
                plate.wear
                / (float) AssemblyItemData.MAX_COMPONENT_WEAR
        );

        plates.add(
                plate
        );

        if (player != null
                && !player.getAbilities().instabuild) {
            source.shrink(
                    1
            );
        }

        configurationChanged();

        return true;
    }

    /**
     * Temporary compatibility path for old interactions. New assembly
     * placement calls addPlateAt with the exact raycast angle.
     */
    public boolean addPlate() {
        return addPlateAt(
                plates.size()
                * Math.PI
                * 2.0
                / Math.max(
                        1,
                        plates.size() + 1
                ),
                new ItemStack(
                        PowerContent.WATER_WHEEL_BLADE_ITEM.get()
                ),
                null
        );
    }

    public boolean rotatePlate(
            int index,
            float amountDegrees
    ) {
        if (!validPlate(
                index
        )) {
            return false;
        }

        Plate plate =
                plates.get(
                        index
                );

        if (plate.nailed) {
            return false;
        }

        plate.tiltDegrees +=
                amountDegrees;

        if (Math.abs(rpm) > 0.25F) {
            plate.looseSwingVelocity +=
                    Mth.clamp(
                            rpm * 0.11F,
                            -3.0F,
                            3.0F
                    );
        }

        while (plate.tiltDegrees > 85.0F) {
            plate.tiltDegrees -=
                    170.0F;
        }

        while (plate.tiltDegrees < -85.0F) {
            plate.tiltDegrees +=
                    170.0F;
        }

        configurationChanged();

        return true;
    }

    public boolean installNail(
            int index,
            ItemStack source,
            ServerPlayer player
    ) {
        if (!validPlate(
                index
        )
                || !AssemblyItemData.isNail(
                        source
                )) {
            return false;
        }

        Plate plate =
                plates.get(
                        index
                );

        if (plate.nailed) {
            return false;
        }

        plate.tiltDegrees =
                Mth.clamp(
                        plate.effectiveTilt(),
                        -85.0F,
                        85.0F
                );

        plate.looseSwingDegrees =
                0.0F;

        plate.looseSwingVelocity =
                0.0F;

        plate.nailed =
                true;

        plate.nail =
                source.copyWithCount(
                        1
                );

        plate.nailWear =
                AssemblyItemData.nailWear(
                        source
                );

        if (!player.getAbilities()
                .instabuild) {
            source.shrink(
                    1
            );
        }

        player.level()
                .playSound(
                        null,
                        worldPosition,
                        SoundEvents.WOOD_PLACE,
                        SoundSource.BLOCKS,
                        0.55F,
                        1.25F
                );

        configurationChanged();

        judgeCompletedAssembly(
                player
        );

        return true;
    }

    private void judgeCompletedAssembly(
            ServerPlayer player
    ) {
        if (!(player.level()
                instanceof ServerLevel level)) {
            return;
        }

        if (plates.size() < 6) {
            return;
        }

        for (Plate plate :
                plates) {
            if (!plate.nailed) {
                return;
            }
        }

        double weightedX =
                0.0;

        double weightedY =
                0.0;

        double totalWeight =
                0.0;

        double[] angles =
                new double[plates.size()];

        for (int i = 0;
                i < plates.size();
                i++) {

            Plate plate =
                    plates.get(i);

            double weight =
                    plate.mass()
                    * plate.anchorRadius;

            weightedX +=
                    Math.cos(
                            plate.anchorAngle
                    )
                    * weight;

            weightedY +=
                    Math.sin(
                            plate.anchorAngle
                    )
                    * weight;

            totalWeight +=
                    weight;

            angles[i] =
                    normalizeAngle(
                            plate.anchorAngle
                    );
        }

        double imbalance =
                totalWeight <= 0.0001
                        ? 1.0
                        : Math.sqrt(
                                weightedX * weightedX
                                + weightedY * weightedY
                        )
                        / totalWeight;

        Arrays.sort(
                angles
        );

        double largestGap =
                0.0;

        for (int i = 0;
                i < angles.length;
                i++) {

            double current =
                    angles[i];

            double next =
                    i + 1 < angles.length
                            ? angles[i + 1]
                            : angles[0]
                            + Math.PI * 2.0;

            largestGap =
                    Math.max(
                            largestGap,
                            next - current
                    );
        }

        boolean clearsFullRotation =
                true;

        for (int step = 0;
                step < 24;
                step++) {

            float testedAngle =
                    step
                    * 15.0F;

            if (wouldCollide(
                    level,
                    testedAngle
            )) {
                clearsFullRotation =
                        false;
                break;
            }
        }

        boolean balanced =
                imbalance <= 0.16;

        boolean covered =
                largestGap <= Math.toRadians(
                        95.0
                );

        if (balanced
                && covered
                && clearsFullRotation) {
            AssemblyAdvancements.waterWheel(
                    player
            );
        } else {
            AssemblyAdvancements.failedWaterWheel(
                    player
            );
        }
    }

    public boolean removeNail(
            int index,
            ServerPlayer player
    ) {
        if (!validPlate(
                index
        )) {
            return false;
        }

        Plate plate =
                plates.get(
                        index
                );

        if (!plate.nailed
                || plate.nail.isEmpty()) {
            return false;
        }

        ItemStack returned =
                AssemblyItemData.withNailWear(
                        plate.nail,
                        plate.nailWear
                );

        giveOrDrop(
                player,
                returned
        );

        plate.nailed =
                false;

        plate.nail =
                ItemStack.EMPTY;

        plate.nailWear =
                0;

        player.level()
                .playSound(
                        null,
                        worldPosition,
                        SoundEvents.WOOD_HIT,
                        SoundSource.BLOCKS,
                        0.45F,
                        1.15F
                );

        configurationChanged();

        return true;
    }

    public boolean removePlate(
            int index,
            ServerPlayer player
    ) {
        if (!validPlate(
                index
        )) {
            return false;
        }

        Plate plate =
                plates.get(
                        index
                );

        if (plate.nailed) {
            return false;
        }

        plate.profile.setWearFraction(
                plate.wear
                / (float) AssemblyItemData.MAX_COMPONENT_WEAR
        );

        ItemStack returned =
                AssemblyItemData.withWear(
                        new ItemStack(
                                PowerContent.WATER_WHEEL_BLADE_ITEM.get()
                        ),
                        plate.wear
                );

        AssemblyItemData.writePart(
                returned,
                plate.profile
        );

        giveOrDrop(
                player,
                returned
        );

        plates.remove(
                index
        );

        configurationChanged();

        return true;
    }

    private static void giveOrDrop(
            ServerPlayer player,
            ItemStack stack
    ) {
        if (!player.getInventory()
                .add(
                        stack
                )) {
            player.drop(
                    stack,
                    false
            );
        }
    }

    public int findPlateAt(
            double angle,
            double radius
    ) {
        if (radius < 1.55
                || radius > 2.95) {
            return -1;
        }

        double localAngle =
                normalizeAngle(
                        angle
                        - Math.toRadians(
                                rotationDegrees
                        )
                );

        int best =
                -1;

        double bestDistance =
                Double.MAX_VALUE;

        for (int i = 0;
                i < plates.size();
                i++) {

            Plate plate =
                    plates.get(i);

            double distance =
                    angleDistance(
                            localAngle,
                            plate.anchorAngle
                    );

            double allowance =
                    Math.max(
                            Math.toRadians(
                                    5.0
                            ),
                            plate.width
                            / Math.max(
                                    0.35,
                                    plate.anchorRadius
                            )
                            * 0.62
                    );

            double radialDistance =
                    Math.abs(
                            radius
                            - plate.anchorRadius
                    );

            if (distance <= allowance
                    && radialDistance <= 0.52
                    && distance < bestDistance) {

                best =
                        i;

                bestDistance =
                        distance;
            }
        }

        return best;
    }

    public boolean frameHit(
            double localAngle,
            double radius
    ) {
        return Math.abs(
                radius
                - hexRadiusAt(
                        localAngle
                )
        ) <= 0.30;
    }

    public static double hexRadiusAt(
            double localAngle
    ) {
        double sector =
                Math.PI
                / 3.0;

        double wrapped =
                localAngle
                % sector;

        if (wrapped < 0.0) {
            wrapped +=
                    sector;
        }

        double delta =
                wrapped
                - sector * 0.5;

        double apothem =
                FRAME_RADIUS
                * 0.8660254037844386;

        return apothem
                / Math.cos(
                        delta
                );
    }

    private void applyWear(
            ServerLevel level
    ) {
        double motion =
                Math.abs(
                        rpm
                );

        double load =
                Math.abs(
                        torque
                );

        if (motion < 0.05
                && load < 0.08) {
            return;
        }

        int frameDamage =
                Math.max(
                        1,
                        (int) Math.ceil(
                                motion / 28.0
                                + load / 5.0
                        )
                );

        frameWear =
                Math.min(
                        AssemblyItemData.MAX_COMPONENT_WEAR,
                        frameWear
                        + frameDamage
                );

        boolean changed =
                frameDamage > 0;

        for (int i = plates.size() - 1;
                i >= 0;
                i--) {

            Plate plate =
                    plates.get(i);

            if (!plate.nailed) {
                continue;
            }

            double plateLoad =
                    Math.abs(
                            plate.lastWaterTorque
                    );

            if (plate.wet
                    || plateLoad > 0.10) {

                int previousWear =
                        plate.wear;

                plate.wear =
                        Math.min(
                                AssemblyItemData.MAX_COMPONENT_WEAR,
                                plate.wear
                                + Math.max(
                                        1,
                                        (int) Math.ceil(
                                                motion / 35.0
                                                + plateLoad / 2.8
                                        )
                                )
                        );

                plate.profile.applyWear(
                        (plate.wear - previousWear)
                        / (float) AssemblyItemData.MAX_COMPONENT_WEAR
                );

                changed =
                        true;
            }

            if (!plate.nail.isEmpty()) {

                plate.nailWear +=
                        Math.max(
                                1,
                                (int) Math.ceil(
                                        plateLoad
                                        * 0.65
                                        + motion / 45.0
                                )
                        );

                int durability =
                        AssemblyItemData.nailDurability(
                                plate.nail
                        );

                if (plate.nailWear >= durability) {
                    level.playSound(
                            null,
                            worldPosition,
                            SoundEvents.WOOD_BREAK,
                            SoundSource.BLOCKS,
                            0.65F,
                            0.8F
                            + level.random.nextFloat()
                            * 0.18F
                    );

                    plate.nailed =
                            false;

                    plate.nail =
                            ItemStack.EMPTY;

                    plate.nailWear =
                            0;
                }
            }

            if (plate.wear
                    >= AssemblyItemData.MAX_COMPONENT_WEAR
                    && plateLoad > 1.4
                    && level.random.nextFloat()
                    < 0.025F) {

                level.playSound(
                        null,
                        worldPosition,
                        SoundEvents.WOOD_BREAK,
                        SoundSource.BLOCKS,
                        0.9F,
                        0.65F
                );

                plates.remove(
                        i
                );
            }
        }

        if (changed) {
            setChanged();
        }
    }

    private void updateCriticalFailure(
            ServerLevel level
    ) {
        boolean stressed =
                Math.abs(rpm) > 2.5F
                || Math.abs(torque) > 0.65F
                || lastMechanicalLoad > 0.25F;

        if (frameWear < 9_150) {
            failureCountdown =
                    -1;
            return;
        }

        if (!stressed) {
            return;
        }

        if (failureCountdown < 0) {
            failureCountdown =
                    100
                    + level.random.nextInt(
                            121
                    );

            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.WOODEN_DOOR_OPEN,
                    SoundSource.BLOCKS,
                    0.82F,
                    0.48F
            );

            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.WOOD_BREAK,
                    SoundSource.BLOCKS,
                    0.95F,
                    0.62F
            );

            level.sendParticles(
                    ParticleTypes.CLOUD,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    7,
                    0.75,
                    0.75,
                    0.75,
                    0.025
            );

            sync();
            return;
        }

        int stressStep =
                1
                + (Math.abs(rpm) > 14.0F
                        ? 1
                        : 0)
                + (Math.abs(torque) > 1.8F
                        ? 1
                        : 0)
                + (lastMechanicalLoad > 1.0F
                        ? 1
                        : 0);

        failureCountdown -=
                stressStep;

        if (failureCountdown > 0
                && failureCountdown % 38 < stressStep) {

            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.WOOD_BREAK,
                    SoundSource.BLOCKS,
                    0.58F,
                    0.72F
                    + level.random.nextFloat()
                    * 0.12F
            );
        }

        if (failureCountdown > 0) {
            return;
        }

        level.playSound(
                null,
                worldPosition,
                SoundEvents.WOOD_BREAK,
                SoundSource.BLOCKS,
                1.25F,
                0.48F
        );

        level.sendParticles(
                ParticleTypes.CLOUD,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5,
                22,
                1.35,
                1.35,
                1.35,
                0.07
        );

        AssemblyHistory.recordFailure(
                level,
                this,
                "water_wheel_structural_collapse"
        );

        level.destroyBlock(
                worldPosition,
                false
        );
    }

    private void emitLooseBoardRattle(
            ServerLevel level
    ) {
        if (looseSoundCooldown > 0
                || Math.abs(rpm) < 1.25F) {
            return;
        }

        int loose =
                0;

        float swing =
                0.0F;

        for (Plate plate :
                plates) {
            if (!plate.nailed) {
                loose++;
                swing =
                        Math.max(
                                swing,
                                Math.abs(
                                        plate.looseSwingVelocity
                                )
                        );
            }
        }

        if (loose <= 0
                || swing < 0.22F) {
            return;
        }

        looseSoundCooldown =
                12
                + level.random.nextInt(
                        10
                );

        level.playSound(
                null,
                worldPosition,
                SoundEvents.WOOD_HIT,
                SoundSource.BLOCKS,
                Mth.clamp(
                        0.12F
                        + loose * 0.035F
                        + swing * 0.018F,
                        0.12F,
                        0.52F
                ),
                0.92F
                + level.random.nextFloat()
                * 0.18F
        );
    }

    private void emitWaterFeedback(
            ServerLevel level
    ) {
        if (plates.isEmpty()) {
            return;
        }

        Direction.Axis axis =
                axleAxis();

        double rotation =
                Math.toRadians(
                        rotationDegrees
                );

        int emitted =
                0;

        for (Plate plate :
                plates) {

            if (!plate.nailed
                    || !plate.wet
                    || Math.abs(
                            plate.lastWaterTorque
                    ) < 0.06F) {
                continue;
            }

            Vec3 contact =
                    Vec3.atCenterOf(
                            worldPosition
                    ).add(
                            radialVector(
                                    axis,
                                    plate.anchorAngle
                                    + rotation
                            ).scale(
                                    plate.anchorRadius
                            )
                    );

            level.sendParticles(
                    ParticleTypes.SPLASH,
                    contact.x,
                    contact.y,
                    contact.z,
                    Math.abs(
                            plate.lastWaterTorque
                    ) > 0.8F
                            ? 3
                            : 1,
                    0.12,
                    0.10,
                    0.12,
                    0.025
            );

            emitted++;

            if (emitted >= 8) {
                break;
            }
        }

        if (emitted > 0) {
            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.GENERIC_SPLASH,
                    SoundSource.BLOCKS,
                    0.17F
                    + Math.min(
                            0.28F,
                            Math.abs(rpm)
                            / 90.0F
                    ),
                    0.90F
                    + level.random.nextFloat()
                    * 0.14F
            );
        }
    }

    private int creakInterval() {
        if (Math.abs(rpm) < 0.18F
                && !jammed) {
            return 0;
        }

        double wear =
                frameWear
                / (double) AssemblyItemData.MAX_COMPONENT_WEAR;

        return Math.max(
                8,
                34
                - (int) Math.round(
                        wear * 20.0
                )
        );
    }

    private void emitWoodCreak(
            ServerLevel level
    ) {
        double wear =
                frameWear
                / (double) AssemblyItemData.MAX_COMPONENT_WEAR;

        float volume =
                (float) Mth.clamp(
                        0.11
                        + wear * 0.38
                        + (
                                jammed
                                        ? 0.18
                                        : 0.0
                        ),
                        0.10,
                        0.65
                );

        float pitch =
                (float) (
                        0.58
                        - wear * 0.16
                        + level.random.nextFloat()
                        * 0.11
                );

        level.playSound(
                null,
                worldPosition,
                jammed
                        ? SoundEvents.WOOD_HIT
                        : SoundEvents.WOODEN_DOOR_OPEN,
                SoundSource.BLOCKS,
                volume,
                pitch
        );
    }

    private void checkLavaSecret(
            ServerLevel level
    ) {
        if (!touchesLava(
                level
        )) {
            return;
        }

        Player nearest =
                level.getNearestPlayer(
                        worldPosition.getX()
                                + 0.5,
                        worldPosition.getY()
                                + 0.5,
                        worldPosition.getZ()
                                + 0.5,
                        8.0,
                        false
                );

        if (nearest
                instanceof ServerPlayer serverPlayer) {
            AssemblyAdvancements.lavaWheel(
                    serverPlayer
            );
        }

        /*
         * Lava is an achievement, not an alternative working fluid. It cooks
         * wooden parts rapidly and makes the joke mechanically honest.
         */
        frameWear =
                Math.min(
                        AssemblyItemData.MAX_COMPONENT_WEAR,
                        frameWear + 120
                );

        Direction.Axis axis =
                axleAxis();

        double rotation =
                Math.toRadians(
                        rotationDegrees
                );

        for (Plate plate :
                plates) {

            Vec3 point =
                    Vec3.atCenterOf(
                            worldPosition
                    ).add(
                            radialVector(
                                    axis,
                                    plate.anchorAngle
                                    + rotation
                            ).scale(
                                    plate.anchorRadius
                            )
                    );

            if (level.getFluidState(
                    BlockPos.containing(
                            point
                    )
            ).is(
                    FluidTags.LAVA
            )) {
                plate.wear =
                        Math.min(
                                AssemblyItemData.MAX_COMPONENT_WEAR,
                                plate.wear + 320
                        );
            }
        }

        level.playSound(
                null,
                worldPosition,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.28F,
                0.82F
        );

        level.sendParticles(
                ParticleTypes.SMOKE,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5,
                7,
                1.1,
                1.1,
                1.1,
                0.025
        );

        setChanged();
    }

    private boolean touchesLava(
            Level level
    ) {
        Direction.Axis axis =
                axleAxis();

        double rotation =
                Math.toRadians(
                        rotationDegrees
                );

        for (Plate plate :
                plates) {

            Vec3 point =
                    Vec3.atCenterOf(
                            worldPosition
                    ).add(
                            radialVector(
                                    axis,
                                    plate.anchorAngle
                                    + rotation
                            ).scale(
                                    plate.anchorRadius
                            )
                    );

            if (level.getFluidState(
                    BlockPos.containing(
                            point
                    )
            ).is(
                    FluidTags.LAVA
            )) {
                return true;
            }
        }

        for (int i = 0;
                i < 12;
                i++) {

            double angle =
                    rotation
                    + i
                    * Math.PI
                    * 2.0
                    / 12.0;

            Vec3 point =
                    Vec3.atCenterOf(
                            worldPosition
                    ).add(
                            radialVector(
                                    axis,
                                    angle
                            ).scale(
                                    FRAME_RADIUS
                            )
                    );

            if (level.getFluidState(
                    BlockPos.containing(
                            point
                    )
            ).is(
                    FluidTags.LAVA
            )) {
                return true;
            }
        }

        return false;
    }

    public void dropAssembly(
            ServerLevel level,
            BlockState state
    ) {
        ItemStack hubStack =
                AssemblyItemData.withWear(
                        new ItemStack(
                                PowerContent.WATER_WHEEL_HUB_ITEM.get()
                        ),
                        frameWear
                );

        Block.popResource(
                level,
                worldPosition,
                hubStack
        );

        if (state.getValue(
                WaterWheelHubBlock.DOUBLE
        )) {
            Block.popResource(
                    level,
                    worldPosition,
                    AssemblyItemData.withWear(
                            new ItemStack(
                                    PowerContent.WATER_WHEEL_HUB_ITEM.get()
                            ),
                            frameWear
                    )
            );
        }

        for (Plate plate :
                plates) {

            Block.popResource(
                    level,
                    worldPosition,
                    AssemblyItemData.withWear(
                            new ItemStack(
                                    PowerContent.WATER_WHEEL_BLADE_ITEM.get()
                            ),
                            plate.wear
                    )
            );

            if (!plate.nail.isEmpty()) {
                Block.popResource(
                        level,
                        worldPosition,
                        AssemblyItemData.withNailWear(
                                plate.nail,
                                plate.nailWear
                        )
                );
            }
        }
    }

    public void restoreFrameWear(
            ItemStack stack
    ) {
        frameWear =
                AssemblyItemData.wear(
                        stack
                );

        setChanged();
    }

    private static boolean hasWaterAround(
            Level level,
            BlockPos pos
    ) {
        if (level.getFluidState(pos)
                .is(FluidTags.WATER)) {
            return true;
        }

        for (Direction direction :
                Direction.values()) {

            if (level.getFluidState(
                    pos.relative(
                            direction
                    )
            ).is(
                    FluidTags.WATER
            )) {
                return true;
            }
        }

        return false;
    }

    public void configurationChanged() {
        sync();
    }

    private void sync() {
        setChanged();

        if (level instanceof ServerLevel server) {
            BlockState state =
                    getBlockState();

            server.sendBlockUpdated(
                    worldPosition,
                    state,
                    state,
                    Block.UPDATE_CLIENTS
            );
        }
    }

    @Nullable
    public IRotationalPower rotationOutput(
            @Nullable Direction side
    ) {
        if (side != null
                && side.getAxis()
                != axleAxis()) {
            return null;
        }

        return rotationOutput;
    }

    public Direction.Axis axleAxis() {
        return getBlockState()
                .getValue(
                        WaterWheelHubBlock.FACING
                ).getAxis();
    }


    @Override
    public ResourceLocation assemblyType() {
        return ResourceLocation.fromNamespaceAndPath(
                WayAround.MODID,
                "water_wheel"
        );
    }

    @Override
    public BlockPos assemblyAnchor() {
        return worldPosition;
    }

    @Override
    public Collection<AssemblyPartNode> assemblyParts() {
        List<AssemblyPartNode> nodes =
                new ArrayList<>();

        AssemblyPartProfile frame =
                AssemblyPartProfile.legacy(
                        AssemblyPartProfile.Kind.FRAME,
                        AssemblyPartProfile.Material.WOOD,
                        ResourceLocation.fromNamespaceAndPath(
                                WayAround.MODID,
                                "water_wheel_hub"
                        ),
                        0,
                        frameWear
                                / (float) AssemblyItemData.MAX_COMPONENT_WEAR
                );

        nodes.add(
                new AssemblyPartNode(
                        "frame",
                        "frame",
                        frame,
                        WaterWheelHubBlock.hasSupport(
                                level,
                                worldPosition,
                                getBlockState()
                        ),
                        2.4F
                )
        );

        for (int index = 0;
             index < plates.size();
             index++) {
            Plate plate =
                    plates.get(
                            index
                    );

            nodes.add(
                    new AssemblyPartNode(
                            "plate_" + index,
                            "paddle",
                            plate.profile,
                            false,
                            (float) Math.max(
                                    0.25,
                                    plate.mass()
                            )
                    )
            );
        }

        return List.copyOf(
                nodes
        );
    }

    @Override
    public Collection<AssemblyConnection> assemblyConnections() {
        List<AssemblyConnection> connections =
                new ArrayList<>();

        for (int index = 0;
             index < plates.size();
             index++) {
            Plate plate =
                    plates.get(
                            index
                    );

            float nailCondition =
                    0.10F;

            float wear =
                    Math.min(
                            1.0F,
                            plate.wear
                                    / (float) AssemblyItemData.MAX_COMPONENT_WEAR
                    );

            if (plate.nailed
                    && !plate.nail.isEmpty()) {
                int durability =
                        Math.max(
                                1,
                                AssemblyItemData.nailDurability(
                                        plate.nail
                                )
                        );

                nailCondition =
                        Math.max(
                                0.05F,
                                1.0F
                                        - plate.nailWear
                                                / (float) durability
                        );
            }

            connections.add(
                    new AssemblyConnection(
                            "frame",
                            "plate_" + index,
                            plate.nailed
                                    ? AssemblyConnection.Type.FASTENED
                                    : AssemblyConnection.Type.CONTACT,
                            plate.nailed
                                    ? 0.55F
                                            + nailCondition
                                                    * 0.45F
                                    : 0.12F,
                            wear
                    )
            );
        }

        return List.copyOf(
                connections
        );
    }

    @Override
    public float currentAssemblyLoad() {
        return (float) (
                Math.abs(
                        torque
                )
                        + lastMechanicalLoad
                        + (
                        jammed
                                ? 2.0
                                : 0.0
                )
        );
    }

    @Override
    public void applyAssemblyWear(
            float fraction
    ) {
        float wear =
                Mth.clamp(
                        fraction,
                        0.0F,
                        1.0F
                );

        if (wear <= 0.0F) {
            return;
        }

        frameWear =
                Math.min(
                        AssemblyItemData.MAX_COMPONENT_WEAR,
                        frameWear
                                + Math.max(
                                1,
                                Math.round(
                                        wear
                                                * AssemblyItemData.MAX_COMPONENT_WEAR
                                )
                        )
                );

        for (Plate plate :
                plates) {
            float plateWear =
                    wear
                            * (
                            plate.nailed
                                    ? 0.80F
                                    : 1.15F
                    );

            plate.profile.applyWear(
                    plateWear
            );

            plate.wear =
                    Math.min(
                            AssemblyItemData.MAX_COMPONENT_WEAR,
                            Math.max(
                                    plate.wear,
                                    Math.round(
                                            plate.profile.wear()
                                                    * AssemblyItemData.MAX_COMPONENT_WEAR
                                    )
                            )
                    );

            if (plate.nailed
                    && !plate.nail.isEmpty()) {
                plate.nailWear +=
                        Math.max(
                                1,
                                Math.round(
                                        wear
                                                * AssemblyItemData.nailDurability(
                                                plate.nail
                                        )
                                                * 0.30F
                                )
                        );

                if (plate.nailWear
                        >= AssemblyItemData.nailDurability(
                        plate.nail
                )) {
                    plate.nailed =
                            false;

                    plate.nail =
                            ItemStack.EMPTY;

                    plate.nailWear =
                            0;
                }
            }
        }

        if (frameWear
                >= 9_150
                && failureCountdown < 0) {
            failureCountdown =
                    40;
        }

        sync();
    }

    @Override
    public BlockPos structuralPosition() {
        return worldPosition;
    }

    @Override
    public float structuralIntegrity() {
        return assemblySnapshot()
                .structuralIntegrity();
    }

    @Override
    public void receiveWorldForce(
            WorldForce force,
            float localMagnitude
    ) {
        float normalized =
                Math.min(
                        8.0F,
                        Math.max(
                                0.0F,
                                localMagnitude
                        )
                );

        pendingMechanicalLoad +=
                normalized
                        * 0.28F;

        applyAssemblyWear(
                AssemblyEngine.externalWearFraction(
                        this,
                        normalized
                )
        );

        if (force.kind()
                == WorldForce.Kind.PULL
                && normalized > 1.35F) {
            for (Plate plate :
                    plates) {
                if (plate.nailed
                        || level == null
                        || level.random.nextFloat()
                                > Math.min(
                                0.55F,
                                normalized
                                        * 0.10F
                        )) {
                    continue;
                }

                plate.looseSwingVelocity +=
                        normalized
                                * (
                                level.random.nextBoolean()
                                        ? 1.0F
                                        : -1.0F
                        );
            }
        }
    }

    @Override
    public void receiveStructuralDamage(
            StructuralDamage damage
    ) {
        float normalized =
                Math.min(
                        10.0F,
                        damage.amount()
                                * 0.24F
                                + damage.impulse()
                                        * 0.18F
                );

        applyAssemblyWear(
                AssemblyEngine.externalWearFraction(
                        this,
                        normalized
                )
                        + damage.amount()
                                * 0.0020F
        );
    }

    /**
     * Positive assembly angle follows this vector's right-hand rule. The
     * axis-X renderer uses y=sin(angle), z=cos(angle), which corresponds to
     * -X rather than +X.
     */
    private static Vec3 axleVector(
            Direction.Axis axis
    ) {
        return axis == Direction.Axis.X
                ? new Vec3(
                        -1.0,
                        0.0,
                        0.0
                )
                : new Vec3(
                        0.0,
                        0.0,
                        1.0
                );
    }

    public static Vec3 radialVector(
            Direction.Axis axis,
            double angle
    ) {
        double sin =
                Math.sin(
                        angle
                );

        double cos =
                Math.cos(
                        angle
                );

        if (axis == Direction.Axis.X) {
            return new Vec3(
                    0.0,
                    sin,
                    cos
            );
        }

        return new Vec3(
                cos,
                sin,
                0.0
        );
    }

    public static Vec3 tangentVector(
            Direction.Axis axis,
            double angle
    ) {
        double sin =
                Math.sin(
                        angle
                );

        double cos =
                Math.cos(
                        angle
                );

        if (axis == Direction.Axis.X) {
            return new Vec3(
                    0.0,
                    cos,
                    -sin
            );
        }

        return new Vec3(
                -sin,
                cos,
                0.0
        );
    }

    public float rpm() {
        return rpm;
    }

    public float torque() {
        return torque;
    }

    public float efficiency() {
        return efficiency;
    }

    public float mechanicalPower() {
        return mechanicalPower;
    }

    public float rotationDegrees() {
        return rotationDegrees;
    }

    public int frameWear() {
        return frameWear;
    }

    public float frameWearRatio() {
        return frameWear
                / (float) AssemblyItemData.MAX_COMPONENT_WEAR;
    }

    public int plateCount() {
        return plates.size();
    }

    public int wetContacts() {
        return wetContacts;
    }

    public boolean unstableFlow() {
        return unstableFlow;
    }

    public boolean jammed() {
        return jammed;
    }

    public double plateBaseAngle(
            int index,
            boolean ignoredDoubleBody
    ) {
        if (!validPlate(
                index
        )) {
            return 0.0;
        }

        return plates.get(
                index
        ).anchorAngle;
    }

    public float plateTiltDegrees(
            int index
    ) {
        if (!validPlate(
                index
        )) {
            return 0.0F;
        }

        return plates.get(
                index
        ).effectiveTilt();
    }

    public int plateWear(
            int index
    ) {
        if (!validPlate(
                index
        )) {
            return 0;
        }

        return plates.get(
                index
        ).wear;
    }

    public boolean plateNailed(
            int index
    ) {
        return validPlate(
                index
        )
                && plates.get(
                        index
                ).nailed;
    }

    public ItemStack plateNail(
            int index
    ) {
        if (!validPlate(
                index
        )) {
            return ItemStack.EMPTY;
        }

        return plates.get(
                index
        ).nail.copy();
    }

    public float plateWearRatio(
            int index
    ) {
        if (!validPlate(
                index
        )) {
            return 0.0F;
        }

        return plates.get(
                index
        ).wear
                / (float) AssemblyItemData.MAX_COMPONENT_WEAR;
    }

    public double plateRadius(
            int index
    ) {
        return validPlate(
                index
        )
                ? plates.get(
                        index
                ).anchorRadius
                : PADDLE_RADIUS;
    }

    public float plateWidth(
            int index
    ) {
        return validPlate(
                index
        )
                ? plates.get(index).width
                : 0.82F;
    }

    public float plateDepth(
            int index
    ) {
        return validPlate(
                index
        )
                ? plates.get(index).depth
                : 0.58F;
    }

    public int adjustSelectedPlate(
            boolean selectNext
    ) {
        if (plates.isEmpty()) {
            return -1;
        }

        int index =
                selectNext
                        ? Math.min(
                                plates.size() - 1,
                                1
                        )
                        : 0;

        rotatePlate(
                index,
                PLATE_ROTATION_STEP
        );

        return index;
    }

    public boolean doubleBody() {
        return getBlockState()
                .getValue(
                        WaterWheelHubBlock.DOUBLE
                );
    }

    private boolean validPlate(
            int index
    ) {
        return index >= 0
                && index < plates.size();
    }

    public Component inspectFrame() {
        float wear =
                frameWear
                / (float) AssemblyItemData.MAX_COMPONENT_WEAR;

        Component wearText =
                Component.translatable(
                        wear < 0.25F
                                ? "message.wayaround.assembly.wear_good"
                                : wear < 0.55F
                                        ? "message.wayaround.assembly.wear_used"
                                        : wear < 0.82F
                                                ? "message.wayaround.assembly.wear_worn"
                                                : "message.wayaround.assembly.wear_critical"
                );

        Component state =
                Component.translatable(
                        failureCountdown >= 0
                                ? "message.wayaround.assembly.failure_imminent"
                                : "message.wayaround.assembly.structure_stable"
                );

        return Component.translatable(
                "message.wayaround.assembly.inspect_frame",
                wearText,
                state
        );
    }

    public Component inspectPlate(
            int index
    ) {
        if (!validPlate(
                index
        )) {
            return Component.translatable(
                    "message.wayaround.assembly.inspect_missing"
            );
        }

        Plate plate =
                plates.get(
                        index
                );

        float wear =
                plate.wear
                / (float) AssemblyItemData.MAX_COMPONENT_WEAR;

        Component wearText =
                Component.translatable(
                        wear < 0.25F
                                ? "message.wayaround.assembly.wear_good"
                                : wear < 0.55F
                                        ? "message.wayaround.assembly.wear_used"
                                        : wear < 0.82F
                                                ? "message.wayaround.assembly.wear_worn"
                                                : "message.wayaround.assembly.wear_critical"
                );

        Component fixing =
                Component.translatable(
                        plate.nailed
                                ? "message.wayaround.assembly.fixed"
                                : "message.wayaround.assembly.loose"
                );

        Component nail =
                plate.nail.isEmpty()
                        ? Component.translatable(
                                "message.wayaround.assembly.no_nail"
                        )
                        : plate.nail.getHoverName();

        return Component.translatable(
                "message.wayaround.assembly.inspect",
                index + 1,
                fixing,
                wearText,
                nail,
                Math.round(
                        plate.profile.quality()
                        * 100.0F
                ),
                Math.round(
                        plate.profile.alignment()
                        * 100.0F
                ),
                Math.round(
                        plate.profile.fatigue()
                        * 100.0F
                )
        );
    }

    public Component status() {
        return Component.translatable(
                "message.wayaround.water_wheel.status_v2",
                doubleBody()
                        ? 2
                        : 1,
                plates.size(),
                String.format(
                        Locale.ROOT,
                        "%.1f",
                        rpm
                ),
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        torque
                ),
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        mechanicalPower
                )
        );
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(
                tag,
                registries
        );

        writeSyncData(
                tag,
                registries
        );
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(
                tag,
                registries
        );

        rpm =
                tag.getFloat(
                        "Rpm"
                );

        torque =
                tag.getFloat(
                        "Torque"
                );

        efficiency =
                tag.getFloat(
                        "Efficiency"
                );

        mechanicalPower =
                tag.getFloat(
                        "MechanicalPower"
                );

        rotationDegrees =
                tag.getFloat(
                        "Rotation"
                );

        frameWear =
                Mth.clamp(
                        tag.getInt(
                                "FrameWear"
                        ),
                        0,
                        AssemblyItemData.MAX_COMPONENT_WEAR
                );

        wetContacts =
                Math.max(
                        0,
                        tag.getInt(
                                "WetContacts"
                        )
                );

        unstableFlow =
                tag.getBoolean(
                        "UnstableFlow"
                );

        jammed =
                tag.getBoolean(
                        "Jammed"
                );

        failureCountdown =
                tag.contains(
                        "FailureCountdown"
                )
                        ? tag.getInt(
                                "FailureCountdown"
                        )
                        : -1;

        burnTicks =
                Math.max(
                        0,
                        tag.getInt(
                                "BurnTicks"
                        )
                );

        burnIntensity =
                Mth.clamp(
                        tag.getFloat(
                                "BurnIntensity"
                        ),
                        0.0F,
                        1.0F
                );

        plates.clear();

        ListTag plateList =
                tag.getList(
                        "AssemblyPlates",
                        Tag.TAG_COMPOUND
                );

        for (int i = 0;
                i < plateList.size()
                && plates.size() < MAX_PLATES;
                i++) {

            CompoundTag plateTag =
                    plateList.getCompound(
                            i
                    );

            Plate plate =
                    new Plate();

            plate.anchorAngle =
                    normalizeAngle(
                            plateTag.getDouble(
                                    "AnchorAngle"
                            )
                    );

            plate.anchorRadius =
                    plateTag.contains(
                            "AnchorRadius"
                    )
                            ? Mth.clamp(
                                    plateTag.getDouble(
                                            "AnchorRadius"
                                    ),
                                    0.35,
                                    3.2
                            )
                            : Math.max(
                                    0.35,
                                    hexRadiusAt(
                                            plate.anchorAngle
                                    )
                                    - 0.08
                            );

            plate.tiltDegrees =
                    Mth.clamp(
                            plateTag.getFloat(
                                    "Tilt"
                            ),
                            -85.0F,
                            85.0F
                    );

            plate.wear =
                    Mth.clamp(
                            plateTag.getInt(
                                    "Wear"
                            ),
                            0,
                            AssemblyItemData.MAX_COMPONENT_WEAR
                    );

            if (plateTag.contains(
                    "PartProfile",
                    Tag.TAG_COMPOUND
            )) {
                plate.profile =
                        AssemblyPartProfile.load(
                                plateTag.getCompound(
                                        "PartProfile"
                                )
                        );
            } else {
                plate.profile =
                        AssemblyPartProfile.legacy(
                                AssemblyPartProfile.Kind.BOARD,
                                AssemblyPartProfile.Material.WOOD,
                                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                                        PowerContent.WATER_WHEEL_BLADE_ITEM.get()
                                ),
                                (int) Math.round(
                                        Math.toDegrees(
                                                plate.anchorAngle
                                        )
                                ),
                                plate.wear
                                / (float) AssemblyItemData.MAX_COMPONENT_WEAR
                        );
            }

            plate.profile.setWearFraction(
                    plate.wear
                    / (float) AssemblyItemData.MAX_COMPONENT_WEAR
            );

            plate.width =
                    Mth.clamp(
                            plateTag.getFloat(
                                    "Width"
                            ),
                            0.25F,
                            1.8F
                    );

            plate.depth =
                    Mth.clamp(
                            plateTag.getFloat(
                                    "Depth"
                            ),
                            0.16F,
                            1.8F
                    );

            plate.nailed =
                    plateTag.getBoolean(
                            "Nailed"
                    );

            plate.nailWear =
                    Math.max(
                            0,
                            plateTag.getInt(
                                    "NailWear"
                            )
                    );

            if (plateTag.contains(
                    "Nail",
                    Tag.TAG_COMPOUND
            )) {
                plate.nail =
                        ItemStack.parseOptional(
                                registries,
                                plateTag.getCompound(
                                        "Nail"
                                )
                        );
            }

            if (plate.nail.isEmpty()) {
                plate.nailed =
                        false;
            }

            plates.add(
                    plate
            );
        }
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        CompoundTag tag =
                super.getUpdateTag(
                        registries
                );

        writeSyncData(
                tag,
                registries
        );

        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener>
            getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(
                this
        );
    }

    private void writeSyncData(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        tag.putFloat(
                "Rpm",
                rpm
        );

        tag.putFloat(
                "Torque",
                torque
        );

        tag.putFloat(
                "Efficiency",
                efficiency
        );

        tag.putFloat(
                "MechanicalPower",
                mechanicalPower
        );

        tag.putFloat(
                "Rotation",
                rotationDegrees
        );

        tag.putInt(
                "FrameWear",
                frameWear
        );

        tag.putInt(
                "WetContacts",
                wetContacts
        );

        tag.putBoolean(
                "UnstableFlow",
                unstableFlow
        );

        tag.putBoolean(
                "Jammed",
                jammed
        );

        tag.putInt(
                "FailureCountdown",
                failureCountdown
        );

        tag.putInt(
                "BurnTicks",
                burnTicks
        );

        tag.putFloat(
                "BurnIntensity",
                burnIntensity
        );

        ListTag plateList =
                new ListTag();

        for (Plate plate :
                plates) {

            CompoundTag plateTag =
                    new CompoundTag();

            plateTag.putDouble(
                    "AnchorAngle",
                    plate.anchorAngle
            );

            plateTag.putDouble(
                    "AnchorRadius",
                    plate.anchorRadius
            );

            plateTag.putFloat(
                    "Tilt",
                    plate.tiltDegrees
            );

            plateTag.putInt(
                    "Wear",
                    plate.wear
            );

            plate.profile.setWearFraction(
                    plate.wear
                    / (float) AssemblyItemData.MAX_COMPONENT_WEAR
            );

            plateTag.put(
                    "PartProfile",
                    plate.profile.save()
            );

            plateTag.putFloat(
                    "Width",
                    plate.width
            );

            plateTag.putFloat(
                    "Depth",
                    plate.depth
            );

            plateTag.putBoolean(
                    "Nailed",
                    plate.nailed
            );

            plateTag.putInt(
                    "NailWear",
                    plate.nailWear
            );

            if (!plate.nail.isEmpty()) {
                plateTag.put(
                        "Nail",
                        plate.nail.saveOptional(
                                registries
                        )
                );
            }

            plateList.add(
                    plateTag
            );
        }

        tag.put(
                "AssemblyPlates",
                plateList
        );
    }

    private static double angleDistance(
            double a,
            double b
    ) {
        double difference =
                normalizeAngle(
                        a - b
                );

        if (difference > Math.PI) {
            difference =
                    Math.PI * 2.0
                    - difference;
        }

        return Math.abs(
                difference
        );
    }

    private static double normalizeAngle(
            double angle
    ) {
        double full =
                Math.PI
                * 2.0;

        angle %=
                full;

        if (angle < 0.0) {
            angle +=
                    full;
        }

        return angle;
    }

    private static float wrapDegrees(
            float degrees
    ) {
        degrees %=
                360.0F;

        if (degrees < 0.0F) {
            degrees +=
                    360.0F;
        }

        return degrees;
    }

    private static final class Plate {

        private double anchorAngle;

        private double anchorRadius =
                PADDLE_RADIUS;

        private float tiltDegrees;

        private float looseSwingDegrees;

        private float looseSwingVelocity;

        private int wear;

        private AssemblyPartProfile profile =
                AssemblyPartProfile.legacy(
                        AssemblyPartProfile.Kind.BOARD,
                        AssemblyPartProfile.Material.WOOD,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                "wayaround",
                                "water_wheel_blade"
                        ),
                        0,
                        0.0F
                );

        private boolean nailed;

        private ItemStack nail =
                ItemStack.EMPTY;

        private int nailWear;

        private float width =
                0.82F;

        private float depth =
                0.58F;

        private boolean wet;

        private float lastWaterTorque;

        private float effectiveTilt() {
            return Mth.clamp(
                    tiltDegrees
                    + looseSwingDegrees,
                    -88.0F,
                    88.0F
            );
        }

        private double mass() {
            double wearMass =
                    1.0
                    - wear
                    / (double) AssemblyItemData.MAX_COMPONENT_WEAR
                    * 0.06;

            return Math.max(
                    0.35,
                    width
                    * depth
                    * 2.15
                    * wearMass
                    * profile.massFactor()
            );
        }
    }
}
