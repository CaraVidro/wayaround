package net.caravidro.wayaround.industrial.pipework;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Bounded pipe routing.
 *
 * Valves keep their legacy self-pumping behavior for compatibility.
 * Mechanical machines can now use the same topology through pullForMachine /
 * pushFromMachine without inventing a second fluid network.
 */
public final class PipeFlow {

    private static final int MAX_NODES = 128;

    private record Step(
            PipeBlockEntity pipe,
            Direction arrival,
            List<PipeBlockEntity> path
    ) {}

    private record SuctionStep(
            PipeBlockEntity pipe,
            BlockPos previous,
            List<PipeBlockEntity> path
    ) {}

    private record Outlet(
            PipeBlockEntity pipe,
            Direction direction,
            List<PipeBlockEntity> path
    ) {}

    private PipeFlow() {
    }

    public static int mouthDistance(
            PipeBlockEntity pipe
    ) {
        return pipe.getBlockState().getBlock()
                instanceof LargePipeBlock
                ? 2
                : 1;
    }

    private static boolean axisAllows(
            PipeBlockEntity pipe,
            Direction direction
    ) {
        return !(pipe.getBlockState().getBlock()
                instanceof LargePipeBlock)
                || pipe.getBlockState()
                        .getValue(
                                LargePipeBlock.AXIS
                        )
                        == direction.getAxis();
    }

    private static PipeBlockEntity neighbor(
            ServerLevel level,
            PipeBlockEntity pipe,
            Direction direction
    ) {
        int step =
                pipe.getBlockState().getBlock()
                        instanceof LargePipeBlock
                        ? 3
                        : 1;

        BlockPos pos =
                pipe.getBlockPos()
                        .relative(
                                direction,
                                step
                        );

        if (level.hasChunkAt(pos)
                && level.getBlockEntity(pos)
                instanceof PipeBlockEntity other
                && other.owner() == null
                && other.complete()
                && axisAllows(
                        other,
                        direction
                )) {
            return other;
        }

        /*
         * Adapter: a regular pipe mouth touches a large duct's open end
         * without pretending the ordinary pipe lives inside the shell.
         */
        pos =
                pipe.getBlockPos()
                        .relative(
                                direction,
                                2
                        );

        if (level.hasChunkAt(pos)
                && level.getBlockEntity(pos)
                instanceof PipeBlockEntity other
                && other.owner() == null
                && other.complete()
                && axisAllows(
                        other,
                        direction
                )
                && (
                pipe.getBlockState().getBlock()
                        instanceof LargePipeBlock
        ) != (
                other.getBlockState().getBlock()
                        instanceof LargePipeBlock
        )) {
            return other;
        }

        return null;
    }

    private static List<Outlet> outlets(
            ServerLevel level,
            PipeBlockEntity root
    ) {
        return outlets(
                level,
                root,
                root.flow()
        );
    }

    private static List<Outlet> outlets(
            ServerLevel level,
            PipeBlockEntity root,
            Direction rootFlow
    ) {
        if (root.hasValve()
                && (
                !root.open()
                        || root.flow() != rootFlow
        )) {
            return List.of();
        }

        ArrayDeque<Step> queue =
                new ArrayDeque<>();

        Set<BlockPos> seen =
                new HashSet<>();

        ArrayList<Outlet> outputs =
                new ArrayList<>();

        queue.add(
                new Step(
                        root,
                        rootFlow,
                        List.of(root)
                )
        );

        while (!queue.isEmpty()
                && seen.size() < MAX_NODES) {

            Step step =
                    queue.removeFirst();

            PipeBlockEntity pipe =
                    step.pipe();

            if (!supportsLiquid(pipe)) {
                continue;
            }

            if (!seen.add(
                    pipe.getBlockPos()
            )) {
                continue;
            }

            if (pipe != root
                    && pipe.hasValve()
                    && (
                    !pipe.open()
                            || pipe.flow()
                            != step.arrival()
            )) {
                continue;
            }

            int connections =
                    0;

            for (Direction direction :
                    Direction.values()) {

                if (!axisAllows(
                        pipe,
                        direction
                )
                        || direction
                        == step.arrival()
                        .getOpposite()) {
                    continue;
                }

                if (pipe == root
                        && direction != rootFlow) {
                    continue;
                }

                if (pipe != root
                        && pipe.hasValve()
                        && direction
                        != pipe.flow()) {
                    continue;
                }

                PipeBlockEntity next =
                        neighbor(
                                level,
                                pipe,
                                direction
                        );

                if (next == null) {
                    continue;
                }

                connections++;

                if (seen.contains(
                        next.getBlockPos()
                )) {
                    continue;
                }

                ArrayList<PipeBlockEntity> path =
                        new ArrayList<>(
                                step.path()
                        );

                path.add(next);

                queue.addLast(
                        new Step(
                                next,
                                direction,
                                List.copyOf(path)
                        )
                );
            }

            if (connections == 0) {
                outputs.add(
                        new Outlet(
                                pipe,
                                step.arrival(),
                                step.path()
                        )
                );
            }
        }

        return outputs;
    }

    public static void pump(
            ServerLevel level,
            PipeBlockEntity valve
    ) {
        List<Outlet> outputs =
                outlets(
                        level,
                        valve
                );

        if (outputs.isEmpty()) {
            return;
        }

        intake(
                level,
                valve
        );

        if (valve.getBlockState().getBlock()
                instanceof LargePipeBlock duct) {

            int budget =
                    duct.colossal()
                            ? 16
                            : 4;

            for (BlockPos source :
                    mouthArea(
                            valve,
                            valve.flow()
                                    .getOpposite()
                    )) {

                if (--budget <= 0) {
                    break;
                }

                intakeAt(
                        level,
                        valve,
                        source
                );
            }
        }

        if (valve.stored().isEmpty()) {
            return;
        }

        int limit =
                valve.getBlockState().getBlock()
                        instanceof LargePipeBlock duct
                        ? (
                        duct.colossal()
                                ? 16000
                                : 4000
                )
                        : 1000;

        int consumed =
                deliver(
                        level,
                        outputs.get(
                                valve.nextOutlet(
                                        outputs.size()
                                )
                        ),
                        valve.stored(),
                        limit
                );

        if (consumed > 0) {
            valve.used(consumed);
        }
    }

    /**
     * Pull liquid from either a direct tank/source or a bounded pipe network.
     *
     * @param intakePos block immediately behind the pump
     * @param towardPump direction from intakePos toward the pump
     * @param preferred current pump-buffer fluid; non-empty prevents mixing
     */
    public static FluidStack pullForMachine(
            ServerLevel level,
            BlockPos intakePos,
            Direction towardPump,
            int limit,
            FluidStack preferred
    ) {
        if (limit <= 0
                || !level.hasChunkAt(
                intakePos
        )) {
            return FluidStack.EMPTY;
        }

        if (level.getBlockEntity(
                intakePos
        ) instanceof PipeBlockEntity root
                && root.owner() == null
                && root.complete()
                && supportsLiquid(root)) {

            return pullFromNetwork(
                    level,
                    root,
                    towardPump,
                    limit,
                    preferred
            );
        }

        return drainExternal(
                level,
                intakePos,
                towardPump,
                limit,
                preferred
        );
    }

    private static FluidStack pullFromNetwork(
            ServerLevel level,
            PipeBlockEntity root,
            Direction towardPump,
            int limit,
            FluidStack preferred
    ) {
        ArrayDeque<SuctionStep> queue =
                new ArrayDeque<>();

        Set<BlockPos> seen =
                new HashSet<>();

        queue.add(
                new SuctionStep(
                        root,
                        null,
                        List.of(root)
                )
        );

        while (!queue.isEmpty()
                && seen.size() < MAX_NODES) {

            SuctionStep step =
                    queue.removeFirst();

            PipeBlockEntity pipe =
                    step.pipe();

            if (!seen.add(
                    pipe.getBlockPos()
            )) {
                continue;
            }

            if (pipe.hasValve()
                    && !pipe.open()) {
                continue;
            }

            for (Direction direction :
                    Direction.values()) {

                if (!axisAllows(
                        pipe,
                        direction
                )) {
                    continue;
                }

                if (pipe == root
                        && direction == towardPump) {
                    continue;
                }

                PipeBlockEntity next =
                        neighbor(
                                level,
                                pipe,
                                direction
                        );

                if (next != null) {
                    if (!supportsLiquid(next)) {
                        continue;
                    }

                    if (step.previous() != null
                            && next.getBlockPos()
                            .equals(
                                    step.previous()
                            )) {
                        continue;
                    }

                    if (!seen.contains(
                            next.getBlockPos()
                    )) {
                        ArrayList<PipeBlockEntity> path =
                                new ArrayList<>(
                                        step.path()
                                );

                        path.add(next);

                        queue.addLast(
                                new SuctionStep(
                                        next,
                                        pipe.getBlockPos(),
                                        List.copyOf(path)
                                )
                        );
                    }

                    continue;
                }

                BlockPos mouth =
                        pipe.getBlockPos()
                                .relative(
                                        direction,
                                        mouthDistance(pipe)
                                );

                FluidStack drained =
                        drainExternal(
                                level,
                                mouth,
                                direction.getOpposite(),
                                limit,
                                preferred
                        );

                if (!drained.isEmpty()) {
                    markSuctionPath(
                            step.path(),
                            drained
                    );

                    return drained;
                }
            }
        }

        return FluidStack.EMPTY;
    }

    private static boolean supportsLiquid(
            PipeBlockEntity pipe
    ) {
        if (pipe.getBlockState().getBlock()
                instanceof LargePipeBlock) {
            return true;
        }

        return pipe.getBlockState().getBlock()
                instanceof IndustrialPipeBlock industrial
                && industrial.spec()
                        .supports(
                                PipeSpec.PipeMedium.LIQUID
                        );
    }

    private static FluidStack drainExternal(
            ServerLevel level,
            BlockPos source,
            Direction side,
            int limit,
            FluidStack preferred
    ) {
        if (limit <= 0
                || !level.hasChunkAt(source)) {
            return FluidStack.EMPTY;
        }

        IFluidHandler handler =
                level.getCapability(
                        Capabilities.FluidHandler.BLOCK,
                        source,
                        side
                );

        if (handler != null) {
            FluidStack simulated =
                    preferred.isEmpty()
                            ? handler.drain(
                            limit,
                            IFluidHandler.FluidAction.SIMULATE
                    )
                            : handler.drain(
                            preferred.copyWithAmount(
                                    limit
                            ),
                            IFluidHandler.FluidAction.SIMULATE
                    );

            if (simulated.isEmpty()
                    || (
                    !preferred.isEmpty()
                            && !FluidStack.isSameFluidSameComponents(
                            simulated,
                            preferred
                    )
            )) {
                return FluidStack.EMPTY;
            }

            return handler.drain(
                    simulated,
                    IFluidHandler.FluidAction.EXECUTE
            );
        }

        if (limit < 1000) {
            return FluidStack.EMPTY;
        }

        var state =
                level.getBlockState(
                        source
                );

        var liquid =
                state.getFluidState();

        if (!(state.getBlock()
                instanceof LiquidBlock)
                || !liquid.isSource()) {
            return FluidStack.EMPTY;
        }

        FluidStack fluid =
                new FluidStack(
                        liquid.getType(),
                        1000
                );

        if (!preferred.isEmpty()
                && !FluidStack.isSameFluidSameComponents(
                fluid,
                preferred
        )) {
            return FluidStack.EMPTY;
        }

        if (!level.setBlock(
                source,
                Blocks.AIR.defaultBlockState(),
                3
        )) {
            return FluidStack.EMPTY;
        }

        jet(
                level,
                source,
                side,
                fluid,
                5
        );

        return fluid;
    }

    /**
     * Push a pump-owned fluid budget through an attached pipe network.
     * The returned amount is the only amount the caller may remove from its
     * own buffer.
     */
    public static int pushFromMachine(
            ServerLevel level,
            PipeBlockEntity root,
            Direction awayFromPump,
            FluidStack supplied,
            int limit
    ) {
        if (root == null
                || supplied.isEmpty()
                || limit <= 0
                || root.owner() != null
                || !root.complete()) {
            return 0;
        }

        if (root.getBlockState().getBlock()
                instanceof IndustrialPipeBlock pipe
                && !pipe.spec()
                .supports(
                        PipeSpec.PipeMedium.LIQUID
                )) {
            return 0;
        }

        List<Outlet> outputs =
                outlets(
                        level,
                        root,
                        awayFromPump
                );

        if (outputs.isEmpty()) {
            return 0;
        }

        Outlet outlet =
                outputs.get(
                        root.nextOutlet(
                                outputs.size()
                        )
                );

        return deliver(
                level,
                outlet,
                supplied,
                limit
        );
    }

    private static int deliver(
            ServerLevel level,
            Outlet outlet,
            FluidStack fluid,
            int machineLimit
    ) {
        BlockPos end =
                outlet.pipe()
                        .getBlockPos()
                        .relative(
                                outlet.direction(),
                                mouthDistance(
                                        outlet.pipe()
                                )
                        );

        if (!level.hasChunkAt(end)
                || fluid.isEmpty()
                || machineLimit <= 0) {
            return 0;
        }

        int limit =
                Math.min(
                        machineLimit,
                        fluid.getAmount()
                );

        for (PipeBlockEntity part :
                outlet.path()) {

            if (part.getBlockState().getBlock()
                    instanceof IndustrialPipeBlock pipe) {
                limit =
                        Math.min(
                                limit,
                                pipe.spec()
                                        .flowPerTick()
                        );
            }
        }

        if (limit <= 0) {
            return 0;
        }

        IFluidHandler receiver =
                level.getCapability(
                        Capabilities.FluidHandler.BLOCK,
                        end,
                        outlet.direction()
                                .getOpposite()
                );

        int consumed =
                0;

        if (receiver != null) {
            consumed =
                    receiver.fill(
                            fluid.copyWithAmount(
                                    limit
                            ),
                            IFluidHandler.FluidAction.EXECUTE
                    );

        } else if (outlet.pipe()
                .getBlockState()
                .getBlock()
                instanceof LargePipeBlock
                || outlet.pipe()
                .getBlockState()
                .is(
                        PipeworkContent.LARGE_WATER_MAIN.get()
                )) {

            if (fluid.getAmount() >= 1000
                    && limit >= 1000) {

                for (BlockPos mouth :
                        mouthArea(
                                outlet.pipe(),
                                outlet.direction()
                        )) {

                    if (consumed + 1000
                            > Math.min(
                            limit,
                            fluid.getAmount()
                    )) {
                        break;
                    }

                    consumed +=
                            spill(
                                    level,
                                    mouth,
                                    fluid,
                                    true
                            );
                }
            }

        } else {
            BlockState endState =
                    level.getBlockState(
                            end
                    );

            /*
             * Open pipe outlets used to "consume" sub-bucket flow into
             * particles only. At common mechanical-pump RPMs the per-tick
             * budget is below 1000 mB, so water visually travelled through the
             * pipe but could never become water at the far end.
             *
             * The terminal pipe now acts as a tiny hydraulic accumulator:
             * partial flow is stored there until one real bucket is available,
             * then a real fluid source is placed at the outlet. Nothing is
             * deleted just because the current tick carries 300-900 mB.
             */
            if (endState.canBeReplaced()
                    && level.getFluidState(
                    end
            ).isEmpty()) {

                PipeBlockEntity terminal =
                        outlet.pipe();

                FluidStack stored =
                        terminal.stored();

                boolean compatible =
                        stored.isEmpty()
                                || FluidStack.isSameFluidSameComponents(
                                stored,
                                fluid
                        );

                if (compatible) {
                    int room =
                            Math.max(
                                    0,
                                    terminal.capacity()
                                            - terminal.amount()
                            );

                    int accepted =
                            Math.min(
                                    limit,
                                    room
                            );

                    if (accepted > 0) {
                        terminal.receive(
                                fluid.copyWithAmount(
                                        accepted
                                )
                        );

                        consumed =
                                accepted;
                    }

                    if (terminal.amount() >= 1000) {
                        int released =
                                spill(
                                        level,
                                        end,
                                        terminal.stored(),
                                        true
                                );

                        if (released > 0) {
                            terminal.used(
                                    released
                            );
                        }
                    } else if (accepted > 0) {
                        jet(
                                level,
                                end,
                                outlet.direction(),
                                fluid,
                                3
                        );
                    }
                }

            } else {
                var endFluid =
                        level.getFluidState(
                                end
                        );

                if (!endFluid.isEmpty()
                        && endFluid.getType()
                        == fluid.getFluid()) {

                    /*
                     * Discharging into an existing body of the same liquid is
                     * a valid sink. The water is not expected to create a
                     * second block on top of an occupied water cell.
                     */
                    consumed =
                            Math.min(
                                    limit,
                                    fluid.getAmount()
                            );

                    jet(
                            level,
                            end,
                            outlet.direction(),
                            fluid,
                            5
                    );
                }
            }
        }

        if (consumed > 0) {
            FluidStack marking =
                    fluid.copyWithAmount(
                            1
                    );

            for (int index = 0;
                 index < outlet.path().size();
                 index++) {

                PipeBlockEntity pipe =
                        outlet.path()
                                .get(index);

                Direction direction =
                        index + 1
                                < outlet.path().size()
                                ? Direction.getNearest(
                                outlet.path()
                                        .get(index + 1)
                                        .getBlockPos()
                                        .getX()
                                        - pipe.getBlockPos()
                                        .getX(),
                                outlet.path()
                                        .get(index + 1)
                                        .getBlockPos()
                                        .getY()
                                        - pipe.getBlockPos()
                                        .getY(),
                                outlet.path()
                                        .get(index + 1)
                                        .getBlockPos()
                                        .getZ()
                                        - pipe.getBlockPos()
                                        .getZ()
                        )
                                : outlet.direction();

                pipe.markFlow(
                        marking,
                        direction
                );
            }
        }

        return consumed;
    }

    private static void markSuctionPath(
            List<PipeBlockEntity> path,
            FluidStack fluid
    ) {
        FluidStack marking =
                fluid.copyWithAmount(
                        1
                );

        for (int index = path.size() - 1;
             index >= 0;
             index--) {

            PipeBlockEntity pipe =
                    path.get(index);

            Direction direction =
                    index > 0
                            ? Direction.getNearest(
                            path.get(index - 1)
                                    .getBlockPos()
                                    .getX()
                                    - pipe.getBlockPos()
                                    .getX(),
                            path.get(index - 1)
                                    .getBlockPos()
                                    .getY()
                                    - pipe.getBlockPos()
                                    .getY(),
                            path.get(index - 1)
                                    .getBlockPos()
                                    .getZ()
                                    - pipe.getBlockPos()
                                    .getZ()
                    )
                            : pipe.flow();

            pipe.markFlow(
                    marking,
                    direction
            );
        }
    }

    private static void intake(
            ServerLevel level,
            PipeBlockEntity pipe
    ) {
        BlockPos source =
                pipe.getBlockPos()
                        .relative(
                                pipe.flow()
                                        .getOpposite(),
                                mouthDistance(pipe)
                        );

        if (!level.hasChunkAt(source)) {
            return;
        }

        intakeAt(
                level,
                pipe,
                source
        );
    }

    private static List<BlockPos> mouthArea(
            PipeBlockEntity pipe,
            Direction direction
    ) {
        BlockPos mouth =
                pipe.getBlockPos()
                        .relative(
                                direction,
                                mouthDistance(pipe)
                        );

        ArrayList<BlockPos> positions =
                new ArrayList<>();

        positions.add(mouth);

        if (pipe.getBlockState().getBlock()
                instanceof LargePipeBlock duct) {

            int radius =
                    duct.radius();

            for (int a = -radius;
                 a <= radius;
                 a++) {
                for (int b = -radius;
                     b <= radius;
                     b++) {

                    if (a == 0
                            && b == 0) {
                        continue;
                    }

                    positions.add(
                            switch (direction.getAxis()) {
                                case X ->
                                        mouth.offset(
                                                0,
                                                a,
                                                b
                                        );
                                case Y ->
                                        mouth.offset(
                                                a,
                                                0,
                                                b
                                        );
                                case Z ->
                                        mouth.offset(
                                                a,
                                                b,
                                                0
                                        );
                            }
                    );
                }
            }
        }

        return positions;
    }

    private static void intakeAt(
            ServerLevel level,
            PipeBlockEntity pipe,
            BlockPos source
    ) {
        if (!level.hasChunkAt(source)) {
            return;
        }

        int room =
                pipe.capacity()
                        - pipe.amount();

        if (room <= 0) {
            return;
        }

        IFluidHandler handler =
                level.getCapability(
                        Capabilities.FluidHandler.BLOCK,
                        source,
                        pipe.flow()
                );

        FluidStack fluid;

        if (handler != null) {
            fluid =
                    pipe.stored().isEmpty()
                            ? handler.drain(
                            Math.min(
                                    1000,
                                    room
                            ),
                            IFluidHandler.FluidAction.SIMULATE
                    )
                            : handler.drain(
                            pipe.stored()
                                    .copyWithAmount(
                                            Math.min(
                                                    1000,
                                                    room
                                            )
                                    ),
                            IFluidHandler.FluidAction.SIMULATE
                    );

            if (fluid.isEmpty()
                    || (
                    !pipe.stored().isEmpty()
                            && !FluidStack.isSameFluidSameComponents(
                            fluid,
                            pipe.stored()
                    )
            )) {
                return;
            }

            fluid =
                    handler.drain(
                            fluid,
                            IFluidHandler.FluidAction.EXECUTE
                    );

            if (fluid.isEmpty()) {
                return;
            }

        } else {
            var state =
                    level.getBlockState(
                            source
                    );

            var liquid =
                    state.getFluidState();

            if (room < 1000
                    || !(state.getBlock()
                    instanceof LiquidBlock)
                    || !liquid.isSource()) {
                return;
            }

            fluid =
                    new FluidStack(
                            liquid.getType(),
                            1000
                    );

            if (!pipe.stored().isEmpty()
                    && !FluidStack.isSameFluidSameComponents(
                    fluid,
                    pipe.stored()
            )) {
                return;
            }

            if (!level.setBlock(
                    source,
                    Blocks.AIR.defaultBlockState(),
                    3
            )) {
                return;
            }
        }

        pipe.receive(fluid);

        jet(
                level,
                source,
                pipe.flow(),
                fluid,
                5
        );
    }

    public static int spill(
            ServerLevel level,
            BlockPos end,
            FluidStack fluid,
            boolean particles
    ) {
        if (!level.hasChunkAt(end)
                || fluid.getAmount() < 1000
                || !level.getBlockState(end)
                .canBeReplaced()
                || !level.getFluidState(end)
                .isEmpty()) {
            return 0;
        }

        var state =
                fluid.getFluid()
                        .defaultFluidState()
                        .createLegacyBlock();

        if (!(state.getBlock()
                instanceof LiquidBlock)) {
            return 0;
        }

        if (!level.setBlock(
                end,
                state,
                3
        )) {
            return 0;
        }

        if (particles) {
            jet(
                    level,
                    end,
                    Direction.DOWN,
                    fluid,
                    12
            );
        }

        return 1000;
    }

    public static ParticleOptions drip(
            FluidStack fluid
    ) {
        return !fluid.isEmpty()
                && fluid.getFluid()
                .is(FluidTags.LAVA)
                ? ParticleTypes.DRIPPING_LAVA
                : ParticleTypes.DRIPPING_WATER;
    }

    public static ParticleOptions spray(
            FluidStack fluid
    ) {
        if (!fluid.isEmpty()
                && fluid.getFluid()
                .is(FluidTags.LAVA)) {
            return ParticleTypes.FALLING_LAVA;
        }

        if (fluid.isEmpty()
                || fluid.getFluid()
                .is(FluidTags.WATER)) {
            return ParticleTypes.SPLASH;
        }

        return new BlockParticleOption(
                ParticleTypes.BLOCK,
                fluid.getFluid()
                        .defaultFluidState()
                        .createLegacyBlock()
        );
    }

    private static void jet(
            ServerLevel level,
            BlockPos pos,
            Direction direction,
            FluidStack fluid,
            int count
    ) {
        for (int index = 0;
             index < count;
             index++) {

            level.sendParticles(
                    spray(fluid),
                    pos.getX()
                            + 0.5
                            + (
                            level.random.nextDouble()
                                    - 0.5
                    )
                            * 0.3,
                    pos.getY()
                            + 0.5
                            + (
                            level.random.nextDouble()
                                    - 0.5
                    )
                            * 0.3,
                    pos.getZ()
                            + 0.5
                            + (
                            level.random.nextDouble()
                                    - 0.5
                    )
                            * 0.3,
                    0,
                    direction.getStepX(),
                    direction.getStepY(),
                    direction.getStepZ(),
                    0.18
            );
        }
    }
}
