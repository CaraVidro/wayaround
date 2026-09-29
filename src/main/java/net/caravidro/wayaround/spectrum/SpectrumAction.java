package net.caravidro.wayaround.spectrum;

import java.util.*;
import net.caravidro.wayaround.network.VoiceIntentC2SPayload;

/** Shared menu catalog. Network IDs stay explicit when entries are reordered. */
public enum SpectrumAction {
    SLASH(1, SpectrumType.TUKUNA, "Desmartelar", -1),
    FUGA(2, SpectrumType.TUKUNA, "Fuga / lançar", -1),
    TUKUNA_DOMAIN(3, SpectrumType.TUKUNA, "Domínio [prévia]", -1),
    FIRE_SLASH(4, SpectrumType.TUKUNA, "Desmartelar: fogo", -1),
    TUKUNA_ENERGY_VISION(5, SpectrumType.TUKUNA, "Visão de energia", -1),
    TUKUNA_BATTLE_STANCE(6, SpectrumType.TUKUNA, "Pose de batalha [segurar]", -1),

    BLUE(10, SpectrumType.VOID, "Técnica imaginária: azul", VoiceIntentC2SPayload.BLUE_SUMMON),
    RED(11, SpectrumType.VOID, "Vermelho / preparar", VoiceIntentC2SPayload.RED_FIRE),
    PURPLE(12, SpectrumType.VOID, "Roxo / lançar", VoiceIntentC2SPayload.PURPLE_VOID),
    INFINITY(13, SpectrumType.VOID, "Infinito: ativar / desativar", VoiceIntentC2SPayload.INFINITY_ON),
    VOID_DOMAIN(14, SpectrumType.VOID, "Expansão de domínio", VoiceIntentC2SPayload.VOID_DOMAIN_EXPAND),
    BLUE_HOLD(15, SpectrumType.VOID, "Azul: parar no lugar", VoiceIntentC2SPayload.BLUE_HOLD),
    BLUE_LAUNCH(16, SpectrumType.VOID, "Azul: lançar", VoiceIntentC2SPayload.BLUE_LAUNCH),
    BLUE_END(17, SpectrumType.VOID, "Azul: encerrar", VoiceIntentC2SPayload.BLUE_STOP),
    RED_LAUNCH(18, SpectrumType.VOID, "Vermelho: lançar", VoiceIntentC2SPayload.RED_LAUNCH),
    RED_MAX(19, SpectrumType.VOID, "Vermelho: energia máxima", VoiceIntentC2SPayload.RED_MAXIMUM),
    BLUE_ORBIT(20, SpectrumType.VOID, "Azul: orbitar", VoiceIntentC2SPayload.BLUE_ORBIT),
    BLUE_MAX(21, SpectrumType.VOID, "Azul: potência máxima", VoiceIntentC2SPayload.BLUE_OUTPUT),
    INFINITY_REINFORCE(22, SpectrumType.VOID, "Infinito: reforçar", VoiceIntentC2SPayload.INFINITY_REINFORCE),
    INFINITY_OFF(23, SpectrumType.VOID, "Infinito: encerrar", VoiceIntentC2SPayload.INFINITY_OFF),
    DUAL(24, SpectrumType.VOID, "Preparar azul + vermelho", VoiceIntentC2SPayload.DUAL_PREPARE),
    VOID_ENERGY_VISION(25, SpectrumType.VOID, "Visão de energia", -1),
    VOID_BATTLE_STANCE(26, SpectrumType.VOID, "Pose de batalha [segurar]", -1),

    JUSTICE_DOMAIN(30, SpectrumType.JUSTICE, "Domínio: tribunal", -1),
    JUSTICE_ENERGY_VISION(31, SpectrumType.JUSTICE, "Visão de energia", -1);

    public final int id, intent;
    public final SpectrumType spectrum;
    public final String label;

    SpectrumAction(int id, SpectrumType spectrum, String label, int intent) {
        this.id=id;
        this.spectrum=spectrum;
        this.label=label;
        this.intent=intent;
    }

    public static SpectrumAction byId(int id) {
        return Arrays.stream(values()).filter(a -> a.id==id).findFirst().orElse(null);
    }

    public boolean visibleInMenu() {
        return this != BLUE_END
                && this != INFINITY_REINFORCE
                && this != INFINITY_OFF;
    }

    public static List<SpectrumAction> forSpectrum(SpectrumType type) {
        return Arrays.stream(values())
                .filter(a -> a.spectrum==type && a.visibleInMenu())
                .toList();
    }
}
