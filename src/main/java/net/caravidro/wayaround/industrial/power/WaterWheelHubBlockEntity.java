package net.caravidro.wayaround.industrial.power;

import java.util.Locale;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class WaterWheelHubBlockEntity
        extends BlockEntity {

    public static final int MAX_PLATES =
            16;

    public static final float PLATE_TILT_STEP =
            15.0F;

    public static final double WHEEL_RADIUS =
            2.65;

    private float rpm;
    private float torque;
    private float efficiency;
    private float mechanicalPower;
    private float rotationDegrees;

    private int plateCount;
    private int wetContacts;

    private final byte[] plateTilt =
            new byte[MAX_PLATES];

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

        hub.rotationDegrees =
                wrapDegrees(
                        hub.rotationDegrees
                        + hub.rpm
                        * 0.30F
                );

        if (Math.floorMod(
                level.getGameTime()
                + pos.asLong(),
                4
        ) != 0) {
            return;
        }

        float oldRpm =
                hub.rpm;

        float oldEfficiency =
                hub.efficiency;

        float oldTorque =
                hub.torque;

        int oldWet =
                hub.wetContacts;

        hub.sampleWheel(
                server,
                state
        );

        if (Math.abs(
                oldRpm - hub.rpm
        ) > 0.03F
                || Math.abs(
                        oldEfficiency - hub.efficiency
                ) > 0.01F
                || Math.abs(
                        oldTorque - hub.torque
                ) > 0.02F
                || oldWet != hub.wetContacts) {

            hub.sync();
        }
    }

    private void sampleWheel(
            ServerLevel level,
            BlockState state
    ) {
        if (plateCount <= 0) {
            rpm =
                    Mth.lerp(
                            0.20F,
                            rpm,
                            0.0F
                    );

            torque =
                    0.0F;

            efficiency =
                    0.0F;

            mechanicalPower =
                    0.0F;

            wetContacts =
                    0;

            return;
        }

        Direction.Axis axis =
                axleAxis();

        boolean doubleBody =
                state.getValue(
                        WaterWheelHubBlock.DOUBLE
                );

        double signedTorque =
                0.0;

        double alignmentTotal =
                0.0;

        double speedTotal =
                0.0;

        int contacts =
                0;

        int wet =
                0;

        double rotation =
                Math.toRadians(
                        rotationDegrees
                );

        for (int i = 0;
                i < plateCount;
                i++) {

            double baseAngle =
                    plateBaseAngle(
                            i,
                            doubleBody
                    );

            if (doubleBody) {
                ContactResult result =
                        sampleContact(
                                level,
                                axis,
                                baseAngle + rotation,
                                plateTiltDegrees(i),
                                1.35
                        );

                signedTorque +=
                        result.torque();

                alignmentTotal +=
                        result.alignment();

                speedTotal +=
                        result.speed();

                contacts++;

                if (result.wet()) {
                    wet++;
                }
            } else {
                /*
                 * A single body uses each plate as a complete diameter:
                 * one plate looks like a line through the center, two become
                 * an X, and more lines progressively fill the wheel.
                 */
                for (int side = 0;
                        side < 2;
                        side++) {

                    ContactResult result =
                            sampleContact(
                                    level,
                                    axis,
                                    baseAngle
                                    + rotation
                                    + side * Math.PI,
                                    plateTiltDegrees(i),
                                    0.72
                            );

                    signedTorque +=
                            result.torque();

                    alignmentTotal +=
                            result.alignment();

                    speedTotal +=
                            result.speed();

                    contacts++;

                    if (result.wet()) {
                        wet++;
                    }
                }
            }
        }

        wetContacts =
                wet;

        if (wet == 0) {
            rpm =
                    Mth.lerp(
                            0.16F,
                            rpm,
                            0.0F
                    );

            torque =
                    0.0F;

            efficiency =
                    Mth.lerp(
                            0.25F,
                            efficiency,
                            0.0F
                    );

            mechanicalPower =
                    0.0F;

            return;
        }

        float plateCoverage =
                1.0F
                - (float) Math.exp(
                        -plateCount
                        / (
                                doubleBody
                                        ? 5.0
                                        : 4.0
                        )
                );

        float waterContact =
                wet
                / (float) Math.max(
                        1,
                        contacts
                );

        float alignment =
                (float) Mth.clamp(
                        alignmentTotal
                        / Math.max(
                                1,
                                wet
                        ),
                        0.0,
                        1.0
                );

        float speedFactor =
                (float) Mth.clamp(
                        speedTotal
                        / Math.max(
                                1,
                                wet
                        )
                        * 1.5,
                        0.12,
                        1.0
                );

        float targetEfficiency =
                Mth.clamp(
                        plateCoverage
                        * (
                                0.35F
                                + waterContact
                                * 0.65F
                        )
                        * alignment
                        * speedFactor,
                        0.0F,
                        1.0F
                );

        /*
         * signedTorque already includes current direction. A waterfall can
         * therefore reverse the wheel when it hits the opposite side.
         */
        float targetRpm =
                (float) Mth.clamp(
                        signedTorque
                        * 78.0,
                        -42.0,
                        42.0
                );

        rpm =
                Mth.lerp(
                        0.24F,
                        rpm,
                        targetRpm
                );

        torque =
                Mth.lerp(
                        0.28F,
                        torque,
                        (float) signedTorque
                        * 9.0F
                );

        efficiency =
                Mth.lerp(
                        0.22F,
                        efficiency,
                        targetEfficiency
                );

        mechanicalPower =
                Math.abs(
                        rpm
                        * torque
                )
                * 0.10472F
                * efficiency;
    }

    private ContactResult sampleContact(
            ServerLevel level,
            Direction.Axis axis,
            double angle,
            float tiltDegrees,
            double area
    ) {
        double sin =
                Math.sin(
                        angle
                );

        double cos =
                Math.cos(
                        angle
                );

        Vec3 radial;

        Vec3 tangent;

        if (axis == Direction.Axis.X) {
            radial =
                    new Vec3(
                            0.0,
                            sin,
                            cos
                    );

            tangent =
                    new Vec3(
                            0.0,
                            cos,
                            -sin
                    );
        } else {
            radial =
                    new Vec3(
                            cos,
                            sin,
                            0.0
                    );

            tangent =
                    new Vec3(
                            -sin,
                            cos,
                            0.0
                    );
        }

        Vec3 center =
                Vec3.atCenterOf(
                        worldPosition
                );

        Vec3 contact =
                center.add(
                        radial.scale(
                                WHEEL_RADIUS
                        )
                );

        BlockPos samplePos =
                BlockPos.containing(
                        contact
                );

        Vec3 current =
                WaterDynamics.currentAround(
                        level,
                        samplePos
                );

        double speed =
                current.length();

        boolean wet =
                hasWaterAround(
                        level,
                        samplePos
                );

        if (!wet
                || speed < 0.008) {
            return new ContactResult(
                    0.0,
                    0.0,
                    0.0,
                    false
            );
        }

        Vec3 flow =
                current.scale(
                        1.0 / speed
                );

        double tilt =
                Math.toRadians(
                        tiltDegrees
                );

        /*
         * The plate's effective face can be turned from tangential toward
         * radial. This makes orientation genuinely dependent on the local
         * direction of the water rather than giving every plate one universal
         * best angle.
         */
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

        double alignment =
                Math.abs(
                        flow.dot(
                                normal
                        )
                );

        double tangentialFlow =
                current.dot(
                        tangent
                );

        double torqueContribution =
                tangentialFlow
                * alignment
                * WHEEL_RADIUS
                * area;

        return new ContactResult(
                torqueContribution,
                alignment,
                speed,
                true
        );
    }

    private static boolean hasWaterAround(
            Level level,
            BlockPos pos
    ) {
        if (level.getFluidState(pos)
                .is(net.minecraft.tags.FluidTags.WATER)) {
            return true;
        }

        for (Direction direction :
                Direction.values()) {

            if (level.getFluidState(
                    pos.relative(
                            direction
                    )
            ).is(
                    net.minecraft.tags.FluidTags.WATER
            )) {
                return true;
            }
        }

        return false;
    }

    public boolean addPlate() {
        if (plateCount >= MAX_PLATES) {
            return false;
        }

        plateTilt[plateCount] =
                0;

        plateCount++;

        configurationChanged();

        return true;
    }

    public int rotateNearestPlate(
            Player player,
            int direction
    ) {
        if (plateCount <= 0) {
            return -1;
        }

        boolean doubleBody =
                getBlockState().getValue(
                        WaterWheelHubBlock.DOUBLE
                );

        double playerAngle =
                playerAngle(
                        player
                );

        double wheelRotation =
                Math.toRadians(
                        rotationDegrees
                );

        int nearest =
                0;

        double nearestDistance =
                Double.MAX_VALUE;

        for (int i = 0;
                i < plateCount;
                i++) {

            double angle =
                    plateBaseAngle(
                            i,
                            doubleBody
                    )
                    + wheelRotation;

            double distance =
                    angleDistance(
                            playerAngle,
                            angle
                    );

            if (!doubleBody) {
                distance =
                        Math.min(
                                distance,
                                angleDistance(
                                        playerAngle,
                                        angle
                                        + Math.PI
                                )
                        );
            }

            if (distance < nearestDistance) {
                nearestDistance =
                        distance;

                nearest =
                        i;
            }
        }

        int next =
                plateTilt[nearest]
                + Integer.signum(
                        direction
                );

        if (next > 4) {
            next =
                    -4;
        }

        if (next < -4) {
            next =
                    4;
        }

        plateTilt[nearest] =
                (byte) next;

        configurationChanged();

        return nearest;
    }

    private double playerAngle(
            Player player
    ) {
        Vec3 center =
                Vec3.atCenterOf(
                        worldPosition
                );

        double vertical =
                player.getEyeY()
                - center.y;

        if (axleAxis()
                == Direction.Axis.X) {

            return normalizeAngle(
                    Math.atan2(
                            vertical,
                            player.getZ()
                            - center.z
                    )
            );
        }

        return normalizeAngle(
                Math.atan2(
                        vertical,
                        player.getX()
                        - center.x
                )
        );
    }

    public double plateBaseAngle(
            int index,
            boolean doubleBody
    ) {
        if (plateCount <= 0) {
            return 0.0;
        }

        if (doubleBody) {
            return index
                    * (
                            Math.PI * 2.0
                            / plateCount
                    );
        }

        /*
         * 45 degree offset makes two diameter plates visually form an X.
         */
        return Math.PI * 0.25
                + index
                * (
                        Math.PI
                        / plateCount
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
                Math.PI * 2.0;

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

    public int plateCount() {
        return plateCount;
    }

    public int wetContacts() {
        return wetContacts;
    }

    public float plateTiltDegrees(
            int index
    ) {
        if (index < 0
                || index >= plateCount) {
            return 0.0F;
        }

        return plateTilt[index]
                * PLATE_TILT_STEP;
    }

    public boolean doubleBody() {
        return getBlockState()
                .getValue(
                        WaterWheelHubBlock.DOUBLE
                );
    }

    public Component status() {
        return Component.translatable(
                "message.wayaround.water_wheel.status_v2",
                doubleBody()
                        ? 2
                        : 1,
                plateCount,
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
                Math.round(
                        efficiency
                        * 100.0F
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
                tag
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

        plateCount =
                Mth.clamp(
                        tag.getInt(
                                "PlateCount"
                        ),
                        0,
                        MAX_PLATES
                );

        wetContacts =
                Math.max(
                        0,
                        tag.getInt(
                                "WetContacts"
                        )
                );

        byte[] savedTilt =
                tag.getByteArray(
                        "PlateTilt"
                );

        int length =
                Math.min(
                        savedTilt.length,
                        plateTilt.length
                );

        System.arraycopy(
                savedTilt,
                0,
                plateTilt,
                0,
                length
        );
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
                tag
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
            CompoundTag tag
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
                "PlateCount",
                plateCount
        );

        tag.putInt(
                "WetContacts",
                wetContacts
        );

        tag.putByteArray(
                "PlateTilt",
                plateTilt
        );
    }

    private record ContactResult(
            double torque,
            double alignment,
            double speed,
            boolean wet
    ) {
    }
}
