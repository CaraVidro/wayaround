package net.caravidro.wayaround.ecology;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared biome-aware palette for Way Around's terrestrial vegetation.
 *
 * Worldgen and long-term ecological succession use the same choices so a biome
 * does not generate with one visual language and then slowly mutate into a
 * completely different one after rain/time-aging.
 */
public final class VegetationPalette {

    private VegetationPalette() {
    }

    public static int groundAttempts(
            String biome
    ) {
        if (biome.contains(
                "jungle"
        )) {
            return 34;
        }

        if (biome.contains(
                "mangrove"
        )
                || biome.contains(
                "swamp"
        )) {
            return 30;
        }

        if (biome.contains(
                "dark_forest"
        )
                || biome.contains(
                "old_growth"
        )) {
            return 28;
        }

        if (biome.contains(
                "cherry"
        )) {
            return 20;
        }

        if (biome.contains(
                "forest"
        )
                || biome.contains(
                "taiga"
        )
                || biome.contains(
                "grove"
        )) {
            return 22;
        }

        if (biome.contains(
                "river"
        )) {
            return 24;
        }

        if (biome.contains(
                "meadow"
        )) {
            return 20;
        }

        if (biome.contains(
                "plains"
        )) {
            return 15;
        }

        if (biome.contains(
                "savanna"
        )) {
            return 13;
        }

        return 0;
    }

    public static BlockState pick(
            String biome,
            boolean wet,
            boolean nearWater,
            RandomSource random
    ) {
        float pick =
                random.nextFloat();

        if (nearWater) {
            if (pick < 0.26F) {
                return EcologyContent.WILD_REEDS.get()
                        .defaultBlockState();
            }

            if (pick < 0.43F) {
                return EcologyContent.RIVER_SPRIG.get()
                        .defaultBlockState();
            }

            if (pick < 0.58F) {
                return EcologyContent.CREEK_CLOVER.get()
                        .defaultBlockState();
            }

            if (pick < 0.74F) {
                return EcologyContent.DAMP_FERN.get()
                        .defaultBlockState();
            }

            if (pick < 0.87F) {
                return EcologyContent.BRACKEN_FERN.get()
                        .defaultBlockState();
            }

            return EcologyContent.MUSHROOM_PATCH.get()
                    .defaultBlockState();
        }

        if (biome.contains(
                "jungle"
        )) {
            if (pick < 0.30F) {
                return EcologyContent.BRACKEN_FERN.get()
                        .defaultBlockState();
            }

            if (pick < 0.54F) {
                return EcologyContent.FOREST_SHRUB.get()
                        .defaultBlockState();
            }

            if (pick < 0.70F) {
                return EcologyContent.DAMP_FERN.get()
                        .defaultBlockState();
            }

            if (pick < 0.83F) {
                return EcologyContent.SHADE_NETTLE.get()
                        .defaultBlockState();
            }

            if (pick < 0.93F) {
                return EcologyContent.MUSHROOM_PATCH.get()
                        .defaultBlockState();
            }

            return Blocks.FERN
                    .defaultBlockState();
        }

        if (biome.contains(
                "mangrove"
        )
                || biome.contains(
                "swamp"
        )) {
            if (pick < 0.24F) {
                return EcologyContent.WILD_REEDS.get()
                        .defaultBlockState();
            }

            if (pick < 0.46F) {
                return EcologyContent.DAMP_FERN.get()
                        .defaultBlockState();
            }

            if (pick < 0.66F) {
                return EcologyContent.FOREST_SHRUB.get()
                        .defaultBlockState();
            }

            if (pick < 0.82F) {
                return EcologyContent.CREEK_CLOVER.get()
                        .defaultBlockState();
            }

            return EcologyContent.MUSHROOM_PATCH.get()
                    .defaultBlockState();
        }

        if (biome.contains(
                "cherry"
        )) {
            if (pick < 0.20F) {
                return EcologyContent.FLOWERING_SHRUB.get()
                        .defaultBlockState();
            }

            if (pick < 0.42F) {
                return EcologyContent.MEADOW_SEDGE.get()
                        .defaultBlockState();
            }

            if (pick < 0.58F) {
                return EcologyContent.CREEK_CLOVER.get()
                        .defaultBlockState();
            }

            if (pick < 0.72F) {
                return Blocks.PINK_PETALS
                        .defaultBlockState();
            }

            if (pick < 0.84F) {
                return Blocks.AZURE_BLUET
                        .defaultBlockState();
            }

            if (pick < 0.93F) {
                return EcologyContent.WOODLAND_SORREL.get()
                        .defaultBlockState();
            }

            return Blocks.SHORT_GRASS
                    .defaultBlockState();
        }

        if (biome.contains(
                "dark_forest"
        )
                || biome.contains(
                "old_growth"
        )
                || biome.contains(
                "taiga"
        )
                || biome.contains(
                "forest"
        )
                || biome.contains(
                "grove"
        )) {

            if (pick < 0.24F) {
                return EcologyContent.BRACKEN_FERN.get()
                        .defaultBlockState();
            }

            if (pick < 0.43F) {
                return EcologyContent.FOREST_SHRUB.get()
                        .defaultBlockState();
            }

            if (pick < 0.57F) {
                return EcologyContent.MUSHROOM_PATCH.get()
                        .defaultBlockState();
            }

            if (pick < 0.72F) {
                return EcologyContent.WOODLAND_SORREL.get()
                        .defaultBlockState();
            }

            if (pick < 0.84F) {
                return EcologyContent.SHADE_NETTLE.get()
                        .defaultBlockState();
            }

            if (wet
                    && pick < 0.94F) {
                return EcologyContent.DAMP_FERN.get()
                        .defaultBlockState();
            }

            return Blocks.FERN
                    .defaultBlockState();
        }

        if (biome.contains(
                "meadow"
        )) {
            if (pick < 0.16F) {
                return EcologyContent.FLOWERING_SHRUB.get()
                        .defaultBlockState();
            }

            if (pick < 0.39F) {
                return EcologyContent.MEADOW_SEDGE.get()
                        .defaultBlockState();
            }

            if (pick < 0.56F) {
                return EcologyContent.CREEK_CLOVER.get()
                        .defaultBlockState();
            }

            if (pick < 0.69F) {
                return Blocks.AZURE_BLUET
                        .defaultBlockState();
            }

            if (pick < 0.81F) {
                return Blocks.OXEYE_DAISY
                        .defaultBlockState();
            }

            if (pick < 0.90F) {
                return Blocks.DANDELION
                        .defaultBlockState();
            }

            return Blocks.SHORT_GRASS
                    .defaultBlockState();
        }

        if (biome.contains(
                "savanna"
        )) {
            if (pick < 0.36F) {
                return EcologyContent.DRY_SHRUB.get()
                        .defaultBlockState();
            }

            if (pick < 0.58F) {
                return EcologyContent.MEADOW_SEDGE.get()
                        .defaultBlockState();
            }

            if (pick < 0.76F) {
                return Blocks.SHORT_GRASS
                        .defaultBlockState();
            }

            return Blocks.DEAD_BUSH
                    .defaultBlockState();
        }

        if (biome.contains(
                "plains"
        )) {
            if (pick < 0.12F) {
                return EcologyContent.FLOWERING_SHRUB.get()
                        .defaultBlockState();
            }

            if (pick < 0.34F) {
                return EcologyContent.MEADOW_SEDGE.get()
                        .defaultBlockState();
            }

            if (pick < 0.49F) {
                return Blocks.DANDELION
                        .defaultBlockState();
            }

            if (pick < 0.61F) {
                return Blocks.POPPY
                        .defaultBlockState();
            }

            return Blocks.SHORT_GRASS
                    .defaultBlockState();
        }

        if (wet) {
            if (pick < 0.40F) {
                return EcologyContent.DAMP_FERN.get()
                        .defaultBlockState();
            }

            if (pick < 0.68F) {
                return EcologyContent.CREEK_CLOVER.get()
                        .defaultBlockState();
            }
        }

        return random.nextBoolean()
                ? Blocks.FERN.defaultBlockState()
                : Blocks.SHORT_GRASS.defaultBlockState();
    }

    public static boolean clumps(
            BlockState state
    ) {
        return state.is(
                EcologyContent.BRACKEN_FERN.get()
        )
                || state.is(
                EcologyContent.WILD_REEDS.get()
        )
                || state.is(
                EcologyContent.MUSHROOM_PATCH.get()
        )
                || state.is(
                EcologyContent.CREEK_CLOVER.get()
        );
    }
}
