package net.caravidro.wayaround.physical;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Compatibility bridge from ordinary Minecraft blocks into WayAround's
 * universal matter vocabulary.
 *
 * <p>This resolver is intentionally conservative. Unknown blocks remain valid
 * matter; they simply use UNKNOWN_SOLID until a more specific material source
 * (tags, data pack or manufactured profile) is registered later.</p>
 */
public final class BlockMatterResolver {

    public record ResolvedMatter(
            MaterialDefinition material,
            MatterPhase phase,
            float exposedFlammability
    ) {
    }

    private BlockMatterResolver() {
    }

    public static ResolvedMatter resolve(
            BlockState state
    ) {
        if (state.isAir()) {
            return resolved(
                    PhysicalMaterials.AIR,
                    MatterPhase.GAS,
                    0.0F
            );
        }

        if (state.is(Blocks.WATER)) {
            return resolved(
                    PhysicalMaterials.WATER,
                    MatterPhase.LIQUID,
                    0.0F
            );
        }

        if (state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.FROSTED_ICE)) {
            return resolved(
                    PhysicalMaterials.WATER,
                    MatterPhase.SOLID,
                    0.0F
            );
        }

        if (state.is(Blocks.LAVA)) {
            return resolved(
                    PhysicalMaterials.MOLTEN_ROCK,
                    MatterPhase.LIQUID,
                    0.0F
            );
        }

        if (state.is(BlockTags.LOGS)
                || state.is(BlockTags.PLANKS)) {
            return resolved(
                    PhysicalMaterials.WOOD,
                    MatterPhase.SOLID,
                    PhysicalMaterials.WOOD.engineering()
                            .flammability()
            );
        }

        if (state.is(BlockTags.LEAVES)
                || state.is(BlockTags.WOOL)) {
            return resolved(
                    PhysicalMaterials.BIOMASS,
                    MatterPhase.SOLID,
                    PhysicalMaterials.BIOMASS.engineering()
                            .flammability()
            );
        }

        if (state.is(Blocks.GRASS_BLOCK)) {
            /*
             * Bulk grass block is soil, but its exposed organic top can carry
             * an ember. Keeping surface flammability separate avoids claiming
             * that an entire cubic metre of soil is "wood-like fuel".
             */
            return resolved(
                    PhysicalMaterials.ORGANIC_SOIL,
                    MatterPhase.SOLID,
                    0.62F
            );
        }

        if (state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)) {
            return resolved(
                    PhysicalMaterials.ORGANIC_SOIL,
                    MatterPhase.SOLID,
                    0.02F
            );
        }

        if (state.is(Blocks.IRON_BLOCK)
                || state.is(Blocks.IRON_BARS)
                || state.is(Blocks.ANVIL)
                || state.is(Blocks.CHIPPED_ANVIL)
                || state.is(Blocks.DAMAGED_ANVIL)) {
            return resolved(
                    PhysicalMaterials.IRON,
                    MatterPhase.SOLID,
                    0.0F
            );
        }

        if (state.is(Blocks.COPPER_BLOCK)
                || state.is(Blocks.EXPOSED_COPPER)
                || state.is(Blocks.WEATHERED_COPPER)
                || state.is(Blocks.OXIDIZED_COPPER)
                || state.is(Blocks.WAXED_COPPER_BLOCK)
                || state.is(Blocks.WAXED_EXPOSED_COPPER)
                || state.is(Blocks.WAXED_WEATHERED_COPPER)
                || state.is(Blocks.WAXED_OXIDIZED_COPPER)) {
            return resolved(
                    PhysicalMaterials.COPPER,
                    MatterPhase.SOLID,
                    0.0F
            );
        }

        if (state.is(Blocks.DIAMOND_BLOCK)) {
            return resolved(
                    PhysicalMaterials.DIAMOND,
                    MatterPhase.SOLID,
                    0.0F
            );
        }

        if (state.is(Blocks.GLASS)
                || state.is(Blocks.GLASS_PANE)) {
            return resolved(
                    PhysicalMaterials.GLASS,
                    MatterPhase.SOLID,
                    0.0F
            );
        }

        if (state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)) {
            return resolved(
                    PhysicalMaterials.STONE,
                    MatterPhase.SOLID,
                    0.0F
            );
        }

        return resolved(
                PhysicalMaterials.UNKNOWN_SOLID,
                MatterPhase.SOLID,
                0.0F
        );
    }

    public static float exposedFlammability(
            BlockState state
    ) {
        return resolve(state)
                .exposedFlammability();
    }

    private static ResolvedMatter resolved(
            MaterialDefinition material,
            MatterPhase phase,
            float exposedFlammability
    ) {
        return new ResolvedMatter(
                material,
                phase,
                Math.clamp(
                        exposedFlammability,
                        0.0F,
                        1.0F
                )
        );
    }
}
