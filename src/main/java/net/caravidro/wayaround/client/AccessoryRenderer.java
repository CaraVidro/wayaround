package net.caravidro.wayaround.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.accessory.AccessoryCustomizationData;
import net.caravidro.wayaround.accessory.AccessoryKind;
import net.caravidro.wayaround.accessory.AccessoryMotion;
import net.caravidro.wayaround.accessory.AccessorySlot;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Procedural textured clothing attached directly to vanilla PlayerModel bones.
 *
 * Every solid piece is rendered through Minecraft's BlockRenderDispatcher,
 * which means the equipment uses real vanilla block textures rather than flat
 * debug colours. Bone-local placement makes sleeves/boots/hats follow whatever
 * pose an animation has already applied to the player model.
 */
public final class AccessoryRenderer {

    private AccessoryRenderer() {
    }

    private static final Map<UUID, float[]> EYE_PUPILS =
            new HashMap<>();

    private record RenderContext(
            AbstractClientPlayer player,
            float partialTick
    ) {
    }

    /**
     * Third-person entry point used by AccessoryPlayerLayer.
     *
     * The incoming pose stack is already inside LivingEntityRenderer's player
     * transform. Do not re-apply body yaw, the -Y model flip or the vanilla
     * 1.501 body translation here: doing that was the reason accessories
     * floated during swimming, fall-flying and cinematic rotations.
     */
    public static void renderAttached(
            AbstractClientPlayer player,
            PlayerModel<?> model,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            float partialTick
    ) {
        if (!WorldFeatureRuntime.clientEnabled(
                WorldFeature.ACCESSORIES
        )
                || player.isInvisible()) {
            return;
        }

        AccessoryClientState.State state =
                AccessoryClientState.get(
                        player.getUUID()
                );

        if (state == null
                || state.empty()) {
            return;
        }

        BlockRenderDispatcher blocks =
                Minecraft.getInstance()
                        .getBlockRenderer();

        RenderContext context =
                new RenderContext(
                        player,
                        partialTick
                );

        pose.pushPose();

        renderHead(
                state,
                pose,
                model,
                blocks,
                buffers,
                light,
                context
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
                light,
                context
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
                context
        );

        renderExtra(
                state,
                pose,
                model,
                blocks,
                buffers,
                light,
                context
        );

        pose.popPose();
    }

    /**
     * First person only renders vanilla arms, not the whole player model.
     * Mirror every accessory component that physically belongs to the visible
     * arm: jacket/shirt sleeves and hand-slot gloves. Head, torso core, legs,
     * feet and back remain correctly absent because vanilla does not render
     * those body bones in first person.
     */
    public static void renderFirstPersonArm(
            AbstractClientPlayer player,
            ModelPart arm,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            boolean left
    ) {
        if (!WorldFeatureRuntime.clientEnabled(
                WorldFeature.ACCESSORIES
        )
                || player.isInvisible()) {
            return;
        }

        AccessoryClientState.State state =
                AccessoryClientState.get(
                        player.getUUID()
                );

        if (state == null
                || state.empty()) {
            return;
        }

        BlockRenderDispatcher blocks =
                Minecraft.getInstance()
                        .getBlockRenderer();

        AccessoryKind torso =
                state.kind(
                        AccessorySlot.TORSO
                );

        if (torso != null) {
            int wear =
                    state.wearStage(
                            AccessorySlot.TORSO
                    );

            switch (torso) {
                case ENGINEER_JACKET -> {
                    BlockState cloth =
                            wear == 2
                                    ? Blocks.BROWN_TERRACOTTA.defaultBlockState()
                                    : Blocks.BROWN_WOOL.defaultBlockState();

                    sleeve(
                            arm,
                            pose,
                            blocks,
                            buffers,
                            light,
                            wear,
                            cloth,
                            Blocks.CUT_COPPER.defaultBlockState(),
                            left
                    );
                }

                case AERO_JACKET -> {
                    BlockState cloth =
                            wornFabric(
                                    Blocks.BLUE_WOOL.defaultBlockState(),
                                    wear,
                                    Blocks.GRAY_WOOL.defaultBlockState()
                            );

                    sleeve(
                            arm,
                            pose,
                            blocks,
                            buffers,
                            light,
                            wear,
                            cloth,
                            Blocks.CUT_COPPER.defaultBlockState(),
                            left
                    );
                }

                case DIVIN_SUIT -> {
                    BlockState cloth =
                            wornFabric(
                                    Blocks.DARK_PRISMARINE.defaultBlockState(),
                                    wear,
                                    Blocks.GRAY_WOOL.defaultBlockState()
                            );

                    sleeve(
                            arm,
                            pose,
                            blocks,
                            buffers,
                            light,
                            wear,
                            cloth,
                            Blocks.SEA_LANTERN.defaultBlockState(),
                            left
                    );
                }

                case CHEF_COAT -> {
                    BlockState cloth =
                            wear == 0
                                    ? Blocks.WHITE_WOOL.defaultBlockState()
                                    : wear == 1
                                    ? Blocks.LIGHT_GRAY_WOOL.defaultBlockState()
                                    : Blocks.GRAY_WOOL.defaultBlockState();

                    sleeve(
                            arm,
                            pose,
                            blocks,
                            buffers,
                            light,
                            wear,
                            cloth,
                            Blocks.LIGHT_GRAY_WOOL.defaultBlockState(),
                            left
                    );
                }

                case FORMAL_JACKET ->
                        formalSleeve(
                                arm,
                                pose,
                                blocks,
                                buffers,
                                light,
                                AccessoryCustomizationData.woolState(
                                        state.customColor(
                                                AccessorySlot.TORSO
                                        )
                                ),
                                left,
                                wear
                        );

                case CASUAL_SHIRT ->
                        casualSleeve(
                                arm,
                                pose,
                                blocks,
                                buffers,
                                light,
                                AccessoryCustomizationData.woolState(
                                        state.customColor(
                                                AccessorySlot.TORSO
                                        )
                                ),
                                left,
                                wear
                        );

                case RAILWAY_COAT ->
                        sleeve(arm, pose, blocks, buffers, light, wear,
                                Blocks.BLUE_WOOL.defaultBlockState(),
                                Blocks.IRON_BLOCK.defaultBlockState(), left);

                case MINER_JACKET ->
                        sleeve(arm, pose, blocks, buffers, light, wear,
                                Blocks.GRAY_WOOL.defaultBlockState(),
                                Blocks.CUT_COPPER.defaultBlockState(), left);

                case STORM_COAT ->
                        sleeve(arm, pose, blocks, buffers, light, wear,
                                Blocks.LIGHT_BLUE_WOOL.defaultBlockState(),
                                Blocks.CUT_COPPER.defaultBlockState(), left);

                case NATURALIST_COAT ->
                        sleeve(arm, pose, blocks, buffers, light, wear,
                                Blocks.GREEN_WOOL.defaultBlockState(),
                                Blocks.BROWN_TERRACOTTA.defaultBlockState(), left);

                case ARCTIC_PARKA ->
                        sleeve(arm, pose, blocks, buffers, light, wear,
                                Blocks.WHITE_WOOL.defaultBlockState(),
                                Blocks.LIGHT_GRAY_WOOL.defaultBlockState(), left);

                default -> {
                }
            }
        }

        AccessoryKind hands =
                state.kind(
                        AccessorySlot.HANDS
                );

        if (hands == null) {
            return;
        }

        int wear =
                state.wearStage(
                        AccessorySlot.HANDS
                );

        BlockState fabric =
                switch (hands) {
                    case CHEF_GLOVES ->
                            Blocks.WHITE_WOOL.defaultBlockState();
                    case AERO_GLOVES ->
                            Blocks.GRAY_WOOL.defaultBlockState();
                    default ->
                            Blocks.BROWN_WOOL.defaultBlockState();
                };

        glove(
                arm,
                pose,
                blocks,
                buffers,
                light,
                wear,
                fabric,
                left,
                hands
        );
    }

    /** Opt-in visual QA uses exactly the worn set's bone-local rendering helpers. */
    public static void renderChefPreview(PoseStack pose, BlockRenderDispatcher blocks,
            MultiBufferSource buffers, int light) {
        var model = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(
                net.minecraft.client.model.geom.ModelLayers.PLAYER), false);
        String[] kinds = new String[AccessorySlot.values().length];
        java.util.Arrays.fill(kinds, "");
        for (var kind : new AccessoryKind[]{AccessoryKind.CHEF_HAT, AccessoryKind.CHEF_COAT,
                AccessoryKind.CHEF_GLOVES, AccessoryKind.CHEF_TROUSERS, AccessoryKind.CHEF_SHOES,
                AccessoryKind.CHEF_APRON}) kinds[kind.slot().ordinal()] = kind.path();
        var state = new AccessoryClientState.State(kinds, new int[kinds.length], new int[kinds.length],
                0, ItemStack.EMPTY, 0, 2, 0, 0, new int[kinds.length], 0);
        // Neutral head only for the QA mannequin; equipment still uses its normal worn helpers.
        piece(pose, blocks, buffers, light, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                0, -.25, 0, .5, .5, .5, 0, 0, 0);
        renderHead(state, pose, model, blocks, buffers, light, null);
        renderTorso(state, pose, model, blocks, buffers, light, null);
        renderHands(state, pose, model, blocks, buffers, light);
        renderLegs(state, pose, model, blocks, buffers, light);
        renderFeet(state, pose, model, blocks, buffers, light);
        apron(model.body, pose, blocks, buffers, light, 0, null);
    }

    private static void renderHead(
            AccessoryClientState.State state,
            PoseStack pose,
            PlayerModel<?> model,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            RenderContext context
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
                            context,
                            state.headMaterial(),
                            state.headSize(),
                            state.headExtras()
                    );

            case CUSTOM_HAT ->
                    customHat(
                            wear,
                            pose,
                            blocks,
                            buffers,
                            light,
                            state.headMaterial(),
                            state.headSize()
                    );

            case SOMBRERO ->
                    sombrero(
                            wear,
                            pose,
                            blocks,
                            buffers,
                            light,
                            state.headMaterial(),
                            state.headSize(),
                            state.headWoolColor()
                    );

            case AERO_ENGINEER_CAP ->
                    engineerCap(
                            wear,
                            pose,
                            blocks,
                            buffers,
                            light,
                            true,
                            context,
                            0,
                            2,
                            0
                    );

            case CHEF_HAT ->
                    chefHat(
                            wear,
                            pose,
                            blocks,
                            buffers,
                            light
                    );

            case RAILWAY_CAP,
                 MINER_HELMET,
                 STORM_HAT,
                 NATURALIST_HAT,
                 ARCTIC_CAP ->
                    kitHat(
                            kind,
                            wear,
                            pose,
                            blocks,
                            buffers,
                            light,
                            context
                    );

            case WATCHING_EYE ->
                    watchingEye(
                            pose,
                            blocks,
                            buffers,
                            light,
                            context
                    );

            case CARDBOARD_BOX ->
                    cardboardBox(
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

            case RAILWAY_GOGGLES ->
                    goggles(
                            pose, blocks, buffers, light, wear, glass,
                            Blocks.IRON_BLOCK.defaultBlockState(),
                            Blocks.ORANGE_STAINED_GLASS.defaultBlockState(),
                            false
                    );

            case MINER_GOGGLES ->
                    goggles(
                            pose, blocks, buffers, light, wear, glass,
                            Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                            Blocks.CYAN_STAINED_GLASS.defaultBlockState(),
                            true
                    );

            case STORM_VISOR ->
                    goggles(
                            pose, blocks, buffers, light, wear, glass,
                            Blocks.CUT_COPPER.defaultBlockState(),
                            Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(),
                            true
                    );

            case ARCTIC_GOGGLES ->
                    goggles(
                            pose, blocks, buffers, light, wear, glass,
                            Blocks.BROWN_WOOL.defaultBlockState(),
                            Blocks.ORANGE_STAINED_GLASS.defaultBlockState(),
                            true
                    );

            case GAS_MASK ->
                    gasMask(
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

    private static void renderTorso(
            AccessoryClientState.State state,
            PoseStack pose,
            PlayerModel<?> model,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            RenderContext context
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

            case DIVIN_SUIT ->
                    jacket(
                            model,
                            pose,
                            blocks,
                            buffers,
                            light,
                            wear,
                            Blocks.DARK_PRISMARINE.defaultBlockState(),
                            Blocks.SEA_LANTERN.defaultBlockState(),
                            true
                    );

            case FORMAL_JACKET ->
                    formalJacket(
                            model,
                            pose,
                            blocks,
                            buffers,
                            light,
                            wear,
                            AccessoryCustomizationData.woolState(
                                    state.customColor(
                                            AccessorySlot.TORSO
                                    )
                            )
                    );

            case CASUAL_SHIRT ->
                    casualShirt(
                            model,
                            pose,
                            blocks,
                            buffers,
                            light,
                            wear,
                            AccessoryCustomizationData.woolState(
                                    state.customColor(
                                            AccessorySlot.TORSO
                                    )
                            )
                    );

            case RAILWAY_COAT ->
                    kitCoat(
                            model, pose, blocks, buffers, light, wear, context,
                            Blocks.BLUE_WOOL.defaultBlockState(),
                            Blocks.IRON_BLOCK.defaultBlockState(),
                            0.45F,
                            0
                    );

            case MINER_JACKET ->
                    kitCoat(
                            model, pose, blocks, buffers, light, wear, context,
                            Blocks.GRAY_WOOL.defaultBlockState(),
                            Blocks.CUT_COPPER.defaultBlockState(),
                            0.18F,
                            1
                    );

            case STORM_COAT ->
                    kitCoat(
                            model, pose, blocks, buffers, light, wear, context,
                            Blocks.LIGHT_BLUE_WOOL.defaultBlockState(),
                            Blocks.CUT_COPPER.defaultBlockState(),
                            1.25F,
                            2
                    );

            case NATURALIST_COAT ->
                    kitCoat(
                            model, pose, blocks, buffers, light, wear, context,
                            Blocks.GREEN_WOOL.defaultBlockState(),
                            Blocks.BROWN_TERRACOTTA.defaultBlockState(),
                            0.70F,
                            3
                    );

            case ARCTIC_PARKA ->
                    kitCoat(
                            model, pose, blocks, buffers, light, wear, context,
                            Blocks.WHITE_WOOL.defaultBlockState(),
                            Blocks.LIGHT_GRAY_WOOL.defaultBlockState(),
                            0.22F,
                            4
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
                            Blocks.GRAY_WOOL.defaultBlockState();
                    case RAILWAY_TROUSERS ->
                            Blocks.BLUE_WOOL.defaultBlockState();
                    case MINER_TROUSERS ->
                            Blocks.GRAY_WOOL.defaultBlockState();
                    case STORM_TROUSERS ->
                            Blocks.DARK_PRISMARINE.defaultBlockState();
                    case NATURALIST_TROUSERS ->
                            Blocks.GREEN_WOOL.defaultBlockState();
                    case ARCTIC_TROUSERS ->
                            Blocks.WHITE_WOOL.defaultBlockState();
                    default ->
                            Blocks.BROWN_WOOL.defaultBlockState();
                };

        if (kind == AccessoryKind.ENGINEER_TROUSERS) {
            engineerTrousers(
                    model,
                    pose,
                    blocks,
                    buffers,
                    light,
                    wear,
                    state.trouserPocket()
            );
            return;
        }

        if (kind == AccessoryKind.FORMAL_TROUSERS
                || kind == AccessoryKind.CASUAL_TROUSERS) {
            conventionalTrousers(
                    model,
                    pose,
                    blocks,
                    buffers,
                    light,
                    wear,
                    AccessoryCustomizationData.woolState(
                            state.customColor(
                                    AccessorySlot.LEGS
                            )
                    ),
                    kind == AccessoryKind.FORMAL_TROUSERS
            );
            return;
        }

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
                    case SUSTENTION_BOOTS ->
                            Blocks.IRON_BLOCK.defaultBlockState();
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
            RenderContext context
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
                context
        );
    }

    private static void renderExtra(
            AccessoryClientState.State state,
            PoseStack pose,
            PlayerModel<?> model,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            RenderContext context
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
                            context
                    );

            case CHEF_APRON ->
                    apron(
                            model.body,
                            pose,
                            blocks,
                            buffers,
                            light,
                            wear,
                            context
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
            RenderContext context,
            int material,
            int size,
            int extras
    ) {
        if (!aero) {
            MotionSample motion =
                    context == null
                            ? new MotionSample(0, 0, 0, 0, 0, 0)
                            : motion(
                                    context
                            );

            if (context == null) {
                TopHatModelRenderer.render(
                        pose,
                        blocks,
                        buffers,
                        light,
                        wear,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        material,
                        size,
                        extras
                );
                return;
            }

            float instability =
                    TopHatClientState.instability(
                            context.player()
                                    .getUUID()
                    );

            if (TopHatClientState.warningActive(
                    context.player()
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
                    motion.time(),
                    material,
                    size,
                    extras
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

    private static void watchingEye(
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            RenderContext context
    ) {
        BlockState white =
                Blocks.QUARTZ_BLOCK.defaultBlockState();
        BlockState iris =
                Blocks.CYAN_CONCRETE.defaultBlockState();
        BlockState pupil =
                Blocks.BLACK_CONCRETE.defaultBlockState();

        // Hollow six-sided shell replaces the visual silhouette of the head.
        piece(pose, blocks, buffers, light, white,
                0.0, -0.255, 0.305,
                0.625, 0.575, 0.040,
                0, 0, 0);
        piece(pose, blocks, buffers, light, white,
                -0.305, -0.255, 0.0,
                0.040, 0.575, 0.625,
                0, 0, 0);
        piece(pose, blocks, buffers, light, white,
                0.305, -0.255, 0.0,
                0.040, 0.575, 0.625,
                0, 0, 0);
        piece(pose, blocks, buffers, light, white,
                0.0, -0.535, 0.0,
                0.625, 0.040, 0.625,
                0, 0, 0);
        piece(pose, blocks, buffers, light, white,
                0.0, 0.025, 0.0,
                0.625, 0.040, 0.625,
                0, 0, 0);

        // Front sclera is a frame around the moving iris.
        piece(pose, blocks, buffers, light, white,
                0.0, -0.462, -0.315,
                0.625, 0.155, 0.035,
                0, 0, 0);
        piece(pose, blocks, buffers, light, white,
                0.0, -0.048, -0.315,
                0.625, 0.155, 0.035,
                0, 0, 0);
        piece(pose, blocks, buffers, light, white,
                -0.250, -0.255, -0.315,
                0.125, 0.300, 0.035,
                0, 0, 0);
        piece(pose, blocks, buffers, light, white,
                0.250, -0.255, -0.315,
                0.125, 0.300, 0.035,
                0, 0, 0);

        float[] offset =
                eyePupilOffset(
                        context
                );

        double pupilX =
                offset[0];

        double pupilY =
                offset[1];

        piece(pose, blocks, buffers, light, iris,
                pupilX, -0.255 + pupilY, -0.342,
                0.285, 0.285, 0.032,
                0, 0, 0);

        piece(pose, blocks, buffers, light, pupil,
                pupilX, -0.255 + pupilY, -0.366,
                0.130, 0.130, 0.026,
                0, 0, 0);

        // Tiny reflected glint makes the eye feel wet/alive at a glance.
        piece(pose, blocks, buffers, light,
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                pupilX - 0.033,
                -0.292 + pupilY,
                -0.384,
                0.035, 0.035, 0.012,
                0, 0, 0);
    }

    private static float[] eyePupilOffset(
            RenderContext context
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        Vec3 camera =
                minecraft.gameRenderer
                        .getMainCamera()
                        .getPosition();

        Vec3 eye =
                context.player()
                        .getEyePosition();

        double dx =
                camera.x
                        - eye.x;
        double dy =
                camera.y
                        - eye.y;
        double dz =
                camera.z
                        - eye.z;

        double horizontal =
                Math.sqrt(
                        dx * dx
                                + dz * dz
                );

        float targetYaw =
                (float) Math.toDegrees(
                        Math.atan2(
                                -dx,
                                dz
                        )
                );

        float relativeYaw =
                Mth.wrapDegrees(
                        targetYaw
                                - context.player()
                                .getYHeadRot()
                );

        float targetPitch =
                (float) -Math.toDegrees(
                        Math.atan2(
                                dy,
                                Math.max(
                                        0.001,
                                        horizontal
                                )
                        )
                );

        float relativePitch =
                Mth.wrapDegrees(
                        targetPitch
                                - context.player()
                                .getXRot()
                );

        float wantedX =
                Mth.clamp(
                        relativeYaw
                                / 75.0F
                                * 0.078F,
                        -0.078F,
                        0.078F
                );

        float wantedY =
                Mth.clamp(
                        relativePitch
                                / 65.0F
                                * 0.060F,
                        -0.060F,
                        0.060F
                );

        if (EYE_PUPILS.size()
                > 128) {
            EYE_PUPILS.clear();
        }

        float[] current =
                EYE_PUPILS.computeIfAbsent(
                        context.player()
                                .getUUID(),
                        ignored ->
                                new float[]{
                                        0.0F,
                                        0.0F
                                }
                );

        float time =
                context.player()
                        .tickCount;

        wantedX +=
                Mth.sin(
                        time * 0.035F
                ) * 0.006F;

        wantedY +=
                Mth.cos(
                        time * 0.027F
                ) * 0.004F;

        current[0] =
                Mth.lerp(
                        0.14F,
                        current[0],
                        wantedX
                );

        current[1] =
                Mth.lerp(
                        0.14F,
                        current[1],
                        wantedY
                );

        return current;
    }

    private static void cardboardBox(
            int wear,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light
    ) {
        BlockState cardboard =
                wear >= 2
                        ? Blocks.BROWN_TERRACOTTA.defaultBlockState()
                        : Blocks.BROWN_WOOL.defaultBlockState();

        BlockState tape =
                Blocks.SMOOTH_SANDSTONE.defaultBlockState();

        // Four walls + roof. Bottom stays open around the neck.
        piece(pose, blocks, buffers, light, cardboard,
                0.0, -0.260, -0.335,
                0.700, 0.610, 0.045,
                0, 0, wear >= 2 ? 3 : 0);
        piece(pose, blocks, buffers, light, cardboard,
                0.0, -0.260, 0.335,
                0.700, 0.610, 0.045,
                0, 0, 0);
        piece(pose, blocks, buffers, light, cardboard,
                -0.335, -0.260, 0.0,
                0.045, 0.610, 0.700,
                0, 0, 0);
        piece(pose, blocks, buffers, light, cardboard,
                0.335, -0.260, 0.0,
                0.045, 0.610, 0.700,
                0, 0, wear >= 1 ? -3 : 0);
        piece(pose, blocks, buffers, light, cardboard,
                0.0, -0.565, 0.0,
                0.700, 0.045, 0.700,
                0, 0, 0);

        // Packing tape crosses the top and continues down front/back.
        piece(pose, blocks, buffers, light, tape,
                0.0, -0.590, 0.0,
                0.095, 0.018, 0.705,
                0, 0, 0);
        piece(pose, blocks, buffers, light, tape,
                0.0, -0.250, -0.362,
                0.095, 0.520, 0.018,
                0, 0, 0);
        piece(pose, blocks, buffers, light, tape,
                0.0, -0.250, 0.362,
                0.095, 0.520, 0.018,
                0, 0, 0);

        // Cut-out viewing slits.
        piece(pose, blocks, buffers, light,
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                -0.145, -0.275, -0.370,
                0.155, 0.070, 0.020,
                0, 0, -4);
        piece(pose, blocks, buffers, light,
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                0.145, -0.275, -0.370,
                0.155, 0.070, 0.020,
                0, 0, 4);

        if (wear >= 1) {
            piece(pose, blocks, buffers, light,
                    Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                    0.230, -0.455, -0.372,
                    0.150, 0.040, 0.018,
                    0, 0, -18);
        }
    }

    private static void gasMask(
            int wear,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light
    ) {
        BlockState rubber =
                wear >= 2
                        ? Blocks.GRAY_CONCRETE.defaultBlockState()
                        : Blocks.BLACK_CONCRETE.defaultBlockState();

        BlockState metal =
                Blocks.POLISHED_DEEPSLATE.defaultBlockState();

        // Main face seal.
        piece(pose, blocks, buffers, light, rubber,
                0.0, -0.155, -0.327,
                0.470, 0.360, 0.060,
                0, 0, 0);

        // Twin glass lenses with thick rims.
        for (int side : new int[]{-1, 1}) {
            double x =
                    side * 0.145;

            piece(pose, blocks, buffers, light, metal,
                    x, -0.230, -0.371,
                    0.175, 0.155, 0.035,
                    0, 0, 0);

            piece(pose, blocks, buffers, light,
                    Blocks.TINTED_GLASS.defaultBlockState(),
                    x, -0.230, -0.395,
                    0.125, 0.105, 0.018,
                    0, 0, 0);
        }

        // Nose bridge and useless filter canister.
        piece(pose, blocks, buffers, light, rubber,
                0.0, -0.105, -0.385,
                0.145, 0.180, 0.075,
                0, 0, 0);
        piece(pose, blocks, buffers, light, metal,
                0.0, 0.025, -0.410,
                0.205, 0.150, 0.115,
                0, 0, 0);
        piece(pose, blocks, buffers, light,
                Blocks.IRON_BARS.defaultBlockState(),
                0.0, 0.028, -0.475,
                0.150, 0.095, 0.025,
                0, 0, 0);

        // Side and rear straps make the mask readable from 360°.
        if (wear < 2) {
            piece(pose, blocks, buffers, light, rubber,
                    -0.295, -0.210, -0.025,
                    0.035, 0.050, 0.560,
                    0, 0, 0);
            piece(pose, blocks, buffers, light, rubber,
                    0.295, -0.210, -0.025,
                    0.035, 0.050, 0.560,
                    0, 0, 0);
            piece(pose, blocks, buffers, light, rubber,
                    0.0, -0.210, 0.278,
                    0.560, 0.050, 0.035,
                    0, 0, 0);
        }
    }

    private static void formalJacket(
            PlayerModel<?> model,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            BlockState suit
    ) {
        pose.pushPose();
        model.body.translateAndRotate(
                pose
        );

        // Full conventional suit shell, intentionally cleaner than engineer gear.
        piece(pose, blocks, buffers, light, suit,
                0.0, 0.340, -0.160,
                0.555, 0.700, 0.070,
                0, 0, 0);
        piece(pose, blocks, buffers, light, suit,
                0.0, 0.340, 0.160,
                0.555, 0.700, 0.070,
                0, 0, 0);
        piece(pose, blocks, buffers, light, suit,
                -0.292, 0.340, 0.0,
                0.035, 0.680, 0.300,
                0, 0, 0);
        piece(pose, blocks, buffers, light, suit,
                0.292, 0.340, 0.0,
                0.035, 0.680, 0.300,
                0, 0, 0);

        // Shirt, lapels, tie and buttons.
        piece(pose, blocks, buffers, light,
                Blocks.WHITE_WOOL.defaultBlockState(),
                0.0, 0.245, -0.205,
                0.185, 0.420, 0.028,
                0, 0, 0);
        piece(pose, blocks, buffers, light, suit,
                -0.125, 0.235, -0.225,
                0.150, 0.350, 0.028,
                0, 0, -16);
        piece(pose, blocks, buffers, light, suit,
                0.125, 0.235, -0.225,
                0.150, 0.350, 0.028,
                0, 0, 16);
        piece(pose, blocks, buffers, light,
                Blocks.RED_WOOL.defaultBlockState(),
                0.0, 0.245, -0.244,
                0.055, 0.300, 0.022,
                0, 0, 0);

        for (int i = 0; i < 3; i++) {
            piece(pose, blocks, buffers, light,
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                    0.105, 0.300 + i * 0.120, -0.238,
                    0.028, 0.028, 0.018,
                    0, 0, 0);
        }

        if (wear >= 1) {
            piece(pose, blocks, buffers, light,
                    Blocks.GRAY_WOOL.defaultBlockState(),
                    -0.180, 0.505, -0.235,
                    0.120, 0.090, 0.020,
                    0, 0, -8);
        }

        pose.popPose();

        formalSleeve(model.leftArm, pose, blocks, buffers, light, suit, true, wear);
        formalSleeve(model.rightArm, pose, blocks, buffers, light, suit, false, wear);
    }

    private static void formalSleeve(
            ModelPart arm,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            BlockState suit,
            boolean left,
            int wear
    ) {
        pose.pushPose();
        arm.translateAndRotate(
                pose
        );

        piece(pose, blocks, buffers, light, suit,
                0.0, 0.315, 0.0,
                wear >= 2 && !left ? 0.265 : 0.310,
                wear >= 2 && !left ? 0.480 : 0.610,
                0.310,
                0, 0, wear >= 2 && !left ? 5 : 0);

        piece(pose, blocks, buffers, light,
                Blocks.WHITE_WOOL.defaultBlockState(),
                0.0, 0.595, -0.165,
                0.250, 0.055, 0.030,
                0, 0, 0);

        pose.popPose();
    }

    private static void casualShirt(
            PlayerModel<?> model,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            BlockState cloth
    ) {
        pose.pushPose();
        model.body.translateAndRotate(
                pose
        );

        // Simple T-shirt volume with front/back/side readability.
        piece(pose, blocks, buffers, light, cloth,
                0.0, 0.310, -0.158,
                0.545, 0.620, 0.065,
                0, 0, 0);
        piece(pose, blocks, buffers, light, cloth,
                0.0, 0.310, 0.158,
                0.545, 0.620, 0.065,
                0, 0, 0);
        piece(pose, blocks, buffers, light, cloth,
                -0.286, 0.300, 0.0,
                0.035, 0.590, 0.300,
                0, 0, 0);
        piece(pose, blocks, buffers, light, cloth,
                0.286, 0.300, 0.0,
                0.035, 0.590, 0.300,
                0, 0, 0);

        // Collar ring impression.
        piece(pose, blocks, buffers, light,
                Blocks.LIGHT_GRAY_WOOL.defaultBlockState(),
                0.0, 0.045, -0.195,
                0.245, 0.055, 0.025,
                0, 0, 0);

        if (wear >= 1) {
            piece(pose, blocks, buffers, light,
                    Blocks.GRAY_WOOL.defaultBlockState(),
                    0.190, 0.440, -0.205,
                    0.115, 0.085, 0.020,
                    0, 0, 7);
        }

        pose.popPose();

        casualSleeve(model.leftArm, pose, blocks, buffers, light, cloth, true, wear);
        casualSleeve(model.rightArm, pose, blocks, buffers, light, cloth, false, wear);
    }

    private static void casualSleeve(
            ModelPart arm,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            BlockState cloth,
            boolean left,
            int wear
    ) {
        pose.pushPose();
        arm.translateAndRotate(
                pose
        );

        piece(pose, blocks, buffers, light, cloth,
                0.0, 0.130, 0.0,
                wear >= 2 && !left ? 0.255 : 0.315,
                wear >= 2 && !left ? 0.170 : 0.245,
                0.315,
                0, 0, wear >= 2 && !left ? 6 : 0);

        pose.popPose();
    }

    private static void conventionalTrousers(
            PlayerModel<?> model,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            BlockState cloth,
            boolean formal
    ) {
        conventionalTrouserLeg(
                model.leftLeg,
                pose,
                blocks,
                buffers,
                light,
                wear,
                cloth,
                true,
                formal
        );

        conventionalTrouserLeg(
                model.rightLeg,
                pose,
                blocks,
                buffers,
                light,
                wear,
                cloth,
                false,
                formal
        );
    }

    private static void conventionalTrouserLeg(
            ModelPart leg,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            BlockState cloth,
            boolean left,
            boolean formal
    ) {
        pose.pushPose();
        leg.translateAndRotate(
                pose
        );

        piece(pose, blocks, buffers, light, cloth,
                0.0, 0.315, 0.0,
                wear >= 2 && !left ? 0.270 : 0.315,
                wear >= 2 && !left ? 0.510 : 0.665,
                0.315,
                0, 0, wear >= 2 && !left ? 4 : 0);

        if (formal) {
            // Thin front/back crease gives the suit trousers their tailored look.
            piece(pose, blocks, buffers, light,
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                    0.0, 0.340, -0.170,
                    0.025, 0.545, 0.020,
                    0, 0, 0);
            piece(pose, blocks, buffers, light,
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                    0.0, 0.340, 0.170,
                    0.025, 0.545, 0.020,
                    0, 0, 0);
        } else {
            // Casual side pocket stitch.
            piece(pose, blocks, buffers, light,
                    Blocks.LIGHT_GRAY_WOOL.defaultBlockState(),
                    left ? -0.155 : 0.155,
                    0.210,
                    -0.135,
                    0.025, 0.155, 0.100,
                    0, 0, left ? -10 : 10);
        }

        pose.popPose();
    }

    private static void customHat(
            int wear,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int material,
            int size
    ) {
        BlockState main =
                AccessoryCustomizationData.materialState(
                        material
                );

        BlockState trim =
                wear >= 2
                        ? Blocks.EXPOSED_COPPER.defaultBlockState()
                        : Blocks.CUT_COPPER.defaultBlockState();

        float scale =
                0.84F
                        + AccessoryCustomizationData.clampSize(
                        size
                ) * 0.08F;

        pose.pushPose();
        pose.translate(
                0.0,
                0.055,
                0.0
        );

        // Flat brim, then a low faceted crown. All sides have actual volume.
        piece(pose, blocks, buffers, light, Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                0.0, -0.490, 0.0,
                0.67 * scale, 0.052, 0.58 * scale,
                0, 0, 0);

        piece(pose, blocks, buffers, light, main,
                0.0, -0.565, 0.0,
                0.49 * scale, 0.16, 0.45 * scale,
                0, 0, wear >= 2 ? -3 : 0);

        piece(pose, blocks, buffers, light, main,
                0.0, -0.700, 0.015,
                0.43 * scale, 0.16, 0.39 * scale,
                0, 0, wear >= 2 ? -5 : 0);

        // Front/back band and side clamps make the model readable in 360°.
        piece(pose, blocks, buffers, light, trim,
                0.0, -0.610, -0.230 * scale,
                0.45 * scale, 0.055, 0.035,
                0, 0, 0);
        piece(pose, blocks, buffers, light, trim,
                0.0, -0.610, 0.230 * scale,
                0.45 * scale, 0.055, 0.035,
                0, 0, 0);
        piece(pose, blocks, buffers, light, trim,
                -0.248 * scale, -0.610, 0.0,
                0.035, 0.055, 0.42 * scale,
                0, 0, 0);
        piece(pose, blocks, buffers, light, trim,
                0.248 * scale, -0.610, 0.0,
                0.035, 0.055, 0.42 * scale,
                0, 0, 0);

        if (wear < 2) {
            piece(pose, blocks, buffers, light, trim,
                    0.16 * scale, -0.615, -0.252 * scale,
                    0.11, 0.085, 0.025,
                    0, 0, 0);
        }

        pose.popPose();
    }

    private static void sombrero(
            int wear,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int material,
            int size,
            int woolColor
    ) {
        BlockState main =
                AccessoryCustomizationData.materialState(
                        material
                );

        BlockState wool =
                AccessoryCustomizationData.woolState(
                        woolColor
                );

        double brim =
                0.78
                        + AccessoryCustomizationData.clampSize(
                        size
                ) * 0.11;

        pose.pushPose();
        pose.translate(
                0.0,
                0.060,
                0.0
        );

        // Broad stepped brim. Separate edge panels keep it from looking like a slab.
        piece(pose, blocks, buffers, light, main,
                0.0, -0.485, 0.0,
                brim, 0.045, brim,
                0, 0, 0);

        piece(pose, blocks, buffers, light, wool,
                0.0, -0.505, -brim * 0.48,
                brim * 0.92, 0.032, 0.065,
                0, 0, -3);
        piece(pose, blocks, buffers, light, wool,
                0.0, -0.505, brim * 0.48,
                brim * 0.92, 0.032, 0.065,
                0, 0, 3);
        piece(pose, blocks, buffers, light, wool,
                -brim * 0.48, -0.505, 0.0,
                0.065, 0.032, brim * 0.92,
                0, 0, 3);
        piece(pose, blocks, buffers, light, wool,
                brim * 0.48, -0.505, 0.0,
                0.065, 0.032, brim * 0.92,
                0, 0, -3);

        // Crown tapers upward and is fully closed from all directions.
        piece(pose, blocks, buffers, light, main,
                0.0, -0.610, 0.0,
                0.48, 0.22, 0.48,
                0, 0, wear >= 2 ? 4 : 0);
        piece(pose, blocks, buffers, light, main,
                0.0, -0.760, 0.0,
                0.39, 0.15, 0.39,
                0, 0, wear >= 2 ? 5 : 0);

        // Wool band wraps the full crown rather than only the front.
        piece(pose, blocks, buffers, light, wool,
                0.0, -0.635, -0.252,
                0.49, 0.065, 0.032,
                0, 0, 0);
        piece(pose, blocks, buffers, light, wool,
                0.0, -0.635, 0.252,
                0.49, 0.065, 0.032,
                0, 0, 0);
        piece(pose, blocks, buffers, light, wool,
                -0.252, -0.635, 0.0,
                0.032, 0.065, 0.49,
                0, 0, 0);
        piece(pose, blocks, buffers, light, wool,
                0.252, -0.635, 0.0,
                0.032, 0.065, 0.49,
                0, 0, 0);

        if (wear < 2) {
            for (int side : new int[]{-1, 1}) {
                piece(pose, blocks, buffers, light, wool,
                        side * brim * 0.32, -0.525, -brim * 0.37,
                        0.055, 0.035, 0.055,
                        0, 0, side * 12);
                piece(pose, blocks, buffers, light, wool,
                        side * brim * 0.32, -0.525, brim * 0.37,
                        0.055, 0.035, 0.055,
                        0, 0, side * -12);
            }
        }

        pose.popPose();
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

    private static void engineerTrousers(
            PlayerModel<?> model,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            ItemStack pocketItem
    ) {
        BlockState cloth =
                wear >= 2
                        ? Blocks.BROWN_TERRACOTTA.defaultBlockState()
                        : Blocks.BROWN_WOOL.defaultBlockState();

        BlockState leather =
                Blocks.DARK_OAK_PLANKS.defaultBlockState();

        BlockState brass =
                wear >= 2
                        ? Blocks.EXPOSED_COPPER.defaultBlockState()
                        : Blocks.CUT_COPPER.defaultBlockState();

        /*
         * Waist belt belongs to the torso bone, while every lower piece follows
         * its own leg. This keeps the silhouette coherent from the back and the
         * sides instead of looking like two painted vanilla legs.
         */
        pose.pushPose();
        model.body.translateAndRotate(
                pose
        );

        piece(pose, blocks, buffers, light, leather,
                0.0, 0.705, 0.0,
                0.59, 0.075, 0.36,
                0, 0, 0);

        piece(pose, blocks, buffers, light, brass,
                0.17, 0.705, -0.195,
                0.105, 0.095, 0.028,
                0, 0, 0);

        if (wear < 2) {
            for (int side : new int[]{-1, 1}) {
                piece(pose, blocks, buffers, light, brass,
                        side * 0.24, 0.705, 0.0,
                        0.030, 0.105, 0.37,
                        0, 0, 0);
            }
        }

        pose.popPose();

        engineerTrouserLeg(
                model.leftLeg,
                pose,
                blocks,
                buffers,
                light,
                wear,
                cloth,
                leather,
                brass,
                true,
                ItemStack.EMPTY
        );

        engineerTrouserLeg(
                model.rightLeg,
                pose,
                blocks,
                buffers,
                light,
                wear,
                cloth,
                leather,
                brass,
                false,
                pocketItem == null
                        ? ItemStack.EMPTY
                        : pocketItem
        );
    }

    private static void engineerTrouserLeg(
            ModelPart leg,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            BlockState cloth,
            BlockState leather,
            BlockState brass,
            boolean left,
            ItemStack pocketItem
    ) {
        pose.pushPose();
        leg.translateAndRotate(
                pose
        );

        double width =
                wear >= 2 && !left
                        ? 0.29
                        : 0.335;

        double length =
                wear >= 2 && !left
                        ? 0.54
                        : 0.675;

        // Main wrapped leg volume.
        piece(pose, blocks, buffers, light, cloth,
                0.0, 0.315, 0.0,
                width, length, 0.345,
                0, 0, wear >= 2 && !left ? 3 : 0);

        // Front knee reinforcement.
        piece(pose, blocks, buffers, light, leather,
                0.0, 0.485, -0.182,
                width + 0.015, 0.205, 0.035,
                0, 0, 0);

        // Rear wear panel makes the back as intentional as the front.
        piece(pose, blocks, buffers, light,
                wear >= 2
                        ? Blocks.GRAY_WOOL.defaultBlockState()
                        : Blocks.BROWN_TERRACOTTA.defaultBlockState(),
                0.0, 0.300, 0.181,
                width - 0.040, 0.220, 0.032,
                0, 0, 0);

        // Outer seam and inner seam.
        double outer =
                left
                        ? -0.178
                        : 0.178;

        piece(pose, blocks, buffers, light, leather,
                outer, 0.315, 0.0,
                0.035, length - 0.045, 0.330,
                0, 0, 0);

        piece(pose, blocks, buffers, light, brass,
                -outer, 0.330, 0.0,
                0.022, length - 0.110, 0.315,
                0, 0, 0);

        // Thigh strap wraps visually around all four sides.
        double strapY =
                0.205;

        piece(pose, blocks, buffers, light, leather,
                0.0, strapY, -0.180,
                width + 0.035, 0.060, 0.035,
                0, 0, 0);
        piece(pose, blocks, buffers, light, leather,
                0.0, strapY, 0.180,
                width + 0.035, 0.060, 0.035,
                0, 0, 0);
        piece(pose, blocks, buffers, light, leather,
                -0.180, strapY, 0.0,
                0.035, 0.060, 0.335,
                0, 0, 0);
        piece(pose, blocks, buffers, light, leather,
                0.180, strapY, 0.0,
                0.035, 0.060, 0.335,
                0, 0, 0);

        if (wear < 2) {
            piece(pose, blocks, buffers, light, brass,
                    outer, 0.485, -0.205,
                    0.060, 0.070, 0.025,
                    0, 0, 0);

            piece(pose, blocks, buffers, light, brass,
                    outer, 0.485, 0.205,
                    0.060, 0.070, 0.025,
                    0, 0, 0);
        }

        /*
         * Single utility pocket lives on the outside of the right thigh.
         * Render the contained item first and the pocket wall second: depth
         * testing naturally hides its lower half, so it actually looks tucked
         * inside instead of pasted onto the front of the trousers.
         */
        if (!left) {
            if (pocketItem != null
                    && !pocketItem.isEmpty()) {
                pose.pushPose();

                pose.translate(
                        0.196,
                        0.145,
                        0.015
                );

                pose.mulPose(
                        Axis.YP.rotationDegrees(
                                -90.0F
                        )
                );

                pose.mulPose(
                        Axis.ZP.rotationDegrees(
                                -7.0F
                        )
                );

                pose.scale(
                        0.24F,
                        0.24F,
                        0.24F
                );

                Minecraft minecraft =
                        Minecraft.getInstance();

                minecraft.getItemRenderer()
                        .renderStatic(
                                pocketItem,
                                ItemDisplayContext.FIXED,
                                light,
                                OverlayTexture.NO_OVERLAY,
                                pose,
                                buffers,
                                minecraft.level,
                                31
                        );

                pose.popPose();
            }

            // Pocket bag/body.
            piece(pose, blocks, buffers, light, leather,
                    0.205, 0.335, 0.015,
                    0.055, 0.245, 0.285,
                    0, 0, 0);

            // Slightly raised flap and brass closure.
            piece(pose, blocks, buffers, light, cloth,
                    0.222, 0.225, 0.015,
                    0.035, 0.075, 0.300,
                    0, 0, -3);

            piece(pose, blocks, buffers, light, brass,
                    0.242, 0.260, 0.015,
                    0.025, 0.050, 0.065,
                    0, 0, 0);

            // Rear hinge/rivet detail keeps the pocket readable from behind.
            piece(pose, blocks, buffers, light, brass,
                    0.215, 0.390, 0.128,
                    0.030, 0.035, 0.045,
                    0, 0, 0);
            piece(pose, blocks, buffers, light, brass,
                    0.215, 0.390, -0.098,
                    0.030, 0.035, 0.045,
                    0, 0, 0);
        }

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
    /** Shared worn/flying toque: tall pleated cylinder and a softly lobed crown. */
    public static void chefHat(int wear, PoseStack pose, BlockRenderDispatcher blocks,
            MultiBufferSource buffers, int light) {
        BlockState cloth = (wear == 0 ? Blocks.WHITE_WOOL : wear == 1
                ? Blocks.LIGHT_GRAY_WOOL : Blocks.GRAY_WOOL).defaultBlockState();
        BlockState seam = Blocks.LIGHT_GRAY_WOOL.defaultBlockState();
        // Band has an open interior rather than a solid cube through the player's head.
        for (int side : new int[]{-1, 1}) {
            piece(pose, blocks, buffers, light, cloth, side * .263, -.535, 0,
                    .065, .12, .59, 0, 0, 0);
            piece(pose, blocks, buffers, light, cloth, 0, -.535, side * .263,
                    .49, .12, .065, 0, 0, 0);
        }
        double height = wear == 2 ? .30 : .43;
        for (int pleat = 0; pleat < 12; pleat++) {
            double angle = pleat * Math.PI / 6;
            double x = Math.sin(angle) * .255, z = Math.cos(angle) * .255;
            piece(pose, blocks, buffers, light, cloth, x, -.60 - height * .5, z,
                    .135, height, .075, (float)(pleat * 30), 0, wear == 2 ? 5 : 0);
            piece(pose, blocks, buffers, light, seam, x * 1.005, -.60 - height * .46, z * 1.005,
                    .012, height * .77, .079, (float)(pleat * 30), 0, 0);
        }
        for (int lobe = 0; lobe < 8; lobe++) {
            double angle = lobe * Math.PI / 4;
            piece(pose, blocks, buffers, light, cloth,
                    Math.sin(angle) * .195, -.65 - height - .012 * (lobe % 3), Math.cos(angle) * .195,
                    .26, .17, .26, (float)(lobe * 45), 0, wear == 2 ? 8 : 0);
        }
        piece(pose, blocks, buffers, light, cloth, 0, -.70 - height, 0,
                .33, .15, .33, 0, 0, 0);
        if (wear > 0) piece(pose, blocks, buffers, light, Blocks.BROWN_WOOL.defaultBlockState(),
                -.17, -.535, -.299, .10, .045, .009, 0, 0, -4);
    }

    private static void kitHat(
            AccessoryKind kind,
            int wear,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            RenderContext context
    ) {
        MotionSample motion =
                context == null
                        ? new MotionSample(0, 0, 0, 0, 0, 0)
                        : motion(context);

        BlockState primary;
        BlockState accent;
        double crownHeight;
        double brimX;
        double brimZ;
        float flutterScale;

        switch (kind) {
            case RAILWAY_CAP -> {
                primary = Blocks.BLUE_WOOL.defaultBlockState();
                accent = Blocks.IRON_BLOCK.defaultBlockState();
                crownHeight = 0.20;
                brimX = 0.44;
                brimZ = 0.27;
                flutterScale = 0.25F;
            }
            case MINER_HELMET -> {
                primary = Blocks.YELLOW_WOOL.defaultBlockState();
                accent = Blocks.IRON_BLOCK.defaultBlockState();
                crownHeight = 0.29;
                brimX = 0.55;
                brimZ = 0.48;
                flutterScale = 0.0F;
            }
            case STORM_HAT -> {
                primary = Blocks.LIGHT_BLUE_WOOL.defaultBlockState();
                accent = Blocks.CYAN_WOOL.defaultBlockState();
                crownHeight = 0.18;
                brimX = 0.62;
                brimZ = 0.52;
                flutterScale = 1.0F;
            }
            case NATURALIST_HAT -> {
                primary = Blocks.BROWN_WOOL.defaultBlockState();
                accent = Blocks.GREEN_WOOL.defaultBlockState();
                crownHeight = 0.25;
                brimX = 0.68;
                brimZ = 0.60;
                flutterScale = 0.65F;
            }
            case ARCTIC_CAP -> {
                primary = Blocks.WHITE_WOOL.defaultBlockState();
                accent = Blocks.LIGHT_GRAY_WOOL.defaultBlockState();
                crownHeight = 0.31;
                brimX = 0.50;
                brimZ = 0.46;
                flutterScale = 0.12F;
            }
            default -> {
                return;
            }
        }

        float wobble =
                (motion.side() * 7.0F
                        + Mth.sin(motion.time() * 0.28F)
                        * motion.wind() * 5.0F)
                        * flutterScale;

        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(wobble));

        piece(pose, blocks, buffers, light, primary,
                0.0, -0.54 - crownHeight * 0.5, 0.0,
                0.50, crownHeight, 0.47,
                0, 0, wear >= 2 ? -4 : 0);

        piece(pose, blocks, buffers, light, primary,
                0.0, -0.50, -0.015,
                brimX, 0.055, brimZ,
                0, motion.back() * 3.0F * flutterScale, 0);

        piece(pose, blocks, buffers, light, accent,
                0.0, -0.53, -0.26,
                Math.min(brimX * 0.55, 0.34), 0.055, 0.07,
                0, 0, 0);

        if (kind == AccessoryKind.MINER_HELMET) {
            piece(pose, blocks, buffers, light, Blocks.SEA_LANTERN.defaultBlockState(),
                    0.0, -0.66, -0.29,
                    0.12, 0.12, 0.08,
                    0, 0, 0);
        }

        if (kind == AccessoryKind.ARCTIC_CAP) {
            for (int side : new int[]{-1, 1}) {
                piece(pose, blocks, buffers, light, accent,
                        side * 0.24, -0.39, 0.0,
                        0.10, 0.34, 0.30,
                        0, 0, side * 5);
            }
        }

        pose.popPose();
    }

    /** Shared by worn and wind-detached versions of the new hats. */
    public static void renderLooseKitHat(
            AccessoryKind kind,
            int wear,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light
    ) {
        kitHat(kind, wear, pose, blocks, buffers, light, null);
    }

    private static void kitCoat(
            PlayerModel<?> model,
            PoseStack pose,
            BlockRenderDispatcher blocks,
            MultiBufferSource buffers,
            int light,
            int wear,
            RenderContext context,
            BlockState cloth,
            BlockState accent,
            float motionStrength,
            int style
    ) {
        jacket(
                model, pose, blocks, buffers, light, wear,
                cloth, accent, false
        );

        MotionSample motion =
                context == null
                        ? new MotionSample(0, 0, 0, 0, 0, 0)
                        : motion(context);

        pose.pushPose();
        model.body.translateAndRotate(pose);

        // Each kit gets a readable front detail.
        if (style == 0) { // Railway crossing straps.
            for (int side : new int[]{-1, 1}) {
                piece(pose, blocks, buffers, light, accent,
                        side * 0.14, 0.34, -0.215,
                        0.055, 0.56, 0.026,
                        0, 0, side * 18);
            }
        } else if (style == 1) { // Miner reinforced belly plate.
            piece(pose, blocks, buffers, light, accent,
                    0.0, 0.42, -0.215,
                    0.38, 0.25, 0.035,
                    0, 0, 0);
        } else if (style == 3) { // Naturalist field pockets.
            for (int side : new int[]{-1, 1}) {
                piece(pose, blocks, buffers, light, Blocks.BROWN_TERRACOTTA.defaultBlockState(),
                        side * 0.18, 0.46, -0.215,
                        0.18, 0.16, 0.055,
                        0, 0, side * 4);
            }
        } else if (style == 4) { // Arctic fur collar.
            piece(pose, blocks, buffers, light, Blocks.QUARTZ_BLOCK.defaultBlockState(),
                    0.0, 0.05, -0.205,
                    0.48, 0.12, 0.07,
                    0, 0, 0);
        }

        // Lower back flap / hanging strap. Same lightweight motion language as
        // cape/apron, but attached directly to the torso slot.
        float flap =
                motionStrength
                        * (
                        motion.back() * 18.0F
                                + motion.speed() * 16.0F
                                + motion.wind() * Mth.sin(motion.time() * 0.31F) * 8.0F
                );

        if (motionStrength > 0.0F) {
            pose.pushPose();
            pose.translate(0.0, 0.61, 0.18);
            pose.mulPose(Axis.XP.rotationDegrees(flap));

            double width =
                    style == 2
                            ? 0.52
                            : style == 4
                            ? 0.46
                            : 0.34;

            double length =
                    style == 2
                            ? 0.34
                            : 0.22;

            piece(pose, blocks, buffers, light, cloth,
                    0.0, length * 0.5, 0.0,
                    width, length, 0.04,
                    0, 0, wear >= 2 ? 6 : 0);

            if (style == 3) {
                // Naturalist shoulder strap / sample bag pendulum.
                piece(pose, blocks, buffers, light, Blocks.BROWN_TERRACOTTA.defaultBlockState(),
                        0.22, 0.17, 0.02,
                        0.18, 0.20, 0.10,
                        0, 0, -motion.side() * 12.0F);
            }

            pose.popPose();
        }

        pose.popPose();
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

        BlockState seam = Blocks.LIGHT_GRAY_WOOL.defaultBlockState();
        for (int side : new int[]{-1, 1}) {
            piece(pose, blocks, buffers, light, cloth, side * .255, .345, 0,
                    .055, .70, .36, 0, 0, 0);
            piece(pose, blocks, buffers, light, cloth, side * .105, .045, -.205,
                    .14, .15, .04, 0, 0, side * -24);
            piece(pose, blocks, buffers, light, seam, side * .27, .37, -.201,
                    .012, .52, .011, 0, 0, 0);
        }
        // Overlapping placket and stitched breast pocket.
        piece(pose, blocks, buffers, light, seam, .045, .38, -.205,
                .012, .57, .009, 0, 0, 0);
        piece(pose, blocks, buffers, light, cloth, -.18, .26, -.212,
                .115, .10, .014, 0, 0, 0);
        piece(pose, blocks, buffers, light, seam, -.18, .22, -.222,
                .115, .012, .009, 0, 0, 0);
        for (int side : new int[]{-1, 1})
            piece(pose, blocks, buffers, light, Blocks.RED_WOOL.defaultBlockState(),
                    side * .043, .16, -.226, .065, .11, .018, 0, 0, side * 18);

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

        boolean engineer =
                kind == AccessoryKind.ENGINEER_GLOVES
                        || kind == AccessoryKind.WORK_GLOVES;

        boolean aero =
                kind == AccessoryKind.AERO_GLOVES;

        BlockState metal =
                aero
                        ? Blocks.CUT_COPPER.defaultBlockState()
                        : engineer
                        ? Blocks.CUT_COPPER.defaultBlockState()
                        : Blocks.IRON_BLOCK.defaultBlockState();

        // Wrist cuff wraps all four sides instead of being a floating front plate.
        piece(pose, blocks, buffers, light, material,
                0.0, 0.475, 0.0,
                0.325, 0.105, 0.325,
                0, 0, 0);

        // Palm block is slightly broader and lower than the forearm.
        piece(pose, blocks, buffers, light, material,
                0.0, 0.610, 0.0,
                wear == 2 && !left ? 0.255 : 0.315,
                wear == 2 && !left ? 0.205 : 0.255,
                0.315,
                0, 0, wear == 2 && !left ? 5 : 0);

        // Back-of-hand armor, palm pad and both side rails make it read in 360°.
        if (kind != AccessoryKind.CHEF_GLOVES) {
            piece(pose, blocks, buffers, light, metal,
                    0.0, 0.585, -0.174,
                    0.225, 0.135, 0.034,
                    0, 0, 0);

            piece(pose, blocks, buffers, light,
                    engineer
                            ? Blocks.DARK_OAK_PLANKS.defaultBlockState()
                            : material,
                    0.0, 0.610, 0.174,
                    0.235, 0.145, 0.032,
                    0, 0, 0);

            if (wear < 2) {
                piece(pose, blocks, buffers, light, metal,
                        -0.173, 0.595, 0.0,
                        0.030, 0.135, 0.285,
                        0, 0, 0);
                piece(pose, blocks, buffers, light, metal,
                        0.173, 0.595, 0.0,
                        0.030, 0.135, 0.285,
                        0, 0, 0);
            }
        }

        if (engineer) {
            BlockState brass =
                    wear >= 2
                            ? Blocks.EXPOSED_COPPER.defaultBlockState()
                            : Blocks.CUT_COPPER.defaultBlockState();

            // Mechanical cuff band wraps around the wrist.
            piece(pose, blocks, buffers, light, brass,
                    0.0, 0.470, -0.172,
                    0.325, 0.050, 0.033,
                    0, 0, 0);
            piece(pose, blocks, buffers, light, brass,
                    0.0, 0.470, 0.172,
                    0.325, 0.050, 0.033,
                    0, 0, 0);
            piece(pose, blocks, buffers, light, brass,
                    -0.172, 0.470, 0.0,
                    0.033, 0.050, 0.325,
                    0, 0, 0);
            piece(pose, blocks, buffers, light, brass,
                    0.172, 0.470, 0.0,
                    0.033, 0.050, 0.325,
                    0, 0, 0);

            if (wear < 2) {
                // Small piston and hinge on the outer side of each glove.
                double outer =
                        left
                                ? -0.190
                                : 0.190;

                piece(pose, blocks, buffers, light, brass,
                        outer, 0.610, -0.055,
                        0.055, 0.170, 0.085,
                        0, 0, 0);

                piece(pose, blocks, buffers, light,
                        Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                        outer, 0.675, 0.020,
                        0.070, 0.055, 0.105,
                        0, 0, 0);

                // Knuckle plates: three independent caps rather than one slab.
                for (int i = -1; i <= 1; i++) {
                    piece(pose, blocks, buffers, light, brass,
                            i * 0.083, 0.700, -0.185,
                            0.060, 0.055, 0.030,
                            0, 0, 0);
                }
            }
        } else if (aero && wear < 2) {
            // Aero glove gets a narrow copper dorsal rail and side vents.
            piece(pose, blocks, buffers, light, metal,
                    0.0, 0.640, -0.190,
                    0.075, 0.210, 0.028,
                    0, 0, 0);
            piece(pose, blocks, buffers, light, Blocks.IRON_BARS.defaultBlockState(),
                    left ? -0.183 : 0.183, 0.620, 0.0,
                    0.026, 0.145, 0.210,
                    0, 0, 0);
        }

        if (wear >= 1) {
            piece(pose, blocks, buffers, light,
                    Blocks.GRAY_WOOL.defaultBlockState(),
                    left ? -0.075 : 0.075,
                    0.635,
                    0.180,
                    0.095,
                    0.095,
                    0.020,
                    0,
                    0,
                    left ? -8 : 8);
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

        if (kind == AccessoryKind.SUSTENTION_BOOTS
                && wear < 2) {
            piece(pose, blocks, buffers, light,
                    Blocks.SLIME_BLOCK.defaultBlockState(),
                    0.0, 0.785, 0.0,
                    0.34, 0.045, 0.35,
                    0, 0, 0);
            piece(pose, blocks, buffers, light,
                    Blocks.COPPER_BLOCK.defaultBlockState(),
                    left ? -0.15 : 0.15,
                    0.61,
                    0.0,
                    0.045, 0.18, 0.18,
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
            RenderContext context
    ) {
        pose.pushPose();
        body.translateAndRotate(
                pose
        );

        MotionSample motion = context == null ? new MotionSample(0, 0, 0, 0, 0, 0) : motion(context);

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
            RenderContext context
    ) {
        pose.pushPose();
        body.translateAndRotate(
                pose
        );

        MotionSample motion = context == null ? new MotionSample(0, 0, 0, 0, 0, 0) : motion(context);

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
            RenderContext context
    ) {
        pose.pushPose();
        body.translateAndRotate(
                pose
        );

        MotionSample motion = context == null ? new MotionSample(0, 0, 0, 0, 0, 0) : motion(context);

        BlockState cloth =
                wear == 0
                        ? Blocks.WHITE_WOOL.defaultBlockState()
                        : wear == 1
                        ? Blocks.LIGHT_GRAY_WOOL.defaultBlockState()
                        : Blocks.GRAY_WOOL.defaultBlockState();

        BlockState seams = Blocks.LIGHT_GRAY_WOOL.defaultBlockState();
        piece(pose, blocks, buffers, light, cloth, 0, .235, -.236,
                .31, .27, .025, 0, 0, 0);
        for (int side : new int[]{-1, 1}) {
            piece(pose, blocks, buffers, light, cloth, side * .145, .09, -.228,
                    .045, .20, .025, 0, 0, side * 12);
            piece(pose, blocks, buffers, light, cloth, side * .24, .43, .188,
                    .25, .045, .035, 0, 0, side * 17);
            piece(pose, blocks, buffers, light, cloth, side * .047, .50, .209,
                    .05, .18, .025, 0, 0, side * 18);
        }

        // Waist strap.
        piece(pose, blocks, buffers, light,
                Blocks.RED_WOOL.defaultBlockState(),
                0.0, 0.42, -0.19,
                0.58, 0.065, 0.04,
                0, 0, 0);

        pose.translate(
                0.0,
                0.44,
                -0.248
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

        piece(pose, blocks, buffers, light, seams, 0, .392, -.024,
                .47, .016, .010, 0, 0, 0);
        for (int side : new int[]{-1, 1}) {
            piece(pose, blocks, buffers, light, cloth, side * .105, .15, -.035,
                    .17, .12, .02, 0, 0, 0);
            piece(pose, blocks, buffers, light, seams, side * .105, .10, -.048,
                    .17, .012, .008, 0, 0, 0);
        }

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
            RenderContext context
    ) {
        Vec3 velocity =
                context.player()
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
                context.player()
                        .tickCount
                        + context.partialTick();

        float wind =
                0.0F;

        float windBack =
                0.0F;

        float windSide =
                0.0F;

        if (context.player()
                .level()
                .dimension()
                .equals(
                        Level.OVERWORLD
                )) {
            LocalWeatherField.Sample sample =
                    LocalWeatherField.sample(
                            context.player()
                                    .getX(),
                            context.player()
                                    .getZ(),
                            context.player()
                                    .level()
                                    .getGameTime()
                    );

            wind =
                    sample.warning();

            double yaw =
                    Math.toRadians(
                            context.player()
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
