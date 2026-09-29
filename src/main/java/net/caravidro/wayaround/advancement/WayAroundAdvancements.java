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

    private static void vistaRoot(
            ServerPlayer player
    ) {
        award(
                player,
                "vista/root",
                "noticed"
        );
    }

    public static void vistaAntarctica(
            ServerPlayer player
    ) {
        vistaRoot(
                player
        );
        award(
                player,
                "vista/antarctica",
                "seen"
        );
    }

    public static void vistaWhaleCarcass(
            ServerPlayer player
    ) {
        vistaRoot(
                player
        );
        award(
                player,
                "vista/whale_carcass",
                "seen"
        );
    }

    public static void vistaWildfireSmoke(
            ServerPlayer player
    ) {
        vistaRoot(
                player
        );
        award(
                player,
                "vista/wildfire_smoke",
                "seen"
        );
    }

    public static void vistaSunfish(
            ServerPlayer player
    ) {
        vistaRoot(
                player
        );
        award(
                player,
                "vista/sunfish",
                "seen"
        );
    }

    public static void vistaSunfishBasking(
            ServerPlayer player
    ) {
        vistaRoot(
                player
        );
        award(
                player,
                "vista/sunfish_basking",
                "seen"
        );
    }

    public static void vistaPufferCarrot(
            ServerPlayer player
    ) {
        vistaRoot(
                player
        );
        award(
                player,
                "vista/puffer_carrot",
                "fed"
        );
    }

    public static void vistaSouthernOcean(
            ServerPlayer player
    ) {
        vistaRoot(
                player
        );
        award(
                player,
                "vista/southern_ocean",
                "seen"
        );
    }

    public static void vistaJellyfish(
            ServerPlayer player
    ) {
        vistaRoot(
                player
        );
        award(
                player,
                "vista/jellyfish",
                "seen"
        );
    }

    public static void vistaGiantFish(
            ServerPlayer player
    ) {
        vistaRoot(
                player
        );
        award(
                player,
                "vista/oarfish",
                "seen"
        );
    }

    public static void vistaWhale(
            ServerPlayer player
    ) {
        vistaRoot(
                player
        );
        award(
                player,
                "vista/whale",
                "seen"
        );
    }

    public static void vistaPriorite(
            ServerPlayer player
    ) {
        vistaRoot(
                player
        );
        award(
                player,
                "vista/priorite",
                "seen"
        );
    }

    public static void vistaOldFriend(
            ServerPlayer player
    ) {
        vistaRoot(
                player
        );
        award(
                player,
                "vista/old_friend",
                "seen"
        );
    }
}
