package net.caravidro.wayaround.time;

import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;

public final class TimeAgingRegressionTest {

    public static void main(String[] args) {
        TemporalState state = new TemporalState();

        state.markSample(100L);
        state.advance(
                24000L,
                false,
                1.0F,
                0.10F,
                0.20F,
                0.15F
        );
        state.markSample(24100L);

        require(state.ageTicks() == 24000L, "One day must advance age by one day");
        require(state.inactiveTicks() == 24000L, "Inactive time must accumulate");
        require(state.weathering() > 0.09F, "Weathering must accumulate");
        require(state.corrosion() > 0.19F, "Corrosion must accumulate");
        require(state.organicGrowth() > 0.14F, "Organic growth must accumulate");
        require(state.moistureMemory() > 0.0F, "Wet exposure must leave moisture memory");

        state.advance(
                200L,
                true,
                0.0F,
                0.0F,
                0.0F,
                0.0F
        );

        require(state.inactiveTicks() == 0L, "Activity must reset abandonment time");

        var iron = TemporalMaterial.forAssemblyMaterial(
                AssemblyPartProfile.Material.IRON
        );
        var wood = TemporalMaterial.forAssemblyMaterial(
                AssemblyPartProfile.Material.WOOD
        );

        require(
                iron.corrosion() > wood.corrosion(),
                "Iron must be more corrosion-sensitive than wood"
        );

        require(
                wood.organicAffinity() > iron.organicAffinity(),
                "Wood must accept more organic growth than iron"
        );

        TemporalState clamp = new TemporalState();
        clamp.advance(
                24000L * 100L,
                false,
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );

        require(clamp.weathering() <= 1.0F, "Weathering must stay bounded");
        require(clamp.corrosion() <= 1.0F, "Corrosion must stay bounded");
        require(clamp.organicGrowth() <= 1.0F, "Growth must stay bounded");

        System.out.println("Time/Aging regression tests passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
