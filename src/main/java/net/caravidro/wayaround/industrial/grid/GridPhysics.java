package net.caravidro.wayaround.industrial.grid;

public final class GridPhysics {

    private GridPhysics() {
    }

    public static float highVoltageEfficiency(
            int cableCount,
            float sourceEfficiency
    ) {
        float line =
                Math.max(
                        0.82F,
                        1.0F
                                - Math.max(
                                0,
                                cableCount
                        )
                                        * 0.0015F
                );

        return Math.clamp(
                line
                        * sourceEfficiency,
                0.50F,
                1.0F
        );
    }

    public static int converted(
            int raw,
            float efficiency
    ) {
        return Math.max(
                0,
                (int) Math.floor(
                        Math.max(
                                0,
                                raw
                        )
                                * Math.clamp(
                                efficiency,
                                0.0F,
                                1.0F
                        )
                )
        );
    }

    public static int rawForConverted(
            int converted,
            float efficiency
    ) {
        float safe =
                Math.max(
                        0.01F,
                        Math.clamp(
                                efficiency,
                                0.0F,
                                1.0F
                        )
                );

        return Math.max(
                0,
                (int) Math.ceil(
                        Math.max(
                                0,
                                converted
                        )
                                / safe
                )
        );
    }

    public static float overloadRatio(
            int offered,
            int safePulse
    ) {
        if (safePulse <= 0) {
            return offered > 0
                    ? 1.0F
                    : 0.0F;
        }

        return Math.max(
                0.0F,
                offered
                        / (float) safePulse
                        - 1.0F
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
