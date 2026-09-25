package net.caravidro.wayaround.spectrum;

import java.util.Objects;
import net.minecraft.world.item.Item;

/**
 * Common item identity for Spectrums.
 * Specialized managers keep owning the actual powers and runtime state.
 */
public class SpectrumItem extends Item {
    private final SpectrumType spectrumType;

    protected SpectrumItem(SpectrumType spectrumType, Properties properties) {
        super(properties);
        this.spectrumType = Objects.requireNonNull(spectrumType, "spectrumType");
    }

    public final SpectrumType spectrumType() {
        return spectrumType;
    }
}
