package net.caravidro.wayaround.spectrum;

import net.caravidro.wayaround.domain.DomainIntroManager;
import net.minecraft.world.entity.player.Player;

/**
 * Lightweight first pass at Spectrum mastery.
 *
 * Vanilla experience level is deliberately used as the progression currency:
 * no parallel XP database is necessary yet, client and server already agree on
 * it, and creative/debug players keep full access.
 */
public final class SpectrumProgression {

    public static final int VOID_PURPLE_LEVEL =
            15;

    public static final int VOID_DOMAIN_LEVEL =
            30;

    public static final int VOID_APEX_LEVEL =
            45;

    public static final int TUKUNA_APEX_LEVEL =
            40;

    private SpectrumProgression() {
    }

    public static boolean isUnlocked(
            Player player,
            SpectrumAction action
    ) {
        if (player == null) {
            return false;
        }

        if (player.getAbilities()
                .instabuild) {
            return true;
        }

        return switch (action) {
            case PURPLE,
                 DUAL ->
                    voidPurpleUnlocked(
                            player
                    );

            case VOID_DOMAIN ->
                    voidDomainUnlocked(
                            player
                    );

            default ->
                    true;
        };
    }

    public static boolean voidPurpleUnlocked(
            Player player
    ) {
        return player != null
                && (
                player.getAbilities()
                        .instabuild
                        || player.experienceLevel
                        >= VOID_PURPLE_LEVEL
        );
    }

    public static boolean voidDomainUnlocked(
            Player player
    ) {
        return player != null
                && (
                player.getAbilities()
                        .instabuild
                        || player.experienceLevel
                        >= VOID_DOMAIN_LEVEL
        );
    }

    public static boolean voidApex(
            Player player
    ) {
        return player != null
                && (
                player.getAbilities()
                        .instabuild
                        || player.experienceLevel
                        >= VOID_APEX_LEVEL
        );
    }

    public static boolean tukunaApex(
            Player player
    ) {
        return player != null
                && (
                player.getAbilities()
                        .instabuild
                        || player.experienceLevel
                        >= TUKUNA_APEX_LEVEL
        );
    }

    public static byte domainVariant(
            Player player,
            byte style
    ) {
        if (style == DomainIntroManager.VOID
                && voidApex(
                player
        )) {
            return DomainIntroManager.APEX;
        }

        if (style == DomainIntroManager.TUKUNA
                && tukunaApex(
                player
        )) {
            return DomainIntroManager.APEX;
        }

        return DomainIntroManager.SIMPLE;
    }

    public static String label(
            Player player,
            SpectrumAction action
    ) {
        if (action == SpectrumAction.VOID_DOMAIN
                && voidApex(
                player
        )) {
            return "Expansão de domínio absoluto";
        }

        if (action == SpectrumAction.TUKUNA_DOMAIN
                && tukunaApex(
                player
        )) {
            return "Expansão de domínio absoluto";
        }

        return action.label;
    }
}
