package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.client.WayAroundReleaseInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenBrandingMixin {

    @Shadow
    private String splash;

    @Unique
    private String wayaround$dialogueSplash;

    @Inject(
            method = "render",
            at = @At("HEAD")
    )
    private void wayaround$prepareReadableSplash(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        /*
         * Vanilla scales a splash by its full single-line width. Authored
         * WayAround quotes are deliberately longer, so that formula can turn
         * them into microscopic text. Hide only our signed dialogue splashes
         * from the vanilla pass and redraw them below as a compact multiline
         * quote.
         */
        if (splash != null
                && splash.contains("§")
                && splash.contains("- ")) {
            wayaround$dialogueSplash =
                    splash;

            splash =
                    null;
        }
    }

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void wayaround$renderBranding(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        int width =
                minecraft.getWindow()
                        .getGuiScaledWidth();

        int height =
                minecraft.getWindow()
                        .getGuiScaledHeight();

        /*
         * Keep the WayAround identity in the same lower UI band as the
         * Update Log, but on the opposite side. The vanilla splash lives
         * around the logo/top-right area, so this removes that collision.
         */
        Component brand =
                Component.literal(
                        "WAYAROUND"
                );

        Component release =
                Component.literal(
                        WayAroundReleaseInfo.menuTitle()
                );

        Component version =
                Component.literal(
                        WayAroundReleaseInfo.menuVersion()
                );

        Component preview =
                WayAroundReleaseInfo.previewLine();

        int railWidth =
                Math.max(
                        minecraft.font.width(
                                brand
                        ),
                        Math.max(
                                minecraft.font.width(
                                        release
                                ),
                                Math.max(
                                        minecraft.font.width(
                                                version
                                        ),
                                        !WayAroundReleaseInfo.RELEASED_NEXT
                                                ? minecraft.font.width(
                                                preview
                                        )
                                                : 0
                                )
                        )
                );

        boolean narrow =
                width
                        < railWidth
                                + 126;

        int y =
                Math.max(
                        8,
                        height
                                - (
                                narrow
                                        ? 112
                                        : 64
                        )
                );

        int x =
                Math.max(
                        8,
                        width - railWidth - 8
                );

        /*
         * Branding belongs to the lower-right rail, never to the center
         * menu stack or the splash-text region.
         */
        graphics.drawString(
                minecraft.font,
                brand,
                x,
                y,
                0xFFB8B8B8,
                false
        );

        graphics.drawString(
                minecraft.font,
                release,
                x,
                y + 12,
                0xFF8C6E46,
                false
        );

        graphics.drawString(
                minecraft.font,
                version,
                x,
                y + 24,
                0xFF4B4B4B,
                false
        );

        if (!WayAroundReleaseInfo.RELEASED_NEXT) {
            graphics.drawString(
                    minecraft.font,
                    preview,
                    x,
                    y + 36,
                    0xFF74664F,
                    false
            );
        }

        wayaround$renderReadableSplash(
                graphics,
                minecraft,
                width
        );
    }

    @Unique
    private void wayaround$renderReadableSplash(
            GuiGraphics graphics,
            Minecraft minecraft,
            int width
    ) {
        if (wayaround$dialogueSplash == null) {
            return;
        }

        String text =
                wayaround$dialogueSplash;

        wayaround$dialogueSplash =
                null;

        /*
         * Restore the field immediately so the TitleScreen keeps the same
         * splash between frames.
         */
        splash =
                text;

        int authorIndex =
                text.indexOf(
                        "§"
                );

        String quote =
                authorIndex > 0
                        ? text.substring(
                        0,
                        authorIndex
                ).trim()
                        : text;

        String author =
                authorIndex > 0
                        ? text.substring(
                        authorIndex
                ).trim()
                        : "";

        java.util.List<String> lines =
                wayaround$wrapSplash(
                        minecraft,
                        quote,
                        width < 430
                                ? 118
                                : 168
                );

        if (!author.isBlank()) {
            lines.add(
                    author
            );
        }

        int maxLineWidth =
                1;

        for (String line : lines) {
            maxLineWidth =
                    Math.max(
                            maxLineWidth,
                            minecraft.font.width(
                                    line
                            )
                    );
        }

        float scale =
                Math.clamp(
                        154.0F
                                / maxLineWidth,
                        width < 430
                                ? 0.92F
                                : 1.08F,
                        1.42F
                );

        graphics.pose()
                .pushPose();

        graphics.pose()
                .translate(
                        width / 2.0F + 92.0F,
                        72.0F,
                        0.0F
                );

        graphics.pose()
                .mulPose(
                        com.mojang.math.Axis.ZP.rotationDegrees(
                                -18.0F
                        )
                );

        graphics.pose()
                .scale(
                        scale,
                        scale,
                        1.0F
                );

        int totalHeight =
                lines.size()
                        * 10;

        int startY =
                -totalHeight / 2;

        for (int index = 0;
             index < lines.size();
             index++) {

            graphics.drawCenteredString(
                    minecraft.font,
                    lines.get(
                            index
                    ),
                    0,
                    startY
                            + index * 10,
                    0xFFFFFF55
            );
        }

        graphics.pose()
                .popPose();
    }

    @Unique
    private java.util.List<String> wayaround$wrapSplash(
            Minecraft minecraft,
            String text,
            int maxWidth
    ) {
        java.util.List<String> result =
                new java.util.ArrayList<>();

        StringBuilder current =
                new StringBuilder();

        for (String word :
                text.split(
                        " "
                )) {

            String candidate =
                    current.isEmpty()
                            ? word
                            : current
                            + " "
                            + word;

            if (!current.isEmpty()
                    && minecraft.font.width(
                    candidate
            ) > maxWidth) {

                result.add(
                        current.toString()
                );

                current =
                        new StringBuilder(
                                word
                        );

            } else {
                if (!current.isEmpty()) {
                    current.append(
                            ' '
                    );
                }

                current.append(
                        word
                );
            }
        }

        if (!current.isEmpty()) {
            result.add(
                    current.toString()
            );
        }

        return result;
    }
}
