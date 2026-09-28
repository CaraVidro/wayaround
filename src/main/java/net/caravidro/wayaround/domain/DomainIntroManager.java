package net.caravidro.wayaround.domain;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.justice.JusticeDomainManager;
import net.caravidro.wayaround.network.DomainIntroS2CPayload;
import net.caravidro.wayaround.spectrum.SpectrumAccess;
import net.caravidro.wayaround.spectrum.SpectrumActions;
import net.caravidro.wayaround.spectrum.SpectrumProgression;
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

    public static final byte SHADOWS =
            4;

    public static final byte SIMPLE =
            0;

    public static final byte APEX =
            1;

    /*
     * The intro starts on the first live keyword. 22 ticks gives Tobias enough
     * time to hear the rest of a normal phrase without adding a second full
     * second AFTER the phrase has already finished.
     */
    public static final int INTRO_TICKS =
            22;

    private static final int CONFIRM_GRACE_TICKS =
            80;

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
                && SpectrumProgression.voidDomainUnlocked(
                player
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

    /**
     * First half of the live-voice handshake. It may start only while the
     * Shift+T Spectrum panel is actually active server-side.
     *
     * PREPARED does not cast anything. A later normal domain intent confirms
     * the same Pending object. If Tobias never hears the full phrase, it dies.
     */
    public static boolean prepareVoice(
            ServerPlayer player,
            byte style
    ) {
        if (style == 0) {
            PENDING.remove(player.getUUID());
            return true;
        }
        if (!player.isAlive() || player.isSpectator()) return false;
        if (!SpectrumActions.combatMode(
                player
        )
                || !canUseStyle(
                player,
                style
        )) {
            return false;
        }

        long now =
                player.server
                        .getTickCount();

        byte variant =
                SpectrumProgression.domainVariant(
                        player,
                        style
                );

        Pending existing =
                PENDING.get(
                        player.getUUID()
                );

        if (existing != null) {
            if (existing.style
                    != style) {
                return false;
            }

            existing.variant =
                    variant;

            return true;
        }

        Pending pending =
                new Pending(
                        style,
                        variant,
                        now + INTRO_TICKS,
                        now
                                + INTRO_TICKS
                                + CONFIRM_GRACE_TICKS,
                        false
                );

        PENDING.put(
                player.getUUID(),
                pending
        );

        sendIntro(
                player,
                pending
        );

        return true;
    }

    public static void previewShadows(
            ServerPlayer player
    ) {
        PacketDistributor.sendToPlayer(
                player,
                new DomainIntroS2CPayload(
                        SHADOWS,
                        APEX,
                        INTRO_TICKS + 8
                )
        );
    }

    private static boolean request(
            ServerPlayer player,
            byte style
    ) {
        if (!player.isAlive()
                || player.isSpectator()
                || !canUseStyle(
                player,
                style
        )) {
            return false;
        }

        long now =
                player.server
                        .getTickCount();

        byte variant =
                SpectrumProgression.domainVariant(
                        player,
                        style
                );

        Pending existing =
                PENDING.get(
                        player.getUUID()
                );

        if (existing != null) {
            if (existing.style
                    != style) {
                return false;
            }

            existing.variant =
                    variant;

            existing.confirmed =
                    true;

            /*
             * A phrase confirmed after the visual has technically closed but
             * still inside the grace window should cast immediately, not replay
             * the whole intro.
             */
            if (now > existing.expiresAt) {
                PENDING.remove(
                        player.getUUID()
                );
                return false;
            }

            return true;
        }

        Pending pending =
                new Pending(
                        style,
                        variant,
                        now + INTRO_TICKS,
                        now
                                + INTRO_TICKS
                                + CONFIRM_GRACE_TICKS,
                        true
                );

        PENDING.put(
                player.getUUID(),
                pending
        );

        sendIntro(
                player,
                pending
        );

        return true;
    }

    private static boolean canUseStyle(
            ServerPlayer player,
            byte style
    ) {
        return switch (style) {
            case VOID ->
                    SpectrumAccess.has(
                            player,
                            SpectrumType.VOID
                    )
                            && SpectrumProgression
                            .voidDomainUnlocked(
                                    player
                            );

            case TUKUNA ->
                    SpectrumAccess.has(
                            player,
                            SpectrumType.TUKUNA
                    );

            case JUSTICE ->
                    SpectrumAccess.has(
                            player,
                            SpectrumType.JUSTICE
                    );

            default ->
                    false;
        };
    }

    private static void sendIntro(
            ServerPlayer player,
            Pending pending
    ) {
        PacketDistributor.sendToPlayer(
                player,
                new DomainIntroS2CPayload(
                        pending.style,
                        pending.variant,
                        INTRO_TICKS
                )
        );
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

            if (now < pending.executeAt) {
                continue;
            }

            if (!pending.confirmed) {
                if (now >= pending.expiresAt) {
                    iterator.remove();
                }

                continue;
            }

            iterator.remove();

            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(
                                    entry.getKey()
                            );

            if (player == null
                    || !player.isAlive() || player.isSpectator()
                    || !canUseStyle(player, pending.style)) {
                continue;
            }

            execute(
                    player,
                    pending.style
            );
        }
    }

    private static void execute(
            ServerPlayer player,
            byte style
    ) {
        switch (style) {
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

    @SubscribeEvent
    public static void stop(
            ServerStoppedEvent event
    ) {
        PENDING.clear();
    }

    private static final class Pending {

        private final byte style;
        private byte variant;
        private final long executeAt;
        private final long expiresAt;
        private boolean confirmed;

        private Pending(
                byte style,
                byte variant,
                long executeAt,
                long expiresAt,
                boolean confirmed
        ) {
            this.style =
                    style;

            this.variant =
                    variant;

            this.executeAt =
                    executeAt;

            this.expiresAt =
                    expiresAt;

            this.confirmed =
                    confirmed;
        }
    }
}
