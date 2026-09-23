package net.caravidro.wayaround.client.weather;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.caravidro.wayaround.client.ClientBlizzardState;
import net.caravidro.wayaround.worldgen.weather.BlizzardWind;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import net.caravidro.wayaround.WayAround;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class ClientWind {

    private static float targetX = 0.0F;
    private static float targetZ = 0.0F;
    private static float targetStrength = 0.0F;

    private static float windX = 0.0F;
    private static float windZ = 0.0F;
    private static float strength = 0.0F;
    private static final Long2ByteOpenHashMap exposure = new Long2ByteOpenHashMap();
    private static ClientLevel exposureLevel;
    private static long exposureTick = Long.MIN_VALUE;

    private ClientWind() {
    }

    /*
     * =========================================================
     * WEATHER -> WIND
     * =========================================================
     *
     * Chame isso quando a nevasca atualizar.
     *
     * directionX / directionZ:
     * vetor da direção do vento.
     *
     * strength:
     * 0 = sem vento
     * 1 = vento forte
     */

    public static void set(
            float directionX,
            float directionZ,
            float newStrength
    ) {

        float length =
                Mth.sqrt(
                        directionX * directionX
                                +
                                directionZ * directionZ
                );

        if (length > 0.0001F) {

            directionX /= length;
            directionZ /= length;

        } else {

            directionX = 0.0F;
            directionZ = 0.0F;
        }

        targetX =
                directionX;

        targetZ =
                directionZ;

        targetStrength =
                Mth.clamp(
                        newStrength,
                        0.0F,
                        1.0F
                );
    }

    public static void stop() {

        targetStrength =
                0.0F;
    }

    /*
     * =========================================================
     * SMOOTHING
     * =========================================================
     */

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            targetX = targetZ = targetStrength = windX = windZ = strength = 0;
            exposureLevel = null;
            exposure.clear();
            return;
        }
        if (minecraft.isPaused()) return;

        if (minecraft.level.dimension().equals(Level.OVERWORLD)) {
            float blizzard = ClientBlizzardState.getIntensity();

            if (blizzard > 0.02F) {
                double angle = BlizzardWind.angle(minecraft.level.getGameTime());

                set(
                        (float) Math.cos(angle),
                        (float) Math.sin(angle),
                        blizzard
                );
            } else {
                LocalWeatherField.Sample weather =
                        LocalWeatherField.sample(
                                minecraft.player.getX(),
                                minecraft.player.getZ(),
                                minecraft.level.getGameTime()
                        );

                set(
                        weather.windX(),
                        weather.windZ(),
                        weather.warning() * 0.86F
                );
            }
        } else {
            stop();
        }

        /*
         * Direção muda lentamente.
         */

        windX =
                Mth.lerp(
                        0.035F,
                        windX,
                        targetX
                );

        windZ =
                Mth.lerp(
                        0.035F,
                        windZ,
                        targetZ
                );

        /*
         * A força reage um pouco mais rápido.
         */

        strength =
                Mth.lerp(
                        0.055F,
                        strength,
                        targetStrength
                );

        /*
         * Evita lixo numérico quando termina.
         */

        if (
                targetStrength == 0.0F
                        &&
                        strength < 0.001F
        ) {

            strength = 0.0F;
        }
    }

    public static float getX() {
        return windX;
    }

    public static float getZ() {
        return windZ;
    }

    public static float getStrength() {
        return strength;
    }

    public static boolean active() {
        return strength > 0.001F;
    }

    public static double getSpeed() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0 : BlizzardWind.speed(minecraft.level.getGameTime(), strength);
    }

    /** Use the particle's position, not whether the camera is sheltered. */
    public static boolean exposed(ClientLevel level, double x, double y, double z) {
        if (!active() || !level.dimension().equals(Level.OVERWORLD)) return false;
        if (level != exposureLevel || exposureTick != level.getGameTime()) {
            exposureLevel = level;
            exposureTick = level.getGameTime();
            exposure.clear();
        }
        BlockPos pos = BlockPos.containing(x, y, z);
        long key = pos.asLong();
        byte cached = exposure.get(key);
        if (cached != 0) return cached == 1;
        boolean open = level.hasChunkAt(pos)
                && level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) <= pos.getY()
                && level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
        exposure.put(key, (byte) (open ? 1 : 2));
        return open;
    }
}
