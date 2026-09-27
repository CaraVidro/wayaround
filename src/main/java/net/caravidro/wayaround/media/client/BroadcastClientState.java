package net.caravidro.wayaround.media.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import net.caravidro.wayaround.media.broadcast.BroadcastEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class BroadcastClientState {
    private static final Map<Long, LiveTexture> TEXTURES = new HashMap<>();
    private static long frameCounter;

    private BroadcastClientState() {}

    public static void receiveImage(
            BlockPos television,
            int width,
            int height,
            byte[] rgb,
            float quality,
            int effectOrdinal
    ) {
        if (width <= 0 || height <= 0 || width > 64 || height > 64
                || rgb == null || rgb.length != width * height * 3) {
            return;
        }

        BroadcastEffect[] effects = BroadcastEffect.values();
        BroadcastEffect effect = effects[Math.max(0, Math.min(effects.length - 1, effectOrdinal))];

        LiveTexture texture = TEXTURES.get(television.asLong());

        if (texture == null || texture.width != width || texture.height != height) {
            if (texture != null) texture.close();

            NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, false);
            DynamicTexture dynamic = new DynamicTexture(image);
            dynamic.setFilter(false, false);

            ResourceLocation location = Minecraft.getInstance()
                    .getTextureManager()
                    .register("wayaround_live_tv_" + Long.toUnsignedString(television.asLong()), dynamic);

            texture = new LiveTexture(width, height, image, dynamic, location);
            TEXTURES.put(television.asLong(), texture);
        }

        float q = Math.max(0.0F, Math.min(1.0F, quality));

        for (int y = 0; y < height; y++) {
            int shift = effect == BroadcastEffect.GLITCH && (y + frameCounter) % 7 == 0
                    ? (int) ((frameCounter + y) % 5) - 2
                    : 0;

            for (int x = 0; x < width; x++) {
                int sourceX = Math.max(0, Math.min(width - 1, x + shift));
                int i = (y * width + sourceX) * 3;

                int r = rgb[i] & 0xFF;
                int g = rgb[i + 1] & 0xFF;
                int b = rgb[i + 2] & 0xFF;

                int hash = x * 73428767 ^ y * 912931 ^ (int) frameCounter * 19937;
                int noise = Math.floorMod(hash, 47) - 23;

                r = clamp((int) (r * q + (128 + noise) * (1.0F - q)));
                g = clamp((int) (g * q + (128 + noise) * (1.0F - q)));
                b = clamp((int) (b * q + (128 + noise) * (1.0F - q)));

                if (effect == BroadcastEffect.DISTANT) {
                    int gray = (r + g + b) / 3;
                    r = (r + gray * 2) / 3;
                    g = (g + gray * 2) / 3;
                    b = (b + gray * 2) / 3;
                }

                // NativeImage stores channels as ABGR in the packed integer.
                int packed = 0xFF000000 | (b << 16) | (g << 8) | r;
                texture.image.setPixelRGBA(x, y, packed);
            }
        }

        if (effect == BroadcastEffect.VHS) {
            VhsFilter.apply(texture.image, (int) frameCounter, Long.hashCode(television.asLong()));
        }

        texture.texture.upload();
        texture.lastFrameNanos = System.nanoTime();
        frameCounter++;
    }

    public static ResourceLocation texture(BlockPos television) {
        LiveTexture texture = TEXTURES.get(television.asLong());
        if (texture == null) return null;

        if (System.nanoTime() - texture.lastFrameNanos > 3_000_000_000L) {
            return null;
        }

        return texture.location;
    }

    public static void cleanup() {
        long now = System.nanoTime();

        Iterator<Map.Entry<Long, LiveTexture>> iterator = TEXTURES.entrySet().iterator();
        while (iterator.hasNext()) {
            LiveTexture texture = iterator.next().getValue();
            if (now - texture.lastFrameNanos > 12_000_000_000L) {
                texture.close();
                iterator.remove();
            }
        }
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static final class LiveTexture {
        private final int width;
        private final int height;
        private final NativeImage image;
        private final DynamicTexture texture;
        private final ResourceLocation location;
        private long lastFrameNanos;

        private LiveTexture(
                int width,
                int height,
                NativeImage image,
                DynamicTexture texture,
                ResourceLocation location
        ) {
            this.width = width;
            this.height = height;
            this.image = image;
            this.texture = texture;
            this.location = location;
            this.lastFrameNanos = System.nanoTime();
        }

        private void close() {
            texture.close();
        }
    }
}
