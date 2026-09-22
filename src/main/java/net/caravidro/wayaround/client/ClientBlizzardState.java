package net.caravidro.wayaround.client;

public final class ClientBlizzardState {

    /*
     * Valor recebido do servidor.
     */
    private static float targetIntensity = 0.0F;

    /*
     * Valor visual suavizado.
     */
    private static float intensity = 0.0F;

    private ClientBlizzardState() {
    }

    public static void receive(float value) {

        targetIntensity =
                clamp(
                        value,
                        0.0F,
                        1.0F
                );
    }

    public static void tick() {

        /*
         * Interpolação suave.
         *
         * Evita:
         *
         * fog OFF
         * fog ON
         * fog OFF
         *
         * instantaneamente.
         */
        intensity +=
                (
                        targetIntensity
                        -
                        intensity
                )
                * 0.10F;

        if (
                Math.abs(
                        targetIntensity
                        -
                        intensity
                )
                < 0.001F
        ) {

            intensity =
                    targetIntensity;
        }
    }

    public static float getIntensity() {
        return intensity;
    }

    public static float getTargetIntensity() {
        return targetIntensity;
    }

    public static void clear() {

        intensity = 0.0F;
        targetIntensity = 0.0F;
    }

    private static float clamp(
            float value,
            float min,
            float max
    ) {

        return Math.max(
                min,
                Math.min(max, value)
        );
    }
}