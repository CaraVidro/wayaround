package net.caravidro.wayaround.domain;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.justice.JusticeDomainManager;
import net.caravidro.wayaround.network.DomainIntroS2CPayload;
import net.caravidro.wayaround.spectrum.SpectrumAccess;
import net.caravidro.wayaround.spectrum.SpectrumType;
import net.caravidro.wayaround.spectrum.TukunaDomainPreview;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = WayAround.MODID)
public final class DomainIntroManager {

    public static final byte VOID =
            1;

    public static final byte TUKUNA =
            2;

    public static final byte JUSTICE =
            3;

    public static final int INTRO_TICKS =
            20;

    private static final Map<UUID, Pending> PENDING =
            new HashMap<>();

    private DomainIntroManager() {
    }

    public static boolean requestVoid(
            ServerPlayer player
    ) {
        return SpectrumAccess.has(
                player,
                SpectrumType.VOID
        )
                && request(
                player,
                VOID
        );
    }

    public static boolean requestTukuna(
            ServerPlayer player
    ) {
        return SpectrumAccess.has(
                player,
                SpectrumType.TUKUNA
        )
                && request(
                player,
                TUKUNA
        );
    }

    public static boolean requestJustice(
            ServerPlayer player
    ) {
        return SpectrumAccess.has(
                player,
                SpectrumType.JUSTICE
        )
                && request(
                player,
                JUSTICE
        );
    }

    private static boolean request(
            ServerPlayer player,
            byte style
    ) {
        if (!player.isAlive()
                || player.isSpectator()
                || PENDING.containsKey(
                player.getUUID()
        )) {
            return false;
        }

        long now =
                player.server
                        .getTickCount();

        PENDING.put(
                player.getUUID(),
                new Pending(
                        style,
                        now + INTRO_TICKS
                )
        );

        PacketDistributor.sendToPlayer(
                player,
                new DomainIntroS2CPayload(
                        style,
                        INTRO_TICKS
                )
        );

        return true;
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long now =
                server.getTickCount();

        Iterator<Map.Entry<UUID, Pending>> iterator =
                PENDING.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Pending> entry =
                    iterator.next();

            Pending pending =
                    entry.getValue();

            if (now < pending.executeAt()) {
                continue;
            }

            iterator.remove();

            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(
                                    entry.getKey()
                            );

            if (player == null
                    || !player.isAlive()) {
                continue;
            }

            switch (pending.style()) {
                case VOID ->
                        VoidDomainManager.expand(
                                player
                        );

                case TUKUNA ->
                        TukunaDomainPreview.start(
                                player
                        );

                case JUSTICE ->
                        JusticeDomainManager.beginTrialNearest(
                                player
                        );

                default -> {
                }
            }
        }
    }

    @SubscribeEvent
    public static void stop(
            ServerStoppedEvent event
    ) {
        PENDING.clear();
    }

    private record Pending(
            byte style,
            long executeAt
    ) {
    }
}
