package net.caravidro.wayaround.client.performance;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

/**
 * Client-side level of detail budget derived from the player's simulation distance.
 *
 * Vanilla already stops authoritative simulation outside ticking chunks. This class
 * therefore never invents a second physics system; it reduces WayAround visual work
 * as distance grows: animation cadence, dynamic lighting, small details and particles.
 */
public final class DistanceLod {
    public enum Tier {
        FULL(1, true, true, true),
        REDUCED(2, true, true, true),
        COARSE(4, false, false, true),
        FAR(8, false, false, false),
        FROZEN(20, false, false, false);

        private final int animationStep;
        private final boolean detailedGeometry;
        private final boolean dynamicLighting;
        private final boolean particles;

        Tier(int animationStep, boolean detailedGeometry, boolean dynamicLighting, boolean particles) {
            this.animationStep = animationStep;
            this.detailedGeometry = detailedGeometry;
            this.dynamicLighting = dynamicLighting;
            this.particles = particles;
        }

        public int animationStep() { return animationStep; }
        public boolean detailedGeometry() { return detailedGeometry; }
        public boolean dynamicLighting() { return dynamicLighting; }
        public boolean particles() { return particles; }
    }

    /*
     * Renderer hot path cache. A scene can ask for LOD hundreds/thousands of
     * times in one client tick. Reading the option tree and taking sqrt() for
     * every entity is wasted work; cache the player/config snapshot once and
     * compare squared distances.
     */
    private static int cachedPlayerTick =
            Integer.MIN_VALUE;

    private static Object cachedPlayerIdentity;

    private static double cachedPlayerX;
    private static double cachedPlayerY;
    private static double cachedPlayerZ;

    private static double fullSqr;
    private static double reducedSqr;
    private static double coarseSqr;
    private static double farSqr;

    private DistanceLod() {}

    public static Tier forEntity(Entity entity) {
        return tier(entity.getX(), entity.getY(), entity.getZ());
    }

    public static Tier forBlock(BlockPos pos) {
        return tier(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public static float quantizeTicks(float ageInTicks, Tier tier) {
        int step = tier.animationStep();
        if (step <= 1) return ageInTicks;
        return (float) (Math.floor(ageInTicks / step) * step);
    }

    public static float quantizeDegrees(float degrees, Tier tier) {
        float step = switch (tier) {
            case FULL -> 0.0F;
            case REDUCED -> 3.0F;
            case COARSE -> 7.5F;
            case FAR -> 15.0F;
            case FROZEN -> 30.0F;
        };
        if (step <= 0.0F) return degrees;
        return Math.round(degrees / step) * step;
    }

    private static Tier tier(double x, double y, double z) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return Tier.FULL;
        }

        refreshSnapshot(
                minecraft
        );

        double dx =
                cachedPlayerX - x;

        double dy =
                cachedPlayerY - y;

        double dz =
                cachedPlayerZ - z;

        double distanceSqr =
                dx * dx
                        + dy * dy
                        + dz * dz;

        if (distanceSqr <= fullSqr) {
            return Tier.FULL;
        }

        if (distanceSqr <= reducedSqr) {
            return Tier.REDUCED;
        }

        if (distanceSqr <= coarseSqr) {
            return Tier.COARSE;
        }

        if (distanceSqr <= farSqr) {
            return Tier.FAR;
        }

        return Tier.FROZEN;
    }

    private static void refreshSnapshot(
            Minecraft minecraft
    ) {
        int tick =
                minecraft.player.tickCount;

        if (cachedPlayerIdentity
                == minecraft.player
                && cachedPlayerTick
                == tick) {
            return;
        }

        cachedPlayerIdentity =
                minecraft.player;

        cachedPlayerTick =
                tick;

        cachedPlayerX =
                minecraft.player.getX();

        cachedPlayerY =
                minecraft.player.getEyeY();

        cachedPlayerZ =
                minecraft.player.getZ();

        int simulationChunks =
                Math.max(
                        2,
                        minecraft.options
                                .simulationDistance()
                                .get()
                );

        double full =
                Math.max(
                        48.0,
                        simulationChunks * 16.0
                );

        double reduced =
                full + 64.0;

        double coarse =
                full + 160.0;

        double far =
                full + 320.0;

        fullSqr =
                full * full;

        reducedSqr =
                reduced * reduced;

        coarseSqr =
                coarse * coarse;

        farSqr =
                far * far;
    }
}
