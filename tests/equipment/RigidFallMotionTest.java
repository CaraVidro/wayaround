import net.caravidro.wayaround.interaction.RigidFallMotion;
public final class RigidFallMotionTest {
    public static void main(String[] args) {
        int checks = 0;
        for (int distance = 0; distance <= 512; distance++) {
            double velocity = 0, integrated = 0, previous = 0;
            int duration = RigidFallMotion.calvingTicks(distance);
            for (int tick = 1; tick <= duration; tick++) {
                velocity = Math.min(.82, velocity + .038);
                integrated = Math.min(distance, integrated + velocity);
                double actual = RigidFallMotion.calvingDrop(tick, distance);
                if (Math.abs(actual - integrated) > 1e-8) throw new AssertionError("Analytic and tick gravity disagree");
                double middle = RigidFallMotion.calvingDrop(tick - .5, distance);
                if (middle < previous || middle > actual || actual > distance) throw new AssertionError("Interpolation overshoots or runs backwards");
                previous = actual; checks += 2;
            }
            if (RigidFallMotion.calvingDrop(duration, distance) != distance) throw new AssertionError("Debris disappears before landing");
            if (RigidFallMotion.drift(distance, distance) != 1) throw new AssertionError("Drift misses final world placement");
            checks += 2;
        }
        if (RigidFallMotion.supportedDrop(java.util.List.of(0,20,20,20,20,20,20,20,20,20),512) != 20)
            throw new AssertionError("One voxel cannot catch an entire glacier/building");
        if (RigidFallMotion.supportedDrop(java.util.List.of(0,0,0,0,20,20,20,20,20,20),512) != 0)
            throw new AssertionError("Broad support must stop a rigid fragment");
        if (RigidFallMotion.supportedDrop(java.util.List.of(3,20),512) != 3)
            throw new AssertionError("Small fragments still respect their highest support");
        checks += 3;
        System.out.println("RIGID FALL: " + checks + " gravity, interpolation and landing checks passed");
    }
}
