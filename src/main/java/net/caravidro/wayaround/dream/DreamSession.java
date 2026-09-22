package net.caravidro.wayaround.dream;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
public final class DreamSession {
    public final UUID player;
    public final int slot;
    public final CompoundTag backup;
    public final DreamRegion region;
    public DreamState state=DreamState.PREPARING;
    public int age;
    public Vec3 frozen;
    public UUID actor;
    public final BaseSemanticMap semantics;
    public final DreamDirector director=new DreamDirector();
    public DreamSession(UUID player,int slot,CompoundTag backup,DreamRegion region,Vec3 frozen,BaseSemanticMap semantics) {
        this.player=player;this.slot=slot;this.backup=backup;this.region=region;this.frozen=frozen;
        this.semantics=semantics;
    }
}
