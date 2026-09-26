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
    public static void render(RenderPlayerEvent.Post event) {
        State state = STATES.get(event.getEntity().getUUID());
        if (state == null || state.progress <= 0) return;

        int visible = Math.max(1, Math.min(
                PIXELS.size(),
                (int)Math.ceil(PIXELS.size() * state.progress)
        ));

        PlayerModel<?> model = event.getRenderer().getModel();
        PoseStack pose = event.getPoseStack();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        int rendered = renderPart(pose, model.body, Part.BODY, visible, 0);
        rendered = renderPart(pose, model.head, Part.HEAD, visible, rendered);
        rendered = renderPart(pose, model.leftArm, Part.LEFT_ARM, visible, rendered);
        renderPart(pose, model.rightArm, Part.RIGHT_ARM, visible, rendered);

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
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
        float width = part == Part.BODY || part == Part.HEAD ? 0.5F : 0.25F;
        float height = part == Part.HEAD ? 0.5F : 0.75F;
        int cols = part == Part.BODY || part == Part.HEAD ? 8 : 4;
        int rows = part == Part.HEAD ? 8 : 12;

        float px = width / cols;
        float py = height / rows;
        float x0 = -width * 0.5F + pixel.x * px;
        float y0 = pixel.y * py;
        float x1 = x0 + px * 0.82F;
        float y1 = y0 + py * 0.82F;
        float z = -0.1268F;

        int a = pixel.argb >>> 24 & 255;
        int r = pixel.argb >>> 16 & 255;
        int g = pixel.argb >>> 8 & 255;
        int blue = pixel.argb & 255;

        b.addVertex(matrix, x0, y0, z).setColor(r, g, blue, a);
        b.addVertex(matrix, x0, y1, z).setColor(r, g, blue, a);
        b.addVertex(matrix, x1, y1, z).setColor(r, g, blue, a);
        b.addVertex(matrix, x1, y0, z).setColor(r, g, blue, a);
    }

    private static List<Pixel> buildPixels() {
        List<Pixel> out = new ArrayList<>();

        for (int y : new int[]{2, 3, 7, 8}) {
            for (int x = 0; x < 8; x++) {
                if ((x + y) % 3 != 1) out.add(new Pixel(Part.BODY, x, y, 0xE8C91522));
            }
        }
        for (int y = 1; y < 11; y++) {
            out.add(new Pixel(Part.BODY, 2 + Math.floorMod(y, 3), y, 0xF0180A0D));
            out.add(new Pixel(Part.BODY, 5 - Math.floorMod(y, 3), y, 0xF0180A0D));
        }

        int[][] face = {
                {1,5},{2,5},{5,5},{6,5},
                {1,4},{6,4},{2,3},{5,3},
                {3,2},{4,2},{3,1},{4,1}
        };
        for (int[] p : face) out.add(new Pixel(Part.HEAD, p[0], p[1], 0xF0B70F1A));

        for (Part part : new Part[]{Part.LEFT_ARM, Part.RIGHT_ARM}) {
            for (int y : new int[]{3,4,8,9}) {
                for (int x = 0; x < 4; x++) {
                    out.add(new Pixel(part, x, y,
                            (x+y)%2==0 ? 0xE8C91522 : 0xE8180A0D));
                }
            }
        }
        return List.copyOf(out);
    }

    private enum Part { BODY, HEAD, LEFT_ARM, RIGHT_ARM }
    private record Pixel(Part part, int x, int y, int argb) {}
    private static final class State {
        float progress;
        float target;
        float step = 1.0F / 40.0F;
    }
}
