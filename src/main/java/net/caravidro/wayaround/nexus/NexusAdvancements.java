package net.caravidro.wayaround.nexus;

import net.caravidro.wayaround.WayAround;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class NexusAdvancements {

    private static final ResourceLocation BEGINNING_OF_END =
            ResourceLocation.fromNamespaceAndPath(
                    WayAround.MODID,
                    "nexus/beginning_of_end"
            );

    private NexusAdvancements() {
    }

    public static void beginningOfEnd(
            ServerPlayer player
    ) {
        AdvancementHolder advancement =
                player.server
                        .getAdvancements()
                        .get(
                                BEGINNING_OF_END
                        );

        if (advancement != null) {
            player.getAdvancements()
                    .award(
                            advancement,
                            "activated"
                    );
        }
    }
}
