package net.caravidro.wayaround.dream;

import com.mojang.brigadier.Command;
import net.caravidro.wayaround.WayAround;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid=WayAround.MODID)
public final class DreamCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        var dream=Commands.literal("dream");
        for(String action:new String[]{"start","stop","info","testplayer","die"}){
            dream.then(Commands.literal(action).then(Commands.argument("player",EntityArgument.player()).executes(context->{
                var p=EntityArgument.getPlayer(context,"player");
                try{
                    switch(action){
                        case "start" -> DreamManager.start(p);
                        case "stop" -> DreamManager.stop(p);
                        case "die" -> DreamManager.die(p);
                        case "testplayer" -> DreamManager.spawnActor(p);
                        case "info" -> {
                            var profile=PlayerBehaviorManager.profile(p);var home=PlayerBehaviorManager.home(p);var session=DreamManager.session(p);
                            String info=String.format(java.util.Locale.ROOT,
                                    "Dream active: %s | Stage: %s | Copy: %d%%\nProbable home: %s | Confidence: %.2f\nJumps/min: %.2f | Containers: %d | Underground: %.1f%% | Preferred Y: %.1f",
                                    DreamManager.active(p),session==null?"NONE":session.state,session==null?0:session.region.progress(),
                                    home==null?"unknown":home.dimension()+" "+home.center().getMiddleBlockPosition(home.preferredY()).toShortString(),
                                    home==null?0:home.confidence(),profile.jumpsPerMinute(),profile.containers,profile.undergroundRatio()*100,profile.preferredY());
                            context.getSource().sendSuccess(()->Component.literal(info),false);
                            BaseSemanticMap semantics=session!=null?session.semantics:new BaseSemanticMap(profile,home);
                            String detail="Main bed: "+semantics.mainBed+" | Main container: "+semantics.mainContainer
                                    +" | Main workstation: "+semantics.mainWorkstation+"\nSemantic zones: "+semantics.zones
                                    +"\nHotspots: "+semantics.hotspots.size()+" | Instability: "+(session==null?0:session.director.instability());
                            if(session!=null&&session.actor!=null&&session.region.target.getEntity(session.actor) instanceof DreamPlayerEntity actor)
                                detail+=" | DreamPlayer target: "+actor.targetChest()+" | Actor state: "+actor.state();
                            String report=detail;context.getSource().sendSuccess(()->Component.literal(report),false);
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                }catch(Exception error){context.getSource().sendFailure(Component.literal(error.getMessage()==null?error.toString():error.getMessage()));return 0;}
            })));
        }
        event.getDispatcher().register(Commands.literal("wayaround").requires(source->source.hasPermission(2)).then(dream));
    }
}
