package net.caravidro.wayaround.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.TukunaMarkS2CPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class TukunaMarkRenderer {

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final List<Pixel> PIXELS = buildPixels();

    private TukunaMarkRenderer() {}

    public static void receive(TukunaMarkS2CPayload payload) {
        State state = STATES.computeIfAbsent(payload.player(), id -> new State());
        state.target = payload.active() ? 1.0F : 0.0F;
        state.step = 1.0F / Math.max(1, payload.transitionTicks());
        if (payload.active() && state.progress <= 0) state.progress = Math.min(1, state.step);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            STATES.clear();
            return;
        }

        STATES.entrySet().removeIf(entry -> {
            State state = entry.getValue();
            if (state.progress < state.target) {
                state.progress = Math.min(state.target, state.progress + state.step);
            } else if (state.progress > state.target) {
                state.progress = Math.max(state.target, state.progress - state.step);
            }
            return state.target == 0 && state.progress == 0;
        });
    }

    @SubscribeEvent
    public static void hideFrozenReceptacle(RenderPlayerEvent.Pre event) {
        UUID rendered =
                event.getEntity()
                        .getUUID();

        // Only one avatar exists visually during possession. The real
        // receptacle ServerPlayer is frozen/spectating and therefore hidden;
        // Tukuna's single moving entity is rendered with this body's skin by
        // TukunaPossessionSkinMixin.
        if (TukunaPossessionClient.controllerForBody(rendered) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void render(RenderPlayerEvent.Post event) {
        State state = STATES.get(event.getEntity().getUUID());
        if (state == null || state.progress <= 0) return;

        int visible = Math.max(1, Math.min(
                PIXELS.size(),
                (int)Math.ceil(PIXELS.size() * state.progress)
        ));

        PlayerModel<?> model = event.getRenderer().getModel();
        PoseStack pose = event.getPoseStack();

        /*
         * RenderPlayerEvent.Post exposes the entity pose stack after the
         * vanilla humanoid body transform has been unwound. ModelPart
         * coordinates still assume the vanilla ~24px-tall humanoid space,
         * so without this translation the complete rune silhouette appears
         * about one player-height below the actual skin.
         */
        pose.pushPose();
        pose.translate(0.0D, -1.501D, 0.0D);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        int rendered = renderPart(pose, model.body, Part.BODY, visible, 0);
        rendered = renderPart(pose, model.head, Part.HEAD, visible, rendered);
        rendered = renderPart(pose, model.leftArm, Part.LEFT_ARM, visible, rendered);
        rendered = renderPart(pose, model.rightArm, Part.RIGHT_ARM, visible, rendered);
        rendered = renderPart(pose, model.leftLeg, Part.LEFT_LEG, visible, rendered);
        renderPart(pose, model.rightLeg, Part.RIGHT_LEG, visible, rendered);

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();

        pose.popPose();
    }

    private static int renderPart(
            PoseStack pose,
            ModelPart part,
            Part wanted,
            int visible,
            int rendered
    ) {
        if (rendered >= visible) return rendered;

        List<Pixel> local = new ArrayList<>();
        for (Pixel p : PIXELS) if (p.part == wanted) local.add(p);
        if (local.isEmpty()) return rendered;

        pose.pushPose();
        part.translateAndRotate(pose);

        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR
        );
        Matrix4f matrix = pose.last().pose();

        int emitted = 0;
        for (Pixel p : local) {
            if (rendered + emitted >= visible) break;
            emit(buffer, matrix, wanted, p);
            emitted++;
        }

        if (emitted > 0) {
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }

        pose.popPose();
        return rendered + emitted;
    }

    private static void emit(
            BufferBuilder b,
            Matrix4f matrix,
            Part part,
            Pixel pixel
    ) {
        Bounds bounds =
                bounds(
                        part
                );

        int cols =
                part == Part.BODY || part == Part.HEAD
                        ? 8
                        : 4;

        int rows =
                part == Part.HEAD
                        ? 8
                        : 12;

        float px =
                (bounds.maxX - bounds.minX)
                        / cols;

        float py =
                (bounds.maxY - bounds.minY)
                        / rows;

        float x0 =
                bounds.minX
                        + pixel.x
                                * px;

        float y0 =
                bounds.minY
                        + pixel.y
                                * py;

        // Nearly connected pixels read as painted/tattooed lines instead of
        // detached glowing LEDs.
        float x1 =
                x0
                        + px
                                * 0.96F;

        float y1 =
                y0
                        + py
                                * 0.96F;

        float z =
                bounds.frontZ;

        int a = pixel.argb >>> 24 & 255;
        int r = pixel.argb >>> 16 & 255;
        int g = pixel.argb >>> 8 & 255;
        int blue = pixel.argb & 255;

        b.addVertex(matrix, x0, y0, z).setColor(r, g, blue, a);
        b.addVertex(matrix, x0, y1, z).setColor(r, g, blue, a);
        b.addVertex(matrix, x1, y1, z).setColor(r, g, blue, a);
        b.addVertex(matrix, x1, y0, z).setColor(r, g, blue, a);
    }

    private static Bounds bounds(
            Part part
    ) {
        return switch (part) {
            case BODY ->
                    new Bounds(
                            -0.2500F,
                            0.2500F,
                            0.0000F,
                            0.7500F,
                            -0.1425F
                    );

            case HEAD ->
                    new Bounds(
                            -0.2500F,
                            0.2500F,
                            -0.5000F,
                            0.0000F,
                            -0.2835F
                    );

            // Vanilla arm cubes are not centered on the part pivot.
            case LEFT_ARM ->
                    new Bounds(
                            -0.0625F,
                            0.1875F,
                            -0.1250F,
                            0.6250F,
                            -0.1425F
                    );

            case RIGHT_ARM ->
                    new Bounds(
                            -0.1875F,
                            0.0625F,
                            -0.1250F,
                            0.6250F,
                            -0.1425F
                    );

            case LEFT_LEG, RIGHT_LEG ->
                    new Bounds(
                            -0.1250F,
                            0.1250F,
                            0.0000F,
                            0.7500F,
                            -0.1425F
                    );
        };
    }

    private static List<Pixel> buildPixels() {
        List<Pixel> out =
                new ArrayList<>();

        int ink =
                0xEE26070C;

        int crimson =
                0xE05D0B16;

        int accent =
                0xD8881422;

        // Torso: one central sigil with two restrained bands. This is
        // deliberately sparse so the skin still reads as the receptacle.
        for (int x = 1; x <= 6; x++) {
            if (x != 3 && x != 4) {
                out.add(new Pixel(Part.BODY, x, 3, crimson));
                out.add(new Pixel(Part.BODY, x, 8, ink));
            }
        }

        for (int y = 1; y <= 10; y++) {
            if (y != 5 && y != 6) {
                out.add(new Pixel(Part.BODY, 3, y, ink));
                if ((y & 1) == 0) {
                    out.add(new Pixel(Part.BODY, 4, y, crimson));
                }
            }
        }

        out.add(new Pixel(Part.BODY, 2, 2, accent));
        out.add(new Pixel(Part.BODY, 5, 2, accent));
        out.add(new Pixel(Part.BODY, 2, 9, crimson));
        out.add(new Pixel(Part.BODY, 5, 9, crimson));

        // Face: cheek slashes + a short forehead mark.
        int[][] face = {
                {1,5},{2,5},
                {5,5},{6,5},
                {2,4},{5,4},
                {3,1},{4,1},
                {3,2},{4,2}
        };

        for (int[] p : face) {
            out.add(
                    new Pixel(
                            Part.HEAD,
                            p[0],
                            p[1],
                            p[1] <= 2
                                    ? ink
                                    : crimson
                    )
            );
        }

        // Arms: continuous bands rather than checkerboard speckles.
        for (Part part : new Part[]{Part.LEFT_ARM, Part.RIGHT_ARM}) {
            for (int y : new int[]{3, 8}) {
                for (int x = 0; x < 4; x++) {
                    out.add(
                            new Pixel(
                                    part,
                                    x,
                                    y,
                                    y == 3
                                            ? crimson
                                            : ink
                            )
                    );
                }
            }

            out.add(new Pixel(part, 1, 5, accent));
            out.add(new Pixel(part, 2, 6, ink));
        }

        // Legs: two lower bands and a small diagonal rune.
        for (Part part : new Part[]{Part.LEFT_LEG, Part.RIGHT_LEG}) {
            for (int y : new int[]{8, 9}) {
                for (int x = 0; x < 4; x++) {
                    out.add(
                            new Pixel(
                                    part,
                                    x,
                                    y,
                                    y == 8
                                            ? ink
                                            : crimson
                            )
                    );
                }
            }

            out.add(new Pixel(part, 1, 4, accent));
            out.add(new Pixel(part, 2, 5, crimson));
        }

        return List.copyOf(
                out
        );
    }

    private record Bounds(
            float minX,
            float maxX,
            float minY,
            float maxY,
            float frontZ
    ) {}

    private enum Part { BODY, HEAD, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG }
    private record Pixel(Part part, int x, int y, int argb) {}
    private static final class State {
        float progress;
        float target;
        float step = 1.0F / 40.0F;
    }
}
