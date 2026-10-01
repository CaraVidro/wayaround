package net.caravidro.wayaround.client;

import net.caravidro.wayaround.domain.DomainIntroManager;
import net.caravidro.wayaround.domain.VoidDomainPresentation;
import net.minecraft.resources.ResourceLocation;

public record DomainIntroProfile(
        int background,
        int line,
        int primaryText,
        int secondaryText,
        int tertiaryText,
        ResourceLocation futureImage,
        boolean useFutureImage,
        int imageWidth,
        int imageHeight,
        byte pose,
        boolean shadowParticles,
        String phrase
) {

    public static final byte POSE_SIMPLE =
            0;

    public static final byte POSE_VOID_APEX =
            1;

    public static final byte POSE_TUKUNA_APEX =
            2;

    public static final byte POSE_SHADOWS =
            3;

    public static final byte POSE_VOID_EXIT =
            4;

    public static DomainIntroProfile forStyle(
            byte style,
            byte variant
    ) {
        return switch (style) {
            case DomainIntroManager.TUKUNA ->
                    new DomainIntroProfile(
                            0xF0050202,
                            0xFFFF261B,
                            0xFFFF261B,
                            0xFFFFD34D,
                            0xFF720900,
                            ResourceLocation.fromNamespaceAndPath(
                                    "wayaround",
                                    "textures/gui/domain/tukuna_apex.png"
                            ),
                            variant
                                    == DomainIntroManager.APEX,
                            1024,
                            256,
                            variant
                                    == DomainIntroManager.APEX
                                    ? POSE_TUKUNA_APEX
                                    : POSE_SIMPLE,
                            false,
                            variant
                                    == DomainIntroManager.APEX
                                    ? "DOMÍNIO DE EXPANSÃO ABSOLUTO"
                                    : "DOMÍNIO DE EXPANSÃO"
                    );

            case DomainIntroManager.JUSTICE ->
                    new DomainIntroProfile(
                            0xED0B0902,
                            0xFFFFD969,
                            0xFFFFE8A0,
                            0xFFFFFFFF,
                            0xFFB78920,
                            ResourceLocation.fromNamespaceAndPath(
                                    "wayaround",
                                    "textures/gui/domain/justice.png"
                            ),
                            false,
                            1024,
                            256,
                            POSE_SIMPLE,
                            false,
                            "DOMÍNIO DE EXPANSÃO"
                    );

            case DomainIntroManager.SHADOWS ->
                    new DomainIntroProfile(
                            0xF5030305,
                            0xFF33333A,
                            0xFF070708,
                            0xFF1A1A1E,
                            0xFF44444C,
                            ResourceLocation.fromNamespaceAndPath(
                                    "wayaround",
                                    "textures/gui/domain/shadows.png"
                            ),
                            true,
                            1024,
                            256,
                            POSE_SHADOWS,
                            true,
                            "DOMÍNIO DE EXPANSÃO"
                    );

            default -> {
                VoidDomainPresentation presentation =
                        VoidDomainPresentation.byId(
                                variant
                        );

                boolean absolute =
                        presentation
                                == VoidDomainPresentation.ABSOLUTE;

                yield new DomainIntroProfile(
                        0xF0030307,
                        0xFFFFFFFF,
                        0xFF448AFF,
                        0xFFFFE2A2,
                        0xFFB55CFF,
                        ResourceLocation.fromNamespaceAndPath(
                                "wayaround",
                                absolute
                                        ? "textures/gui/domain/void_absolute.png"
                                        : "textures/gui/domain/void_apex.png"
                        ),
                        absolute,
                        absolute
                                ? 320
                                : 1024,
                        absolute
                                ? 180
                                : 256,
                        absolute
                                ? POSE_VOID_APEX
                                : POSE_SIMPLE,
                        false,
                        absolute
                                ? "DOMÍNIO DE EXPANSÃO ABSOLUTO"
                                : "DOMÍNIO DE EXPANSÃO"
                );
            }
        };
    }
}
