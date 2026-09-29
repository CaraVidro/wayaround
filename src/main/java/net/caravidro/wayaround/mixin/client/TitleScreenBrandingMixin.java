package net.caravidro.wayaround.mixin.client;

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

    /*
     * RELEASE BRANDING:
     * Change these two constants whenever Way Around gets a major named update.
     * Future release chats should update both the semantic version and the
     * subtitle together so the title screen always advertises the current era.
     */
    private static final String WAYAROUND_VERSION =
            "v1.2.0";

    private static final String WAYAROUND_RELEASE_TITLE =
            "Grande Novo Mundo";

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
                        WAYAROUND_RELEASE_TITLE
                ),
                center,
                94,
                0xFF8C6E46
        );

        graphics.drawCenteredString(
                minecraft.font,
                Component.literal(
                        WAYAROUND_VERSION
                ),
                center,
                106,
                0xFF4B4B4B
        );
    }
}
