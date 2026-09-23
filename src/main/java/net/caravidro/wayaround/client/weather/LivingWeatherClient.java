package net.caravidro.wayaround.client.weather;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.AntarcticClientLighting;
import net.caravidro.wayaround.particle.WayAroundParticles;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client presentation for the lightweight local-weather field.
 *
 * Clouds are intentionally made from large soft particle billboards. They are
 * much cheaper and more Minecraft-like than a full volumetric ray-march, while
 * still producing actual moving cloud masses and localized rain.
 */
@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class LivingWeatherClient {

    private static final double CLOUD_RENDER_RANGE = 620.0;
    private static int ticks;

    private LivingWeatherClient() {
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null || minecraft.isPaused()) {
            return;
        }

        if (!minecraft.level.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        /*
         * Antarctica already has its own blizzard language.
         */
        if (AntarcticClientLighting.isAntarctic(minecraft)) {
            return;
        }

        ticks++;

        if (ticks % 5 == 0) {
            spawnCloudMass(minecraft);
        }

        if (ticks % 3 == 0) {
            reactFoliage(minecraft);
        }
    }

    private static void spawnCloudMass(Minecraft minecraft) {
        var level = minecraft.level;
        var player = minecraft.player;

        long time = level.getGameTime();
        double px = player.getX();
        double pz = player.getZ();

        LocalWeatherField.Sample weather =
                LocalWeatherField.sample(px, pz, time);

        for (LocalWeatherField.CloudCell cell :
                LocalWeatherField.nearbyCells(px, pz, time, CLOUD_RENDER_RANGE)) {

            double dx = cell.x() - px;
            double dz = cell.z() - pz;
            double distance = Math.sqrt(dx * dx + dz * dz);

            /*
             * Fewer puffs far away, more when the cloud dominates the sky.
             */
            int puffs = distance < 260.0 ? 7 : 4;

            if (cell.storm() > 0.62F) {
                puffs += 2;
            }

            for (int i = 0; i < puffs; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2.0;
                double radial =
                        Math.sqrt(level.random.nextDouble())
                        * cell.radius()
                        * 0.86;

                double x = cell.x() + Math.cos(angle) * radial;
                double z = cell.z() + Math.sin(angle) * radial;

                /*
                 * Flatten the bottom slightly and let the top be messier.
                 */
                double y =
                        cell.y()
                        - 4.0
                        + level.random.nextDouble() * 13.0
                        + (1.0 - radial / cell.radius()) * 4.0;

                minecraft.particleEngine.createParticle(
                        WayAroundParticles.LIVING_CLOUD.get(),
                        x,
                        y,
                        z,
                        weather.windX() * 0.024,
                        cell.storm(),
                        weather.windZ() * 0.024
                );
            }
        }
    }

    private static void reactFoliage(Minecraft minecraft) {
        var level = minecraft.level;
        var player = minecraft.player;

        LocalWeatherField.Sample weather =
                LocalWeatherField.sample(
                        player.getX(),
                        player.getZ(),
                        level.getGameTime()
                );

        float wind = weather.warning();

        if (wind < 0.12F) {
            return;
        }

        int attempts =
                3
                + (int) (wind * 8.0F);

        for (int i = 0; i < attempts; i++) {
            int x =
                    player.getBlockX()
                    + level.random.nextInt(49)
                    - 24;

            int z =
                    player.getBlockZ()
                    + level.random.nextInt(49)
                    - 24;

            int top =
                    level.getHeight(
                            Heightmap.Types.WORLD_SURFACE,
                            x,
                            z
                    );

            BlockPos leaf = null;

            for (int y = top; y >= top - 8; y--) {
                BlockPos candidate = new BlockPos(x, y, z);

                if (level.getBlockState(candidate).is(BlockTags.LEAVES)) {
                    leaf = candidate;
                    break;
                }
            }

            if (leaf == null) {
                continue;
            }

            BlockPos air = leaf.above();

            if (!level.getBlockState(air).isAir()) {
                continue;
            }

            double speed =
                    0.035
                    + wind * 0.085;

            minecraft.particleEngine.createParticle(
                    WayAroundParticles.WIND_LEAF.get(),
                    leaf.getX() + level.random.nextDouble(),
                    leaf.getY() + 1.02,
                    leaf.getZ() + level.random.nextDouble(),
                    weather.windX() * speed,
                    0.012 + level.random.nextDouble() * 0.025,
                    weather.windZ() * speed
            );

            /*
             * Tiny local rustle. Many leaves together become a soft moving
             * canopy instead of one loud repeated block sound.
             */
            if (level.random.nextFloat() < 0.16F * wind) {
                level.playLocalSound(
                        leaf.getX() + 0.5,
                        leaf.getY() + 0.5,
                        leaf.getZ() + 0.5,
                        SoundEvents.GRASS_STEP,
                        SoundSource.AMBIENT,
                        0.035F + wind * 0.045F,
                        Mth.lerp(level.random.nextFloat(), 0.78F, 1.18F),
                        false
                );
            }
        }
    }
}
