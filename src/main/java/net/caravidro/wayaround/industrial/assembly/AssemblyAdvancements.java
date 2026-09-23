package net.caravidro.wayaround.industrial.assembly;

import net.caravidro.wayaround.WayAround;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class AssemblyAdvancements {

    private static final ResourceLocation WATER_WHEEL =
            ResourceLocation.fromNamespaceAndPath(
                    WayAround.MODID,
                    "assembly_objects/water_wheel"
            );

    private static final ResourceLocation LAVA_WHEEL =
            ResourceLocation.fromNamespaceAndPath(
                    WayAround.MODID,
                    "assembly_objects/lava_wheel"
            );

    private AssemblyAdvancements() {
    }

    public static void waterWheel(ServerPlayer player) {
        award(player, WATER_WHEEL, "assembled");
    }

    public static void lavaWheel(ServerPlayer player) {
        award(player, LAVA_WHEEL, "lava");
    }

    private static void award(
            ServerPlayer player,
            ResourceLocation id,
            String criterion
    ) {
        AdvancementHolder advancement =
                player.server
                        .getAdvancements()
                        .get(id);

        if (advancement != null) {
            player.getAdvancements()
                    .award(
                            advancement,
                            criterion
                    );
        }
    }
}
