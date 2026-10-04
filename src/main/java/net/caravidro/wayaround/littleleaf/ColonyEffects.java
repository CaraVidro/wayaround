package net.caravidro.wayaround.littleleaf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.level.block.state.BlockState;
public final class ColonyEffects {
 public static void work(ServerLevel l,BlockPos p,BlockState state,SoundEvent sound,float volume){
  // Only close observers receive tiny work effects; no background particle entities.
  if(l.getNearestPlayer(p.getX(),p.getY(),p.getZ(),32,false)==null)return;
  l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,state),p.getX()+.5,p.getY()+.5,p.getZ()+.5,4,.18,.18,.18,.02);l.playSound(null,p,sound,SoundSource.NEUTRAL,volume,1.1F+l.random.nextFloat()*.3F);
 }
}
