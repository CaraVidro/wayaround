package net.caravidro.wayaround.animation;

/**
 * Client-friendly continuous motion helpers for object animation.
 *
 * The server can continue updating machinery at 20 TPS while renderers predict
 * motion between those updates. Rotation speed is exponentially smoothed, and
 * optional authoritative angles are corrected gradually to avoid visible
 * snapping or long-term multiplayer drift.
 */
public final class SmoothObjectAnimation {

    private SmoothObjectAnimation() {
    }

    public static final class Rotation {

        private double lastRenderTime =
                Double.NaN;

        private float angle;
        private float smoothedRpm;

        private final float responseRate;
        private final float correctionRate;
        private final float snapThreshold;

        public Rotation(
                float initialAngle
        ) {
            this(
                    initialAngle,
                    0.24F,
                    0.16F,
                    18.0F
            );
        }

        public Rotation(
                float initialAngle,
                float responseRate,
                float correctionRate,
                float snapThreshold
        ) {
            angle =
                    wrap(
                            initialAngle
                    );

            this.responseRate =
                    Math.max(
                            0.001F,
                            responseRate
                    );

            this.correctionRate =
                    Math.max(
                            0.0F,
                            correctionRate
                    );

            this.snapThreshold =
                    Math.max(
                            0.0F,
                            snapThreshold
                    );
        }

        /**
         * RPM follows the same convention used by WayAround mechanical
         * machines: rpm * 0.30 degrees per game tick.
         */
        public float update(
                double renderTime,
                float targetRpm
        ) {
            return updateInternal(
                    renderTime,
                    targetRpm,
                    Float.NaN
            );
        }

        public float update(
                double renderTime,
                float targetRpm,
                float authoritativeAngle
        ) {
            return updateInternal(
                    renderTime,
                    targetRpm,
                    authoritativeAngle
            );
        }

        public float angle() {
            return angle;
        }

        public float smoothedRpm() {
            return smoothedRpm;
        }

        public void reset(
                double renderTime,
                float newAngle,
                float rpm
        ) {
            lastRenderTime =
                    renderTime;

            angle =
                    wrap(
                            newAngle
                    );

            smoothedRpm =
                    rpm;
        }

        private float updateInternal(
                double renderTime,
                float targetRpm,
                float authoritativeAngle
        ) {
            if (!Double.isFinite(
                    lastRenderTime
            )) {
                lastRenderTime =
                        renderTime;

                smoothedRpm =
                        targetRpm;

                if (Float.isFinite(
                        authoritativeAngle
                )) {
                    angle =
                            wrap(
                                    authoritativeAngle
                            );
                }

                return angle;
            }

            double delta =
                    Math.max(
                            0.0,
                            Math.min(
                                    2.0,
                                    renderTime
                                            - lastRenderTime
                            )
                    );

            lastRenderTime =
                    renderTime;

            float response =
                    1.0F
                            - (float) Math.exp(
                            -delta
                                    * responseRate
                    );

            smoothedRpm +=
                    (
                            targetRpm
                                    - smoothedRpm
                    )
                            * response;

            angle =
                    wrap(
                            angle
                                    + smoothedRpm
                                            * 0.30F
                                            * (float) delta
                    );

            if (!Float.isFinite(
                    authoritativeAngle
            )) {
                return angle;
            }

            float correction =
                    shortestDelta(
                            angle,
                            authoritativeAngle
                    );

            if (Math.abs(
                    correction
            ) > snapThreshold) {

                angle =
                        wrap(
                                authoritativeAngle
                        );

            } else {
                angle =
                        wrap(
                                angle
                                        + correction
                                                * correctionRate
                        );
            }

            return angle;
        }
    }

    public static final class Value {

        private double lastRenderTime =
                Double.NaN;

        private float value;

        private final float responseRate;

        public Value(
                float initialValue
        ) {
            this(
                    initialValue,
                    0.30F
            );
        }

        public Value(
                float initialValue,
                float responseRate
        ) {
            value =
                    initialValue;

            this.responseRate =
                    Math.max(
                            0.001F,
                            responseRate
                    );
        }

        public float update(
                double renderTime,
                float target
        ) {
            if (!Double.isFinite(
                    lastRenderTime
            )) {
                lastRenderTime =
                        renderTime;

                value =
                        target;

                return value;
            }

            double delta =
                    Math.max(
                            0.0,
                            Math.min(
                                    2.0,
                                    renderTime
                                            - lastRenderTime
                            )
                    );

            lastRenderTime =
                    renderTime;

            float response =
                    1.0F
                            - (float) Math.exp(
                            -delta
                                    * responseRate
                    );

            value +=
                    (
                            target
                                    - value
                    )
                            * response;

            return value;
        }

        public float value() {
            return value;
        }
    }

    public static float wrap(
            float value
    ) {
        value %=
                360.0F;

        if (value < 0.0F) {
            value +=
                    360.0F;
        }

        return value;
    }

    public static float shortestDelta(
            float from,
            float to
    ) {
        float delta =
                wrap(
                        to
                )
                        - wrap(
                        from
                );

        if (delta > 180.0F) {
            delta -=
                    360.0F;
        }

        if (delta < -180.0F) {
            delta +=
                    360.0F;
        }

        return delta;
    }
}
