package net.caravidro.wayaround.dream;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class DreamConfig {
    private static final ModConfigSpec.Builder BUILDER=new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue FAKE_MENU_TICKS=BUILDER.comment("Duration of the inert death menu, in ticks.")
            .defineInRange("fakeMenuTicks",60,40,100);
    public static final ModConfigSpec.IntValue COPY_BUDGET=BUILDER.comment("Maximum block positions copied or cleared per server tick (shared by sessions).")
            .defineInRange("copyBlocksPerTick",16384,1024,65536);
    public static final ModConfigSpec.IntValue MAX_SESSIONS=BUILDER.comment("One shared sky: V2 intentionally allows only one session.")
            .defineInRange("maxConcurrentDreams",1,1,1);
    public static final ModConfigSpec SPEC=BUILDER.build();
    private DreamConfig() {}
}
