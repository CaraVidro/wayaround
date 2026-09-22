package net.caravidro.wayaround.dream;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class DreamContent {
    public static final ResourceKey<Level> DIMENSION=ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(WayAround.MODID,"dream"));
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,WayAround.MODID);
    public static final DeferredHolder<EntityType<?>,EntityType<DreamPlayerEntity>> PLAYER=ENTITIES.register("dream_player",
            ()->EntityType.Builder.of(DreamPlayerEntity::new,MobCategory.MISC).sized(.6F,1.8F)
                    .clientTrackingRange(10).updateInterval(2).build("wayaround:dream_player"));
    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        // Vanilla GameTestServer discards datapack dimensions when it bakes the flat preset.
        // This pack is registered only in that test process, never in ordinary worlds.
        bus.addListener((net.neoforged.neoforge.event.AddPackFindersEvent event)->{
            if(Boolean.getBoolean("neoforge.gameTestServer"))event.addPackFinders(
                    ResourceLocation.fromNamespaceAndPath(WayAround.MODID,"dream_tests"),
                    net.minecraft.server.packs.PackType.SERVER_DATA,net.minecraft.network.chat.Component.literal("Dream test dimensions"),
                    net.minecraft.server.packs.repository.PackSource.BUILT_IN,true,net.minecraft.server.packs.repository.Pack.Position.TOP);
        });
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event)->
                event.put(PLAYER.get(),DreamPlayerEntity.attributes().build()));
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event)->
                event.registrar("1").playToClient(DreamPayload.TYPE,DreamPayload.CODEC,
                        (payload,context)->context.enqueueWork(()->net.caravidro.wayaround.dream.client.DreamClientState.receive(payload))));
    }
}
