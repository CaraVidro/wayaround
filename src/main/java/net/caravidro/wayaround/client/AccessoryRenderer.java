package net.caravidro.wayaround.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.accessory.AccessoryKind;
import net.caravidro.wayaround.accessory.AccessoryMotion;
import net.caravidro.wayaround.accessory.AccessorySlot;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Procedural textured clothing attached directly to vanilla PlayerModel bones.
 *
 * Every solid piece is rendered through Minecraft's BlockRenderDispatcher,
 * which means the equipment uses real vanilla block textures rather than flat
 * debug colours. Bone-local placement makes sleeves/boots/hats follow whatever
 * pose an animation has already applied to the player model.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class AccessoryRenderer {

    private AccessoryRenderer() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void render(
            RenderPlayerEvent.Post event
    ) {
        if (!WorldFeatureRuntime.clientEnabled(
                WorldFeature.ACCESSORIES
        )) {
            return;
        }

        AccessoryClientState.State state =
                AccessoryClientState.get(
                        event.getEntity()
                                .getUUID()
                );

        if (state == null
                || state.empty()) {
            return;
        }

        PoseStack pose =
                event.getPoseStack();

        PlayerModel<?> model =
                event.getRenderer()
                        .getModel();

        BlockRenderDispatcher blocks =
                Minecraft.getInstance()
                        .getBlockRenderer();

        MultiBufferSource buffers =
                event.getMultiBufferSource();

        int light =
                event.getPackedLight();

        pose.pushPose();

        float bodyYaw =
                Mth.rotLerp(
                        event.getPartialTick(),
                        event.getEntity().yBodyRotO,
                        event.getEntity().yBodyRot
                );

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        180.0F - bodyYaw
                )
        );

        pose.scale(
                -1.0F,
                -1.0F,
                1.0F
        );

        pose.translate(
                0.0D,
                -1.501D,
                0.0D
        );

        renderHead(
                state,
                pose,
                model,
                blocks,
                buffers,
                light,
                event
        );

        renderFace(
                state,
                pose,
                model,
                blocks,
                buffers,
                light
        );

        renderTorso(
                state,
                pose,
                model,
                blocks,
                buffers,
                light
        );

        renderHands(
                state,
                pose,
                model,
                blocks,
                buffers,
                light
        );

        renderLegs(
                state,
                pose,
                model,
                blocks,
                buffers,
                light
        );

        renderFeet(
                state,
                pose,
                model,
                blocks,
                buffers,
                light
        );

        renderBack(
                state,
                pose,
                model,
                blocks,
                buffers,
                light,
                event
        );

        renderExtra(
                state,
                pose,
                model,
                blocks,
                buffers,
                light,
                event
        );

        pose.popPose();
    }

    private static void renderHead(
            AccessoryClientState.State state,
            PoseStack pose,
            PlayerModel<?> model,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            RenderPlayerEvent.Post event
    ) {
        AccessoryKind kind =
                state.kind(
                        AccessorySlot.HEAD
                );

        if (kind == null) {
            return;
        }

        int wear =
                state.wearStage(
                        AccessorySlot.HEAD
                );

        pose.pushPose();
        model.head.translateAndRotate(
                pose
        );

        switch (kind) {
            case ENGINEER_CAP ->
                    engineerCap(
                            wear,
                            pose,
                            blocks,
                            buffers,
                            light,
                            false,
                            event
                    );

            case AERO_ENGINEER_CAP ->
                    engineerCap(
                            wear,
                            pose,
                            blocks,
                            buffers,
                            light,
                            true,
                            event
                    );

            case CHEF_HAT ->
                    chefHat(
                            wear,
                            pose,
                            blocks,
                            buffers,
                            light
                    );

            default -> {
            }
        }

        pose.popPose();
    }

    private static void renderFace(
            AccessoryClientState.State state,
            PoseStack pose,
            PlayerModel<?> model,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light
    ) {
        AccessoryKind kind =
                state.kind(
                        AccessorySlot.FACE
                );

        if (kind == null) {
            return;
        }

        int wear =
                state.wearStage(
                        AccessorySlot.FACE
                );

        int glass =
                state.glass(
                        AccessorySlot.FACE
                );

        pose.pushPose();
        model.head.translateAndRotate(
                pose
        );

        boolean crown =
                kind == AccessoryKind.SPECTRAL_GLASSES
                        && state.glassesMode() == 1;

        if (crown) {
            pose.translate(
                    0.0,
                    -0.11,
                    0.018
            );

            pose.mulPose(
                    Axis.XP.rotationDegrees(
                            -13.0F
                    )
            );
        }

        switch (kind) {
            case SPECTRAL_GLASSES ->
                    goggles(
                            pose,
                            blocks,
                            buffers,
                            light,
                            wear,
                            glass,
                            Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                            Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(),
                            false
                    );

            case ENGINEER_GOGGLES ->
                    engineerSpectacles(pose, blocks, buffers, light, wear, glass);

            case AERO_GOGGLES ->
                    goggles(
                            pose,
                            blocks,
                            buffers,
                            light,
                            wear,
                            glass,
                            Blocks.CUT_COPPER.defaultBlockState(),
                            Blocks.TINTED_GLASS.defaultBlockState(),
                            true
                    );

            default -> {
            }
        }

        pose.popPose();
    }

    private static void renderTorso(
            AccessoryClientState.State state,
            PoseStack pose,
            PlayerModel<?> model,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light
    ) {
        AccessoryKind kind =
                state.kind(
                        AccessorySlot.TORSO
                );

        if (kind == null) {
            return;
        }

        int wear =
                state.wearStage(
                        AccessorySlot.TORSO
                );

        switch (kind) {
            case ENGINEER_JACKET ->
                    steampunkWaistcoat(model, pose, blocks, buffers, light, wear);

            case AERO_JACKET ->
                    jacket(
                            model,
                            pose,
                            blocks,
                            buffers,
                            light,
                            wear,
                            Blocks.BLUE_WOOL.defaultBlockState(),
                            Blocks.CUT_COPPER.defaultBlockState(),
                            true
                    );

            case CHEF_COAT ->
                    chefCoat(
                            model,
                            pose,
                            blocks,
                            buffers,
                            light,
                            wear
                    );

            default -> {
            }
        }
    }

    private static void renderHands(
            AccessoryClientState.State state,
            PoseStack pose,
            PlayerModel<?> model,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light
    ) {
        AccessoryKind kind =
                state.kind(
                        AccessorySlot.HANDS
                );

        if (kind == null) {
            return;
        }

        int wear =
                state.wearStage(
                        AccessorySlot.HANDS
                );

        BlockState fabric =
                switch (kind) {
                    case CHEF_GLOVES ->
                            Blocks.WHITE_WOOL.defaultBlockState();
                    case AERO_GLOVES ->
                            Blocks.GRAY_WOOL.defaultBlockState();
                    default ->
                            Blocks.BROWN_WOOL.defaultBlockState();
                };

        glove(
                model.leftArm,
                pose,
                blocks,
                buffers,
                light,
                wear,
                fabric,
                true,
                kind
        );

        glove(
                model.rightArm,
                pose,
                blocks,
                buffers,
                light,
                wear,
                fabric,
                false,
                kind
        );
    }

    private static void renderLegs(
            AccessoryClientState.State state,
            PoseStack pose,
            PlayerModel<?> model,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light
    ) {
        AccessoryKind kind =
                state.kind(
                        AccessorySlot.LEGS
                );

        if (kind == null) {
            return;
        }

        int wear =
                state.wearStage(
                        AccessorySlot.LEGS
                );

        BlockState fabric =
                switch (kind) {
                    case AERO_TROUSERS ->
                            Blocks.BLUE_WOOL.defaultBlockState();
                    case CHEF_TROUSERS ->
                            Blocks.LIGHT_GRAY_WOOL.defaultBlockState();
                    default ->
                            Blocks.BROWN_WOOL.defaultBlockState();
                };

        trousers(
                model.leftLeg,
                pose,
                blocks,
                buffers,
                light,
                wear,
                fabric,
                true
        );

        trousers(
                model.rightLeg,
                pose,
                blocks,
                buffers,
                light,
                wear,
                fabric,
                false
        );
        if (kind == AccessoryKind.ENGINEER_TROUSERS) {
            engineerTrouserDetail(model.leftLeg, pose, blocks, buffers, light, wear, true);
            engineerTrouserDetail(model.rightLeg, pose, blocks, buffers, light, wear, false);
        }
    }

    private static void renderFeet(
            AccessoryClientState.State state,
            PoseStack pose,
            PlayerModel<?> model,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light
    ) {
        AccessoryKind kind =
                state.kind(
                        AccessorySlot.FEET
                );

        if (kind == null) {
            return;
        }

        int wear =
                state.wearStage(
                        AccessorySlot.FEET
                );

        BlockState shell =
                switch (kind) {
                    case CHEF_SHOES ->
                            Blocks.BLACK_WOOL.defaultBlockState();
                    case AERO_BOOTS,
                         WIND_BOOTS ->
                            Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                    default ->
                            Blocks.BROWN_TERRACOTTA.defaultBlockState();
                };

        boot(
                model.leftLeg,
                pose,
                blocks,
                buffers,
                light,
                wear,
                shell,
                true,
                kind
        );

        boot(
                model.rightLeg,
                pose,
                blocks,
                buffers,
                light,
                wear,
                shell,
                false,
                kind
        );
    }

    private static void renderBack(
            AccessoryClientState.State state,
            PoseStack pose,
            PlayerModel<?> model,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            RenderPlayerEvent.Post event
    ) {
        AccessoryKind kind =
                state.kind(
                        AccessorySlot.BACK
                );

        if (kind == null
                || !kind.motion()
                .cloth()) {
            return;
        }

        int wear =
                state.wearStage(
                        AccessorySlot.BACK
                );

        BlockState cloth =
                kind == AccessoryKind.AERO_CAPE
                        ? Blocks.DARK_OAK_PLANKS.defaultBlockState()
                        : Blocks.BROWN_WOOL.defaultBlockState();

        cape(
                model.body,
                pose,
                blocks,
                buffers,
                light,
                wear,
                cloth,
                event
        );
    }

    private static void renderExtra(
            AccessoryClientState.State state,
            PoseStack pose,
            PlayerModel<?> model,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            RenderPlayerEvent.Post event
    ) {
        AccessoryKind kind =
                state.kind(
                        AccessorySlot.EXTRA
                );

        if (kind == null) {
            return;
        }

        int wear =
                state.wearStage(
                        AccessorySlot.EXTRA
                );

        switch (kind) {
            case ENGINEER_GEAR_HARNESS,
                 AERO_GEAR_CLUSTER ->
                    gearHarness(
                            model.body,
                            pose,
                            blocks,
                            buffers,
                            light,
                            wear,
                            kind,
                            event
                    );

            case CHEF_APRON ->
                    apron(
                            model.body,
                            pose,
                            blocks,
                            buffers,
                            light,
                            wear,
                            event
                    );

            default -> {
            }
        }
    }

    private static void engineerCap(
            int wear,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            boolean aero,
            RenderPlayerEvent.Post event
    ) {
        if (!aero) {
            MotionSample motion =
                    motion(
                            event
                    );

            float instability =
                    TopHatClientState.instability(
                            event.getEntity()
                                    .getUUID()
                    );

            if (TopHatClientState.warningActive(
                    event.getEntity()
                            .getUUID()
            )) {
                instability =
                        Math.max(
                                instability,
                                0.92F
                        );
            }

            TopHatModelRenderer.render(
                    pose,
                    blocks,
                    buffers,
                    light,
                    wear,
                    motion.back()
                            + motion.speed()
                            * 0.18F,
                    motion.side(),
                    instability,
                    motion.time()
            );

            return;
        }

        BlockState cloth =
                aero
                        ? Blocks.BLUE_WOOL.defaultBlockState()
                        : Blocks.BROWN_WOOL.defaultBlockState();

        BlockState metal =
                aero
                        ? Blocks.CUT_COPPER.defaultBlockState()
                        : Blocks.COPPER_BLOCK.defaultBlockState();

        piece(
                pose,
                blocks,
                buffers,
                light,
                cloth,
                0.0,
                -0.535,
                0.0,
                0.56,
                0.13,
                0.56,
                0,
                0,
                wear == 2 ? -4 : 0
        );

        piece(
                pose,
                blocks,
                buffers,
                light,
                cloth,
                0.0,
                -0.625,
                0.045,
                0.45,
                0.12,
                0.42,
                0,
                0,
                wear == 2 ? -7 : 0
        );

        piece(
                pose,
                blocks,
                buffers,
                light,
                aero
                        ? metal
                        : Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                0.0,
                -0.525,
                -0.35,
                wear == 2 ? 0.30 : 0.48,
                0.055,
                0.22,
                0,
                wear == 1 ? -4 : wear == 2 ? -9 : -2,
                0
        );

        if (wear < 2) {
            piece(
                    pose,
                    blocks,
                    buffers,
                    light,
                    metal,
                    -0.24,
                    -0.585,
                    0.0,
                    0.055,
                    0.16,
                    0.58,
                    0,
                    0,
                    0
            );
        }

        if (wear >= 1) {
            piece(
                    pose,
                    blocks,
                    buffers,
                    light,
                    Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                    0.17,
                    -0.65,
                    -0.12,
                    0.16,
                    0.045,
                    0.15,
                    12,
                    0,
                    0
            );
        }
    }

    private static void gauge(PoseStack pose, BlockRenderDispatcher blocks,
            MultiBufferSource buffers, int light, double x, double y, double z,
            double diameter, int wear) {
        BlockState brass = Blocks.CUT_COPPER.defaultBlockState();
        piece(pose, blocks, buffers, light, brass, x, y, z,
                diameter + 0.036, diameter + 0.036, 0.039, 0, 0, 0);
        piece(pose, blocks, buffers, light,
                wear == 2 ? Blocks.LIGHT_GRAY_WOOL.defaultBlockState()
                        : Blocks.QUARTZ_BLOCK.defaultBlockState(),
                x, y, z - 0.023, diameter, diameter, 0.014, 0, 0, 0);
        piece(pose, blocks, buffers, light, Blocks.RED_TERRACOTTA.defaultBlockState(),
                x, y, z - 0.034, diameter * 0.07, diameter * 0.60,
                0.013, 0, 0, wear == 1 ? 30 : -24);
    }

    private static void engineerSpectacles(PoseStack pose,
            BlockRenderDispatcher blocks, MultiBufferSource buffers,
            int light, int wear, int glass) {
        BlockState brass = Blocks.CUT_COPPER.defaultBlockState();
        BlockState strap = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        for (int side : new int[]{-1, 1}) {
            double x = side * 0.142;
            // Faceted octagonal rim, open center, so cracks remain readable.
            piece(pose, blocks, buffers, light, brass, x, -0.247, -0.337,
                    0.20, 0.027, 0.045, 0, 0, 0);
            piece(pose, blocks, buffers, light, brass, x, -0.095, -0.337,
                    0.20, 0.027, 0.045, 0, 0, 0);
            for (int edge : new int[]{-1, 1}) {
                piece(pose, blocks, buffers, light, brass,
                        x + edge * 0.095, -0.17, -0.337,
                        0.028, 0.123, 0.045, 0, 0, edge * 16);
            }
            if (glass == 0 || (glass == 1 && side < 0)) {
                piece(pose, blocks, buffers, light,
                        Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(),
                        x, -0.172, -0.360, 0.16, 0.13, 0.018, 0, 0, 0);
            } else if (glass < 2) {
                piece(pose, blocks, buffers, light,
                        Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(),
                        x - 0.033, -0.195, -0.360,
                        0.065, 0.061, 0.018, 0, 0, -15);
            }
            if (wear < 2) piece(pose, blocks, buffers, light, strap,
                    side * 0.298, -0.17, -0.055,
                    0.027, 0.055, 0.48, 0, 0, 0);
        }
        piece(pose, blocks, buffers, light, brass,
                0, -0.17, -0.356, 0.080, 0.031, 0.05, 0, 0, 0);
    }

    private static void steampunkWaistcoat(PlayerModel<?> model, PoseStack pose,
            BlockRenderDispatcher blocks, MultiBufferSource buffers,
            int light, int wear) {
        BlockState jacket = wear == 2 ? Blocks.BROWN_TERRACOTTA.defaultBlockState()
                : Blocks.BROWN_WOOL.defaultBlockState();
        BlockState vest = wear == 2 ? Blocks.ORANGE_TERRACOTTA.defaultBlockState()
                : Blocks.RED_TERRACOTTA.defaultBlockState();
        BlockState leather = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState brass = Blocks.CUT_COPPER.defaultBlockState();
        pose.pushPose();
        model.body.translateAndRotate(pose);
        piece(pose, blocks, buffers, light, jacket,
                0, 0.35, 0.163, 0.57, 0.70, 0.07, 0, 0, 0);
        for (int side : new int[]{-1, 1}) {
            piece(pose, blocks, buffers, light, jacket,
                    side * 0.20, 0.33, -0.162,
                    0.19, wear == 2 && side > 0 ? 0.51 : 0.66, 0.074,
                    0, 0, side * 2);
            piece(pose, blocks, buffers, light, vest,
                    side * 0.10, 0.35, -0.208,
                    0.19, 0.52, 0.043, 0, 0, 0);
            piece(pose, blocks, buffers, light, leather,
                    side * 0.22, 0.19, -0.224,
                    0.11, 0.24, 0.033, 0, 0, side * 24);
            piece(pose, blocks, buffers, light, brass,
                    side * 0.23, 0.102, -0.236,
                    0.045, 0.045, 0.024, 0, 0, 0);
        }
        // Jacket is tailored with a waist and an off-center tool belt.
        piece(pose, blocks, buffers, light, leather,
                0, 0.67, -0.185, 0.57, 0.065, 0.06, 0, 0, 0);
        piece(pose, blocks, buffers, light, brass,
                0.18, 0.67, -0.224, 0.105, 0.095, 0.025, 0, 0, 0);
        for (int i = 0; i < 3 - (wear == 2 ? 1 : 0); i++) {
            piece(pose, blocks, buffers, light, brass,
                    0, 0.28 + i * 0.13, -0.242,
                    0.045, 0.045, 0.018, 0, 0, 0);
        }
        piece(pose, blocks, buffers, light, Blocks.RED_WOOL.defaultBlockState(),
                0, 0.09, -0.238, 0.20, 0.07, 0.028, 0, 0, 0);
        pose.popPose();
        sleeve(model.leftArm, pose, blocks, buffers, light, wear, jacket, brass, true);
        sleeve(model.rightArm, pose, blocks, buffers, light, wear, jacket, brass, false);
        for (ModelPart arm : new ModelPart[]{model.leftArm, model.rightArm}) {
            pose.pushPose();
            arm.translateAndRotate(pose);
            piece(pose, blocks, buffers, light, leather,
                    0, 0.49, -0.174, 0.26, 0.07, 0.035, 0, 0, 0);
            if (wear < 2) piece(pose, blocks, buffers, light, brass,
                    0, 0.53, -0.184, 0.13, 0.025, 0.021, 0, 0, 0);
            pose.popPose();
        }
    }

    private static void engineerTrouserDetail(ModelPart leg, PoseStack pose,
            BlockRenderDispatcher blocks, MultiBufferSource buffers,
            int light, int wear, boolean left) {
        pose.pushPose();
        leg.translateAndRotate(pose);
        piece(pose, blocks, buffers, light, Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                0, 0.47, -0.174, 0.32, 0.062, 0.036, 0, 0, 0);
        if (wear < 2) piece(pose, blocks, buffers, light,
                Blocks.CUT_COPPER.defaultBlockState(),
                left ? -0.10 : 0.10, 0.47, -0.197,
                0.05, 0.055, 0.021, 0, 0, 0);
        pose.popPose();
    }

    private static void engineerInstrumentHarness(PoseStack pose,
            BlockRenderDispatcher blocks, MultiBufferSource buffers,
            int light, int wear, MotionSample motion) {
        BlockState leather = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState brass = Blocks.CUT_COPPER.defaultBlockState();
        // Two diagonal straps attach a pressure gauge and an exposed tube to
        // the jacket. They move with the chest, not like loose backpack straps.
        piece(pose, blocks, buffers, light, leather,
                -0.18, 0.33, -0.228, 0.065, 0.58, 0.042, 0, 0, -23);
        piece(pose, blocks, buffers, light, leather,
                0.17, 0.35, -0.223, 0.065, 0.51, 0.042, 0, 0, 22);
        piece(pose, blocks, buffers, light, brass,
                0.23, 0.12, -0.208, 0.20, 0.07, 0.09, 0, 0, 0);
        piece(pose, blocks, buffers, light, brass,
                0.285, 0.29, -0.205, 0.052, 0.24, 0.046, 0, 0, 9);
        piece(pose, blocks, buffers, light, brass,
                0.24, 0.41, -0.208, 0.13, 0.04, 0.045, 0, 0, 0);
        gauge(pose, blocks, buffers, light,
                0.205, 0.31, -0.266, 0.12, wear);
        if (wear < 2) {
            pose.pushPose();
            pose.translate(-0.22, 0.52, -0.265);
            pose.mulPose(Axis.ZP.rotationDegrees(
                    Mth.sin(motion.time() * 0.11F) * 7F));
            gear(pose, blocks, buffers, light, brass, 0.092,
                    motion.time() * 3F);
            pose.popPose();
        }
    }

    /**
     * Intentionally leaves a hollow centre above the player's head. The future
     * rat system can use AccessoryKind#ratHost and place a passenger in that
     * cavity without the hat model needing to be rebuilt.
     */
    private static void chefHat(
            int wear,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light
    ) {
        BlockState white =
                wear == 0
                        ? Blocks.WHITE_WOOL.defaultBlockState()
                        : wear == 1
                        ? Blocks.LIGHT_GRAY_WOOL.defaultBlockState()
                        : Blocks.GRAY_WOOL.defaultBlockState();

        piece(pose, blocks, buffers, light, white,
                0.0, -0.54, 0.0,
                0.57, 0.13, 0.57,
                0, 0, wear == 2 ? 6 : 0);

        double y =
                wear == 2
                        ? -0.69
                        : -0.75;

        double spread =
                0.20;

        piece(pose, blocks, buffers, light, white,
                -spread, y, -spread,
                0.34, 0.30, 0.34,
                0, 0, -4);

        piece(pose, blocks, buffers, light, white,
                spread, y, -spread,
                wear == 2 ? 0.25 : 0.34, 0.30, 0.34,
                0, 0, wear == 2 ? 15 : 4);

        piece(pose, blocks, buffers, light, white,
                -spread, y - 0.02, spread,
                0.34, wear == 1 ? 0.27 : 0.31, 0.34,
                0, 0, 3);

        if (wear < 2) {
            piece(pose, blocks, buffers, light, white,
                    spread, y - 0.03, spread,
                    0.34, 0.32, 0.34,
                    0, 0, -3);
        }

        if (wear >= 1) {
            piece(pose, blocks, buffers, light,
                    Blocks.BROWN_WOOL.defaultBlockState(),
                    -0.17, -0.57, -0.31,
                    0.12, 0.06, 0.08,
                    0, 0, 0);
        }
    }

    private static void goggles(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            int glass,
            BlockState frame,
            BlockState lens,
            boolean aero
    ) {
        double y =
                -0.25;

        double z =
                -0.315;

        double lensWidth =
                aero
                        ? 0.185
                        : 0.17;

        piece(pose, blocks, buffers, light, frame,
                -0.14, y, z,
                0.24, 0.16, 0.045,
                0, 0, wear == 2 ? -5 : 0);

        piece(pose, blocks, buffers, light, frame,
                0.14, y, z,
                wear == 2 ? 0.19 : 0.24, 0.16, 0.045,
                0, 0, wear == 2 ? 7 : 0);

        piece(pose, blocks, buffers, light, frame,
                0.0, y, z - 0.006,
                0.09, 0.045, 0.055,
                0, 0, 0);

        // Intact lenses.
        if (glass == 0) {
            piece(pose, blocks, buffers, light, lens,
                    -0.14, y, z - 0.027,
                    lensWidth, 0.105, 0.018,
                    0, 0, 0);

            piece(pose, blocks, buffers, light, lens,
                    0.14, y, z - 0.027,
                    lensWidth, 0.105, 0.018,
                    0, 0, 0);
        } else if (glass == 1) {
            // One lens remains mostly intact; the other is visibly fragmented.
            piece(pose, blocks, buffers, light, lens,
                    -0.14, y, z - 0.027,
                    lensWidth, 0.105, 0.018,
                    0, 0, 0);

            piece(pose, blocks, buffers, light, lens,
                    0.105, y - 0.012, z - 0.027,
                    lensWidth * 0.48, 0.075, 0.018,
                    0, 0, 13);

            piece(pose, blocks, buffers, light,
                    Blocks.IRON_BARS.defaultBlockState(),
                    0.16, y, z - 0.045,
                    0.025, 0.14, 0.018,
                    0, 0, 28);
        } else {
            // Broken: tiny shards only, frames stay.
            piece(pose, blocks, buffers, light, lens,
                    -0.185, y + 0.02, z - 0.027,
                    0.065, 0.05, 0.018,
                    0, 0, -18);

            if (wear < 2) {
                piece(pose, blocks, buffers, light, lens,
                        0.18, y - 0.025, z - 0.027,
                        0.055, 0.04, 0.018,
                        0, 0, 24);
            }
        }

        // Side straps.
        if (wear < 2) {
            piece(pose, blocks, buffers, light,
                    aero
                            ? Blocks.BROWN_WOOL.defaultBlockState()
                            : frame,
                    -0.285, y, -0.08,
                    0.035, 0.055, 0.45,
                    0, 0, 0);

            piece(pose, blocks, buffers, light,
                    aero
                            ? Blocks.BROWN_WOOL.defaultBlockState()
                            : frame,
                    0.285, y, -0.08,
                    0.035, 0.055, 0.45,
                    0, 0, 0);
        }
    }

    private static void jacket(
            PlayerModel<?> model,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            BlockState cloth,
            BlockState metal,
            boolean aero
    ) {
        pose.pushPose();
        model.body.translateAndRotate(
                pose
        );

        BlockState wornCloth =
                wornFabric(
                        cloth,
                        wear,
                        aero
                                ? Blocks.GRAY_WOOL.defaultBlockState()
                                : Blocks.BROWN_TERRACOTTA.defaultBlockState()
                );

        piece(pose, blocks, buffers, light, wornCloth,
                0.0, 0.34, -0.155,
                0.54, 0.70, 0.075,
                0, 0, 0);

        piece(pose, blocks, buffers, light, wornCloth,
                0.0, 0.34, 0.155,
                0.54, wear == 2 ? 0.59 : 0.70, 0.075,
                0, 0, wear == 2 ? 2 : 0);

        piece(pose, blocks, buffers, light, metal,
                -0.22, 0.12, -0.205,
                0.09, 0.10, 0.055,
                0, 0, 0);

        if (wear < 2) {
            piece(pose, blocks, buffers, light, metal,
                    0.22, 0.12, -0.205,
                    0.09, 0.10, 0.055,
                    0, 0, 0);
        }

        // Belt / lower hem.
        piece(pose, blocks, buffers, light,
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                0.0, 0.68, 0.0,
                0.58, 0.075, 0.33,
                0, 0, 0);

        if (wear >= 1) {
            piece(pose, blocks, buffers, light,
                    aero
                            ? Blocks.BROWN_WOOL.defaultBlockState()
                            : Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                    wear == 2 ? -0.12 : 0.13,
                    0.46,
                    -0.205,
                    0.17,
                    0.13,
                    0.035,
                    wear == 2 ? 8 : -5,
                    0,
                    0);
        }

        pose.popPose();

        sleeve(
                model.leftArm,
                pose,
                blocks,
                buffers,
                light,
                wear,
                wornCloth,
                metal,
                true
        );

        sleeve(
                model.rightArm,
                pose,
                blocks,
                buffers,
                light,
                wear,
                wornCloth,
                metal,
                false
        );
    }

    private static void sleeve(
            ModelPart arm,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            BlockState cloth,
            BlockState metal,
            boolean left
    ) {
        pose.pushPose();
        arm.translateAndRotate(
                pose
        );

        double length =
                wear == 2
                        && !left
                        ? 0.42
                        : 0.58;

        piece(pose, blocks, buffers, light, cloth,
                0.0, 0.28, 0.0,
                0.31, length, 0.31,
                0, 0, 0);

        if (wear < 2
                || left) {
            piece(pose, blocks, buffers, light, metal,
                    0.0, 0.48, -0.17,
                    0.20, 0.11, 0.035,
                    0, 0, 0);
        }

        pose.popPose();
    }

    private static void chefCoat(
            PlayerModel<?> model,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear
    ) {
        BlockState cloth =
                wear == 0
                        ? Blocks.WHITE_WOOL.defaultBlockState()
                        : wear == 1
                        ? Blocks.LIGHT_GRAY_WOOL.defaultBlockState()
                        : Blocks.GRAY_WOOL.defaultBlockState();

        pose.pushPose();
        model.body.translateAndRotate(
                pose
        );

        piece(pose, blocks, buffers, light, cloth,
                0.0, 0.34, -0.16,
                0.55, wear == 2 ? 0.61 : 0.72, 0.075,
                0, 0, wear == 2 ? -2 : 0);

        piece(pose, blocks, buffers, light, cloth,
                0.0, 0.34, 0.15,
                0.54, 0.70, 0.07,
                0, 0, 0);

        // Double-breasted buttons.
        for (int i = 0;
             i < 3;
             i++) {
            if (wear == 2
                    && i == 2) {
                continue;
            }

            double y =
                    0.20
                            + i * 0.16;

            piece(pose, blocks, buffers, light,
                    Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                    -0.08, y, -0.207,
                    0.035, 0.035, 0.025,
                    0, 0, 0);

            piece(pose, blocks, buffers, light,
                    Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                    0.08, y, -0.207,
                    0.035, 0.035, 0.025,
                    0, 0, 0);
        }

        // Red neck cloth gives the kit a readable silhouette.
        piece(pose, blocks, buffers, light,
                Blocks.RED_WOOL.defaultBlockState(),
                0.0, 0.08, -0.21,
                0.22, 0.07, 0.035,
                0, 0, 0);

        if (wear >= 1) {
            piece(pose, blocks, buffers, light,
                    Blocks.BROWN_WOOL.defaultBlockState(),
                    0.15, 0.47, -0.21,
                    0.15, 0.11, 0.025,
                    -8, 0, 0);
        }

        pose.popPose();

        sleeve(model.leftArm, pose, blocks, buffers, light,
                wear, cloth, Blocks.LIGHT_GRAY_WOOL.defaultBlockState(), true);

        sleeve(model.rightArm, pose, blocks, buffers, light,
                wear, cloth, Blocks.LIGHT_GRAY_WOOL.defaultBlockState(), false);
    }

    private static void glove(
            ModelPart arm,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            BlockState fabric,
            boolean left,
            AccessoryKind kind
    ) {
        pose.pushPose();
        arm.translateAndRotate(
                pose
        );

        BlockState material =
                wornFabric(
                        fabric,
                        wear,
                        Blocks.BROWN_TERRACOTTA.defaultBlockState()
                );

        piece(pose, blocks, buffers, light, material,
                0.0, 0.62, 0.0,
                wear == 2 && !left ? 0.24 : 0.30,
                wear == 2 && !left ? 0.20 : 0.29,
                0.30,
                0, 0, wear == 2 && !left ? 5 : 0);

        if (kind != AccessoryKind.CHEF_GLOVES
                && wear < 2) {
            piece(pose, blocks, buffers, light,
                    kind == AccessoryKind.AERO_GLOVES
                            ? Blocks.CUT_COPPER.defaultBlockState()
                            : Blocks.IRON_BLOCK.defaultBlockState(),
                    0.0, 0.58, -0.17,
                    0.18, 0.10, 0.035,
                    0, 0, 0);
        }

        if (kind == AccessoryKind.ENGINEER_GLOVES
                || kind == AccessoryKind.WORK_GLOVES) {
            BlockState brass = Blocks.CUT_COPPER.defaultBlockState();
            piece(pose, blocks, buffers, light, brass,
                    0.0, 0.48, -0.168, 0.31, 0.045, 0.045, 0, 0, 0);
            if (wear < 2) {
                piece(pose, blocks, buffers, light, brass,
                        left ? -0.08 : 0.08, 0.69, -0.176,
                        0.07, 0.12, 0.025, 0, 0, 0);
            }
        }
        pose.popPose();
    }

    private static void trousers(
            ModelPart leg,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            BlockState fabric,
            boolean left
    ) {
        pose.pushPose();
        leg.translateAndRotate(
                pose
        );

        BlockState material =
                wornFabric(
                        fabric,
                        wear,
                        Blocks.GRAY_WOOL.defaultBlockState()
                );

        double length =
                wear == 2
                        && !left
                        ? 0.48
                        : 0.66;

        piece(pose, blocks, buffers, light, material,
                0.0, 0.31, 0.0,
                0.31, length, 0.31,
                0, 0, wear == 2 && !left ? 3 : 0);

        if (wear >= 1) {
            piece(pose, blocks, buffers, light,
                    Blocks.BROWN_WOOL.defaultBlockState(),
                    left ? -0.09 : 0.09,
                    0.37,
                    -0.17,
                    0.11,
                    0.14,
                    0.025,
                    0, 0, left ? -5 : 5);
        }

        pose.popPose();
    }

    private static void boot(
            ModelPart leg,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            BlockState shell,
            boolean left,
            AccessoryKind kind
    ) {
        pose.pushPose();
        leg.translateAndRotate(
                pose
        );

        piece(pose, blocks, buffers, light, shell,
                0.0, 0.61, 0.0,
                wear == 2 && !left ? 0.26 : 0.31,
                wear == 2 && !left ? 0.24 : 0.31,
                0.32,
                0, 0, 0);

        /*
         * Engineer footwear is deliberately blocky. The old front extension
         * looked like a normal shoe/toe; this keeps the whole boot footprint a
         * compact square around the vanilla foot.
         */
        piece(pose, blocks, buffers, light,
                shell,
                0.0, 0.68, 0.0,
                wear == 2 && !left ? 0.26 : 0.33,
                0.18,
                wear == 2 && !left ? 0.27 : 0.34,
                0, 0, 0);

        piece(pose, blocks, buffers, light,
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                0.0, 0.76, 0.0,
                wear == 2 && !left ? 0.28 : 0.36,
                0.065,
                wear == 2 && !left ? 0.29 : 0.36,
                0, 0, 0);

        if (kind == AccessoryKind.ENGINEER_BOOTS && wear < 2) {
            piece(pose, blocks, buffers, light, Blocks.CUT_COPPER.defaultBlockState(),
                    0, 0.49, -0.17, 0.31, 0.048, 0.038, 0, 0, 0);
            piece(pose, blocks, buffers, light, Blocks.CUT_COPPER.defaultBlockState(),
                    0, 0.68, -0.18, 0.22, 0.045, 0.03, 0, 0, 0);
        }

        if ((kind == AccessoryKind.AERO_BOOTS
                || kind == AccessoryKind.WIND_BOOTS)
                && wear < 2) {
            piece(pose, blocks, buffers, light,
                    Blocks.COPPER_BLOCK.defaultBlockState(),
                    left ? -0.16 : 0.16,
                    0.61,
                    0.02,
                    0.055,
                    0.20,
                    0.18,
                    0, 0, 0);
        }

        pose.popPose();
    }

    private static void cape(
            ModelPart body,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            BlockState cloth,
            RenderPlayerEvent.Post event
    ) {
        pose.pushPose();
        body.translateAndRotate(
                pose
        );

        MotionSample motion =
                motion(
                        event
                );

        piece(pose, blocks, buffers, light,
                Blocks.COPPER_BLOCK.defaultBlockState(),
                -0.21, 0.09, 0.19,
                0.10, 0.11, 0.08,
                0, 0, 0);

        piece(pose, blocks, buffers, light,
                Blocks.COPPER_BLOCK.defaultBlockState(),
                0.21, 0.09, 0.19,
                0.10, 0.11, 0.08,
                0, 0, 0);

        pose.translate(
                0.0,
                0.06,
                0.20
        );

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        motion.side()
                                * 8.0F
                )
        );

        float base =
                5.0F
                        + motion.back()
                                * 24.0F
                        + motion.speed()
                                * 30.0F
                        + motion.fall()
                                * 12.0F;

        int segments =
                wear == 2
                        ? 4
                        : 5;

        for (int i = 0;
             i < segments;
             i++) {
            float flutter =
                    Mth.sin(
                            motion.time()
                                    * (
                                    0.22F
                                            + motion.speed()
                                            * 0.55F
                            )
                                    + i * 0.72F
                    )
                            * (
                            1.2F
                                    + motion.wind()
                                    * 5.5F
                                    + motion.speed()
                                    * 4.5F
                    );

            pose.mulPose(
                    Axis.XP.rotationDegrees(
                            i == 0
                                    ? base + flutter
                                    : 2.5F
                                    + flutter
                                    * (
                                    0.45F
                                            + i * 0.12F
                            )
                    )
            );

            double width =
                    0.55
                            - i * 0.018;

            if (wear == 2
                    && i == segments - 1) {
                width *=
                        0.72;
            }

            piece(pose, blocks, buffers, light,
                    wornFabric(
                            cloth,
                            wear,
                            Blocks.BROWN_TERRACOTTA.defaultBlockState()
                    ),
                    wear == 2 && i == segments - 1 ? -0.07 : 0.0,
                    0.105,
                    0.0,
                    width,
                    0.22,
                    0.045,
                    0, 0,
                    wear == 2 && i == segments - 1 ? -7 : 0);

            pose.translate(
                    0.0,
                    0.215,
                    0.0
            );
        }

        pose.popPose();
    }

    private static void gearHarness(
            ModelPart body,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            AccessoryKind kind,
            RenderPlayerEvent.Post event
    ) {
        pose.pushPose();
        body.translateAndRotate(
                pose
        );

        MotionSample motion =
                motion(
                        event
                );

        BlockState metal =
                kind == AccessoryKind.AERO_GEAR_CLUSTER
                        ? Blocks.CUT_COPPER.defaultBlockState()
                        : Blocks.COPPER_BLOCK.defaultBlockState();

        if (kind == AccessoryKind.ENGINEER_GEAR_HARNESS) {
            engineerInstrumentHarness(pose, blocks, buffers, light, wear, motion);
            pose.popPose();
            return;
        }

        // Harness strap.
        piece(pose, blocks, buffers, light,
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                kind == AccessoryKind.AERO_GEAR_CLUSTER ? 0.22 : -0.22,
                0.38,
                -0.205,
                0.10,
                0.62,
                0.04,
                0, 0,
                kind == AccessoryKind.AERO_GEAR_CLUSTER ? 8 : -8);

        int gears =
                wear == 2
                        ? 2
                        : 3;

        for (int i = 0;
             i < gears;
             i++) {
            pose.pushPose();

            double x =
                    kind == AccessoryKind.AERO_GEAR_CLUSTER
                            ? 0.30
                            : -0.30;

            double y =
                    0.24
                            + i * 0.19;

            pose.translate(
                    x,
                    y,
                    -0.245
            );

            float swing =
                    (
                            motion.back()
                                    * 16.0F
                                    + motion.side()
                                    * (
                                    kind == AccessoryKind.AERO_GEAR_CLUSTER
                                            ? 11.0F
                                            : -11.0F
                            )
                                    + Mth.sin(
                                    motion.time()
                                            * 0.32F
                                            + i * 0.9F
                            )
                                    * (
                                    2.5F
                                            + motion.wind()
                                            * 5.0F
                            )
                    )
                            * (
                            0.55F
                                    + i * 0.18F
                    );

            pose.mulPose(
                    Axis.XP.rotationDegrees(
                            swing
                    )
            );

            pose.mulPose(
                    Axis.ZP.rotationDegrees(
                            motion.side()
                                    * 8.0F
                                    * (
                                    i + 1
                            )
                                    / gears
                    )
            );

            double scale =
                    0.13
                            - i * 0.014;

            gear(
                    pose,
                    blocks,
                    buffers,
                    light,
                    metal,
                    scale,
                    motion.time()
                            * (
                            8.0F
                                    + i * 2.0F
                    )
            );

            // Little hanging link below each gear.
            if (i < gears - 1) {
                piece(pose, blocks, buffers, light,
                        Blocks.CHAIN.defaultBlockState(),
                        0.0, 0.13, 0.0,
                        0.035, 0.17, 0.035,
                        0, 0, 0);
            }

            pose.popPose();
        }

        pose.popPose();
    }

    private static void apron(
            ModelPart body,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            RenderPlayerEvent.Post event
    ) {
        pose.pushPose();
        body.translateAndRotate(
                pose
        );

        MotionSample motion =
                motion(
                        event
                );

        BlockState cloth =
                wear == 0
                        ? Blocks.WHITE_WOOL.defaultBlockState()
                        : wear == 1
                        ? Blocks.LIGHT_GRAY_WOOL.defaultBlockState()
                        : Blocks.GRAY_WOOL.defaultBlockState();

        // Waist strap.
        piece(pose, blocks, buffers, light,
                Blocks.RED_WOOL.defaultBlockState(),
                0.0, 0.42, -0.19,
                0.58, 0.065, 0.04,
                0, 0, 0);

        pose.translate(
                0.0,
                0.44,
                -0.205
        );

        pose.mulPose(
                Axis.XP.rotationDegrees(
                        -motion.back()
                                * 5.0F
                                - motion.speed()
                                * 3.0F
                )
        );

        piece(pose, blocks, buffers, light, cloth,
                wear == 2 ? -0.055 : 0.0,
                0.20,
                0.0,
                wear == 2 ? 0.40 : 0.50,
                wear == 2 ? 0.36 : 0.43,
                0.04,
                0, 0,
                wear == 2 ? -5 : 0);

        if (wear >= 1) {
            piece(pose, blocks, buffers, light,
                    Blocks.BROWN_WOOL.defaultBlockState(),
                    0.11, 0.19, -0.03,
                    0.14, 0.10, 0.025,
                    0, 0, 6);
        }

        pose.popPose();
    }

    private static void gear(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            BlockState metal,
            double scale,
            float spin
    ) {
        pose.pushPose();
        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        spin
                )
        );

        piece(pose, blocks, buffers, light, metal,
                0.0, 0.0, 0.0,
                scale * 2.4, scale * 0.50, scale * 0.42,
                0, 0, 0);

        piece(pose, blocks, buffers, light, metal,
                0.0, 0.0, 0.0,
                scale * 0.50, scale * 2.4, scale * 0.42,
                0, 0, 0);

        piece(pose, blocks, buffers, light,
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                0.0, 0.0, -scale * 0.23,
                scale * 0.60, scale * 0.60, scale * 0.22,
                0, 0, 0);

        pose.popPose();
    }

    private static BlockState wornFabric(
            BlockState normal,
            int wear,
            BlockState worn
    ) {
        return wear == 0
                ? normal
                : worn;
    }

    private static MotionSample motion(
            RenderPlayerEvent.Post event
    ) {
        Vec3 velocity =
                event.getEntity()
                        .getDeltaMovement();

        float speed =
                Mth.clamp(
                        (float) velocity.horizontalDistance()
                                * 3.2F,
                        0.0F,
                        1.0F
                );

        float fall =
                Mth.clamp(
                        (float) (
                                -velocity.y
                                        * 2.8
                        ),
                        -0.65F,
                        1.0F
                );

        float time =
                event.getEntity()
                        .tickCount
                        + event.getPartialTick();

        float wind =
                0.0F;

        float windBack =
                0.0F;

        float windSide =
                0.0F;

        if (event.getEntity()
                .level()
                .dimension()
                .equals(
                        Level.OVERWORLD
                )) {
            LocalWeatherField.Sample sample =
                    LocalWeatherField.sample(
                            event.getEntity()
                                    .getX(),
                            event.getEntity()
                                    .getZ(),
                            event.getEntity()
                                    .level()
                                    .getGameTime()
                    );

            wind =
                    sample.warning();

            double yaw =
                    Math.toRadians(
                            event.getEntity()
                                    .getYRot()
                    );

            double forwardX =
                    -Math.sin(
                            yaw
                    );

            double forwardZ =
                    Math.cos(
                            yaw
                    );

            double rightX =
                    Math.cos(
                            yaw
                    );

            double rightZ =
                    Math.sin(
                            yaw
                    );

            windBack =
                    (float) (
                            -(
                                    sample.windX()
                                            * forwardX
                                            + sample.windZ()
                                            * forwardZ
                            )
                                    * wind
                    );

            windSide =
                    (float) (
                            (
                                    sample.windX()
                                            * rightX
                                            + sample.windZ()
                                            * rightZ
                            )
                                    * wind
                    );
        }

        return new MotionSample(
                time,
                speed,
                fall,
                wind,
                Mth.clamp(
                        windBack,
                        -1.0F,
                        1.0F
                ),
                Mth.clamp(
                        windSide,
                        -1.0F,
                        1.0F
                )
        );
    }

    private static void piece(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            BlockState material,
            double x,
            double y,
            double z,
            double sizeX,
            double sizeY,
            double sizeZ,
            float yaw,
            float pitch,
            float roll
    ) {
        pose.pushPose();

        pose.translate(
                x,
                y,
                z
        );

        if (yaw != 0.0F) {
            pose.mulPose(
                    Axis.YP.rotationDegrees(
                            yaw
                    )
            );
        }

        if (pitch != 0.0F) {
            pose.mulPose(
                    Axis.XP.rotationDegrees(
                            pitch
                    )
            );
        }

        if (roll != 0.0F) {
            pose.mulPose(
                    Axis.ZP.rotationDegrees(
                            roll
                    )
            );
        }

        pose.scale(
                (float) sizeX,
                (float) sizeY,
                (float) sizeZ
        );

        pose.translate(
                -0.5,
                -0.5,
                -0.5
        );

        blocks.renderSingleBlock(
                material,
                pose,
                buffers,
                light,
                OverlayTexture.NO_OVERLAY
        );

        pose.popPose();
    }

    private record MotionSample(
            float time,
            float speed,
            float fall,
            float wind,
            float back,
            float side
    ) {
    }
}
