package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.sounds.WayAroundSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Sparse positional ambience layered over vanilla soundscapes.
 *
 * <p>These are intentionally occasional signatures rather than permanent
 * loops: coast water, exposed high-altitude wind, and unsettling cave
 * resonance.</p>
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class EnvironmentalSoundscape {

    private static int seaWait =
            120;

    private static int mountainWait =
            160;

    private static int caveWait =
            220;

    private static int sampleCooldown;

    private static boolean nearOcean;

    private EnvironmentalSoundscape() {
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.isPaused()
                || minecraft.player == null
                || minecraft.level == null) {
            return;
        }

        BlockPos pos =
                minecraft.player.blockPosition();

        if (--sampleCooldown <= 0) {
            nearOcean =
                    sampleOcean(
                            minecraft,
                            pos
                    );

            sampleCooldown =
                    80;
        }

        boolean exposed =
                minecraft.level.canSeeSky(
                        pos.above()
                );

        boolean mountain =
                exposed
                        && pos.getY()
                                >= 142
                        && !AntarcticClientLighting.isAntarctic(
                                minecraft
                        );

        boolean cave =
                pos.getY()
                        <= 48
                        && !exposed;

        if (nearOcean
                && exposed) {
            if (--seaWait <= 0) {
                minecraft.level.playLocalSound(
                        minecraft.player.getX(),
                        minecraft.player.getY(),
                        minecraft.player.getZ(),
                        SoundEvents.WATER_AMBIENT,
                        SoundSource.AMBIENT,
                        0.42F,
                        0.82F
                                + minecraft.level.random.nextFloat()
                                        * 0.24F,
                        false
                );

                if (minecraft.level.random.nextFloat()
                        < 0.34F) {
                    minecraft.level.playLocalSound(
                            minecraft.player.getX()
                                    + (
                                    minecraft.level.random.nextDouble()
                                            - 0.5
                            )
                                            * 18.0,
                            minecraft.player.getY(),
                            minecraft.player.getZ()
                                    + (
                                    minecraft.level.random.nextDouble()
                                            - 0.5
                            )
                                            * 18.0,
                            SoundEvents.GENERIC_SPLASH,
                            SoundSource.AMBIENT,
                            0.30F,
                            0.72F
                                    + minecraft.level.random.nextFloat()
                                            * 0.30F,
                            false
                    );
                }

                seaWait =
                        90
                                + minecraft.level.random.nextInt(
                                181
                        );
            }

        } else {
            seaWait =
                    Math.min(
                            seaWait,
                            100
                    );
        }

        if (mountain) {
            if (--mountainWait <= 0) {
                minecraft.level.playLocalSound(
                        minecraft.player.getX(),
                        minecraft.player.getY(),
                        minecraft.player.getZ(),
                        WayAroundSounds.BLIZZARD_WIND.get(),
                        SoundSource.WEATHER,
                        0.16F
                                + minecraft.level.random.nextFloat()
                                        * 0.10F,
                        0.90F
                                + minecraft.level.random.nextFloat()
                                        * 0.16F,
                        false
                );

                mountainWait =
                        140
                                + minecraft.level.random.nextInt(
                                260
                        );
            }

        } else {
            mountainWait =
                    Math.min(
                            mountainWait,
                            160
                    );
        }

        if (cave) {
            if (--caveWait <= 0) {
                minecraft.level.playLocalSound(
                        minecraft.player.getX()
                                + (
                                minecraft.level.random.nextDouble()
                                        - 0.5
                        )
                                        * 12.0,
                        minecraft.player.getY()
                                + (
                                minecraft.level.random.nextDouble()
                                        - 0.5
                        )
                                        * 5.0,
                        minecraft.player.getZ()
                                + (
                                minecraft.level.random.nextDouble()
                                        - 0.5
                        )
                                        * 12.0,
                        SoundEvents.AMBIENT_CAVE.value(),
                        SoundSource.AMBIENT,
                        0.58F,
                        0.72F
                                + minecraft.level.random.nextFloat()
                                        * 0.34F,
                        false
                );

                caveWait =
                        260
                                + minecraft.level.random.nextInt(
                                620
                        );
            }

        } else {
            caveWait =
                    Math.min(
                            caveWait,
                            240
                    );
        }
    }

    private static boolean sampleOcean(
            Minecraft minecraft,
            BlockPos center
    ) {
        if (minecraft.level.getBiome(
                center
        ).is(
                BiomeTags.IS_OCEAN
        )) {
            return true;
        }

        int[] offsets = {
                -32,
                -16,
                16,
                32
        };

        for (int offset :
                offsets) {
            if (minecraft.level.getBiome(
                    center.offset(
                            offset,
                            0,
                            0
                    )
            ).is(
                    BiomeTags.IS_OCEAN
            )
                    || minecraft.level.getBiome(
                    center.offset(
                            0,
                            0,
                            offset
                    )
            ).is(
                    BiomeTags.IS_OCEAN
            )) {
                return true;
            }
        }

        return false;
    }
}
