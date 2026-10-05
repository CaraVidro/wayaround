package net.caravidro.wayaround.physical;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * One cell-scale wall face around a physical volume.
 *
 * <p>Area is deliberately coarse in this first implementation. Thickness and
 * material already give future pressure/heat/structural solvers a shared wall
 * description without forcing those solvers to know Minecraft block types.</p>
 */
public record PhysicalBoundaryFace(
        BlockPos interiorCell,
        BlockPos boundaryCell,
        Direction outward,
        MaterialDefinition material,
        double areaM2,
        double thicknessM
) {
    public PhysicalBoundaryFace {
        interiorCell =
                Objects.requireNonNull(
                        interiorCell,
                        "interiorCell"
                ).immutable();

        boundaryCell =
                Objects.requireNonNull(
                        boundaryCell,
                        "boundaryCell"
                ).immutable();

        Objects.requireNonNull(
                outward,
                "outward"
        );

        Objects.requireNonNull(
                material,
                "material"
        );

        if (!Double.isFinite(
                areaM2
        )
                || areaM2 <= 0.0) {
            throw new IllegalArgumentException(
                    "areaM2 must be finite and > 0"
            );
        }

        if (!Double.isFinite(
                thicknessM
        )
                || thicknessM <= 0.0) {
            throw new IllegalArgumentException(
                    "thicknessM must be finite and > 0"
            );
        }
    }
}
