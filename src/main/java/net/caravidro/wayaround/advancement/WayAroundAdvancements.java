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

    public static void revoke(
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
                .revoke(
                        advancement,
                        criterion
                );
    }

    public static boolean completed(
            ServerPlayer player,
            String path
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

        return advancement != null
                && player.getAdvancements()
                        .getOrStartProgress(
                                advancement
                        )
                        .isDone();
    }

    private static void hintUnlessCompleted(
            ServerPlayer player,
            String target,
            String hint
    ) {
        if (!completed(
                player,
                target
        )) {
            award(
                    player,
                    hint,
                    "hint"
            );
        }
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

        hintUnlessCompleted(
                player,
                "accessories/cardboard_head",
                "accessories/cardboard_hint"
        );

        hintUnlessCompleted(
                player,
                "accessories/useless_filter",
                "accessories/filter_hint"
        );
    }

    public static void cardboardHead(
            ServerPlayer player
    ) {
        revoke(
                player,
                "accessories/cardboard_hint",
                "hint"
        );

        award(
                player,
                "accessories/cardboard_head",
                "box"
        );
    }

    public static void uselessGasMask(
            ServerPlayer player
    ) {
        revoke(
                player,
                "accessories/filter_hint",
                "hint"
        );

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

        hintUnlessCompleted(
                player,
                "media/video",
                "media/video_hint"
        );
    }

    public static void mediaVideo(
            ServerPlayer player
    ) {
        revoke(
                player,
                "media/video_hint",
                "hint"
        );

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

        hintUnlessCompleted(
                player,
                "war/overkill",
                "war/overkill_hint"
        );
    }

    public static void warRocket(
            ServerPlayer player
    ) {
        revoke(
                player,
                "war/overkill_hint",
                "hint"
        );

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

        hintUnlessCompleted(
                player,
                "assembly_objects/four_lines",
                "assembly_objects/four_lines_hint"
        );
    }

    public static void reinforcedPulley(
            ServerPlayer player
    ) {
        revoke(
                player,
                "assembly_objects/four_lines_hint",
                "hint"
        );

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
        /*
         * Preserve the visual route even for teleport/command entry:
         * Southern Ocean is the direct parent of Antarctica.
         */
        vistaSouthernOcean(
                player
        );

        revoke(
                player,
                "vista/antarctica_hint",
                "hint"
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
        hintUnlessCompleted(
                player,
                "vista/sunfish_basking",
                "vista/sunfish_basking_hint"
        );
    }

    public static void vistaSunfishBasking(
            ServerPlayer player
    ) {
        vistaRoot(
                player
        );
        revoke(
                player,
                "vista/sunfish_basking_hint",
                "hint"
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
        revoke(
                player,
                "vista/puffer_carrot_hint",
                "hint"
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
        hintUnlessCompleted(
                player,
                "vista/antarctica",
                "vista/antarctica_hint"
        );
    }

    public static void vistaPufferHint(
            ServerPlayer player
    ) {
        vistaRoot(
                player
        );
        hintUnlessCompleted(
                player,
                "vista/puffer_carrot",
                "vista/puffer_carrot_hint"
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
