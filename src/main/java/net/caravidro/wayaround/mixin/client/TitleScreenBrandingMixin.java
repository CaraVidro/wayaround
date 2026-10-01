package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.client.WayAroundReleaseInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenBrandingMixin {

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

        int y =
                8;

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

        int x =
                Math.max(
                        8,
                        width - railWidth - 8
                );

        /*
         * Branding belongs to the side rail, never to the center menu stack.
         * This remains stable even when the window is aggressively minimized.
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
    }
}
