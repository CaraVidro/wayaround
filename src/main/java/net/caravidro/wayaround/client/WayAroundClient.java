package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.calving.ClientCalvingEffects;
import net.caravidro.wayaround.network.ClientPayloadBridge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Bootstrap carregado exclusivamente no cliente físico.
 *
 * <p>Toda ligação entre networking common e classes de renderização/GUI deve
 * nascer aqui (ou em outra classe client-only), nunca em classes carregadas
 * pelo dedicated server.</p>
 */
@Mod(value = WayAround.MODID, dist = Dist.CLIENT)
public final class WayAroundClient {

    public WayAroundClient(IEventBus modEventBus) {
        ClientPayloadBridge.install(
                FrostRenderer::receive,
                payload -> ClientBlizzardState.receive(payload.intensity()),
                ClientCalvingEffects::receive
        );

        WayAround.LOGGER.info("Way Around client inicializado.");
    }
}
