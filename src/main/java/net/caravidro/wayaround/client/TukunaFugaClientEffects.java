package net.caravidro.wayaround.client;

import net.minecraft.sounds.SoundSource;

import net.minecraft.sounds.SoundEvents;

import net.caravidro.wayaround.client.cinematic.CinematicCameraController;
import net.caravidro.wayaround.client.cinematic.PlayerAnimationController;
import net.caravidro.wayaround.network.PlayerCinematicPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.TukunaFugaVisualPayload;
import net.caravidro.wayaround.client.debris.DebrisSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class TukunaFugaClientEffects {

    private TukunaFugaClientEffects() {
    }

    private static final Map<UUID, Pillar> PILLARS =
            new HashMap<>();

    private static boolean hadLevel;

    public static void receive(
            TukunaFugaVisualPayload payload
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        long tick =
                minecraft.level
                        .getGameTime();

        PILLARS.put(
                payload.owner(),
                new Pillar(
                        payload.owner(),
                        new Vec3(
                                payload.x(),
                                payload.y(),
                                payload.z()
                        ),
                        tick,
                        tick
                                + Math.max(
                                20,
                                payload.durationTicks()
                        )
                )
        );

        DebrisSystem.spawnFugaBurst(
                new Vec3(
                        payload.x(),
                        payload.y(),
                        payload.z()
                )
        );

        /*
         * 64 volume gives the positional explosion an attenuation radius of
         * roughly 1024 blocks, matching the visual packet radius.
         */
        minecraft.level.playLocalSound(
                payload.x(),
                payload.y(),
                payload.z(),
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS,
                64.0F,
                0.42F,
                false
        );

        if (minecraft.player != null) {
            Vec3 center =
                    new Vec3(
                            payload.x(),
                            payload.y(),
                            payload.z()
                    );

            double distance =
                    minecraft.player
                            .position()
                            .distanceTo(
                                    center
                            );

            if (distance <= 260.0) {
                float proximity =
                        Mth.clamp(
                                1.0F
                                        - (float) (
                                        distance / 260.0
                                ),
                                0.0F,
                                1.0F
                        );

                CinematicCameraController.shake(
                        100,
                        0.45F
                                + proximity
                                        * 1.55F
                );
            }
        }
    }

    public static List<PillarVisual> visuals() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return List.of();
        }

        long tick =
                minecraft.level
                        .getGameTime();

        ArrayList<PillarVisual> result =
                new ArrayList<>();

        for (Pillar pillar :
                PILLARS.values()) {

            long duration =
                    Math.max(
                            1L,
                            pillar.expiresAt
                                    - pillar.startedAt
                    );

            float age =
                    tick
                            - pillar.startedAt;

            float progress =
                    Mth.clamp(
                            age
                                    / duration,
                            0.0F,
                            1.0F
                    );

            float birth =
                    Mth.clamp(
                            age / 12.0F,
                            0.0F,
                            1.0F
                    );

            float fade =
                    progress < 0.72F
                            ? 1.0F
                            : Mth.clamp(
                            1.0F
                                    - (
                                    progress - 0.72F
                            )
                                    / 0.28F,
                            0.0F,
                            1.0F
                    );

            result.add(
                    new PillarVisual(
                            pillar.owner,
                            pillar.center,
                            birth,
                            fade,
                            age
                    )
            );
        }

        return result;
    }

    @SubscribeEvent
    public static void onTick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            if (hadLevel) {
                PILLARS.clear();
            }

            hadLevel =
                    false;

            return;
        }

        hadLevel =
                true;

        long tick =
                minecraft.level
                        .getGameTime();

        if (tick % 3L == 0L && minecraft.player != null) {
            for (var player : minecraft.level.players()) {
                if (PlayerAnimationController.animationAge(player.getUUID(),
                        PlayerCinematicPayload.FUGA_CHARGE) <= 16
                        || player.distanceToSqr(minecraft.player) > 96 * 96) continue;
                Vec3 bow = player.getEyePosition().add(player.getLookAngle().scale(1.8));
                minecraft.level.addParticle(tick % 9L == 0L
                                ? ParticleTypes.LAVA : ParticleTypes.FLAME,
                        bow.x, bow.y - .2 + minecraft.level.random.nextDouble() * 1.7,
                        bow.z, 0, .07, 0);
            }
        }

        Iterator<Map.Entry<UUID, Pillar>> iterator =
                PILLARS.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Pillar pillar =
                    iterator.next()
                            .getValue();

            if (tick > pillar.expiresAt) {
                iterator.remove();
                continue;
            }

            if (tick % 2L == 0L) {
                float phase =
                        (float) (
                                tick
                                        + pillar.owner.hashCode()
                                                * 0.001
                        );

                for (int i = 0;
                     i < 4;
                     i++) {

                    double angle =
                            phase
                                    * 0.33
                                    + i
                                            * Math.PI
                                            * 0.5;

                    double radius =
                            2.0
                                    + i
                                            * 0.45;

                    minecraft.level
                            .addParticle(
                                    i % 2 == 0
                                            ? ParticleTypes.FLAME
                                            : ParticleTypes.END_ROD,
                                    pillar.center.x
                                            + Math.cos(angle)
                                                    * radius,
                                    pillar.center.y
                                            + 2.0
                                            + minecraft.level.random
                                                    .nextDouble()
                                                    * 8.0,
                                    pillar.center.z
                                            + Math.sin(angle)
                                                    * radius,
                                    0.0,
                                    0.10
                                            + minecraft.level.random
                                                    .nextDouble()
                                                    * 0.18,
                                    0.0
                            );
                }
            }
        }
    }

    public record PillarVisual(
            UUID owner,
            Vec3 center,
            float birth,
            float fade,
            float age
    ) {
    }

    private record Pillar(
            UUID owner,
            Vec3 center,
            long startedAt,
            long expiresAt
    ) {
    }
}
