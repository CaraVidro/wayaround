package net.caravidro.wayaround.industrial.power;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lightweight render anchor for shafts and gearboxes.
 *
 * It does not tick or store RPM. The mechanical source remains authoritative;
 * clients resolve the connected network and animate from that source.
 */
public final class MechanicalTransmissionBlockEntity
        extends BlockEntity {

    public MechanicalTransmissionBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.MECHANICAL_TRANSMISSION_ENTITY.get(),
                pos,
                state
        );
    }
}
