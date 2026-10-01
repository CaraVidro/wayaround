package net.caravidro.wayaround.industrial.steam;

/**
 * A machine that can accept physical steam from a WayAround steam network.
 *
 * Steam is intentionally not Forge Energy. It carries amount, pressure and
 * temperature, and consumers decide what work can be extracted from it.
 */
public interface SteamReceiver {

    /**
     * @return accepted steam units; callers remove only this amount.
     */
    int receiveSteam(
            int amount,
            float pressureBar,
            int temperatureC
    );
}
