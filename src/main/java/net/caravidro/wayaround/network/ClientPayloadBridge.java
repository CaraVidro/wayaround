package net.caravidro.wayaround.network;

import java.util.Objects;
import java.util.function.Consumer;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Ponte common-safe para payloads que só produzem efeitos no cliente.
 *
 * <p>Esta classe não pode importar nenhuma classe de {@code net.minecraft.client}
 * nem classes de renderização/tela do Way Around. No dedicated server os
 * handlers permanecem no-op; no cliente, {@code WayAroundClient} instala as
 * implementações reais durante a inicialização física do cliente.</p>
 */
public final class ClientPayloadBridge {

    private static final Consumer<FrostPayload> NOOP_FROST = payload -> {};
    private static final Consumer<BlizzardStatePayload> NOOP_BLIZZARD = payload -> {};
    private static final Consumer<CalvingNetwork.CalvingShakePayload> NOOP_CALVING = payload -> {};

    private static volatile Consumer<FrostPayload> frostHandler = NOOP_FROST;
    private static volatile Consumer<BlizzardStatePayload> blizzardHandler = NOOP_BLIZZARD;
    private static volatile Consumer<CalvingNetwork.CalvingShakePayload> calvingHandler = NOOP_CALVING;

    private ClientPayloadBridge() {
    }

    public static void install(
            Consumer<FrostPayload> frost,
            Consumer<BlizzardStatePayload> blizzard,
            Consumer<CalvingNetwork.CalvingShakePayload> calving
    ) {
        frostHandler = Objects.requireNonNull(frost, "frost");
        blizzardHandler = Objects.requireNonNull(blizzard, "blizzard");
        calvingHandler = Objects.requireNonNull(calving, "calving");
    }

    public static void handleFrost(FrostPayload payload, IPayloadContext context) {
        frostHandler.accept(payload);
    }

    public static void handleBlizzard(BlizzardStatePayload payload, IPayloadContext context) {
        blizzardHandler.accept(payload);
    }

    public static void handleCalving(
            CalvingNetwork.CalvingShakePayload payload,
            IPayloadContext context
    ) {
        calvingHandler.accept(payload);
    }
}
