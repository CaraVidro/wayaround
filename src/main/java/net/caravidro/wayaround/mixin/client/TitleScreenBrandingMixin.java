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

        int height =
                minecraft.getWindow()
                        .getGuiScaledHeight();

        int x =
                8;

        int y =
                Math.max(
                        8,
                        Math.min(
                                70,
                                height - 112
                        )
                );

        /*
         * Branding belongs to the side rail, never to the center menu stack.
         * This remains stable even when the window is aggressively minimized.
         */
        graphics.drawString(
                minecraft.font,
                Component.literal(
                        "WAYAROUND"
                ),
                x,
                y,
                0xFFB8B8B8,
                false
        );

        graphics.drawString(
                minecraft.font,
                Component.literal(
                        WayAroundReleaseInfo.menuTitle()
                ),
                x,
                y + 12,
                0xFF8C6E46,
                false
        );

        graphics.drawString(
                minecraft.font,
                Component.literal(
                        WayAroundReleaseInfo.menuVersion()
                ),
                x,
                y + 24,
                0xFF4B4B4B,
                false
        );

        if (!WayAroundReleaseInfo.RELEASED_NEXT) {
            graphics.drawString(
                    minecraft.font,
                    WayAroundReleaseInfo.previewLine(),
                    x,
                    y + 36,
                    0xFF74664F,
                    false
            );
        }
    }
}
