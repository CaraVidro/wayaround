package net.caravidro.wayaround.flow;

import net.caravidro.wayaround.physical.MaterialDefinition;
import net.caravidro.wayaround.physical.MatterPhase;
import net.caravidro.wayaround.physical.PhysicalMaterials;
import net.caravidro.wayaround.physical.PhysicalOpening;
import net.caravidro.wayaround.physical.PhysicalRegionSnapshot;
import net.caravidro.wayaround.pressure.NaturalPressure;
import net.caravidro.wayaround.pressure.PressureMath;
import net.caravidro.wayaround.pressure.PressureState;
import net.caravidro.wayaround.pressure.RegionPressureModel;
import net.caravidro.wayaround.pressure.UniversalPressure;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.caravidro.wayaround.worldgen.weather.BlizzardManager;
import net.caravidro.wayaround.worldgen.weather.BlizzardWind;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Canonical facade for universal flow.
 *
 * <p>Different domains keep specialized bounded solvers. This class translates
 * their output into the same physical language.</p>
 */
public final class UniversalFlow {

    private static final double VENT_WIND_COUPLING =
            0.35;

    private UniversalFlow() {
    }

    public static FlowState atmosphereAt(
            ServerLevel level,
            Vec3 position
    ) {
        double pressure =
                NaturalPressure.atmosphericKPa(
                        level,
                        position.y
                );

        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.LIVING_WEATHER
        )) {
            return new FlowState(
                    PhysicalMaterials.AIR,
                    MatterPhase.GAS,
                    Vec3.ZERO,
                    pressure,
                    0.0,
                    0.0,
                    FlowState.Source.ATMOSPHERE
            );
        }

        long time =
                level.getGameTime();

        LocalWeatherField.Sample weather =
                LocalWeatherField.sample(
                        level,
                        position.x,
                        position.z,
                        time
                );

        double blizzard =
                WorldFeatureRuntime.enabled(
                        level,
                        WorldFeature.ANTARCTICA
                )
                        ? BlizzardManager.getIntensity(
                        level,
                        position
                )
                        : 0.0;

        double directionX;
        double directionZ;

        if (blizzard > 0.02) {
            double angle =
                    BlizzardWind.angle(
                            time
                    );

            directionX =
                    Math.cos(
                            angle
                    );

            directionZ =
                    Math.sin(
                            angle
                    );

        } else {
            directionX =
                    weather.windX();

            directionZ =
                    weather.windZ();
        }

        double length =
                Math.sqrt(
                        directionX
                                * directionX
                                + directionZ
                                * directionZ
                );

        if (length <= 1.0E-9) {
            directionX =
                    0.0;

            directionZ =
                    0.0;

        } else {
            directionX /=
                    length;

            directionZ /=
                    length;
        }

        double vanillaWeather =
                level.isThundering()
                        ? 1.0
                        : level.isRaining()
                                ? 0.58
                                : 0.18;

        double weatherStrength =
                Math.max(
                        vanillaWeather,
                        weather.warning()
                                * 0.86
                );

        double gust =
                0.86
                        + Math.sin(
                        time / 57.0
                )
                        * 0.09
                        + Math.sin(
                        time / 131.0
                )
                        * 0.05;

        double blocksPerTick =
                (
                        0.010
                                + weatherStrength
                                        * 0.022
                                + blizzard
                                        * 0.032
                )
                        * gust;

        double speedMPerS =
                FlowMath.blocksPerTickToMPerS(
                        blocksPerTick
                );

        double turbulence =
                Math.clamp(
                        weather.warning()
                                * 0.55
                                + blizzard
                                        * 0.72
                                + Math.abs(
                                gust - 0.86
                        )
                                        * 0.85,
                        0.0,
                        1.0
                );

        return new FlowState(
                PhysicalMaterials.AIR,
                MatterPhase.GAS,
                new Vec3(
                        directionX
                                * speedMPerS,
                        0.0,
                        directionZ
                                * speedMPerS
                ),
                pressure,
                0.0,
                turbulence,
                FlowState.Source.ATMOSPHERE
        );
    }

    public static FlowState waterAt(
            Level level,
            BlockPos pos
    ) {
        Vec3 current =
                WaterDynamics.current(
                        level,
                        pos
                );

        return waterState(
                level,
                pos,
                current
        );
    }

    public static FlowState waterAround(
            Level level,
            BlockPos center
    ) {
        Vec3 current =
                WaterDynamics.currentAround(
                        level,
                        center
                );

        return waterState(
                level,
                center,
                current
        );
    }

    private static FlowState waterState(
            Level level,
            BlockPos pos,
            Vec3 blocksPerTick
    ) {
        boolean water =
                level.getFluidState(
                        pos
                ).is(
                        FluidTags.WATER
                );

        MaterialDefinition material =
                level instanceof ServerLevel server
                        && water
                        && NaturalPressure.isOcean(
                        server,
                        pos
                )
                        ? PhysicalMaterials.SALT_WATER
                        : PhysicalMaterials.WATER;

        double pressure =
                level instanceof ServerLevel server
                        && water
                        ? UniversalPressure.naturalAt(
                        server,
                        pos
                ).absoluteKPa()
                        : PressureMath.STANDARD_ATMOSPHERE_KPA;

        double turbulence =
                water
                        ? WaterDynamics.turbulence(
                        level,
                        pos
                )
                        : 0.0;

        return new FlowState(
                material,
                MatterPhase.LIQUID,
                blocksPerTick.scale(
                        FlowMath.TICKS_PER_SECOND
                ),
                pressure,
                0.0,
                turbulence,
                FlowState.Source.OPEN_WATER
        );
    }

    public static FlowState conduit(
            MaterialDefinition material,
            MatterPhase phase,
            Vec3 direction,
            double absolutePressureKPa,
            double volumetricRateM3PerS,
            double crossSectionM2,
            double turbulence,
            FlowState.Source source
    ) {
        Vec3 normalized =
                direction.lengthSqr()
                        > 1.0E-12
                        ? direction.normalize()
                        : Vec3.ZERO;

        double velocity =
                FlowMath.velocityFromVolumetricRateMPerS(
                        volumetricRateM3PerS,
                        crossSectionM2
                );

        return new FlowState(
                material,
                phase,
                normalized.scale(
                        velocity
                ),
                absolutePressureKPa,
                volumetricRateM3PerS,
                turbulence,
                source
        );
    }

    public static FlowState minecraftLiquidConduit(
            MaterialDefinition material,
            Vec3 direction,
            double absolutePressureKPa,
            double milliBucketsPerTick,
            double crossSectionM2,
            double turbulence
    ) {
        return conduit(
                material,
                MatterPhase.LIQUID,
                direction,
                absolutePressureKPa,
                FlowMath.minecraftFluidRateM3PerS(
                        milliBucketsPerTick
                ),
                crossSectionM2,
                turbulence,
                FlowState.Source.CONDUIT_LIQUID
        );
    }

    /**
     * Ventilation through a real opening. Pressure-driven flow and external
     * wind projection are combined without inventing another room-only scale.
     */
    public static FlowState ventilationAt(
            ServerLevel level,
            PhysicalRegionSnapshot region,
            PhysicalOpening opening
    ) {
        RegionPressureModel insideModel =
                UniversalPressure.regionAt(
                        level,
                        region,
                        opening.interiorCell()
                );

        PressureState outside =
                UniversalPressure.naturalAt(
                        level,
                        opening.outsideCell()
                );

        FlowState pressureFlow =
                OpeningFlow.between(
                        PhysicalMaterials.AIR,
                        MatterPhase.GAS,
                        insideModel.pressure(),
                        outside,
                        opening
                );

        FlowState wind =
                atmosphereAt(
                        level,
                        Vec3.atCenterOf(
                                opening.outsideCell()
                        )
                );

        Vec3 outward =
                new Vec3(
                        opening.outward()
                                .getStepX(),
                        opening.outward()
                                .getStepY(),
                        opening.outward()
                                .getStepZ()
                );

        double pressureSign =
                pressureFlow.velocityMPerS()
                        .dot(
                                outward
                        )
                        >= 0.0
                        ? 1.0
                        : -1.0;

        double signedPressureRate =
                pressureFlow.volumetricRateM3PerS()
                        * pressureSign;

        double signedWindRate =
                wind.velocityMPerS()
                        .dot(
                                outward
                        )
                        * opening.areaM2()
                        * VENT_WIND_COUPLING;

        double signedTotal =
                signedPressureRate
                        + signedWindRate;

        double speed =
                FlowMath.velocityFromVolumetricRateMPerS(
                        Math.abs(
                                signedTotal
                        ),
                        opening.areaM2()
                );

        Vec3 direction =
                signedTotal >= 0.0
                        ? outward
                        : outward.scale(
                                -1.0
                        );

        return new FlowState(
                PhysicalMaterials.AIR,
                MatterPhase.GAS,
                direction.scale(
                        speed
                ),
                Math.max(
                        insideModel.pressure()
                                .absoluteKPa(),
                        outside.absoluteKPa()
                ),
                Math.abs(
                        signedTotal
                ),
                Math.max(
                        pressureFlow.turbulence(),
                        wind.turbulence()
                ),
                FlowState.Source.VENTILATION
        );
    }
}
