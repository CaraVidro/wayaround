package net.caravidro.wayaround.client.weather;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.AntarcticClientLighting;
import net.caravidro.wayaround.particle.WayAroundParticles;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
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
 * Local weather presentation that is not part of the main cloud geometry.
 *
 * LivingCloudRenderer owns the actual voxel cloud mesh. This class keeps
 * foliage response and the close-range fog/smoke that appears while the
 * camera is physically inside a cloud.
 */
@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class LivingWeatherClient {

    private static int ticks;

    private LivingWeatherClient() {
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (!WorldFeatureRuntime.clientEnabled(WorldFeature.LIVING_WEATHER)) {
            return;
        }

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

        if (ticks % 2 == 0) {
            spawnInsideCloudFog(minecraft);
        }

        if (ticks % 3 == 0) {
            reactFoliage(minecraft);
        }
    }

    private static void spawnInsideCloudFog(Minecraft minecraft) {
        var level = minecraft.level;
        var player = minecraft.player;

        if (!LivingCloudRenderer.isInsideCloud(player.getEyePosition())) {
            return;
        }

        /*
         * The mesh shell becomes transparent when entered. These local
         * particles make the interior read as wet suspended fog/smoke.
         */
        int amount =
                2
                + level.random.nextInt(3);

        for (int i = 0; i < amount; i++) {
            double x =
                    player.getX()
                    + (level.random.nextDouble() - 0.5) * 7.0;

            double y =
                    player.getEyeY()
                    + (level.random.nextDouble() - 0.5) * 4.0;

            double z =
                    player.getZ()
                    + (level.random.nextDouble() - 0.5) * 7.0;

            level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.CLOUD,
                    x,
                    y,
                    z,
                    (level.random.nextDouble() - 0.5) * 0.012,
                    (level.random.nextDouble() - 0.5) * 0.006,
                    (level.random.nextDouble() - 0.5) * 0.012
            );
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
