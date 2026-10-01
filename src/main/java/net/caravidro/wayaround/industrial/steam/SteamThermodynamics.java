package net.caravidro.wayaround.industrial.steam;

import net.minecraft.util.Mth;

/** Shared, bounded steam math. Values are gameplay engineering units. */
public final class SteamThermodynamics {

    public static final int WATER_CAPACITY = 16_000;
    public static final int STEAM_CAPACITY = 12_000;
    public static final int FUEL_TICKS_PER_COAL = 1_600;
    public static final float NOMINAL_SAFE_PRESSURE_BAR = 12.0F;

    public record BoilerStep(
            int water,
            int fuelTicks,
            int heat,
            int steam,
            boolean consumeCoal,
            int vented,
            float pressureBar,
            int temperatureC
    ) {}

    private SteamThermodynamics() {
    }

    public static BoilerStep tickSecond(
            int water,
            int fuelTicks,
            int heat,
            int steam,
            boolean coalAvailable,
            float vesselCondition,
            float valveCondition
    ) {
        water =
                Math.clamp(
                        water,
                        0,
                        WATER_CAPACITY
                );

        fuelTicks =
                Math.max(
                        0,
                        fuelTicks
                );

        heat =
                Math.clamp(
                        heat,
                        0,
                        100
                );

        steam =
                Math.clamp(
                        steam,
                        0,
                        STEAM_CAPACITY
                );

        boolean demand =
                water > 0
                        && steam < STEAM_CAPACITY;

        boolean ignite =
                demand
                        && fuelTicks <= 0
                        && coalAvailable;

        if (ignite) {
            fuelTicks =
                    FUEL_TICKS_PER_COAL;
        }

        if (fuelTicks > 0) {
            fuelTicks =
                    Math.max(
                            0,
                            fuelTicks - 20
                    );

            heat =
                    Math.min(
                            100,
                            heat + 5
                    );

        } else {
            heat =
                    Math.max(
                            0,
                            heat - 3
                    );
        }

        if (heat >= 58
                && water > 0
                && steam < STEAM_CAPACITY) {

            int rate =
                    Math.clamp(
                            (heat - 48) * 3,
                            12,
                            156
                    );

            int boiled =
                    Math.min(
                            water,
                            Math.min(
                                    rate,
                                    STEAM_CAPACITY - steam
                            )
                    );

            water -=
                    boiled;

            steam +=
                    boiled;
        }

        float pressure =
                pressureBar(
                        steam,
                        heat
                );

        int temperature =
                temperatureC(
                        heat,
                        steam
                );

        float safePressure =
                safePressureBar(
                        vesselCondition,
                        valveCondition
                );

        int vented =
                0;

        if (pressure > safePressure
                && steam > 0) {

            float excess =
                    pressure - safePressure;

            vented =
                    Math.min(
                            steam,
                            Math.max(
                                    25,
                                    Math.round(
                                            excess * 95.0F
                                    )
                            )
                    );

            steam -=
                    vented;

            pressure =
                    pressureBar(
                            steam,
                            heat
                    );
        }

        return new BoilerStep(
                water,
                fuelTicks,
                heat,
                steam,
                ignite,
                vented,
                pressure,
                temperature
        );
    }

    public static float pressureBar(
            int steam,
            int heat
    ) {
        float fill =
                Mth.clamp(
                        steam / (float) STEAM_CAPACITY,
                        0.0F,
                        1.0F
                );

        float thermal =
                0.62F
                        + Mth.clamp(
                        heat / 100.0F,
                        0.0F,
                        1.0F
                )
                        * 0.52F;

        return fill <= 0.001F
                ? 0.0F
                : 0.35F
                        + fill
                                * 16.5F
                                * thermal;
    }

    public static int temperatureC(
            int heat,
            int steam
    ) {
        if (steam <= 0) {
            return 20
                    + Math.round(
                    Mth.clamp(
                            heat / 100.0F,
                            0.0F,
                            1.0F
                    )
                            * 80.0F
            );
        }

        return 100
                + Math.round(
                Mth.clamp(
                        heat / 100.0F,
                        0.0F,
                        1.0F
                )
                        * 175.0F
        );
    }

    public static float safePressureBar(
            float vesselCondition,
            float valveCondition
    ) {
        float vessel =
                Mth.clamp(
                        vesselCondition,
                        0.05F,
                        1.0F
                );

        float valve =
                Mth.clamp(
                        valveCondition,
                        0.05F,
                        1.0F
                );

        return NOMINAL_SAFE_PRESSURE_BAR
                * (
                0.45F
                        + vessel * 0.40F
                        + valve * 0.15F
        );
    }

    public static float engineRpm(
            float pressureBar,
            int steamStored
    ) {
        if (steamStored <= 0
                || pressureBar < 0.55F) {
            return 0.0F;
        }

        return Mth.clamp(
                (pressureBar - 0.45F)
                        * 8.2F,
                0.0F,
                118.0F
        );
    }

    public static float engineTorque(
            float pressureBar,
            int temperatureC,
            int steamStored
    ) {
        if (steamStored <= 0
                || pressureBar < 0.55F) {
            return 0.0F;
        }

        float thermal =
                Mth.clamp(
                        (temperatureC - 90)
                                / 170.0F,
                        0.45F,
                        1.35F
                );

        float reserve =
                Mth.clamp(
                        steamStored / 1_000.0F,
                        0.20F,
                        1.0F
                );

        return Mth.clamp(
                (pressureBar - 0.35F)
                        * 0.48F
                        * thermal
                        * reserve,
                0.0F,
                8.5F
        );
    }

    public static float mechanicalPower(
            float rpm,
            float torque
    ) {
        return Math.max(
                0.0F,
                rpm
                        / 36.0F
                        * torque
        );
    }
}
