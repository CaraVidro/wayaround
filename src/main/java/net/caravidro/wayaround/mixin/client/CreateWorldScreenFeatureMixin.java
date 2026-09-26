package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.client.worldconfig.WorldFeatureCreationFlow;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenFeatureMixin {

    @Inject(
            method = "onCreate",
            at = @At("HEAD"),
            cancellable = true
    )
    private void wayaround$chooseWorldFeatures(
            CallbackInfo ci
    ) {
        CreateWorldScreen screen =
                (CreateWorldScreen) (Object) this;

        if (WorldFeatureCreationFlow.isApproved(
                screen
        )) {
            return;
        }

        ci.cancel();

        WorldFeatureCreationFlow.open(
                screen
        );
    }
}
