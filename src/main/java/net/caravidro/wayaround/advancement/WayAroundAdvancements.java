package net.caravidro.wayaround.advancement;

import net.caravidro.wayaround.WayAround;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Small manual-award bridge for Way Around's "emblems".
 *
 * Keeping gameplay awards here is intentional: secret systems can grant a
 * generic public emblem without leaking the hidden subtype in advancement IDs
 * or toast text.
 */
public final class WayAroundAdvancements {

    private WayAroundAdvancements() {
    }

    public static void award(
            ServerPlayer player,
            String path,
            String criterion
    ) {
        AdvancementHolder advancement =
                player.server
                        .getAdvancements()
                        .get(
                                ResourceLocation.fromNamespaceAndPath(
                                        WayAround.MODID,
                                        path
                                )
                        );

        if (advancement == null) {
            return;
        }

        player.getAdvancements()
                .award(
                        advancement,
                        criterion
                );
    }

    public static void jujutsuAwakened(
            ServerPlayer player
    ) {
        award(
                player,
                "jujutsu/awakened",
                "awakened"
        );
    }

    public static void accessoryEquipped(
            ServerPlayer player
    ) {
        award(
                player,
                "accessories/first_fit",
                "equipped"
        );
    }

    public static void cardboardHead(
            ServerPlayer player
    ) {
        award(
                player,
                "accessories/cardboard_head",
                "box"
        );
    }

    public static void uselessGasMask(
            ServerPlayer player
    ) {
        award(
                player,
                "accessories/useless_filter",
                "mask"
        );
    }

    public static void mediaPhoto(
            ServerPlayer player
    ) {
        award(
                player,
                "media/photo",
                "photo"
        );
    }

    public static void mediaVideo(
            ServerPlayer player
    ) {
        award(
                player,
                "media/video",
                "recorded"
        );
    }

    public static void warShot(
            ServerPlayer player
    ) {
        award(
                player,
                "war/first_shot",
                "shot"
        );
    }

    public static void warRocket(
            ServerPlayer player
    ) {
        award(
                player,
                "war/overkill",
                "rocket"
        );
    }

    public static void manualCrank(
            ServerPlayer player
    ) {
        award(
                player,
                "assembly_objects/manual_crank",
                "crank"
        );
    }

    public static void reinforcedPulley(
            ServerPlayer player
    ) {
        award(
                player,
                "assembly_objects/four_lines",
                "four"
        );
    }
}
