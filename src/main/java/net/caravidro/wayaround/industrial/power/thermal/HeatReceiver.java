package net.caravidro.wayaround.industrial.power.thermal;

import net.minecraft.core.Direction;

/** A component that can accept thermal energy from a neighboring component. */
public interface HeatReceiver {
    int receiveHeat(Direction from, int heatUnits, boolean simulate);
}
