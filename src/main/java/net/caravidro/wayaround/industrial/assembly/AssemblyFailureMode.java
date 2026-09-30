package net.caravidro.wayaround.industrial.assembly;

/**
 * Generic physical failure families. Machines translate these into concrete
 * consequences; the failure model never knows about "a furnace door" or "a
 * dam explosion".
 */
public enum AssemblyFailureMode {
    SPLIT,
    FRACTURE,
    SHEAR,
    PULL_OUT,
    BUCKLE,
    BEND,
    TWIST,
    SNAP,
    SLIP,
    SEIZE,
    TEAR,
    RUPTURE,
    DETACH
}
