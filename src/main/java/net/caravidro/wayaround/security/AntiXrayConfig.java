package net.caravidro.wayaround.security;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class AntiXrayConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue ENABLED = B.comment("Audit resource packs on multiplayer servers. Client reports are not tamper-proof.").define("enabled", true);
    public static final ModConfigSpec.BooleanValue VISUAL = B.comment("Ephemeral world/depth samples with no image save/upload; repeated server-verified occlusion contributes evidence.").define("visualOcclusionAudit", true);
    public static final ModConfigSpec.BooleanValue ENFORCE = B.comment("Apply public warnings, kicks and permanent vanilla bans. False-positive review is available with /wayanticheat.").define("enforce", true);
    public static final ModConfigSpec.IntValue PRIVATE_SECONDS = B.defineInRange("privateWarningEvidenceSeconds", 10, 5, 120);
    public static final ModConfigSpec.IntValue PUBLIC_SECONDS = B.defineInRange("publicWarningEvidenceSeconds", 30, 20, 180);
    public static final ModConfigSpec.IntValue KICK_SECONDS = B.comment("Confirmed active use per punishment round, including reconnects. At least 60 seconds and 20 seconds after public warning.").defineInRange("kickEvidenceSeconds", 60, 60, 600);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> APPROVED = B.comment("SHA-256 resource fingerprints approved by the owner; no approval by editable pack name.").defineListAllowEmpty("approvedFingerprints", List.of(), () -> "", v -> v instanceof String s && s.matches("[0-9a-f]{64}"));
    public static final ModConfigSpec SPEC = B.build();
    private AntiXrayConfig() {}
}
