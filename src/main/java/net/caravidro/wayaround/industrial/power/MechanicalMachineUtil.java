package net.caravidro.wayaround.industrial.power;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.industrial.mechanical.MechanicalTransmission;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

final class MechanicalMachineUtil {

    private MechanicalMachineUtil() {
    }

    @Nullable
    static IRotationalPower findBestSource(
            Level level,
            BlockPos pos
    ) {
        IRotationalPower best =
                null;

        float score =
                -1.0F;

        for (Direction direction :
                Direction.values()) {

            IRotationalPower source =
                    MechanicalTransmission.findSource(
                            level,
                            pos,
                            direction
                    );

            float candidate =
                    MechanicalTransmission.sourceScore(
                            source
                    );

            if (candidate > score) {
                score =
                        candidate;

                best =
                        source;
            }
        }

        return best;
    }
}
