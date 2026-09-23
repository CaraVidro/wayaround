package net.caravidro.wayaround.industrial.power;

import java.util.Locale;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
    private int selectedPlate;

    private boolean unstableFlow;

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

        long time =
                level.getGameTime();

        /*
         * This is physical state only. The client renderer no longer uses
         * every network update as its visual angle anchor.
         */
        hub.rotationDegrees =
                wrapDegrees(
                        hub.rotationDegrees
                        + hub.rpm
                        * 0.30F
                );

        if (Math.floorMod(
                time + pos.asLong(),
                4
        ) == 0) {

            float oldRpm =
                    hub.rpm;

            float oldEfficiency =
                    hub.efficiency;

            float oldTorque =
                    hub.torque;

            int oldWet =
                    hub.wetContacts;

            boolean oldUnstable =
                    hub.unstableFlow;

            hub.sampleWheel(
                    server,
                    state
            );

            if (Math.abs(
                    oldRpm - hub.rpm
            ) > 0.12F
                    || Math.abs(
                            oldEfficiency - hub.efficiency
                    ) > 0.035F
                    || Math.abs(
                            oldTorque - hub.torque
                    ) > 0.075F
                    || oldWet != hub.wetContacts
                    || oldUnstable != hub.unstableFlow) {

                hub.sync();
            }
        }

        if (Math.floorMod(
                time + pos.asLong(),
                8
        ) == 0) {
            hub.emitWaterFeedback(
                    server,
                    state
            );
        }

        if (Math.floorMod(
                time + pos.asLong(),
                27
        ) == 0) {
            hub.emitWoodCreak(
                    server
            );
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

            unstableFlow =
                    false;

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

        int directed =
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

                contacts++;

                if (result.wet()) {
                    wet++;
                }

                if (result.directed()) {
                    directed++;
                    signedTorque +=
                            result.torque();
                    alignmentTotal +=
                            result.alignment();
                    speedTotal +=
                            result.speed();
                }
            } else {
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

                    contacts++;

                    if (result.wet()) {
                        wet++;
                    }

                    if (result.directed()) {
                        directed++;
                        signedTorque +=
                                result.torque();
                        alignmentTotal +=
                                result.alignment();
                        speedTotal +=
                                result.speed();
                    }
                }
            }
        }

        wetContacts =
                wet;

        if (wet == 0) {
            unstableFlow =
                    false;

            rpm =
                    Mth.lerp(
                            0.18F,
                            rpm,
                            0.0F
                    );

            torque =
                    0.0F;

            efficiency =
                    Mth.lerp(
                            0.24F,
                            efficiency,
                            0.0F
                    );

            mechanicalPower =
                    0.0F;

            return;
        }

        /*
         * Water is physically touching the wheel, but none of it forms a
         * coherent directional stream. This is a lake / confused eddy case.
         * It may wobble visually, but it produces no mechanical output.
         */
        if (directed == 0) {
            unstableFlow =
                    true;

            rpm =
                    Mth.lerp(
                            0.34F,
                            rpm,
                            0.0F
                    );

            torque =
                    0.0F;

            efficiency =
                    0.0F;

            mechanicalPower =
                    0.0F;

            return;
        }

        unstableFlow =
                false;

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
                directed
                / (float) Math.max(
                        1,
                        contacts
                );

        float alignment =
                (float) Mth.clamp(
                        alignmentTotal
                        / Math.max(
                                1,
                                directed
                        ),
                        0.0,
                        1.0
                );

        float speedFactor =
                (float) Mth.clamp(
                        speedTotal
                        / Math.max(
                                1,
                                directed
                        )
                        * 1.55,
                        0.10,
                        1.0
                );

        float targetEfficiency =
                Mth.clamp(
                        plateCoverage
                        * (
                                0.28F
                                + waterContact
                                * 0.72F
                        )
                        * alignment
                        * speedFactor,
                        0.0F,
                        1.0F
                );

        float targetRpm =
                (float) Mth.clamp(
                        signedTorque
                        * 78.0,
                        -42.0,
                        42.0
                );

        rpm =
                Mth.lerp(
                        0.20F,
                        rpm,
                        targetRpm
                );

        torque =
                Mth.lerp(
                        0.24F,
                        torque,
                        (float) signedTorque
                        * 9.0F
                );

        efficiency =
                Mth.lerp(
                        0.20F,
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

        Vec3 contact =
                Vec3.atCenterOf(
                        worldPosition
                ).add(
                        radial.scale(
                                WHEEL_RADIUS
                        )
                );

        BlockPos samplePos =
                BlockPos.containing(
                        contact
                );

        boolean wet =
                hasWaterAround(
                        level,
                        samplePos
                );

        if (!wet) {
            return new ContactResult(
                    0.0,
                    0.0,
                    0.0,
                    false,
                    false
            );
        }

        WaterDynamics.MechanicalFlow mechanical =
                WaterDynamics.mechanicalFlow(
                        level,
                        samplePos
                );

        Vec3 current =
                mechanical.vector();

        double speed =
                current.length();

        if (!mechanical.stable()
                || speed < 0.012) {

            return new ContactResult(
                    0.0,
                    0.0,
                    0.0,
                    true,
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
                * area
                * mechanical.coherence();

        return new ContactResult(
                torqueContribution,
                alignment,
                speed,
                true,
                true
        );
    }

    private void emitWaterFeedback(
            ServerLevel level,
            BlockState state
    ) {
        if (plateCount <= 0
                || wetContacts <= 0) {
            return;
        }

        boolean doubleBody =
                state.getValue(
                        WaterWheelHubBlock.DOUBLE
                );

        Direction.Axis axis =
                axleAxis();

        double rotation =
                Math.toRadians(
                        rotationDegrees
                );

        int emitted =
                0;

        for (int i = 0;
                i < plateCount
                && emitted < 7;
                i++) {

            int sides =
                    doubleBody
                            ? 1
                            : 2;

            for (int side = 0;
                    side < sides
                    && emitted < 7;
                    side++) {

                double angle =
                        plateBaseAngle(
                                i,
                                doubleBody
                        )
                        + rotation
                        + (
                                doubleBody
                                        ? 0.0
                                        : side * Math.PI
                        );

                Vec3 contact =
                        contactPosition(
                                axis,
                                angle
                        );

                BlockPos samplePos =
                        BlockPos.containing(
                                contact
                        );

                if (!hasWaterAround(
                        level,
                        samplePos
                )) {
                    continue;
                }

                WaterDynamics.MechanicalFlow flow =
                        WaterDynamics.mechanicalFlow(
                                level,
                                samplePos
                        );

                if (!flow.stable()) {
                    continue;
                }

                Vec3 velocity =
                        flow.vector();

                double speed =
                        velocity.length();

                int count =
                        speed > 0.18
                                ? 2
                                : 1;

                level.sendParticles(
                        ParticleTypes.SPLASH,
                        contact.x,
                        contact.y,
                        contact.z,
                        count,
                        0.10,
                        0.08,
                        0.10,
                        0.025
                );

                emitted++;
            }
        }

        if (emitted > 0
                && Math.floorMod(
                        level.getGameTime()
                                + worldPosition.asLong(),
                        16
                ) == 0) {

            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.GENERIC_SPLASH,
                    SoundSource.BLOCKS,
                    0.20F
                            + Math.min(
                                    0.28F,
                                    Math.abs(rpm)
                                    / 90.0F
                            ),
                    0.88F
                            + level.random.nextFloat()
                            * 0.16F
            );
        }
    }

    private void emitWoodCreak(
            ServerLevel level
    ) {
        float motion =
                Math.abs(
                        rpm
                );

        if (motion < 0.25F
                && !unstableFlow) {
            return;
        }

        float volume =
                unstableFlow
                        ? 0.12F
                        : Mth.clamp(
                                0.10F
                                + motion / 85.0F,
                                0.10F,
                                0.34F
                        );

        float pitch =
                unstableFlow
                        ? 0.55F
                        + level.random.nextFloat()
                        * 0.08F
                        : 0.54F
                        + level.random.nextFloat()
                        * 0.16F;

        level.playSound(
                null,
                worldPosition,
                SoundEvents.WOODEN_DOOR_OPEN,
                SoundSource.BLOCKS,
                volume,
                pitch
        );
    }

    private Vec3 contactPosition(
            Direction.Axis axis,
            double angle
    ) {
        return Vec3.atCenterOf(
                worldPosition
        ).add(
                radialVector(
                        axis,
                        angle
                ).scale(
                        WHEEL_RADIUS
                )
        );
    }

    private static Vec3 radialVector(
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

    private static Vec3 tangentVector(
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

        if (plateCount == 1) {
            selectedPlate =
                    0;
        }

        configurationChanged();

        return true;
    }

    public int adjustSelectedPlate(
            boolean selectNext
    ) {
        if (plateCount <= 0) {
            return -1;
        }

        selectedPlate =
                Math.floorMod(
                        selectedPlate,
                        plateCount
                );

        if (selectNext) {
            selectedPlate =
                    (
                            selectedPlate
                            + 1
                    )
                    % plateCount;
        }

        int next =
                plateTilt[selectedPlate]
                + 1;

        if (next > 4) {
            next =
                    -4;
        }

        plateTilt[selectedPlate] =
                (byte) next;

        configurationChanged();

        return selectedPlate;
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

        return Math.PI * 0.25
                + index
                * (
                        Math.PI
                        / plateCount
                );
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

    public boolean unstableFlow() {
        return unstableFlow;
    }

    public int selectedPlate() {
        if (plateCount <= 0) {
            return -1;
        }

        return Math.floorMod(
                selectedPlate,
                plateCount
        );
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

        selectedPlate =
                plateCount <= 0
                        ? 0
                        : Math.floorMod(
                                tag.getInt(
                                        "SelectedPlate"
                                ),
                                plateCount
                        );

        unstableFlow =
                tag.getBoolean(
                        "UnstableFlow"
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

        tag.putInt(
                "SelectedPlate",
                selectedPlate
        );

        tag.putBoolean(
                "UnstableFlow",
                unstableFlow
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
            boolean wet,
            boolean directed
    ) {
    }
}
