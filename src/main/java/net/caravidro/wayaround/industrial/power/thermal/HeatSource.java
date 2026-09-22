package net.caravidro.wayaround.industrial.power.thermal;

import net.minecraft.core.Direction;

/** A thermal producer. Heat is deliberately separate from electrical FE. */
public interface HeatSource {
    int heatOutputPerTick();

    default boolean canOutputHeat(Direction side) {
        return true;
    }
}
