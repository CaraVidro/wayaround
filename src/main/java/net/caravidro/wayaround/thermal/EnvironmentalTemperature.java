package net.caravidro.wayaround.thermal;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import net.caravidro.wayaround.physical.PhysicalRegionSnapshot;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Shared environmental temperature API.
 *
 * <p>Systems should ask this service for temperature instead of maintaining
 * parallel ideas of "hot" and "cold". RegionalTemperature owns sparse local
 * thermal disturbances while ambient modifiers describe geography/weather.</p>
 */
public final class EnvironmentalTemperature {

    public interface AmbientModifier {
        double modify(
                ServerLevel level,
                BlockPos pos,
                double currentCelsius
        );
    }

    private static final List<AmbientModifier> AMBIENT_MODIFIERS =
            new CopyOnWriteArrayList<>();

    private EnvironmentalTemperature() {
    }

    public static void registerAmbientModifier(
            AmbientModifier modifier
    ) {
        AMBIENT_MODIFIERS.add(
                Objects.requireNonNull(
                        modifier
                )
        );
    }

    public static double ambientAt(
            ServerLevel level,
            BlockPos pos
    ) {
        double temperature =
                TemperatureCurve.AMBIENT;

        for (AmbientModifier modifier :
                AMBIENT_MODIFIERS) {
            temperature =
                    TemperatureCurve.clamp(
                            modifier.modify(
                                    level,
                                    pos,
                                    temperature
                            )
                    );
        }

        return temperature;
    }

    public static double at(
            ServerLevel level,
            BlockPos pos
    ) {
        double ambient =
                ambientAt(
                        level,
                        pos
                );

        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.THERMAL_SYSTEM
        )) {
            return ambient;
        }

        return RegionalTemperature.resolve(
                level,
                pos,
                ambient
        );
    }

    public static void pulseAbsolute(
            ServerLevel level,
            Vec3 center,
            double radius,
            double targetCelsius
    ) {
        RegionalTemperature.pulseAbsolute(
                level,
                center,
                radius,
                targetCelsius
        );
    }

    public static void pulseDelta(
            ServerLevel level,
            Vec3 center,
            double radius,
            double deltaCelsius
    ) {
        RegionalTemperature.pulseDelta(
                level,
                center,
                radius,
                deltaCelsius
        );
    }

    /**
     * Canonical energy-input API for systems that know their physical region.
     */
    public static void injectEnergy(
            ServerLevel level,
            PhysicalRegionSnapshot region,
            double energyJ
    ) {
        UniversalTemperature.injectEnergy(
                level,
                region,
                energyJ
        );
    }

    /**
     * Vanilla/mod adapter entry point for real heat-producing blocks.
     */
    public static boolean applyBlockSource(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        return UniversalTemperature.applyBlockSource(
                level,
                pos,
                state
        );
    }
}
