package net.caravidro.wayaround.industrial.power;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.assembly.AssemblyAdvancements;
import net.caravidro.wayaround.industrial.assembly.AssemblyItemData;
import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.worldgen.water.WaterDynamics;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class WaterWheelHubBlockEntity
        extends BlockEntity {

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

        hub.simulate(
                server,
                state
        );

        if (Math.floorMod(
                time + pos.asLong(),
                8
        ) == 0) {
            hub.emitWaterFeedback(
                    server
            );
        }

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
                )) {
            hub.sync();
        }
    }

    private void simulate(
            ServerLevel level,
            BlockState state
    ) {
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
                            PADDLE_RADIUS
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

            if (!plate.nailed) {
                continue;
            }

            nailedCount++;

            /*
             * Every fixed board has real mass. An asymmetric build therefore
             * rotates under gravity until its center of mass hangs below the
             * axle.
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
                            plate.tiltDegrees
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
            if (plate.nailed) {
                inertia +=
                        plate.mass()
                        * PADDLE_RADIUS
                        * PADDLE_RADIUS;
            }
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

            /*
             * No clipping through terrain. The first board/frame contact
             * arrests the assembly. If later torque reverses, it can move away
             * from the obstacle naturally.
             */
            rpm =
                    0.0F;

            mechanicalPower =
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

            Vec3 center =
                    Vec3.atCenterOf(
                            worldPosition
                    ).add(
                            radial.scale(
                                    PADDLE_RADIUS
                            )
                    );

            double halfWidth =
                    Math.max(
                            0.22,
                            plate.width * 0.52
                    );

            Vec3[] checks =
                    new Vec3[] {
                            center,
                            center.add(
                                    tangent.scale(
                                            halfWidth
                                    )
                            ),
                            center.subtract(
                                    tangent.scale(
                                            halfWidth
                                    )
                            )
                    };

            for (Vec3 check :
                    checks) {
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

        /*
         * The frame itself is physical too. Twelve samples are enough for the
         * rotating hexagonal rim/spokes without turning this into voxel-CAD.
         */
        for (int sample = 0;
                sample < 12;
                sample++) {

            double angle =
                    rotation
                    + sample
                    * Math.PI
                    * 2.0
                    / 12.0;

            Vec3 check =
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

            if (solidAssemblyObstacle(
                    level,
                    BlockPos.containing(
                            check
                    )
            )) {
                return true;
            }
        }

        return false;
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

        plate.tiltDegrees =
                0.0F;

        plate.wear =
                AssemblyItemData.wear(
                        source
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

        AssemblyAdvancements.waterWheel(
                player
        );

        configurationChanged();

        return true;
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

        ItemStack returned =
                AssemblyItemData.withWear(
                        new ItemStack(
                                PowerContent.WATER_WHEEL_BLADE_ITEM.get()
                        ),
                        plate.wear
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
        if (radius < 1.72
                || radius > 2.92) {
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
                            / PADDLE_RADIUS
                            * 0.62
                    );

            if (distance <= allowance
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
            double radius
    ) {
        return Math.abs(
                radius
                - FRAME_RADIUS
        ) <= 0.42;
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
                                    PADDLE_RADIUS
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
                                    PADDLE_RADIUS
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
        ).tiltDegrees;
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

            plateTag.putFloat(
                    "Tilt",
                    plate.tiltDegrees
            );

            plateTag.putInt(
                    "Wear",
                    plate.wear
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

        private float tiltDegrees;

        private int wear;

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
            );
        }
    }
}
