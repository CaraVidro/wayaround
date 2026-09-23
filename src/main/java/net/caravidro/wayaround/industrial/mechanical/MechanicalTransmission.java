package net.caravidro.wayaround.industrial.mechanical;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.power.MechanicalShaftBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class MechanicalTransmission {

    private static final int MAX_SHAFT_LENGTH =
            32;

    private MechanicalTransmission() {
    }

    @Nullable
    public static IRotationalPower findSource(
            Level level,
            BlockPos consumerPos,
            Direction direction
    ) {
        BlockPos cursor =
                consumerPos.relative(
                        direction
                );

        for (int distance = 0;
                distance <= MAX_SHAFT_LENGTH;
                distance++) {

            BlockState state =
                    level.getBlockState(
                            cursor
                    );

            if (state.getBlock()
                    instanceof MechanicalShaftBlock) {

                if (state.getValue(
                        MechanicalShaftBlock.AXIS
                ) != direction.getAxis()) {
                    return null;
                }

                cursor =
                        cursor.relative(
                                direction
                        );

                continue;
            }

            IRotationalPower source =
                    level.getCapability(
                            MechanicalCapabilities.ROTATION,
                            cursor,
                            direction.getOpposite()
                    );

            if (source == null
                    || source.axis()
                    != direction.getAxis()) {
                return null;
            }

            return source;
        }

        return null;
    }
}
