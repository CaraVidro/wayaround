package net.caravidro.wayaround.time;

import net.caravidro.wayaround.industrial.assembly.AssemblyMachine;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartNode;
import net.caravidro.wayaround.thermal.EnvironmentalTemperature;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;

/**
 * Shared low-frequency aging service.
 *
 * Systems call this at coarse intervals; elapsed game time is reconstructed
 * from the persistent state, so unloaded time does not require per-tick work.
 */
public final class TimeAgingEngine {

    private TimeAgingEngine() {}

    public record Sample(
            long elapsedTicks,
            long ageTicks,
            long inactiveTicks,
            float wetness,
            float weathering,
            float corrosion,
            float organicGrowth,
            float wearFraction
    ) {}

    public record Snapshot(
            long ageTicks,
            long inactiveTicks,
            float weathering,
            float corrosion,
            float organicGrowth,
            float moisture
    ) {}

    public static Sample sampleAssembly(
            ServerLevel level,
            BlockPos pos,
            AssemblyMachine machine,
            boolean active
    ) {
        if (!WorldFeatureRuntime.enabled(level, WorldFeature.TIME_AGING)) {
            return new Sample(0L,0L,0L,0,0,0,0,0);
        }

        TemporalAgingData data = TemporalAgingData.get(level);
        TemporalState state = data.state(pos);

        long now = level.getGameTime();
        long previous = state.lastSampleGameTime();
        long elapsed = previous <= 0L
                ? 0L
                : Math.max(0L, now - previous);

        float wetness = wetness(level, pos);
        double temperature = EnvironmentalTemperature.at(level, pos);

        float weatherRate = 0.0F;
        float corrosionRate = 0.0F;
        float organicRate = 0.0F;
        int count = 0;

        for (AssemblyPartNode part : machine.assemblyParts()) {
            var rates = TemporalMaterial.forAssemblyMaterial(
                    part.profile().material()
            );

            weatherRate += rates.weathering();
            corrosionRate += rates.corrosion();
            organicRate += rates.organicAffinity();
            count++;
        }

        if (count > 0) {
            weatherRate /= count;
            corrosionRate /= count;
            organicRate /= count;
        }

        float exposed = level.canSeeSky(pos.above()) ? 1.0F : 0.45F;
        float rain = level.isRainingAt(pos.above()) ? 1.0F : 0.0F;
        float damp = Mth.clamp(wetness * 0.8F + rain * 0.35F, 0.0F, 1.0F);

        float temperatureStress = (float) Mth.clamp(
                Math.abs(temperature - 18.0) / 90.0,
                0.0,
                1.8
        );

        float inactivityDays = state.inactiveTicks() / 24000.0F;
        float abandonment = Mth.clamp(inactivityDays / 10.0F, 0.0F, 1.0F);

        weatherRate *= 0.25F + exposed * 0.75F;
        weatherRate *= 1.0F + temperatureStress * 0.20F;

        corrosionRate *= damp * (0.55F + exposed * 0.45F);

        organicRate *= damp
                * (0.35F + abandonment * 0.90F)
                * (level.canSeeSky(pos.above()) ? 0.75F : 1.0F);

        state.advance(
                elapsed,
                active,
                damp,
                weatherRate,
                corrosionRate,
                organicRate
        );
        state.markSample(now);
        data.setDirty();

        float wearFraction = 0.0F;
        if (elapsed > 0L && count > 0) {
            float days = elapsed / 24000.0F;
            wearFraction = days * (
                    state.weathering() * 0.00055F
                            + state.corrosion() * 0.00125F
            );

            if (active) {
                wearFraction *= 0.65F;
            } else {
                wearFraction *= 1.15F;
            }

            wearFraction = Mth.clamp(wearFraction, 0.0F, 0.02F);
        }

        return new Sample(
                elapsed,
                state.ageTicks(),
                state.inactiveTicks(),
                damp,
                state.weathering(),
                state.corrosion(),
                state.organicGrowth(),
                wearFraction
        );
    }

    public static TemporalState sampleWorldSurface(
            ServerLevel level,
            BlockPos pos,
            boolean active
    ) {
        TemporalAgingData data = TemporalAgingData.get(level);
        TemporalState state = data.state(pos);

        long now = level.getGameTime();
        long previous = state.lastSampleGameTime();
        long elapsed = previous <= 0L ? 0L : Math.max(0L, now - previous);

        float wetness = wetness(level, pos);
        boolean shaded = !level.canSeeSky(pos.above());

        long projectedInactive =
                active
                        ? 0L
                        : state.inactiveTicks()
                                + elapsed;

        float abandonment =
                Mth.clamp(
                        projectedInactive
                                / (24000.0F * 5.0F),
                        0.0F,
                        1.0F
                );

        float organic =
                wetness
                        * (shaded ? 0.028F : 0.010F)
                        * (0.08F + abandonment * 0.92F);

        state.advance(
                elapsed,
                active,
                wetness,
                0.003F,
                0.0F,
                organic
        );
        state.markSample(now);
        data.setDirty();

        return state;
    }

    public static Snapshot snapshot(
            ServerLevel level,
            BlockPos pos
    ) {
        TemporalState state =
                TemporalAgingData.get(level)
                        .state(pos);

        return new Snapshot(
                state.ageTicks(),
                state.inactiveTicks(),
                state.weathering(),
                state.corrosion(),
                state.organicGrowth(),
                state.moistureMemory()
        );
    }

    private static float wetness(
            ServerLevel level,
            BlockPos pos
    ) {
        float wet = 0.0F;

        if (level.getFluidState(pos).is(FluidTags.WATER)
                || level.getFluidState(pos.below()).is(FluidTags.WATER)) {
            wet = 1.0F;
        } else if (level.isRainingAt(pos.above())) {
            wet = 0.82F;
        } else {
            for (var direction : net.minecraft.core.Direction.values()) {
                if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                    wet = Math.max(wet, 0.62F);
                }
            }
        }

        return wet;
    }
}
