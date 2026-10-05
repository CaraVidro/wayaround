package net.caravidro.wayaround.weaponry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.weaponry.WayWeaponItem;
import net.caravidro.wayaround.weaponry.WeaponFamily;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class WeaponHolsterRenderer {

    private WeaponHolsterRenderer() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void render(
            RenderPlayerEvent.Post event
    ) {
        ItemStack katana =
                bestStored(
                        event,
                        WeaponFamily.KATANA
                );

        ItemStack scythe =
                bestStored(
                        event,
                        WeaponFamily.SCYTHE
                );

        if (katana.isEmpty()
                && scythe.isEmpty()) {
            return;
        }

        PoseStack pose =
                event.getPoseStack();

        PlayerModel<?> model =
                event.getRenderer()
                        .getModel();

        float bodyYaw =
                Mth.rotLerp(
                        event.getPartialTick(),
                        event.getEntity().yBodyRotO,
                        event.getEntity().yBodyRot
                );

        pose.pushPose();

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        180.0F
                                - bodyYaw
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

        if (!katana.isEmpty()) {
            renderKatana(
                    katana,
                    event,
                    model,
                    pose
            );
        }

        if (!scythe.isEmpty()) {
            renderScythe(
                    scythe,
                    event,
                    model,
                    pose
            );
        }

        pose.popPose();
    }

    private static ItemStack bestStored(
            RenderPlayerEvent.Post event,
            WeaponFamily family
    ) {
        var player =
                event.getEntity();

        if (heldFamily(
                player.getMainHandItem(),
                family
        )
                || heldFamily(
                player.getOffhandItem(),
                family
        )) {
            return ItemStack.EMPTY;
        }

        ItemStack best =
                ItemStack.EMPTY;

        int rank =
                -1;

        for (ItemStack stack :
                player.getInventory().items) {

            if (!(stack.getItem()
                    instanceof WayWeaponItem weapon)
                    || weapon.family()
                            != family) {
                continue;
            }

            int candidate =
                    materialRank(
                            weapon.materialId()
                    );

            if (candidate > rank) {
                rank =
                        candidate;

                best =
                        stack;
            }
        }

        return best;
    }

    private static boolean heldFamily(
            ItemStack stack,
            WeaponFamily family
    ) {
        return stack.getItem()
                instanceof WayWeaponItem weapon
                && weapon.family()
                        == family;
    }

    private static int materialRank(
            String material
    ) {
        return switch (material) {
            case "netherite" -> 5;
            case "diamond" -> 4;
            case "iron" -> 3;
            case "stone" -> 2;
            default -> 1;
        };
    }

    private static void renderKatana(
            ItemStack stack,
            RenderPlayerEvent.Post event,
            PlayerModel<?> model,
            PoseStack pose
    ) {
        pose.pushPose();

        model.body.translateAndRotate(
                pose
        );

        pose.translate(
                0.31,
                0.48,
                0.10
        );

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        -74.0F
                )
        );

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        8.0F
                )
        );

        pose.mulPose(
                Axis.XP.rotationDegrees(
                        176.0F
                )
        );

        pose.scale(
                0.72F,
                0.72F,
                0.72F
        );

        renderItem(
                stack,
                event,
                pose
        );

        pose.popPose();
    }

    private static void renderScythe(
            ItemStack stack,
            RenderPlayerEvent.Post event,
            PlayerModel<?> model,
            PoseStack pose
    ) {
        pose.pushPose();

        model.body.translateAndRotate(
                pose
        );

        pose.translate(
                -0.02,
                0.37,
                0.24
        );

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        -34.0F
                )
        );

        pose.mulPose(
                Axis.XP.rotationDegrees(
                        182.0F
                )
        );

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        5.0F
                )
        );

        pose.scale(
                0.82F,
                0.82F,
                0.82F
        );

        renderItem(
                stack,
                event,
                pose
        );

        pose.popPose();
    }

    private static void renderItem(
            ItemStack stack,
            RenderPlayerEvent.Post event,
            PoseStack pose
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        minecraft.getItemRenderer()
                .renderStatic(
                        stack,
                        ItemDisplayContext.FIXED,
                        event.getPackedLight(),
                        OverlayTexture.NO_OVERLAY,
                        pose,
                        event.getMultiBufferSource(),
                        event.getEntity()
                                .level(),
                        event.getEntity()
                                .getId()
                );
    }
}
