package net.caravidro.wayaround.industrial.pipework;

public enum PipeMedium {
    EMPTY(false, 20.0F),
    WATER(false, 20.0F),
    AIR(true, 20.0F),
    STEAM(true, 180.0F);

    private final boolean gas;
    private final float nominalTemperatureC;

    PipeMedium(
            boolean gas,
            float nominalTemperatureC
    ) {
        this.gas = gas;
        this.nominalTemperatureC = nominalTemperatureC;
    }

    public boolean gas() {
        return gas;
    }

    public float nominalTemperatureC() {
        return nominalTemperatureC;
    }

    public static PipeMedium fromName(
            String raw
    ) {
        try {
            return PipeMedium.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return EMPTY;
        }
    }
}
