package net.caravidro.wayaround.physical;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A known connection from a bounded physical region to another environment.
 */
public record PhysicalOpening(
        BlockPos interiorCell,
        BlockPos outsideCell,
        Direction outward,
        Kind kind,
        double areaM2
) {
    public enum Kind {
        ATMOSPHERE,
        WORLD_EDGE
    }

    public PhysicalOpening {
        interiorCell =
                Objects.requireNonNull(
                        interiorCell,
                        "interiorCell"
                ).immutable();

        outsideCell =
                Objects.requireNonNull(
                        outsideCell,
                        "outsideCell"
                ).immutable();

        Objects.requireNonNull(
                outward,
                "outward"
        );

        Objects.requireNonNull(
                kind,
                "kind"
        );

        if (!Double.isFinite(
                areaM2
        )
                || areaM2 <= 0.0) {
            throw new IllegalArgumentException(
                    "areaM2 must be finite and > 0"
            );
        }
    }
}
