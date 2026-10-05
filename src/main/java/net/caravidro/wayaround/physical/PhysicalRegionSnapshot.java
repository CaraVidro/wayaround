package net.caravidro.wayaround.physical;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.minecraft.core.BlockPos;

/**
 * Immutable result of a bounded world-space cavity scan.
 */
public record PhysicalRegionSnapshot(
        BlockPos seed,
        Set<BlockPos> cells,
        double volumeM3,
        List<PhysicalBoundaryFace> boundaries,
        List<PhysicalOpening> openings,
        Closure closure,
        boolean hitCellLimit,
        boolean hitDistanceLimit,
        boolean touchedUnloadedChunk
) implements PhysicalVolume {

    public enum Closure {
        SEALED,
        VENTED,
        INDETERMINATE
    }

    public PhysicalRegionSnapshot {
        seed =
                Objects.requireNonNull(
                        seed,
                        "seed"
                ).immutable();

        cells =
                Set.copyOf(
                        Objects.requireNonNull(
                                cells,
                                "cells"
                        )
                );

        boundaries =
                List.copyOf(
                        Objects.requireNonNull(
                                boundaries,
                                "boundaries"
                        )
                );

        openings =
                List.copyOf(
                        Objects.requireNonNull(
                                openings,
                                "openings"
                        )
                );

        Objects.requireNonNull(
                closure,
                "closure"
        );

        if (!Double.isFinite(
                volumeM3
        )
                || volumeM3 < 0.0) {
            throw new IllegalArgumentException(
                    "volumeM3 must be finite and >= 0"
            );
        }
    }

    @Override
    public boolean fullyCharacterized() {
        return closure
                != Closure.INDETERMINATE;
    }

    public boolean sealed() {
        return closure
                == Closure.SEALED;
    }

    public boolean vented() {
        return closure
                == Closure.VENTED;
    }

    public boolean contains(
            BlockPos pos
    ) {
        return cells.contains(
                pos
        );
    }
}
