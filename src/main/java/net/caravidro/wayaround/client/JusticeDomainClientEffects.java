package net.caravidro.wayaround.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.JusticeDomainVisualPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Vector3f;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class JusticeDomainClientEffects {

    private JusticeDomainClientEffects() {
    }

    private static final int FORM_TICKS =
            34;

    private static final Map<UUID, Shell> SHELLS =
            new HashMap<>();

    private static final List<Fragment> FRAGMENTS =
            new ArrayList<>();

    private static int whiteningTicks;
    private static int whiteFlashTicks;
    private static boolean hadLevel;

    public static void receive(
            JusticeDomainVisualPayload payload
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        long tick =
                minecraft.level
                        .getGameTime();

        if (payload.action()
                == JusticeDomainVisualPayload.OPEN) {

            SHELLS.put(
                    payload.owner(),
                    new Shell(
                            payload.owner(),
                            new Vec3(
                                    payload.x(),
                                    payload.y(),
                                    payload.z()
                            ),
                            payload.radius(),
                            tick,
                            tick
                                    + Math.max(
                                    FORM_TICKS,
                                    payload.durationTicks()
                            )
                    )
            );

            whiteningTicks =
                    Math.max(
                            whiteningTicks,
                            FORM_TICKS
                    );

            return;
        }

        if (payload.action()
                == JusticeDomainVisualPayload.ENTER) {
            whiteFlashTicks =
                    Math.max(
                            whiteFlashTicks,
                            18
                    );
            return;
        }

        if (payload.action()
                == JusticeDomainVisualPayload.CLOSE) {

            Shell shell =
                    SHELLS.remove(
                            payload.owner()
                    );

            Vec3 center =
                    shell == null
                            ? new Vec3(
                            payload.x(),
                            payload.y(),
                            payload.z()
                    )
                            : shell.center;

            float radius =
                    shell == null
                            ? payload.radius()
                            : shell.radius;

            spawnFragments(
                    payload.owner(),
                    center,
                    radius
            );

            whiteFlashTicks =
                    Math.max(
                            whiteFlashTicks,
                            10
                    );
        }
    }

    public static List<ShellVisual> shells() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return List.of();
        }

        long tick =
                minecraft.level
                        .getGameTime();

        List<ShellVisual> result =
                new ArrayList<>();

        for (Shell shell :
                SHELLS.values()) {

            float formation =
                    Mth.clamp(
                            (tick - shell.openedAt)
                                    / (float) FORM_TICKS,
                            0.0F,
                            1.0F
                    );

            result.add(
                    new ShellVisual(
                            shell.owner,
                            shell.center,
                            shell.radius,
                            formation
                    )
            );
        }

        return result;
    }

    public static List<FragmentVisual> fragments() {
        List<FragmentVisual> result =
                new ArrayList<>(
                        FRAGMENTS.size()
                );

        for (Fragment fragment :
                FRAGMENTS) {

            float alpha =
                    fragment.restTicks <= 24
                            ? 1.0F
                            : Mth.clamp(
                            1.0F
                                    - (
                                    fragment.restTicks
                                            - 24
                            )
                                    / 34.0F,
                            0.0F,
                            1.0F
                    );

            result.add(
                    new FragmentVisual(
                            fragment.position,
                            fragment.size,
                            fragment.spin,
                            alpha
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
                SHELLS.clear();
                FRAGMENTS.clear();
                whiteningTicks = 0;
                whiteFlashTicks = 0;
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

        SHELLS.entrySet()
                .removeIf(
                        entry ->
                                tick > entry.getValue()
                                        .expiresAt
                );

        if (whiteningTicks > 0) {
            whiteningTicks--;
        }

        if (whiteFlashTicks > 0) {
            whiteFlashTicks--;
        }

        Iterator<Fragment> iterator =
                FRAGMENTS.iterator();

        while (iterator.hasNext()) {
            Fragment fragment =
                    iterator.next();

            if (!fragment.resting) {
                Vec3 velocity =
                        fragment.velocity.add(
                                0.0,
                                -0.035,
                                0.0
                        );

                Vec3 next =
                        fragment.position.add(
                                velocity
                        );

                int ground =
                        minecraft.level
                                .getHeight(
                                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                        Mth.floor(
                                                next.x
                                        ),
                                        Mth.floor(
                                                next.z
                                        )
                                );

                double groundY =
                        ground + 0.04;

                if (next.y <= groundY) {
                    fragment.position =
                            new Vec3(
                                    next.x,
                                    groundY,
                                    next.z
                            );

                    fragment.velocity =
                            Vec3.ZERO;

                    fragment.resting =
                            true;

                    for (int i = 0;
                         i < 3;
                         i++) {

                        minecraft.level
                                .addParticle(
                                        new DustParticleOptions(
                                                new Vector3f(
                                                        0.86F,
                                                        0.68F,
                                                        0.18F
                                                ),
                                                0.8F
                                        ),
                                        fragment.position.x,
                                        fragment.position.y + 0.08,
                                        fragment.position.z,
                                        (
                                                i - 1
                                        )
                                                * 0.015,
                                        0.025,
                                        (
                                                1 - i
                                        )
                                                * 0.012
                                );
                    }

                } else {
                    fragment.position =
                            next;

                    fragment.velocity =
                            velocity;

                    fragment.spin +=
                            fragment.spinSpeed;
                }

            } else {
                fragment.restTicks++;

                if (fragment.restTicks > 36
                        && fragment.restTicks % 4 == 0) {

                    minecraft.level
                            .addParticle(
                                    new DustParticleOptions(
                                            new Vector3f(
                                                    0.78F,
                                                    0.64F,
                                                    0.22F
                                            ),
                                            0.55F
                                    ),
                                    fragment.position.x,
                                    fragment.position.y + 0.06,
                                    fragment.position.z,
                                    0.0,
                                    0.015,
                                    0.0
                            );
                }

                if (fragment.restTicks > 58) {
                    iterator.remove();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onGui(
            RenderGuiLayerEvent.Post event
    ) {
        if (!event.getName()
                .equals(
                        VanillaGuiLayers.CAMERA_OVERLAYS
                )) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        GuiGraphics graphics =
                event.getGuiGraphics();

        int width =
                minecraft.getWindow()
                        .getGuiScaledWidth();

        int height =
                minecraft.getWindow()
                        .getGuiScaledHeight();

        int alpha =
                0;

        if (whiteningTicks > 0) {
            float progress =
                    1.0F
                            - whiteningTicks
                                    / (float) FORM_TICKS;

            alpha =
                    Math.max(
                            alpha,
                            Math.round(
                                    148.0F
                                            * progress
                                            * progress
                            )
                    );
        }

        if (whiteFlashTicks > 0) {
            float progress =
                    whiteFlashTicks
                            / 18.0F;

            alpha =
                    Math.max(
                            alpha,
                            Math.round(
                                    255.0F
                                            * Math.min(
                                            1.0F,
                                            progress
                                                    * 1.35F
                                    )
                            )
                    );
        }

        if (alpha > 0) {
            graphics.fill(
                    0,
                    0,
                    width,
                    height,
                    Mth.clamp(
                            alpha,
                            0,
                            255
                    )
                            << 24
                            | 0xFFFFFF
            );
        }
    }

    private static void spawnFragments(
            UUID owner,
            Vec3 center,
            float radius
    ) {
        Random random =
                new Random(
                        owner.getMostSignificantBits()
                                ^ owner.getLeastSignificantBits()
                );

        for (int i = 0;
             i < 72;
             i++) {

            double u =
                    random.nextDouble()
                            * 2.0
                            - 1.0;

            double theta =
                    random.nextDouble()
                            * Math.PI
                            * 2.0;

            double horizontal =
                    Math.sqrt(
                            Math.max(
                                    0.0,
                                    1.0
                                            - u * u
                            )
                    );

            Vec3 normal =
                    new Vec3(
                            Math.cos(theta)
                                    * horizontal,
                            u,
                            Math.sin(theta)
                                    * horizontal
                    );

            Vec3 position =
                    center.add(
                            normal.scale(
                                    radius
                            )
                    );

            Vec3 velocity =
                    normal.scale(
                            0.05
                                    + random.nextDouble()
                                            * 0.08
                    )
                            .add(
                                    0.0,
                                    0.04
                                            + random.nextDouble()
                                                    * 0.08,
                                    0.0
                            );

            FRAGMENTS.add(
                    new Fragment(
                            position,
                            velocity,
                            0.10F
                                    + random.nextFloat()
                                            * 0.22F,
                            random.nextFloat()
                                    * 360.0F,
                            (
                                    random.nextFloat()
                                            - 0.5F
                            )
                                    * 7.0F
                    )
            );
        }
    }

    public record ShellVisual(
            UUID owner,
            Vec3 center,
            float radius,
            float formation
    ) {
    }

    public record FragmentVisual(
            Vec3 position,
            float size,
            float spin,
            float alpha
    ) {
    }

    private record Shell(
            UUID owner,
            Vec3 center,
            float radius,
            long openedAt,
            long expiresAt
    ) {
    }

    private static final class Fragment {
        private Vec3 position;
        private Vec3 velocity;
        private final float size;
        private float spin;
        private final float spinSpeed;
        private boolean resting;
        private int restTicks;

        private Fragment(
                Vec3 position,
                Vec3 velocity,
                float size,
                float spin,
                float spinSpeed
        ) {
            this.position = position;
            this.velocity = velocity;
            this.size = size;
            this.spin = spin;
            this.spinSpeed = spinSpeed;
        }
    }
}
