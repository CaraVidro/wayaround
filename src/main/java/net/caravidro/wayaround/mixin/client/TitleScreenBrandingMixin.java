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

        int center =
                minecraft.getWindow()
                        .getGuiScaledWidth()
                        / 2;

        graphics.drawCenteredString(
                minecraft.font,
                Component.literal(
                        "WAYAROUND"
                ),
                center,
                82,
                0xFFB8B8B8
        );

        graphics.drawCenteredString(
                minecraft.font,
                Component.literal(
                        WayAroundReleaseInfo.menuTitle()
                ),
                center,
                94,
                0xFF8C6E46
        );

        graphics.drawCenteredString(
                minecraft.font,
                Component.literal(
                        WayAroundReleaseInfo.menuVersion()
                ),
                center,
                106,
                0xFF4B4B4B
        );

        if (!WayAroundReleaseInfo.RELEASED_NEXT) {
            graphics.drawCenteredString(
                    minecraft.font,
                    WayAroundReleaseInfo.previewLine(),
                    center,
                    117,
                    0xFF74664F
            );
        }
    }
}
