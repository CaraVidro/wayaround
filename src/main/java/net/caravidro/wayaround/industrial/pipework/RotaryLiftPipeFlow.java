package net.caravidro.wayaround.industrial.pipework;

import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.industrial.mechanical.MechanicalLoad;
import net.caravidro.wayaround.industrial.mechanical.MechanicalTransmission;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Runtime for the simple rotary intake heads.
 *
 * The head owns no invisible infinite fluid source. Narrow families only
 * visualize circulation; the large-water family conserves real source blocks
 * through a small persistent buffer in the existing PipeBlockEntity.
 */
public final class RotaryLiftPipeFlow {

    private static final float MIN_WORK_RPM =
            5.0F;

    private RotaryLiftPipeFlow() {
    }

    public static void tick(
            ServerLevel level,
            PipeBlockEntity head,
            RotaryLiftPipeBlock block
    ) {
        BlockState state =
                head.getBlockState();

        Direction output =
                state.getValue(
                        RotaryLiftPipeBlock.FACING
                );

        IRotationalPower source =
                bestSource(
                        level,
                        head.getBlockPos(),
                        output
                );

        PipeBlockEntity downstream =
                downstream(
                        level,
                        head.getBlockPos(),
                        output
                );

        boolean sourceWater =
                isWaterSource(
                        level,
                        head.getBlockPos()
                                .below()
                );

        boolean routeReady =
                downstream != null;

        boolean workingLoad =
                routeReady
                        && (
                        sourceWater
                                || (
                                block.physicalTransfer()
                                        && head.amount() > 0
                        )
                );

        float familyScale =
                Mth.clamp(
                        block.spec()
                                .flowPerTick()
                                / 980.0F,
                        0.20F,
                        2.60F
                );

        float requestedPower =
                workingLoad
                        ? 0.55F
                                + familyScale * 0.78F
                        : 0.16F
                                + familyScale * 0.08F;

        float requiredTorque =
                workingLoad
                        ? 0.16F
                                + familyScale * 0.25F
                        : 0.045F;

        MechanicalLoad.OperatingPoint operating =
                MechanicalLoad.operate(
                        source,
                        requestedPower,
                        requiredTorque,
                        68.0F,
                        0.0F,
                        head.rotaryLiftVibration(),
                        head.rotaryLiftCondition()
                );

        float targetRpm =
                operating.torqueStarved()
                        ? 0.0F
                        : operating.targetRpm();

        head.updateRotaryLift(
                targetRpm,
                operating.fulfillment(),
                operating.torqueStarved()
        );

        if (downstream == null
                || Math.abs(
                head.rotaryLiftRpm()
        ) < MIN_WORK_RPM) {
            return;
        }

        int cadence =
                block.physicalTransfer()
                        ? 4
                        : 3;

        if (Math.floorMod(
                level.getGameTime()
                        + head.getBlockPos()
                                .asLong(),
                cadence
        ) != 0) {
            return;
        }

        if (block.physicalTransfer()) {
            physicalTick(
                    level,
                    head,
                    downstream,
                    output,
                    sourceWater,
                    block
            );

        } else if (sourceWater) {
            visualTick(
                    level,
                    head,
                    downstream,
                    output,
                    block
            );
        }
    }

    private static void visualTick(
            ServerLevel level,
            PipeBlockEntity head,
            PipeBlockEntity downstream,
            Direction output,
            RotaryLiftPipeBlock block
    ) {
        int visualStrength =
                Math.max(
                        40,
                        Math.min(
                                block.spec()
                                        .flowPerTick(),
                                Math.round(
                                        Math.abs(
                                                head.rotaryLiftRpm()
                                        )
                                                * 10.0F
                                )
                        )
                );

        PipeFlow.visualFromMachine(
                level,
                downstream,
                output,
                new FluidStack(
                        Fluids.WATER,
                        1
                ),
                visualStrength
        );

        head.markFlow(
                new FluidStack(
                        Fluids.WATER,
                        1
                ),
                output
        );
    }

    private static void physicalTick(
            ServerLevel level,
            PipeBlockEntity head,
            PipeBlockEntity downstream,
            Direction output,
            boolean sourceWater,
            RotaryLiftPipeBlock block
    ) {
        if (!PipeFlow.hasPhysicalWaterOutlet(
                level,
                downstream,
                output
        )) {
            return;
        }

        if (sourceWater
                && head.amount() < 1000) {

            int room =
                    head.capacity()
                            - head.amount();

            if (room >= 1000) {
                FluidStack pulled =
                        PipeFlow.pullForMachine(
                                level,
                                head.getBlockPos()
                                        .below(),
                                Direction.UP,
                                1000,
                                head.stored()
                        );

                if (!pulled.isEmpty()) {
                    head.receive(
                            pulled
                    );
                }
            }
        }

        if (head.amount() < 1000) {
            return;
        }

        int machineLimit =
                Math.min(
                        block.spec()
                                .flowPerTick(),
                        Math.max(
                                1000,
                                Math.round(
                                        Math.abs(
                                                head.rotaryLiftRpm()
                                        )
                                                * 28.0F
                                )
                        )
                );

        int moved =
                PipeFlow.pushPhysicalWaterFromMachine(
                        level,
                        downstream,
                        output,
                        head.stored(),
                        machineLimit
                );

        if (moved > 0) {
            head.used(
                    moved
            );

            head.markFlow(
                    new FluidStack(
                            Fluids.WATER,
                            1
                    ),
                    output
            );
        }
    }

    private static PipeBlockEntity downstream(
            ServerLevel level,
            BlockPos head,
            Direction output
    ) {
        BlockPos pos =
                head.relative(
                        output
                );

        if (!level.hasChunkAt(
                pos
        )) {
            return null;
        }

        return level.getBlockEntity(
                pos
        ) instanceof PipeBlockEntity pipe
                && pipe.owner() == null
                && pipe.complete()
                && supportsLiquid(
                        pipe
                )
                ? pipe
                : null;
    }

    private static boolean supportsLiquid(
            PipeBlockEntity pipe
    ) {
        if (pipe.getBlockState()
                .getBlock()
                instanceof LargePipeBlock) {
            return true;
        }

        return pipe.getBlockState()
                .getBlock()
                instanceof IndustrialPipeBlock industrial
                && industrial.spec()
                        .supports(
                                PipeSpec.PipeMedium.LIQUID
                        );
    }

    private static boolean isWaterSource(
            ServerLevel level,
            BlockPos pos
    ) {
        if (!level.hasChunkAt(
                pos
        )) {
            return false;
        }

        var state =
                level.getFluidState(
                        pos
                );

        return state.isSource()
                && state.getType()
                        == Fluids.WATER;
    }

    private static IRotationalPower bestSource(
            ServerLevel level,
            BlockPos pos,
            Direction output
    ) {
        IRotationalPower best =
                null;

        float score =
                -1.0F;

        for (Direction direction :
                Direction.values()) {

            if (direction == Direction.DOWN
                    || direction == output) {
                continue;
            }

            IRotationalPower candidate =
                    MechanicalTransmission.findSource(
                            level,
                            pos,
                            direction
                    );

            float candidateScore =
                    MechanicalTransmission.sourceScore(
                            candidate
                    );

            if (candidateScore > score) {
                best =
                        candidate;

                score =
                        candidateScore;
            }
        }

        return best;
    }
}
