package net.caravidro.wayaround.industrial.power;

import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.industrial.steam.SteamReceiver;
import net.caravidro.wayaround.industrial.steam.SteamThermodynamics;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SteamEngineBlockEntity
        extends BlockEntity
        implements SteamReceiver, IRotationalPower {

    public static final int STEAM_CAPACITY = 4_000;

    private int steam;
    private float pressureBar;
    private int temperatureC = 20;

    private float rpm;
    private float torque;
    private float availablePower;
    private float accumulatedDraw;

    public SteamEngineBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.STEAM_ENGINE_ENTITY.get(),
                pos,
                state
        );
    }

    @Override
    public int receiveSteam(
            int amount,
            float incomingPressureBar,
            int incomingTemperatureC
    ) {
        if (amount <= 0
                || incomingPressureBar <= 0.05F
                || incomingTemperatureC < 95) {
            return 0;
        }

        int accepted =
                Math.min(
                        amount,
                        STEAM_CAPACITY - steam
                );

        if (accepted <= 0) {
            return 0;
        }

        int oldSteam =
                steam;

        int combined =
                oldSteam + accepted;

        pressureBar =
                combined <= 0
                        ? incomingPressureBar
                        : (
                        pressureBar * oldSteam
                                + incomingPressureBar * accepted
                )
                                / combined;

        temperatureC =
                combined <= 0
                        ? incomingTemperatureC
                        : Math.round(
                        (
                                temperatureC * oldSteam
                                        + incomingTemperatureC * accepted
                        )
                                / (float) combined
                );

        steam =
                combined;

        setChanged();
        return accepted;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            SteamEngineBlockEntity engine
    ) {
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.POWER_NETWORKS
        )
                || !(level instanceof ServerLevel server)) {
            return;
        }

        float previousDraw =
                engine.accumulatedDraw;

        engine.accumulatedDraw =
                0.0F;

        float targetRpm =
                SteamThermodynamics.engineRpm(
                        engine.pressureBar,
                        engine.steam
                );

        float targetTorque =
                SteamThermodynamics.engineTorque(
                        engine.pressureBar,
                        engine.temperatureC,
                        engine.steam
                );

        engine.rpm +=
                (
                        targetRpm
                                - engine.rpm
                )
                        * (
                        targetRpm > engine.rpm
                                ? 0.13F
                                : 0.075F
                );

        engine.torque +=
                (
                        targetTorque
                                - engine.torque
                )
                        * 0.16F;

        if (Math.abs(
                engine.rpm
        ) < 0.02F) {
            engine.rpm = 0.0F;
        }

        if (engine.torque < 0.01F) {
            engine.torque = 0.0F;
        }

        engine.availablePower =
                SteamThermodynamics.mechanicalPower(
                        engine.rpm,
                        engine.torque
                );

        if (engine.steam > 0
                && engine.rpm > 0.1F) {

            int consumption =
                    Math.min(
                            engine.steam,
                            1
                                    + Math.max(
                                    0,
                                    Math.round(
                                            previousDraw
                                                    * 0.75F
                                    )
                            )
                    );

            engine.steam -=
                    consumption;

            float reserve =
                    Mth.clamp(
                            engine.steam
                                    / (float) STEAM_CAPACITY,
                            0.0F,
                            1.0F
                    );

            engine.pressureBar =
                    Math.max(
                            0.0F,
                            engine.pressureBar
                                    * (
                                    0.996F
                                            - (
                                            1.0F - reserve
                                    )
                                            * 0.002F
                            )
                    );

            engine.temperatureC =
                    Math.max(
                            90,
                            engine.temperatureC
                                    - (
                                    engine.steam <= 0
                                            ? 4
                                            : 1
                            )
                    );
        } else {
            engine.pressureBar =
                    Math.max(
                            0.0F,
                            engine.pressureBar
                                    - 0.008F
                    );

            engine.temperatureC =
                    Math.max(
                            20,
                            engine.temperatureC - 1
                    );
        }

        boolean running =
                engine.active();

        if (state.getValue(
                SteamEngineBlock.LIT
        ) != running) {
            level.setBlock(
                    pos,
                    state.setValue(
                            SteamEngineBlock.LIT,
                            running
                    ),
                    3
            );
        }

        if (running
                && Math.floorMod(
                level.getGameTime()
                        + pos.asLong(),
                18
        ) == 0) {

            server.sendParticles(
                    ParticleTypes.CLOUD,
                    pos.getX() + 0.5,
                    pos.getY() + 0.9,
                    pos.getZ() + 0.5,
                    1,
                    0.10,
                    0.04,
                    0.10,
                    0.018
            );

            server.playSound(
                    null,
                    pos,
                    SoundEvents.PISTON_EXTEND,
                    SoundSource.BLOCKS,
                    0.16F,
                    Mth.clamp(
                            0.58F
                                    + Math.abs(
                                    engine.rpm
                            )
                                    / 170.0F,
                            0.58F,
                            1.05F
                    )
            );
        }

        if (Math.floorMod(
                level.getGameTime()
                        + pos.asLong(),
                4
        ) == 0) {
            engine.sync();
        }
    }

    public IRotationalPower rotationOutput(
            Direction side
    ) {
        return side != null
                && side.getAxis()
                        == Direction.Axis.X
                ? this
                : null;
    }

    @Override
    public float rpm() {
        return rpm;
    }

    @Override
    public float torque() {
        return torque;
    }

    @Override
    public float power() {
        return availablePower;
    }

    @Override
    public Direction.Axis axis() {
        return Direction.Axis.X;
    }

    @Override
    public int rotationDirection() {
        return rpm > 0.01F
                ? 1
                : 0;
    }

    @Override
    public float consumePower(
            float requestedPower
    ) {
        float accepted =
                Math.min(
                        Math.max(
                                0.0F,
                                requestedPower
                        ),
                        Math.max(
                                0.0F,
                                availablePower
                        )
                );

        availablePower -=
                accepted;

        accumulatedDraw +=
                accepted;

        return accepted;
    }

    public boolean boiling() {
        return active();
    }

    /**
     * Renderer compatibility: the old engine showed boiler water in this
     * gauge. It now shows the amount of steam available to the cylinder.
     */
    public float waterFill() {
        return Mth.clamp(
                steam
                        / (float) STEAM_CAPACITY,
                0.0F,
                1.0F
        );
    }

    public Component status() {
        Component steamState =
                Component.translatable(
                        steam <= 0
                                ? "message.wayaround.steam_engine.steam_empty"
                                : steam < STEAM_CAPACITY * 0.25F
                                        ? "message.wayaround.steam_engine.steam_low"
                                        : "message.wayaround.steam_engine.steam_ready"
                );

        Component motionState =
                Component.translatable(
                        rpm < 1.0F
                                ? "message.wayaround.steam_engine.motion_stopped"
                                : rpm < 35.0F
                                        ? "message.wayaround.steam_engine.motion_slow"
                                        : rpm < 85.0F
                                                ? "message.wayaround.steam_engine.motion_working"
                                                : "message.wayaround.steam_engine.motion_fast"
                );

        return Component.translatable(
                "message.wayaround.steam_engine.status_v3",
                steamState,
                motionState
        );
    }

    public Component measurement() {
        return Component.translatable(
                "message.wayaround.steam_engine.measurement",
                steam,
                STEAM_CAPACITY,
                String.format(
                        java.util.Locale.ROOT,
                        "%.2f",
                        pressureBar
                ),
                temperatureC,
                String.format(
                        java.util.Locale.ROOT,
                        "%.2f",
                        rpm
                ),
                String.format(
                        java.util.Locale.ROOT,
                        "%.3f",
                        torque
                ),
                String.format(
                        java.util.Locale.ROOT,
                        "%.3f",
                        availablePower
                )
        );
    }

    private void sync() {
        setChanged();

        if (level != null) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    3
            );
        }
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        return saveWithoutMetadata(
                registries
        );
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(
                this
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

        tag.putInt("Steam", steam);
        tag.putFloat("PressureBar", pressureBar);
        tag.putInt("TemperatureC", temperatureC);
        tag.putFloat("Rpm", rpm);
        tag.putFloat("Torque", torque);
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

        if (tag.contains(
                "Steam"
        )) {
            steam =
                    Math.clamp(
                            tag.getInt(
                                    "Steam"
                            ),
                            0,
                            STEAM_CAPACITY
                    );

            pressureBar =
                    Math.max(
                            0.0F,
                            tag.getFloat(
                                    "PressureBar"
                            )
                    );

            temperatureC =
                    Math.max(
                            20,
                            tag.getInt(
                                    "TemperatureC"
                            )
                    );

        } else {
            /*
             * Legacy migration: old combined steam engines stored boiler
             * water/heat directly. Preserve some thermal state, but never
             * recreate the old free-FE buffer.
             */
            steam =
                    Math.clamp(
                            tag.getInt(
                                    "Water"
                            ) / 2,
                            0,
                            STEAM_CAPACITY
                    );

            int legacyHeat =
                    Math.clamp(
                            tag.getInt(
                                    "Heat"
                            ),
                            0,
                            100
                    );

            pressureBar =
                    SteamThermodynamics.pressureBar(
                            steam,
                            legacyHeat
                    );

            temperatureC =
                    SteamThermodynamics.temperatureC(
                            legacyHeat,
                            steam
                    );
        }

        rpm =
                Math.max(
                        0.0F,
                        tag.getFloat(
                                "Rpm"
                        )
                );

        torque =
                Math.max(
                        0.0F,
                        tag.getFloat(
                                "Torque"
                        )
                );

        availablePower =
                SteamThermodynamics.mechanicalPower(
                        rpm,
                        torque
                );
    }
}
