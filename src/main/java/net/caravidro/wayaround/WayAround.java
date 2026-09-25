package net.caravidro.wayaround;

import com.mojang.logging.LogUtils;

import net.caravidro.wayaround.blue.BlueManager;
import net.caravidro.wayaround.blue.ImaginaryBetaManager;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.industrial.IndustrialContent;
import net.caravidro.wayaround.infinity.InfinityManager;
import net.caravidro.wayaround.media.MediaContent;
import net.caravidro.wayaround.domain.DomainCommands;
import net.caravidro.wayaround.domain.DomainManager;
import net.caravidro.wayaround.domain.VoidDomainManager;
import net.caravidro.wayaround.network.WayAroundNetwork;
import net.caravidro.wayaround.particle.WayAroundParticles;
import net.caravidro.wayaround.sounds.WayAroundSounds;
import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.caravidro.wayaround.worldgen.WayAroundFeatures;
import net.caravidro.wayaround.worldgen.WorldgenRegistry;
import net.caravidro.wayaround.worldgen.weather.AntarcticBlizzard;
import net.caravidro.wayaround.worldgen.weather.AntarcticTorches;
import net.caravidro.wayaround.worldgen.weather.BlizzardChunkTracker;
import net.caravidro.wayaround.worldgen.weather.BlizzardManager;
import net.caravidro.wayaround.worldgen.weather.avalanche.AntarcticAvalanche;
import net.caravidro.wayaround.worldgen.weather.avalanche.AvalancheCommand;
import net.caravidro.wayaround.worldgen.weather.avalanche.AvalancheManager;
import net.caravidro.wayaround.worldgen.weather.command.BlizzardCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(WayAround.MODID)
public class WayAround {
    public static final String MODID = "wayaround";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WayAround(IEventBus modEventBus) {
        LOGGER.info("Way Around V1 iniciando!");
        // Registros do mod.
        WayAroundContent.register(modEventBus);
        IndustrialContent.register(modEventBus);
        MediaContent.register(modEventBus);
        net.caravidro.wayaround.dream.DreamContent.register(modEventBus);
        net.neoforged.fml.ModLoadingContext.get().getActiveContainer().registerConfig(
                net.neoforged.fml.config.ModConfig.Type.SERVER, net.caravidro.wayaround.dream.DreamConfig.SPEC);
        WorldgenRegistry.register(modEventBus);
        WayAroundFeatures.register(modEventBus);
        WayAroundSounds.register(modEventBus);
        WayAroundParticles.register(modEventBus);
        net.caravidro.wayaround.effect.WayAroundEffects.register(modEventBus);
        AntarcticTorches.BLOCKS.register(modEventBus);
        modEventBus.addListener(WayAroundNetwork::register);

        // Ciclo de vida do mundo e dos chunks.
        NeoForge.EVENT_BUS.addListener(WayAroundBiomes::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(BlizzardChunkTracker::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(BlizzardChunkTracker::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);

        // Atualizacao das mecanicas do servidor.
        NeoForge.EVENT_BUS.addListener(AntarcticAvalanche::onServerTick);
        NeoForge.EVENT_BUS.addListener(AntarcticBlizzard::onServerTick);
        NeoForge.EVENT_BUS.addListener(DomainManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(VoidDomainManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(BlueManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(InfinityManager::onServerTick);

        // Comandos.
        NeoForge.EVENT_BUS.addListener(AvalancheCommand::register);
        NeoForge.EVENT_BUS.addListener(BlizzardCommand::register);
        NeoForge.EVENT_BUS.addListener(DomainCommands::register);

        // Assembly objects: procedural interaction against moving machine parts.
        NeoForge.EVENT_BUS.addListener(net.caravidro.wayaround.industrial.assembly.AssemblyInteractionEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(net.caravidro.wayaround.industrial.assembly.AssemblyInteractionEvents::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(net.caravidro.wayaround.industrial.assembly.AssemblyInteractionEvents::onRightClickEmpty);
    }

    private void onServerStopped(ServerStoppedEvent event) {
        AvalancheManager.clearAll();
        WayAroundBiomes.clear();
        BlizzardManager.clearAll();
        BlizzardChunkTracker.clear();
        DomainManager.clearAll();
        VoidDomainManager.clearAll();
        BlueManager.clearAll();
        ImaginaryBetaManager.clearAll();
        InfinityManager.clearAll();
        LOGGER.info("Caches do WayAround limpos.");
    }
}
