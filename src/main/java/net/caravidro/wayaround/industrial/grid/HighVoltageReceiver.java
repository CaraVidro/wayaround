package net.caravidro.wayaround.industrial.grid;

import net.minecraft.core.Direction;

/**
 * Explicit high-voltage endpoint.
 *
 * High-voltage lines never expose generic FE directly, so ordinary machines
 * cannot silently bypass transformers.
 */
public interface HighVoltageReceiver {

    int receiveHighVoltage(
            int amount,
            Direction side
    );
}
