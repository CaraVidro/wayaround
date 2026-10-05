package net.caravidro.wayaround.physical;

import java.util.List;

/**
 * Common geometric contract for a region that can contain matter.
 *
 * <p>Rooms, tanks, pipes, boilers and vehicle compartments should eventually
 * expose this language instead of inventing unrelated capacity concepts.</p>
 */
public interface PhysicalVolume {

    double volumeM3();

    List<PhysicalBoundaryFace> boundaries();

    List<PhysicalOpening> openings();

    boolean fullyCharacterized();

    default double boundaryAreaM2() {
        double total =
                0.0;

        for (PhysicalBoundaryFace boundary :
                boundaries()) {
            total +=
                    boundary.areaM2();
        }

        return total;
    }

    default double openingAreaM2() {
        double total =
                0.0;

        for (PhysicalOpening opening :
                openings()) {
            total +=
                    opening.areaM2();
        }

        return total;
    }
}
