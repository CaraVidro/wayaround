package net.caravidro.wayaround.ecology.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.ecology.EcologyContent;
import net.caravidro.wayaround.ecology.RegionalFishSpecies;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class EcologyClientModEvents {

    private EcologyClientModEvents() {
    }

    @SubscribeEvent
    public static void renderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        for (var species : RegionalFishSpecies.values()) {
            event.registerEntityRenderer(RegionalFishSpecies.TYPES.get(species).get(), context ->
                    new AguaWorldSpeciesRenderer<>(context, AguaWorldSpeciesRenderer.Profile.valueOf(species.name())));
        }
        event.registerEntityRenderer(
                EcologyContent.DEEP_SEA_CAPSULE.get(),
                DeepSeaCapsuleRenderer::new
        );

        event.registerEntityRenderer(
                EcologyContent.SUNFISH.get(),
                SunfishRenderer::new
        );

        event.registerEntityRenderer(
                EcologyContent.SARDINE.get(),
                SardineRenderer::new
        );

        event.registerEntityRenderer(
                EcologyContent.FISH_CARCASS.get(),
                FishCarcassRenderer::new
        );

        event.registerEntityRenderer(
                EcologyContent.REEF_SHARK.get(),
                ReefSharkRenderer::new
        );

        event.registerEntityRenderer(
                EcologyContent.MANTA_RAY.get(),
                context -> new AguaWorldSpeciesRenderer<>(
                        context,
                        AguaWorldSpeciesRenderer.Profile.MANTA_RAY
                )
        );

        event.registerEntityRenderer(
                EcologyContent.BARRACUDA.get(),
                context -> new AguaWorldSpeciesRenderer<>(
                        context,
                        AguaWorldSpeciesRenderer.Profile.BARRACUDA
                )
        );

        event.registerEntityRenderer(
                EcologyContent.SEAHORSE.get(),
                context -> new AguaWorldSpeciesRenderer<>(
                        context,
                        AguaWorldSpeciesRenderer.Profile.SEAHORSE
                )
        );

        event.registerEntityRenderer(
                EcologyContent.JELLYFISH.get(),
                context -> new AguaWorldSpeciesRenderer<>(
                        context,
                        AguaWorldSpeciesRenderer.Profile.JELLYFISH
                )
        );

        event.registerEntityRenderer(
                EcologyContent.OARFISH.get(),
                context -> new AguaWorldSpeciesRenderer<>(
                        context,
                        AguaWorldSpeciesRenderer.Profile.OARFISH
                )
        );

        event.registerEntityRenderer(
                EcologyContent.CLOWNFISH.get(),
                context -> new AguaWorldSpeciesRenderer<>(
                        context,
                        AguaWorldSpeciesRenderer.Profile.CLOWNFISH
                )
        );

        event.registerEntityRenderer(
                EcologyContent.FLYING_FISH.get(),
                context -> new AguaWorldSpeciesRenderer<>(
                        context,
                        AguaWorldSpeciesRenderer.Profile.FLYING_FISH
                )
        );

        event.registerEntityRenderer(
                EcologyContent.LANTERNFISH.get(),
                context -> new AguaWorldSpeciesRenderer<>(
                        context,
                        AguaWorldSpeciesRenderer.Profile.LANTERNFISH
                )
        );

        event.registerEntityRenderer(
                EcologyContent.CLEINTON.get(),
                CleintonRenderer::new
        );

        event.registerEntityRenderer(
                EcologyContent.MORAY_EEL.get(),
                context -> new AguaWorldSpeciesRenderer<>(
                        context,
                        AguaWorldSpeciesRenderer.Profile.MORAY_EEL
                )
        );

        event.registerEntityRenderer(
                EcologyContent.WHALE.get(),
                context -> new AguaWorldSpeciesRenderer<>(
                        context,
                        AguaWorldSpeciesRenderer.Profile.WHALE
                )
        );

        event.registerEntityRenderer(
                EcologyContent.SPERM_WHALE.get(),
                context -> new AguaWorldSpeciesRenderer<>(
                        context,
                        AguaWorldSpeciesRenderer.Profile.SPERM_WHALE
                )
        );

        event.registerEntityRenderer(
                EcologyContent.WHALE_CARCASS.get(),
                context -> new WhaleCarcassRenderer(
                        context,
                        false
                )
        );

        event.registerEntityRenderer(
                EcologyContent.SPERM_WHALE_CARCASS.get(),
                context -> new WhaleCarcassRenderer(
                        context,
                        true
                )
        );

        event.registerEntityRenderer(
                EcologyContent.SEAGULL.get(),
                SeagullRenderer::new
        );

        event.registerEntityRenderer(
                EcologyContent.CRAB.get(),
                CrabRenderer::new
        );
    }

    @SubscribeEvent
    public static void blockColors(
            RegisterColorHandlersEvent.Block event
    ) {
        event.register(
                EcologyClientModEvents::groundTint,
                EcologyContent.RIVER_SPRIG.get(),
                EcologyContent.WOODLAND_SORREL.get(),
                EcologyContent.DAMP_FERN.get(),
                EcologyContent.MEADOW_SEDGE.get(),
                EcologyContent.CREEK_CLOVER.get(),
                EcologyContent.SHADE_NETTLE.get(),
                Blocks.OAK_LEAVES,
                Blocks.SPRUCE_LEAVES,
                Blocks.BIRCH_LEAVES,
                Blocks.JUNGLE_LEAVES,
                Blocks.ACACIA_LEAVES,
                Blocks.DARK_OAK_LEAVES,
                Blocks.MANGROVE_LEAVES,
                Blocks.CHERRY_LEAVES,
                Blocks.AZALEA_LEAVES,
                Blocks.FLOWERING_AZALEA_LEAVES
        );
    }

    private static int groundTint(
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos,
            int tintIndex
    ) {
        if (level == null
                || pos == null) {
            return 0x79A84C;
        }

        BlockPos.MutableBlockPos probe =
                pos.mutable();

        for (int depth = 0;
             depth < 20;
             depth++) {
            probe.move(
                    Direction.DOWN
            );

            BlockState below =
                    level.getBlockState(
                            probe
                    );

            if (below.isAir()
                    || below.is(
                    BlockTags.LEAVES
            )) {
                continue;
            }

            if (below.is(Blocks.MOSS_BLOCK)
                    || below.is(Blocks.MOSS_CARPET)) {
                return 0x5D8E42;
            }

            if (below.is(Blocks.SAND)
                    || below.is(Blocks.SANDSTONE)) {
                return 0xA8A66A;
            }

            if (below.is(Blocks.RED_SAND)
                    || below.is(Blocks.RED_SANDSTONE)) {
                return 0x9B8450;
            }

            if (below.is(Blocks.MUD)
                    || below.is(Blocks.MUDDY_MANGROVE_ROOTS)) {
                return 0x587347;
            }

            if (below.is(Blocks.SNOW_BLOCK)
                    || below.is(Blocks.SNOW)
                    || below.is(Blocks.POWDER_SNOW)) {
                return 0xA9C7A0;
            }

            return BiomeColors.getAverageGrassColor(
                    level,
                    probe
            );
        }

        return BiomeColors.getAverageGrassColor(
                level,
                pos
        );
    }
}
