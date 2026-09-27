package net.caravidro.wayaround.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.HashMap;
import java.util.Map;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.FrostPayload;
import net.caravidro.wayaround.worldgen.weather.frost.FrostLayers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class FrostRenderer {

    /*
     * =========================================================
     * CONFIG
     * =========================================================
     */

    private static final int MAX_RENDERED_BLOCKS = 8192;

    private static final double MAX_DISTANCE =
            64.0;

    private static final double MAX_DISTANCE_SQR =
            MAX_DISTANCE * MAX_DISTANCE;

    private static final Direction[] DIRECTIONS =
            Direction.values();

    private static final ResourceLocation FROST_TEXTURE =
            ResourceLocation.withDefaultNamespace(
                    "textures/block/snow.png"
            );

    private static final RenderType TYPE =
            NeoForgeRenderTypes.getTranslucentParticlesTarget(
                    FROST_TEXTURE
            );

    /*
     * =========================================================
     * CLIENT CACHE
     * =========================================================
     *
     * chunk long
     *      ↓
     * posição
     *      ↓
     * informação de frost
     */

    private static final Map<
            Long,
            Map<BlockPos, FrostPayload.Entry>
            > CHUNKS =
            new HashMap<>();

    private static ClientLevel world;

    private FrostRenderer() {
    }

    /*
     * =========================================================
     * NETWORK
     * =========================================================
     */

    public static void receive(
            FrostPayload payload
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        ClientLevel current =
                minecraft.level;

        if (current == null) {
            return;
        }

        /*
         * Payload de outra dimensão.
         */
        if (
                !current
                        .dimension()
                        .location()
                        .equals(
                                payload.dimension()
                        )
        ) {
            return;
        }

        /*
         * Mudou de mundo.
         */
        if (world != current) {

            CHUNKS.clear();

            world =
                    current;
        }

        /*
         * Snapshot completo daquele chunk.
         */
        if (payload.replace()) {

            CHUNKS.remove(
                    payload.chunk()
            );
        }

        if (
                payload
                        .entries()
                        .isEmpty()
        ) {
            return;
        }

        Map<BlockPos, FrostPayload.Entry> entries =
                CHUNKS.computeIfAbsent(
                        payload.chunk(),
                        ignored -> new HashMap<>()
                );

        for (
                FrostPayload.Entry entry :
                payload.entries()
        ) {

            BlockPos pos =
                    entry
                            .pos()
                            .immutable();

            /*
             * faces == 0
             *
             * Frost removido.
             */
            if (entry.faces() == 0) {

                entries.remove(
                        pos
                );

                continue;
            }

            entries.put(
                    pos,
                    entry
            );
        }

        if (entries.isEmpty()) {

            CHUNKS.remove(
                    payload.chunk()
            );
        }
    }

    /*
     * =========================================================
     * LOGOUT
     * =========================================================
     */

    @SubscribeEvent
    public static void logout(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {

        CHUNKS.clear();

        world =
                null;
    }

    /*
     * =========================================================
     * RENDER
     * =========================================================
     */

    @SubscribeEvent
    public static void render(
            RenderLevelStageEvent event
    ) {

        /*
         * Renderizamos depois dos blocos translucent.
         */
        if (
                event.getStage()
                        !=
                        RenderLevelStageEvent.Stage
                                .AFTER_PARTICLES
        ) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        ClientLevel current =
                minecraft.level;

        if (current == null) {

            CHUNKS.clear();

            world =
                    null;

            return;
        }

        /*
         * Mundo acabou de mudar.
         */
        if (world != current) {

            CHUNKS.clear();

            world =
                    current;

            return;
        }

        if (CHUNKS.isEmpty()) {
            return;
        }

        Vec3 camera =
                event
                        .getCamera()
                        .getPosition();

        PoseStack pose =
                event
                        .getPoseStack();

        var buffers =
                minecraft
                        .renderBuffers()
                        .bufferSource();

        VertexConsumer vertices =
                buffers
                        .getBuffer(
                                TYPE
                        );

        RandomSource random =
                RandomSource.create();

        int drawn =
                0;

        /*
         * =====================================================
         * CHUNKS
         * =====================================================
         */

        for (
                Map<BlockPos, FrostPayload.Entry> chunk :
                CHUNKS.values()
        ) {

            for (
                    FrostPayload.Entry entry :
                    chunk.values()
            ) {

                BlockPos pos =
                        entry.pos();

                /*
                 * Distância.
                 */
                if (
                        pos.distToCenterSqr(
                                camera
                        )
                                >
                                MAX_DISTANCE_SQR
                ) {
                    continue;
                }

                /*
                 * Chunk não carregado.
                 */
                if (
                        !world.hasChunkAt(
                                pos
                        )
                ) {
                    continue;
                }

                /*
                 * Frustum culling.
                 */
                AABB renderBox =
                        new AABB(
                                pos
                        ).inflate(
                                0.01
                        );

                if (
                        event.getFrustum() != null
                                &&
                                !event
                                        .getFrustum()
                                        .isVisible(
                                                renderBox
                                        )
                ) {
                    continue;
                }

                BlockState state =
                        world.getBlockState(
                                pos
                        );

                /*
                 * Old worlds may still contain cached frost entries from
                 * before foliage was excluded server-side. Never render the
                 * white coat on living leaves, even for stale snapshots.
                 */
                if (state.is(
                        BlockTags.LEAVES
                )) {
                    continue;
                }

                /*
                 * O bloco mudou desde o snapshot.
                 *
                 * Comparamos o BLOCO, não todas as propriedades.
                 *
                 * Assim:
                 *
                 * chest facing norte
                 *     ↓
                 * chest facing sul
                 *
                 * continua podendo manter frost.
                 */
                BlockState snapshotState =
                        Block.stateById(
                                entry.state()
                        );

                if (
                        snapshotState == null
                                ||
                                state.getBlock()
                                        !=
                                        snapshotState.getBlock()
                ) {
                    continue;
                }

                pose.pushPose();

                pose.translate(
                        pos.getX()
                                -
                                camera.x,

                        pos.getY()
                                -
                                camera.y,

                        pos.getZ()
                                -
                                camera.z
                );

                BakedModel model =
                        minecraft
                                .getBlockRenderer()
                                .getBlockModel(
                                        state
                                );

                /*
                 * Muito importante para modelos NeoForge.
                 */
                ModelData modelData =
                        model.getModelData(
                                world,
                                pos,
                                state,
                                world.getModelDataManager().getAt(pos)
                        );

                boolean hasModelQuads =
                        false;

                long seed =
                        state.getSeed(
                                pos
                        );

                /*
                 * =================================================
                 * QUADS DAS 6 FACES
                 * =================================================
                 */

                for (
                        Direction direction :
                        DIRECTIONS
                ) {

                    random.setSeed(
                            seed
                    );

                    var quads =
                            model.getQuads(
                                    state,
                                    direction,
                                    random,
                                    modelData,
                                    null
                            );

                    if (!quads.isEmpty()) {
                        hasModelQuads = true;
                    }

                    renderQuads(
                            vertices,
                            pose,
                            entry,
                            pos,
                            quads
                    );
                }

                /*
                 * =================================================
                 * QUADS NÃO ASSOCIADOS A UMA FACE
                 * =================================================
                 *
                 * Alguns modelos colocam geometria aqui.
                 */

                random.setSeed(
                        seed
                );

                var generalQuads =
                        model.getQuads(
                                state,
                                null,
                                random,
                                modelData,
                                null
                        );

                if (!generalQuads.isEmpty()) {
                    hasModelQuads = true;
                }

                renderQuads(
                        vertices,
                        pose,
                        entry,
                        pos,
                        generalQuads
                );

                /*
                 * =================================================
                 * FALLBACK
                 * =================================================
                 *
                 * Chest e vários BlockEntities podem não possuir
                 * geometria útil no BakedModel.
                 */

                if (!hasModelQuads) {

                    for (
                            AABB box :
                            state
                                    .getShape(
                                            world,
                                            pos
                                    )
                                    .toAabbs()
                    ) {

                        for (
                                Direction direction :
                                DIRECTIONS
                        ) {

                            int layers =
                                    visibleLayers(
                                            entry.faces(),
                                            pos,
                                            direction
                                    );

                            if (layers <= 0) {
                                continue;
                            }

                            int light =
                                    getFaceLight(
                                            pos,
                                            direction
                                    );

                            boxFace(
                                    vertices,
                                    pose.last(),
                                    box,
                                    direction,
                                    layers,
                                    light
                            );
                        }
                    }
                }

                pose.popPose();

                drawn++;

                /*
                 * Proteção contra o glorioso evento:
                 *
                 * "meu FPS virou powerpoint".
                 */
                if (
                        drawn
                                >=
                                MAX_RENDERED_BLOCKS
                ) {

                    buffers.endBatch(
                            TYPE
                    );

                    return;
                }
            }
        }

        buffers.endBatch(
                TYPE
        );
    }

    /*
     * =========================================================
     * QUADS
     * =========================================================
     */

    private static void renderQuads(
            VertexConsumer vertices,
            PoseStack pose,
            FrostPayload.Entry entry,
            BlockPos pos,
            Iterable<BakedQuad> quads
    ) {

        for (
                BakedQuad quad :
                quads
        ) {

            Direction face =
                    quad.getDirection();

            int layers =
                    visibleLayers(
                            entry.faces(),
                            pos,
                            face
                    );

            if (layers <= 0) {
                continue;
            }

            int light =
                    getFaceLight(
                            pos,
                            face
                    );

            int[] data =
                    quad.getVertices();

            /*
             * Minecraft usa quatro vértices por quad.
             */
            int stride =
                    data.length / 4;

            /*
             * Precisamos pelo menos de XYZ.
             */
            if (stride < 3) {
                continue;
            }

            for (
                    int vertexIndex = 0;
                    vertexIndex < 4;
                    vertexIndex++
            ) {

                int offset =
                        vertexIndex
                                *
                                stride;

                float x =
                        Float.intBitsToFloat(
                                data[offset]
                        );

                float y =
                        Float.intBitsToFloat(
                                data[offset + 1]
                        );

                float z =
                        Float.intBitsToFloat(
                                data[offset + 2]
                        );

                vertex(
                        vertices,
                        pose.last(),
                        face,

                        x,
                        y,
                        z,

                        layers,
                        light
                );
            }
        }
    }

    /*
     * =========================================================
     * VISIBILIDADE DA FACE
     * =========================================================
     */

    private static int visibleLayers(
            int mask,
            BlockPos pos,
            Direction face
    ) {

        if (world == null) {
            return 0;
        }

        int layers =
                FrostLayers.get(
                        mask,
                        face.ordinal()
                );

        if (layers <= 0) {
            return 0;
        }

        BlockPos neighbor =
                pos.relative(
                        face
                );

        /*
         * Não renderiza apontando para chunk inexistente.
         */
        if (
                !world.hasChunkAt(
                        neighbor
                )
        ) {
            return 0;
        }

        BlockState neighborState =
                world.getBlockState(
                        neighbor
                );

        /*
         * Se existe um cubo sólido inteiro colado nessa face,
         * ela está escondida.
         */
        if (
                neighborState.isSolidRender(
                        world,
                        neighbor
                )
        ) {
            return 0;
        }

        return layers;
    }

    /*
     * =========================================================
     * LUZ
     * =========================================================
     */

    private static int getFaceLight(
            BlockPos pos,
            Direction face
    ) {

        BlockPos lightPos =
                pos.relative(
                        face
                );

        return LevelRenderer.getLightColor(
                world,
                lightPos
        );
    }

    /*
     * =========================================================
     * VERTEX
     * =========================================================
     */

    private static void vertex(
            VertexConsumer out,
            PoseStack.Pose pose,
            Direction face,

            float x,
            float y,
            float z,

            int layers,
            int light
    ) {

        /*
         * UV projetado conforme a orientação da face.
         */
        float u;

        float v;

        switch (
                face.getAxis()
        ) {

            case X -> {

                u =
                        z;

                v =
                        1.0F
                                -
                                y;
            }

            case Y -> {

                u =
                        x;

                v =
                        z;
            }

            case Z -> {

                u =
                        x;

                v =
                        1.0F
                                -
                                y;
            }

            default -> {

                u =
                        x;

                v =
                        y;
            }
        }

        /*
         * Pequeno afastamento para evitar z-fighting.
         *
         * Quanto mais frost, ligeiramente mais para fora.
         */
        float offset =
                0.0015F
                        +
                        Math.min(
                                layers,
                                4
                        )
                                *
                                0.00045F;

        /*
         * Opacidade progressiva.
         */
        float alpha =
                Math.min(
                        0.97F,
                        0.14F
                                + layers
                                        * 0.205F
                );

        out.addVertex(
                        pose.pose(),

                        x
                                +
                                face.getStepX()
                                        *
                                        offset,

                        y
                                +
                                face.getStepY()
                                        *
                                        offset,

                        z
                                +
                                face.getStepZ()
                                        *
                                        offset
                )

                .setColor(
                        1.0F,
                        1.0F,
                        1.0F,
                        alpha
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
                        pose,

                        face.getStepX(),
                        face.getStepY(),
                        face.getStepZ()
                );
    }

    /*
     * =========================================================
     * FALLBACK BOX FACE
     * =========================================================
     */

    private static void boxFace(
            VertexConsumer out,
            PoseStack.Pose pose,

            AABB box,

            Direction face,

            int layers,
            int light
    ) {

        float x0 =
                (float) box.minX;

        float x1 =
                (float) box.maxX;

        float y0 =
                (float) box.minY;

        float y1 =
                (float) box.maxY;

        float z0 =
                (float) box.minZ;

        float z1 =
                (float) box.maxZ;

        float[][] points =
                switch (face) {

                    case UP ->
                            new float[][]{
                                    {x0, y1, z0},
                                    {x0, y1, z1},
                                    {x1, y1, z1},
                                    {x1, y1, z0}
                            };

                    case DOWN ->
                            new float[][]{
                                    {x0, y0, z1},
                                    {x0, y0, z0},
                                    {x1, y0, z0},
                                    {x1, y0, z1}
                            };

                    case NORTH ->
                            new float[][]{
                                    {x1, y0, z0},
                                    {x0, y0, z0},
                                    {x0, y1, z0},
                                    {x1, y1, z0}
                            };

                    case SOUTH ->
                            new float[][]{
                                    {x0, y0, z1},
                                    {x1, y0, z1},
                                    {x1, y1, z1},
                                    {x0, y1, z1}
                            };

                    case WEST ->
                            new float[][]{
                                    {x0, y0, z0},
                                    {x0, y0, z1},
                                    {x0, y1, z1},
                                    {x0, y1, z0}
                            };

                    case EAST ->
                            new float[][]{
                                    {x1, y0, z1},
                                    {x1, y0, z0},
                                    {x1, y1, z0},
                                    {x1, y1, z1}
                            };
                };

        for (
                float[] point :
                points
        ) {

            vertex(
                    out,
                    pose,
                    face,

                    point[0],
                    point[1],
                    point[2],

                    layers,
                    light
            );
        }
    }
}
