package net.caravidro.wayaround.industrial.pipework;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class PipeNetwork {

    private static final int MAX_PIPES = 512;

    public record NetworkInfo(
            int pipeCount,
            int bottleneckFlowPerTick,
            float bottleneckPressureBar,
            int bottleneckTemperatureC,
            Set<PipeSpec.PipeMedium> supportedMedia
    ) {}

    private PipeNetwork() {
    }

    public static NetworkInfo inspect(
            Level level,
            BlockPos start,
            PipeSpec.PipeMedium medium
    ) {
        BlockState startState =
                level.getBlockState(
                        start
                );

        if (!(startState.getBlock()
                instanceof IndustrialPipeBlock startPipe)
                || !startPipe.spec()
                .supports(
                        medium
                )) {
            return new NetworkInfo(
                    0,
                    0,
                    0.0F,
                    0,
                    Set.of()
            );
        }

        Set<BlockPos> visited =
                new HashSet<>();

        ArrayDeque<BlockPos> pending =
                new ArrayDeque<>();

        visited.add(
                start
        );

        pending.add(
                start
        );

        int flow =
                startPipe.spec()
                        .flowPerTick();

        float pressure =
                startPipe.spec()
                        .maxPressureBar();

        int temperature =
                startPipe.spec()
                        .maxTemperatureC();

        while (!pending.isEmpty()
                && visited.size() < MAX_PIPES) {

            BlockPos current =
                    pending.removeFirst();

            IndustrialPipeBlock currentPipe =
                    (IndustrialPipeBlock) level
                            .getBlockState(
                                    current
                            )
                            .getBlock();

            PipeSpec currentSpec =
                    currentPipe.spec();

            flow =
                    Math.min(
                            flow,
                            currentSpec.flowPerTick()
                    );

            pressure =
                    Math.min(
                            pressure,
                            currentSpec.maxPressureBar()
                    );

            temperature =
                    Math.min(
                            temperature,
                            currentSpec.maxTemperatureC()
                    );

            for (Direction direction :
                    Direction.values()) {

                BlockPos next =
                        current.relative(
                                direction
                        );

                if (!level.hasChunkAt(
                        next
                )
                        || visited.contains(
                        next
                )) {
                    continue;
                }

                BlockState nextState =
                        level.getBlockState(
                                next
                        );

                if (!(nextState.getBlock()
                        instanceof IndustrialPipeBlock nextPipe)
                        || !nextPipe.spec()
                        .supports(
                                medium
                        )) {
                    continue;
                }

                visited.add(
                        next
                );

                pending.addLast(
                        next
                );
            }
        }

        return new NetworkInfo(
                visited.size(),
                flow,
                pressure,
                temperature,
                Set.of(
                        medium
                )
        );
    }

    public static NetworkInfo inspect(
            Level level,
            BlockPos start
    ) {
        BlockState startState =
                level.getBlockState(start);

        if (!(startState.getBlock()
                instanceof IndustrialPipeBlock startPipe)) {
            return new NetworkInfo(
                    0,
                    0,
                    0.0F,
                    0,
                    Set.of()
            );
        }

        Set<BlockPos> visited =
                new HashSet<>();

        ArrayDeque<BlockPos> pending =
                new ArrayDeque<>();

        visited.add(
                start
        );

        pending.add(
                start
        );

        int flow =
                startPipe.spec()
                        .flowPerTick();

        float pressure =
                startPipe.spec()
                        .maxPressureBar();

        int temperature =
                startPipe.spec()
                        .maxTemperatureC();

        Set<PipeSpec.PipeMedium> media =
                new HashSet<>(
                        startPipe.spec()
                                .media()
                );

        while (!pending.isEmpty()
                && visited.size() < MAX_PIPES) {

            BlockPos current =
                    pending.removeFirst();

            IndustrialPipeBlock currentPipe =
                    (IndustrialPipeBlock) level
                            .getBlockState(current)
                            .getBlock();

            PipeSpec currentSpec =
                    currentPipe.spec();

            flow =
                    Math.min(
                            flow,
                            currentSpec.flowPerTick()
                    );

            pressure =
                    Math.min(
                            pressure,
                            currentSpec.maxPressureBar()
                    );

            temperature =
                    Math.min(
                            temperature,
                            currentSpec.maxTemperatureC()
                    );

            media.retainAll(
                    currentSpec.media()
            );

            for (Direction direction :
                    Direction.values()) {

                BlockPos next =
                        current.relative(
                                direction
                        );

                if (!level.hasChunkAt(next)
                        || visited.contains(next)) {
                    continue;
                }

                BlockState nextState =
                        level.getBlockState(next);

                if (!(nextState.getBlock()
                        instanceof IndustrialPipeBlock nextPipe)
                        || !currentSpec.compatible(
                        nextPipe.spec()
                )) {
                    continue;
                }

                visited.add(next);
                pending.addLast(next);
            }
        }

        return new NetworkInfo(
                visited.size(),
                flow,
                pressure,
                temperature,
                Set.copyOf(media)
        );
    }
}
