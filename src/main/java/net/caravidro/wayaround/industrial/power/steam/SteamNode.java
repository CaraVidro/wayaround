package net.caravidro.wayaround.industrial.power.steam;

import net.minecraft.core.Direction;

public interface SteamNode {
    int steamStored();
    int steamCapacity();
    double pressureBar();
    int receiveSteam(int amount, boolean simulate);
    int extractSteam(int amount, boolean simulate);

    default boolean canConnectSteam(Direction side) {
        return true;
    }
}
