package net.caravidro.wayaround.interaction;

import net.minecraft.core.BlockPos;

/**
 * Implemented by block entities or structures that react to shared physical
 * interactions.
 */
public interface StructuralReceiver {

    BlockPos structuralPosition();

    float structuralIntegrity();

    void receiveWorldForce(
            WorldForce force,
            float localMagnitude
    );

    void receiveStructuralDamage(
            StructuralDamage damage
    );
}
