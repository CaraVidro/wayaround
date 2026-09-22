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

        registrar.playToClient(FrostPayload.TYPE, FrostPayload.STREAM_CODEC, FrostPayload::handle);

        registrar.playToClient(
                BlizzardStatePayload.TYPE,
                BlizzardStatePayload.STREAM_CODEC,
                BlizzardStatePayload::handle
        );
    }
}
