package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.client.MenuBrandingState;
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

        Component context =
                switch (MenuBrandingState.context()) {
                    case SINGLEPLAYER ->
                            Component.translatable(
                                    "menu.wayaround.return.singleplayer"
                            );

                    case MULTIPLAYER ->
                            Component.translatable(
                                    "menu.wayaround.return.multiplayer"
                            );

                    default ->
                            Component.translatable(
                                    "menu.wayaround.return.none"
                            );
                };

        graphics.drawCenteredString(
                minecraft.font,
                context,
                center,
                94,
                0xFF666666
        );

        graphics.drawCenteredString(
                minecraft.font,
                Component.literal(
                        "v1.2.0"
                ),
                center,
                105,
                0xFF4B4B4B
        );
    }
}
