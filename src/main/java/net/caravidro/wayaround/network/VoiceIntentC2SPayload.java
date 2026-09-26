package net.caravidro.wayaround.network;

import net.caravidro.wayaround.cinematic.PlayerControlLockManager;

import net.caravidro.wayaround.spectrum.SpectrumType;

import net.caravidro.wayaround.spectrum.SpectrumAccess;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.blue.BlueManager;
import net.caravidro.wayaround.blue.ImaginaryBetaManager;
import net.caravidro.wayaround.infinity.InfinityManager;
import net.caravidro.wayaround.domain.VoidDomainManager;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.cursed.TukunaManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record VoiceIntentC2SPayload(
        byte intent,
        float output,
        float urgency
) implements CustomPacketPayload {

    public VoiceIntentC2SPayload(
            byte intent,
            float output
    ) {
        this(
                intent,
                output,
                0.0F
        );
    }

    public static final byte BLUE_SUMMON = 1;
    public static final byte BLUE_ORBIT = 2;
    public static final byte BLUE_LAUNCH = 3;
    public static final byte BLUE_STOP = 4;
    public static final byte BLUE_HOLD = 5;
    public static final byte RED_FIRE = 6;
    public static final byte BLUE_OUTPUT = 7;
    public static final byte INFINITY_REINFORCE = 8;
    public static final byte INFINITY_OFF = 9;
    public static final byte DUAL_PREPARE = 10;
    public static final byte PURPLE_VOID = 11;
    public static final byte INFINITY_ON = 12;
    public static final byte TUKUNA_SWAP_CONFIRM = 13;
    public static final byte TUKUNA_DESMARTELAR = 14;
    public static final byte VOID_DOMAIN_EXPAND = 15;
    public static final byte RED_LAUNCH = 16;
    public static final byte RED_MAXIMUM = 17;
    public static final byte TUKUNA_DESMARTELAR_FIRE = 18;
    public static final byte TUKUNA_FUGA = 19;

    public static final Type<VoiceIntentC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "voice_intent_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            VoiceIntentC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeByte(
                                payload.intent()
                        );
                        buf.writeFloat(
                                payload.output()
                        );
                        buf.writeFloat(
                                payload.urgency()
                        );
                    },
                    buf ->
                            new VoiceIntentC2SPayload(
                                    buf.readByte(),
                                    buf.readFloat(),
                                    buf.readFloat()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            VoiceIntentC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) execute(player, payload);
        });
    }

    public static void execute(ServerPlayer player, VoiceIntentC2SPayload payload) {
        if (!player.isAlive() || (player.isSpectator() && payload.intent()!=TUKUNA_SWAP_CONFIRM) || TukunaManager.isSilencedHost(player)
                || !Float.isFinite(payload.output()) || !Float.isFinite(payload.urgency())) return;
        if (TukunaManager.isDraftingPact(player)) return;

        if (PlayerControlLockManager.actionsLocked(
                player
        )) {
            return;
        }

        if (payload.intent()
                == TUKUNA_SWAP_CONFIRM) {
            TukunaManager.confirmSwap(
                    player
            );
            return;
        }

        if (payload.intent()
                == TUKUNA_DESMARTELAR) {
            TukunaManager.castPossessedDesmartelar(
                    player,
                    false
            );
            return;
        }

        if (payload.intent()
                == TUKUNA_DESMARTELAR_FIRE) {
            TukunaManager.castPossessedDesmartelar(
                    player,
                    true
            );
            return;
        }

        if (payload.intent()
                == TUKUNA_FUGA) {
            net.caravidro.wayaround.spectrum.SpectrumActions.perform(player, net.caravidro.wayaround.spectrum.SpectrumAction.FUGA);
            return;
        }

        if (!hasTechniqueAccess(
                player
        )) {
            return;
        }

        /*
         * Voice and menu casts share the same short visual tell.
         * It is deliberately cosmetic: no new permission or cooldown lives here.
         */
        net.caravidro.wayaround.spectrum.SpectrumActions
                .pulseVoidSkillAura(
                        player
                );

        switch (payload.intent()) {
            case BLUE_SUMMON ->
                    BlueManager.invokeFromVoice(
                            player,
                            payload.output(),
                            payload.urgency()
                    );

            case BLUE_ORBIT ->
                    BlueManager.orbitActive(
                            player
                    );

            case BLUE_LAUNCH ->
                    BlueManager.launchActive(
                            player
                    );

            case BLUE_STOP ->
                    BlueManager.releaseActive(
                            player
                    );

            case BLUE_HOLD ->
                    BlueManager.holdActive(
                            player
                    );

            case RED_FIRE ->
                    ImaginaryBetaManager.prepareRed(
                            player
                    );

            case RED_LAUNCH ->
                    ImaginaryBetaManager.launchRed(
                            player
                    );

            case RED_MAXIMUM ->
                    ImaginaryBetaManager.chargeRedMaximum(
                            player
                    );

            case BLUE_OUTPUT ->
                    BlueManager.setActiveOutput(
                            player,
                            payload.output()
                    );

            case INFINITY_REINFORCE -> {
                if (hasInfinityAccess(
                        player
                )) {
                    InfinityManager.reinforce(
                            player,
                            payload.output(),
                            payload.urgency()
                    );
                }
            }

            case INFINITY_OFF -> {
                if (hasInfinityAccess(
                        player
                )) {
                    InfinityManager.deactivate(
                            player
                    );
                }
            }

            case INFINITY_ON -> {
                if (hasInfinityAccess(
                        player
                )) {
                    InfinityManager.activateMax(
                            player
                    );
                }
            }

            case DUAL_PREPARE ->
                    ImaginaryBetaManager.prepareDual(
                            player
                    );

            case PURPLE_VOID ->
                    ImaginaryBetaManager.launchPurpleVoid(
                            player
                    );

            case VOID_DOMAIN_EXPAND ->
                    VoidDomainManager.expand(
                            player
                    );

            default -> {
            }
        }
    }

    private static boolean hasTechniqueAccess(
            ServerPlayer player
    ) {
        /*
         * A legacy BLUE item is only a debug controller for Blue itself.
         * It is never permission to invoke Red, Purple, Infinity or a Domain.
         * Voice techniques require the real Void Spectrum.
         */
        return SpectrumAccess.has(
                player,
                SpectrumType.VOID
        );
    }

    private static boolean hasInfinityAccess(
            ServerPlayer player
    ) {
        return SpectrumAccess.has(player, SpectrumType.VOID);
    }
}

