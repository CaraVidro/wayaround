package net.caravidro.wayaround.dream;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

/** A bounded 5x5 chunk copy, processed only on the gameplay server thread. */
public final class DreamRegion {
    private static final net.minecraft.server.level.TicketType<ChunkPos> TICKET=
            net.minecraft.server.level.TicketType.create("wayaround_dream",Comparator.comparingLong(ChunkPos::toLong));
    private final Set<ChunkPos> tickets=new HashSet<>();
    public static final int WIDTH=80;
    public final ServerLevel source,target;
    public final int sourceX,sourceZ,targetX,targetZ=0;
    public final List<BlockPos> containers=new ArrayList<>();
    public final List<BlockPos> doors=new ArrayList<>(),torches=new ArrayList<>();
    private final BlockPos preferredBed;
    public BlockPos originalBed;
    public BlockPos copiedBed;
    private int chunkIndex,blockIndex;
    private LevelChunk from,to;
    private boolean clearing;
    public DreamRegion(ServerLevel source,ServerLevel target,ChunkPos center,int slot,BlockPos preferredBed) {
        this.source=source;this.target=target;sourceX=(center.x-2)*16;sourceZ=(center.z-2)*16;targetX=slot*512;
        this.originalBed=preferredBed;this.preferredBed=preferredBed;
    }
    public BlockPos map(BlockPos pos){return pos.offset(targetX-sourceX,0,targetZ-sourceZ);}
    public AABB bounds(){return new AABB(targetX,source.getMinBuildHeight(),targetZ,targetX+WIDTH,source.getMaxBuildHeight(),targetZ+WIDTH);}
    public boolean contains(double x,double z){return x>=targetX+1&&x<targetX+WIDTH-1&&z>=targetZ+1&&z<targetZ+WIDTH-1;}
    public int progress(){return chunkIndex*100/25;}
    public void clear() {
        clearing=true;chunkIndex=0;blockIndex=0;from=null;to=null;
        target.getEntities((net.minecraft.world.entity.Entity)null,bounds().inflate(4),e->!(e instanceof net.minecraft.world.entity.player.Player))
                .forEach(net.minecraft.world.entity.Entity::discard);
    }
    public boolean step(int budget) {
        if(chunkIndex>=25)return true;
        int cx=chunkIndex%5,cz=chunkIndex/5;
        if(to==null) {
            ChunkPos targetChunk=new ChunkPos(targetX/16+cx,targetZ/16+cz);
            if(tickets.add(targetChunk))target.getChunkSource().addRegionTicket(TICKET,targetChunk,2,targetChunk);
            to=target.getChunk(targetX/16+cx,targetZ/16+cz);
            if(!clearing)from=source.getChunk(sourceX/16+cx,sourceZ/16+cz);
            // Only load one source/destination chunk pair in this tick.
            return false;
        }
        int volume=source.getHeight()*256;
        BlockPos.MutableBlockPos a=new BlockPos.MutableBlockPos(),b=new BlockPos.MutableBlockPos();
        long deadline=System.nanoTime()+5_000_000L;
        for(int count=0;count<budget&&blockIndex<volume;count++,blockIndex++){
            if((count&255)==0&&System.nanoTime()>deadline)break;
            int x=blockIndex&15,z=(blockIndex>>4)&15,y=(blockIndex>>8)+source.getMinBuildHeight();
            a.set(sourceX+cx*16+x,y,sourceZ+cz*16+z);b.set(targetX+cx*16+x,y,targetZ+cz*16+z);
            BlockState state=clearing?Blocks.AIR.defaultBlockState():from.getBlockState(a);
            if(to.getBlockState(b)!=state) target.setBlock(b,state,2|16|32);
            if(!clearing&&state.getBlock() instanceof BedBlock&&state.getValue(BedBlock.PART)==BedPart.HEAD) {
                if(copiedBed==null||a.equals(preferredBed)) {originalBed=a.immutable();copiedBed=b.immutable();}
            }
            if(!clearing&&state.getBlock() instanceof DoorBlock&&state.getValue(DoorBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER&&doors.size()<512)
                doors.add(b.immutable());
            if(!clearing&&state.getBlock() instanceof TorchBlock&&torches.size()<512)torches.add(b.immutable());
        }
        if(blockIndex==volume) {
            if(!clearing)copyBlockEntities();
            to.setUnsaved(true);chunkIndex++;blockIndex=0;from=null;to=null;
        }
        if(chunkIndex==25&&clearing){
            target.getEntities((net.minecraft.world.entity.Entity)null,bounds().inflate(4),
                    e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(net.minecraft.world.entity.Entity::discard);
            releaseTickets();
        }
        return chunkIndex==25;
    }
    public void releaseTickets(){
        for(ChunkPos chunk:tickets)target.getChunkSource().removeRegionTicket(TICKET,chunk,2,chunk);
        tickets.clear();
    }
    private void copyBlockEntities(){
        for(var entry:from.getBlockEntities().entrySet()){
            BlockEntity original=entry.getValue();BlockPos pos=map(entry.getKey());
            if(original instanceof ChestBlockEntity||original instanceof BarrelBlockEntity)containers.add(pos);
            boolean safe=original instanceof ChestBlockEntity||original instanceof BarrelBlockEntity
                    ||original instanceof AbstractFurnaceBlockEntity||original instanceof SignBlockEntity
                    ||original instanceof net.caravidro.wayaround.industrial.ReforcedBlasterBlockEntity
                    ||original instanceof net.caravidro.wayaround.industrial.power.SolarPanelBlockEntity
                    ||original instanceof net.caravidro.wayaround.industrial.power.SteamEngineBlockEntity;
            if(!safe)continue;
            CompoundTag data=original.saveWithFullMetadata(source.registryAccess());
            data.putInt("x",pos.getX());data.putInt("y",pos.getY());data.putInt("z",pos.getZ());
            if(original instanceof SignBlockEntity) {
                // Keep the written text, but strip executable click actions from copied signs.
                for(String side:List.of("front_text","back_text")) {
                    CompoundTag text=data.getCompound(side);
                    for(String key:List.of("messages","filtered_messages")) {
                        ListTag messages=text.getList(key,Tag.TAG_STRING),plain=new ListTag();
                        for(Tag line:messages){
                            var component=net.minecraft.network.chat.Component.Serializer.fromJson(line.getAsString(),source.registryAccess());
                            plain.add(StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(
                                    net.minecraft.network.chat.Component.literal(component==null?"":component.getString()),target.registryAccess())));
                        }
                        if(!messages.isEmpty())text.put(key,plain);
                    }
                }
            }
            BlockEntity clone=target.getBlockEntity(pos);
            if(clone!=null){clone.loadWithComponents(data,target.registryAccess());clone.setChanged();}
        }
    }
}
