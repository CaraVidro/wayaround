package net.caravidro.wayaround.client;

import net.caravidro.wayaround.domain.DomainIntroManager;
import net.minecraft.resources.ResourceLocation;

public record DomainIntroProfile(
        int background,
        int line,
        int primaryText,
        int secondaryText,
        int tertiaryText,
        ResourceLocation futureImage,
        boolean useFutureImage
) {

    public static DomainIntroProfile forStyle(
            byte style
    ) {
        return switch (style) {
            case DomainIntroManager.TUKUNA ->
                    new DomainIntroProfile(
                            0xED050000,
                            0xFFFF2118,
                            0xFFFF2118,
                            0xFFFFD04A,
                            0xFF7A0800,
                            ResourceLocation.fromNamespaceAndPath(
                                    "wayaround",
                                    "textures/gui/domain/tukuna.png"
                            ),
                            false
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
                            false
                    );

            default ->
                    new DomainIntroProfile(
                            0xF0030307,
                            0xFFFFFFFF,
                            0xFF448AFF,
                            0xFFFF304A,
                            0xFFB55CFF,
                            ResourceLocation.fromNamespaceAndPath(
                                    "wayaround",
                                    "textures/gui/domain/void.png"
                            ),
                            false
                    );
        };
    }
}
