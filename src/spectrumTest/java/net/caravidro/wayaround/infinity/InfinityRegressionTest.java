package net.caravidro.wayaround.infinity;

public final class InfinityRegressionTest {
    public static void main(String[] args) {
        require(InfinityMath.meleeFraction(0) == 0, "First contact does no damage");
        require(Math.abs(InfinityMath.meleeFraction(1) - .05) < 1e-6, "Next hit starts at five percent");
        require(InfinityMath.meleeFraction(1000) == .75F, "Adaptation can never bypass the 75% ceiling");
        for (double initial : new double[]{.2, 1.6, 7.5, 8, 40, 1000}) {
            double speed = initial, gap = 10.8;
            int visibleSteps = 0;
            while (visibleSteps < 200) {
                double next = InfinityMath.slowedSpeed(speed, gap);
                require(next <= speed && next < gap, "No acceleration or inner-boundary tunnelling");
                if (gap < .08 || next < .015) break;
                gap -= next; speed = next; visibleSteps++;
            }
            require(visibleSteps >= 3 && visibleSteps < 200, "Even a very fast bullet visibly slows before stopping");
        }
        require(InfinityMath.slowedSpeed(8, 20) / 8 < InfinityMath.slowedSpeed(1.6, 20) / 1.6,
                "Bullets brake harder than arrows");
        require(InfinityMath.slowedSpeed(Double.NaN, 1) == 0, "Reject invalid physics input");
        System.out.println("Infinity adaptation and braking regression tests passed");
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
