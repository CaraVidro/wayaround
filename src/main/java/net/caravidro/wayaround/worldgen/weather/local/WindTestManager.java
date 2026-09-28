package net.caravidro.wayaround.worldgen.weather.local;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.WindTestStateS2CPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Temporary, local wind amplifier used only for debugging weather-responsive
 * systems such as the engineer top hat.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class WindTestManager {

    private static volatile State state = State.inactive();

    private WindTestManager() {}

    public static void start(
            ServerLevel level,
            Vec3 center,
            double radius,
            float limit,
            int rampTicks
    ) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        State next = new State(
                true,
                center.x,
                center.z,
                Math.max(4.0, radius),
                level.getGameTime(),
                Math.max(20, rampTicks),
                40,
                Mth.clamp(limit, 0.05F, 1.0F)
        );

        state = next;
        broadcast(level, next);
    }

    public static void stop(ServerLevel level) {
        state = State.inactive();
        broadcast(level, state);
    }

    public static boolean active() {
        return state.active;
    }

    /**
     * Called by the networking payload on physical clients. This class is
     * common-safe on purpose so LocalWeatherField can consult the same state
     * on both sides.
     */
    public static void acceptRemote(
            boolean active,
            double centerX,
            double centerZ,
            double radius,
            long startedAt,
            int rampTicks,
            int fadeTicks,
            float limit
    ) {
        state = active
                ? new State(
                        true,
                        centerX,
                        centerZ,
                        radius,
                        startedAt,
                        rampTicks,
                        fadeTicks,
                        Mth.clamp(limit, 0.0F, 1.0F)
                )
                : State.inactive();
    }

    public static float strengthAt(
            double x,
            double z,
            long gameTime
    ) {
        State current = state;

        if (!current.active) {
            return 0.0F;
        }

        long age = gameTime - current.startedAt;

        if (age < 0L) {
            return 0.0F;
        }

        long rampEnd = current.rampTicks;
        long end = rampEnd + current.fadeTicks;

        if (age >= end) {
            return 0.0F;
        }

        double dx = x - current.centerX;
        double dz = z - current.centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance >= current.radius) {
            return 0.0F;
        }

        float temporal;

        if (age <= rampEnd) {
            temporal = Mth.clamp(
                    age / (float) current.rampTicks,
                    0.0F,
                    1.0F
            );
        } else {
            temporal = 1.0F - Mth.clamp(
                    (age - rampEnd) / (float) current.fadeTicks,
                    0.0F,
                    1.0F
            );
        }

        float spatial = 1.0F - (float) (distance / current.radius);
        spatial = spatial * spatial * (3.0F - 2.0F * spatial);

        return Mth.clamp(
                current.limit * temporal * spatial,
                0.0F,
                1.0F
        );
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        State current = state;

        if (!current.active) {
            return;
        }

        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        long now = level.getGameTime();
        long end = current.startedAt + current.rampTicks + current.fadeTicks;

        if (now < current.startedAt
                || now >= end) {
            state = State.inactive();
            broadcast(level, state);
            return;
        }

        // Refresh occasionally so a player who joins during the test sees it.
        if (now % 40L == 0L) {
            broadcast(level, current);
        }
    }

    private static void broadcast(
            ServerLevel level,
            State current
    ) {
        WindTestStateS2CPayload payload =
                new WindTestStateS2CPayload(
                        current.active,
                        current.centerX,
                        current.centerZ,
                        current.radius,
                        current.startedAt,
                        current.rampTicks,
                        current.fadeTicks,
                        current.limit
                );

        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(
                    player,
                    payload
            );
        }
    }

    private record State(
            boolean active,
            double centerX,
            double centerZ,
            double radius,
            long startedAt,
            int rampTicks,
            int fadeTicks,
            float limit
    ) {
        private static State inactive() {
            return new State(
                    false,
                    0.0,
                    0.0,
                    0.0,
                    0L,
                    1,
                    1,
                    0.0F
            );
        }
    }
}
