package net.caravidro.wayaround.spectrum;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Minimal contract for abilities that opt into the shared Spectrum layer. */
public interface SpectrumAbility {
    ResourceLocation id();
    SpectrumType spectrum();

    default boolean canUse(ServerPlayer player) {
        return SpectrumAccess.has(player, spectrum());
    }
}
