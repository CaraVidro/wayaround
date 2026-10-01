package net.caravidro.wayaround.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.VoidDomainVisualPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Client state for the Void Domain.
 *
 * Important: the black/star environment is NOT drawn here. It is real
 * world-space geometry emitted by VoidDomainWorldRenderer. This class only
 * owns the opening white flash and the information-overload text for victims.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class VoidDomainClientEffects {

    private VoidDomainClientEffects() {
    }

    private static final Map<UUID, DomainVisual> DOMAINS =
            new HashMap<>();

    private static final String[] INFORMATION = {
            "0.0000000001 -> 1 -> infinito -> 0",
            "x = x + aquilo que voce ainda nao percebeu",
            "7 13 2 89 144 3 5 8 13 21 34",
            "memoria[agora] = memoria[antes] + memoria[depois]",
            "a distancia entre dois pontos contem pontos demais",
            "01110110 01101111 01101001 01100100",
            "se voce entendeu isto, ainda falta entender o resto",
            "tempo / tempo / tempo / tempo / tempo",
            "objeto observado altera objeto observando observador",
            "N = N + 1, para todo N, sem fim",
            "ver nao implica compreender",
            "som -> palavra -> sentido -> ruido -> sentido",
            "coordenada inexistente: [?, ?, ?]",
            "entrada recebida antes de ser enviada",
            "quantidade de informacao: indefinida",
            "um segundo contem detalhes demais",
            "A = B, B = C, C != A",
            "processando todas as possibilidades simultaneamente",
            "voce ja leu esta linha antes",
            "nao tente terminar o pensamento"
    };

    private static final int FORMATION_TICKS =
            32;

    private static UUID localDomain;
    private static UUID preparingDomain;
    private static boolean trapped;
    private static boolean lingeringFromTrap;
    private static int insideTicks;
    private static int aftershockTicks;
    private static int whiteFlashTicks;
    private static int formationWhiteTicks;
    private static int formationWhiteTotal;
    private static boolean hadLevel;

    public static void receive(
            VoidDomainVisualPayload payload
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        long tick =
                minecraft.level
                        .getGameTime();

        if (payload.action()
                == VoidDomainVisualPayload.PREPARE) {

            preparingDomain =
                    payload.owner();

            formationWhiteTotal =
                    Math.max(
                            1,
                            payload.durationTicks()
                    );

            formationWhiteTicks =
                    formationWhiteTotal;

            return;
        }

        if (payload.action()
                == VoidDomainVisualPayload.OPEN) {

            DOMAINS.put(
                    payload.owner(),
                    new DomainVisual(
                            payload.owner(),
                            new Vec3(
                                    payload.x(),
                                    payload.y(),
                                    payload.z()
                            ),
                            payload.radius(),
                            tick,
                            tick
                                    + payload.durationTicks()
                                    + 20L
                    )
            );

            return;
        }

        if (payload.action()
                == VoidDomainVisualPayload.ENTER) {

            localDomain =
                    payload.owner();

            if (payload.owner()
                    .equals(
                            preparingDomain
                    )) {
                preparingDomain =
                        null;

                formationWhiteTicks =
                        0;

                formationWhiteTotal =
                        0;
            }

            trapped =
                    payload.trapped();

            lingeringFromTrap =
                    false;

            insideTicks =
                    Math.max(
                            1,
                            payload.durationTicks()
                    );

            aftershockTicks =
                    0;

            whiteFlashTicks =
                    Math.max(
                            whiteFlashTicks,
                            12
                    );

            /*
             * ENTER replaces the local copy of the exterior shell with the
             * real pocket-space center.
             */
            DOMAINS.put(
                    payload.owner(),
                    new DomainVisual(
                            payload.owner(),
                            new Vec3(
                                    payload.x(),
                                    payload.y(),
                                    payload.z()
                            ),
                            payload.radius(),
                            tick - FORMATION_TICKS,
                            tick
                                    + payload.durationTicks()
                                    + 20L
                    )
            );

            return;
        }

        if (payload.action()
                == VoidDomainVisualPayload.CLOSE) {

            DOMAINS.remove(
                    payload.owner()
            );

            if (payload.owner()
                    .equals(
                            preparingDomain
                    )) {
                preparingDomain =
                        null;
                formationWhiteTicks =
                        0;
                formationWhiteTotal =
                        0;
            }

            if (payload.owner()
                    .equals(
                            localDomain
                    )) {

                lingeringFromTrap =
                        trapped;

                localDomain =
                        null;

                trapped =
                        false;

                insideTicks =
                        0;

                aftershockTicks =
                        120;
            }
        }
    }

    public static boolean isInside(
            UUID owner
    ) {
        return owner != null
                && owner.equals(
                        localDomain
                )
                && insideTicks > 0;
    }

    public static VisualDomain localInterior() {
        if (localDomain == null
                || insideTicks <= 0) {
            return null;
        }

        DomainVisual state =
                DOMAINS.get(
                        localDomain
                );

        if (state == null) {
            return null;
        }

        return new VisualDomain(
                state.owner,
                state.center,
                state.radius,
                1.0F
        );
    }

    public static List<VisualDomain> visuals() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return List.of();
        }

        long tick =
                minecraft.level
                        .getGameTime();

        List<VisualDomain> result =
                new ArrayList<>();

        for (DomainVisual state :
                DOMAINS.values()) {

            if (tick > state.expiresAt) {
                continue;
            }

            float formation =
                    Mth.clamp(
                            (tick - state.openedAt)
                                    / (float) FORMATION_TICKS,
                            0.0F,
                            1.0F
                    );

            result.add(
                    new VisualDomain(
                            state.owner,
                            state.center,
                            state.radius,
                            formation
                    )
            );
        }

        return result;
    }

    @SubscribeEvent
    public static void onTick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            if (hadLevel) {
                DOMAINS.clear();
                localDomain = null;
                trapped = false;
                lingeringFromTrap = false;
                insideTicks = 0;
                aftershockTicks = 0;
                whiteFlashTicks = 0;
                preparingDomain = null;
                formationWhiteTicks = 0;
                formationWhiteTotal = 0;
            }

            hadLevel =
                    false;

            return;
        }

        hadLevel =
                true;

        long tick =
                minecraft.level
                        .getGameTime();

        Iterator<Map.Entry<UUID, DomainVisual>> iterator =
                DOMAINS.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            DomainVisual state =
                    iterator.next()
                            .getValue();

            if (tick > state.expiresAt) {
                iterator.remove();
            }
        }

        if (formationWhiteTicks > 0) {
            formationWhiteTicks--;
        }

        if (whiteFlashTicks > 0) {
            whiteFlashTicks--;
        }

        if (insideTicks > 0) {
            insideTicks--;

            if (insideTicks == 0
                    && localDomain != null) {

                localDomain =
                        null;

                lingeringFromTrap =
                        trapped;

                trapped =
                        false;

                aftershockTicks =
                        120;
            }

        } else if (aftershockTicks > 0) {
            aftershockTicks--;
        }

        if (minecraft.player == null) {
            return;
        }

        if (trapped
                && localDomain != null
                && tick % 10L == 0L) {

            addInformationToChat(
                    minecraft,
                    tick
            );

        } else if (lingeringFromTrap
                && aftershockTicks > 0
                && tick % 32L == 0L) {

            addInformationToChat(
                    minecraft,
                    tick + 7919L
            );
        }
    }

    @SubscribeEvent
    public static void onGui(
            RenderGuiLayerEvent.Post event
    ) {
        if (!event.getName()
                .equals(
                        VanillaGuiLayers.CAMERA_OVERLAYS
                )) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        GuiGraphics graphics =
                event.getGuiGraphics();

        int width =
                minecraft.getWindow()
                        .getGuiScaledWidth();

        int height =
                minecraft.getWindow()
                        .getGuiScaledHeight();

        /*
         * PREPARE whiteout belongs in the GUI pass, not the client tick.
         * The tick owns only timing/state; this pass owns actual drawing.
         */
        if (formationWhiteTicks > 0
                && formationWhiteTotal > 0) {

            float elapsed =
                    1.0F
                            - formationWhiteTicks
                                    / (float) formationWhiteTotal;

            float fade =
                    Mth.clamp(
                            (elapsed - 0.62F)
                                    / 0.38F,
                            0.0F,
                            1.0F
                    );

            fade =
                    fade * fade
                            * (
                            3.0F
                                    - 2.0F * fade
                    );

            int alpha =
                    Mth.clamp(
                            Math.round(
                                    255.0F
                                            * fade
                            ),
                            0,
                            255
                    );

            if (alpha > 0) {
                graphics.fill(
                        0,
                        0,
                        width,
                        height,
                        alpha << 24
                                | 0xFFFFFF
                );
            }
        }

        if (whiteFlashTicks > 0) {
            float progress =
                    whiteFlashTicks
                            / 12.0F;

            int alpha =
                    Mth.clamp(
                            Math.round(
                                    255.0F
                                            * progress
                                            * progress
                            ),
                            0,
                            255
                    );

            graphics.fill(
                    0,
                    0,
                    width,
                    height,
                    alpha << 24
                            | 0xFFFFFF
            );
        }

        boolean overload =
                trapped
                        && localDomain != null
                        && insideTicks > 0;

        boolean lingering =
                lingeringFromTrap
                        && aftershockTicks > 0;

        if (!overload
                && !lingering) {
            return;
        }

        float strength =
                lingering
                        ? aftershockTicks
                                / 120.0F
                        : 1.0F;

        drawInformation(
                minecraft,
                graphics,
                width,
                height,
                minecraft.level
                        .getGameTime()
                        / 2L,
                strength
        );
    }

    private static void drawInformation(
            Minecraft minecraft,
            GuiGraphics graphics,
            int width,
            int height,
            long phase,
            float strength
    ) {
        int lines =
                14;

        /*
         * The overload should feel intrusive without becoming an opaque wall
         * over the scene. Keep the text ghost-like so the player can still see
         * the singularity and distant geometry behind it.
         */
        int alpha =
                Mth.clamp(
                        Math.round(
                                92.0F
                                        * strength
                        ),
                        0,
                        92
                );

        for (int index = 0;
             index < lines;
             index++) {

            long hash =
                    mix(
                            phase
                                    * 131L
                                    + index
                                            * 0xD1B54A32D192ED03L
                    );

            String text =
                    INFORMATION[
                            Math.floorMod(
                                    (int) hash,
                                    INFORMATION.length
                            )
                            ];

            int x =
                    Math.floorMod(
                            (int) (
                                    hash >>> 11
                            ),
                            Math.max(
                                    1,
                                    width
                                            - 120
                            )
                    );

            int y =
                    Math.floorMod(
                            (int) (
                                    hash >>> 37
                            ),
                            Math.max(
                                    1,
                                    height
                                            - 10
                            )
                    );

            int gray =
                    150
                            + Math.floorMod(
                            (int) (
                                    hash >>> 19
                            ),
                            106
                    );

            int color =
                    alpha << 24
                            | gray << 16
                            | gray << 8
                            | gray;

            graphics.drawString(
                    minecraft.font,
                    text,
                    x,
                    y,
                    color,
                    false
            );
        }
    }

    private static void addInformationToChat(
            Minecraft minecraft,
            long seed
    ) {
        long hash =
                mix(
                        seed
                                * 0x9E3779B97F4A7C15L
                );

        String line =
                INFORMATION[
                        Math.floorMod(
                                (int) hash,
                                INFORMATION.length
                        )
                        ];

        minecraft.gui
                .getChat()
                .addMessage(
                        Component.literal(
                                        "[VOID] "
                                                + line
                                )
                                .withStyle(
                                        ChatFormatting.GRAY
                                )
                );
    }

    private static long mix(
            long value
    ) {
        value ^=
                value >>> 30;

        value *=
                0xBF58476D1CE4E5B9L;

        value ^=
                value >>> 27;

        value *=
                0x94D049BB133111EBL;

        return value
                ^ value >>> 31;
    }

    private static final class DomainVisual {

        private final UUID owner;
        private final Vec3 center;
        private final float radius;
        private final long openedAt;
        private final long expiresAt;

        private DomainVisual(
                UUID owner,
                Vec3 center,
                float radius,
                long openedAt,
                long expiresAt
        ) {
            this.owner =
                    owner;

            this.center =
                    center;

            this.radius =
                    radius;

            this.openedAt =
                    openedAt;

            this.expiresAt =
                    expiresAt;
        }
    }

    public record VisualDomain(
            UUID owner,
            Vec3 center,
            float radius,
            float formationProgress
    ) {
    }
}
