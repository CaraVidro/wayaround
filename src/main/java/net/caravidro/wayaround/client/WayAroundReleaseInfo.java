package net.caravidro.wayaround.client;

import java.util.List;

import net.minecraft.network.chat.Component;

/**
 * Single client-facing source of truth for named Way Around releases.
 *
 * Keep the currently published release intact while the next named era is in
 * development. When THE WORLD CONNECTS is actually ready, flip RELEASED_NEXT
 * and bump gradle.properties in the release commit.
 */
public final class WayAroundReleaseInfo {

    public static final String CURRENT_VERSION = "v1.2.0";
    public static final String CURRENT_TITLE = "Grande Novo Mundo";

    public static final String NEXT_VERSION = "v1.2.1";
    public static final String NEXT_TITLE = "THE WORLD CONNECTS";

    public static final boolean RELEASED_NEXT = false;

    private WayAroundReleaseInfo() {
    }

    public static String menuVersion() {
        return RELEASED_NEXT
                ? NEXT_VERSION
                : CURRENT_VERSION;
    }

    public static String menuTitle() {
        return RELEASED_NEXT
                ? NEXT_TITLE
                : CURRENT_TITLE;
    }

    public static Component previewLine() {
        return RELEASED_NEXT
                ? Component.literal(NEXT_TITLE)
                : Component.translatable(
                        "screen.wayaround.update_log.preview",
                        NEXT_VERSION,
                        NEXT_TITLE
                );
    }

    public static List<Component> updateLogLines() {
        return List.of(
                Component.translatable("screen.wayaround.update_log.dev"),
                Component.translatable("screen.wayaround.update_log.milestone"),
                Component.translatable("screen.wayaround.update_log.failure"),
                Component.translatable("screen.wayaround.update_log.milestone14"),
                Component.translatable("screen.wayaround.update_log.material_memory"),
                Component.translatable("screen.wayaround.update_log.electronics"),
                Component.translatable("screen.wayaround.update_log.circuit_building"),
                Component.translatable("screen.wayaround.update_log.future")
        );
    }
}
