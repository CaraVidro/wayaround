package net.caravidro.wayaround.worldgen.weather.cold;

public final class ColdExposureTest {
    public static void main(String[] args) {
        int clearDay = secondsToFrostbite(0, 0);
        int clearNight = secondsToFrostbite(1, 0);
        int stormDay = secondsToFrostbite(0, 1);
        int stormNight = secondsToFrostbite(1, 1);
        check(clearDay >= 499 && clearDay <= 501, "Clear day should take about 500 seconds");
        check(clearNight >= 100 && clearNight <= 101, "Night must progressively become dangerous");
        check(stormNight < stormDay && stormDay < clearNight && clearNight < clearDay,
                "Weather and darkness must increase exposure rate");

        ColdExposure.State sheltered = ColdExposure.State.WARM;
        for (int i = 0; i < 3600; i++) sheltered = ColdExposure.step(sheltered, false, 1, 1);
        check(sheltered.equals(ColdExposure.State.WARM), "Caves and underground must not accumulate cold");
        ColdExposure.State cold = new ColdExposure.State(120, 1);
        for (int i = 0; i < 120; i++) cold = ColdExposure.step(cold, false, 1, 1);
        check(cold.cold() == 0 && cold.tremor() > 0.3, "Tremors must outlast exposure and cold recovery");
        for (int i = 0; i < 61; i++) cold = ColdExposure.step(cold, false, 0, 0);
        check(cold.equals(ColdExposure.State.WARM), "Tremors must eventually end");

        ColdExposure.State buildUp = ColdExposure.State.WARM;
        double previousTremor = 0;
        for (int i = 0; i < 600; i++) {
            buildUp = ColdExposure.step(buildUp, true, 1, 1);
            check(buildUp.tremor() >= previousTremor, "Ongoing exposure must gradually strengthen tremors");
            check(buildUp.cold() <= 120 && buildUp.tremor() <= 1, "Exposure must be bounded");
            previousTremor = buildUp.tremor();
        }
        check(ColdExposure.nightFactor(6000) == 0 && ColdExposure.nightFactor(18000) == 1,
                "Noon and midnight must match the Antarctic lighting cycle");
        check(ColdExposure.nightFactor(13000) > 0 && ColdExposure.nightFactor(13000) < 1,
                "Dusk must transition smoothly");
        check(ColdExposure.nightFactor(-6000) == ColdExposure.nightFactor(18000), "Time wraps safely");
        ColdExposure.State invalid = ColdExposure.step(new ColdExposure.State(Double.NaN, Double.POSITIVE_INFINITY), true, 0, 0);
        check(Double.isFinite(invalid.cold()) && Double.isFinite(invalid.tremor()), "Bad saved data must recover");
        System.out.println("Cold exposure passed: day=" + clearDay + "s, night=" + clearNight
                + "s, storm=" + stormDay + "s, night storm=" + stormNight + "s; shelter and lingering recovery.");
    }

    private static int secondsToFrostbite(double night, double storm) {
        ColdExposure.State state = ColdExposure.State.WARM;
        for (int second = 1; second <= 1000; second++) {
            state = ColdExposure.step(state, true, night, storm);
            if (state.cold() >= ColdExposure.FROSTBITE_THRESHOLD) return second;
        }
        throw new AssertionError("Exposure never caused frostbite");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
