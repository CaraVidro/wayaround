package net.caravidro.wayaround.client.worldconfig;

import net.caravidro.wayaround.mixin.client.CreateWorldScreenInvoker;
import net.caravidro.wayaround.worldconfig.WorldFeatureService;
import net.caravidro.wayaround.worldconfig.WorldFeatureSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

public final class WorldFeatureCreationFlow {

    private WorldFeatureCreationFlow() {}

    private static CreateWorldScreen approvedParent;

    public static boolean isApproved(
            CreateWorldScreen screen
    ) {
        return approvedParent
                == screen;
    }

    public static void clearApproval() {
        approvedParent =
                null;
    }

    public static void open(
            CreateWorldScreen parent
    ) {
        Minecraft.getInstance()
                .setScreen(
                        new WorldFeatureSelectionScreen(
                                parent
                        )
                );
    }

    public static void createWorld(
            CreateWorldScreen parent,
            WorldFeatureSettings settings
    ) {
        WorldFeatureService.prepareNewWorld(
                settings
        );

        approvedParent =
                parent;

        Minecraft minecraft =
                Minecraft.getInstance();

        minecraft.setScreen(
                parent
        );

        /*
         * Invoke the exact vanilla CreateWorldScreen path after our modal is
         * submitted. The mixin consumes bypassNextCreate once, so this call is
         * not intercepted recursively.
         */
        (
                (CreateWorldScreenInvoker) parent
        ).wayaround$invokeOnCreate();
    }
}
