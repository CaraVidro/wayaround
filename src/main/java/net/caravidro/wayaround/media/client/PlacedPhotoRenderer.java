package net.caravidro.wayaround.media.client;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.caravidro.wayaround.media.PhotoData;
import net.caravidro.wayaround.media.PlacedPhotoBlock;
import net.caravidro.wayaround.media.PlacedPhotoBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix4f;

public final class PlacedPhotoRenderer
        implements BlockEntityRenderer<PlacedPhotoBlockEntity> {

    private static final Map<String, CachedPhoto> CACHE =
            new HashMap<>();

    private static long lastCleanup;

    private final BlockRenderDispatcher blocks;

    public PlacedPhotoRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blocks =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            PlacedPhotoBlockEntity photo,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        if (photo.getLevel() == null
                || photo.photo()
                .isEmpty()) {
            return;
        }

        PhotoData.Info info =
                PhotoData.read(
                        photo.photo()
                )
                        .orElse(null);

        if (info == null) {
            return;
        }

        CachedPhoto cached =
                CACHE.get(
                        info.photoId()
                );

        if (cached == null) {
            cached =
                    load(
                            info.photoId()
                    );

            if (cached == null) {
                return;
            }

            CACHE.put(
                    info.photoId(),
                    cached
            );
        }

        cached.lastUsed =
                photo.getLevel()
                        .getGameTime();

        Direction facing =
                photo.getBlockState()
                        .getValue(
                                PlacedPhotoBlock.FACING
                        );

        pose.pushPose();
        pose.translate(
                0.5,
                0.5,
                0.5
        );

        switch (facing) {
            case SOUTH ->
                    pose.mulPose(
                            Axis.YP.rotationDegrees(
                                    180.0F
                            )
                    );

            case WEST ->
                    pose.mulPose(
                            Axis.YP.rotationDegrees(
                                    90.0F
                            )
                    );

            case EAST ->
                    pose.mulPose(
                            Axis.YP.rotationDegrees(
                                    -90.0F
                            )
                    );

            case UP ->
                    pose.mulPose(
                            Axis.XP.rotationDegrees(
                                    90.0F
                            )
                    );

            case DOWN ->
                    pose.mulPose(
                            Axis.XP.rotationDegrees(
                                    -90.0F
                            )
                    );

            default -> {
            }
        }

        float maxWidth =
                0.88F;

        float maxHeight =
                0.66F;

        float aspect =
                cached.width
                        / (float) Math.max(
                        1,
                        cached.height
                );

        float width =
                maxWidth;

        float height =
                width
                        / Math.max(
                        0.05F,
                        aspect
                );

        if (height > maxHeight) {
            height =
                    maxHeight;

            width =
                    height
                            * aspect;
        }

        width =
                Math.min(
                        width,
                        maxWidth
                );

        // Warm paper backing: a physical thin object on the wall.
        pose.pushPose();
        pose.translate(
                -(
                        width + 0.07F
                ) * 0.5F,
                -(
                        height + 0.07F
                ) * 0.5F,
                -0.497
        );
        pose.scale(
                width + 0.07F,
                height + 0.07F,
                0.025F
        );

        blocks.renderSingleBlock(
                Blocks.SMOOTH_SANDSTONE
                        .defaultBlockState(),
                pose,
                buffers,
                Math.max(
                        light,
                        0x00B000B0
                ),
                OverlayTexture.NO_OVERLAY
        );

        pose.popPose();

        Matrix4f matrix =
                pose.last()
                        .pose();

        VertexConsumer consumer =
                buffers.getBuffer(
                        RenderType.entityCutoutNoCull(
                                cached.location
                        )
                );

        float left =
                -width * 0.5F;

        float right =
                width * 0.5F;

        float bottom =
                -height * 0.5F;

        float top =
                height * 0.5F;

        float z =
                -0.514F;

        vertex(
                consumer,
                matrix,
                pose,
                left,
                bottom,
                z,
                0.0F,
                1.0F,
                light
        );

        vertex(
                consumer,
                matrix,
                pose,
                right,
                bottom,
                z,
                1.0F,
                1.0F,
                light
        );

        vertex(
                consumer,
                matrix,
                pose,
                right,
                top,
                z,
                1.0F,
                0.0F,
                light
        );

        vertex(
                consumer,
                matrix,
                pose,
                left,
                top,
                z,
                0.0F,
                0.0F,
                light
        );

        pose.popPose();

        long now =
                photo.getLevel()
                        .getGameTime();

        if (now - lastCleanup > 200L) {
            lastCleanup =
                    now;

            cleanup(
                    now
            );
        }
    }

    private static CachedPhoto load(
            String id
    ) {
        try {
            Path path =
                    Minecraft.getInstance()
                            .gameDirectory
                            .toPath()
                            .resolve(
                                    "wayaround-photos"
                            )
                            .resolve(
                                    id + ".png"
                            );

            if (!Files.isRegularFile(
                    path
            )) {
                return null;
            }

            try (
                    InputStream input =
                            Files.newInputStream(
                                    path
                            )
            ) {
                NativeImage image =
                        NativeImage.read(
                                input
                        );

                int width =
                        image.getWidth();

                int height =
                        image.getHeight();

                DynamicTexture texture =
                        new DynamicTexture(
                                image
                        );

                texture.setFilter(
                        false,
                        false
                );

                ResourceLocation location =
                        Minecraft.getInstance()
                                .getTextureManager()
                                .register(
                                        "wayaround_wall_photo_"
                                                + id.substring(
                                                0,
                                                Math.min(
                                                        8,
                                                        id.length()
                                                )
                                        ),
                                        texture
                                );

                return new CachedPhoto(
                        texture,
                        location,
                        width,
                        height
                );
            }

        } catch (Exception ignored) {
            return null;
        }
    }

    private static void cleanup(
            long now
    ) {
        Iterator<Map.Entry<String, CachedPhoto>> iterator =
                CACHE.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            CachedPhoto photo =
                    iterator.next()
                            .getValue();

            if (now - photo.lastUsed <= 400L) {
                continue;
            }

            Minecraft.getInstance()
                    .getTextureManager()
                    .release(
                            photo.location
                    );

            iterator.remove();
        }
    }

    private static void vertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            PoseStack pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int light
    ) {
        consumer.addVertex(
                        matrix,
                        x,
                        y,
                        z
                )
                .setColor(
                        255,
                        255,
                        255,
                        255
                )
                .setUv(
                        u,
                        v
                )
                .setOverlay(
                        OverlayTexture.NO_OVERLAY
                )
                .setLight(
                        light
                )
                .setNormal(
                        pose.last(),
                        0.0F,
                        0.0F,
                        -1.0F
                );
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    private static final class CachedPhoto {

        private final DynamicTexture texture;
        private final ResourceLocation location;
        private final int width;
        private final int height;

        private long lastUsed;

        private CachedPhoto(
                DynamicTexture texture,
                ResourceLocation location,
                int width,
                int height
        ) {
            this.texture =
                    texture;
            this.location =
                    location;
            this.width =
                    width;
            this.height =
                    height;
        }
    }
}
