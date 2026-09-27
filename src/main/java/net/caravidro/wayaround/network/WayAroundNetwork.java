package net.caravidro.wayaround.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class WayAroundNetwork {

    private WayAroundNetwork() {
    }

    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        PayloadRegistrar registrar =
                event.registrar("1");

        /*
         * IMPORTANTE:
         *
         * Os handlers abaixo precisam permanecer common-safe.
         * ClientPayloadBridge nao importa Minecraft client, Screen,
         * renderizadores ou qualquer outra classe exclusiva do cliente.
         *
         * No cliente fisico, WayAroundClient instala as implementacoes reais.
         * No dedicated server, a ponte permanece no-op.
         */
        registrar.playToClient(
                FrostPayload.TYPE,
                FrostPayload.STREAM_CODEC,
                ClientPayloadBridge::handleFrost
        );

        registrar.playToClient(
                BlizzardStatePayload.TYPE,
                BlizzardStatePayload.STREAM_CODEC,
                ClientPayloadBridge::handleBlizzard
        );
    }
}
