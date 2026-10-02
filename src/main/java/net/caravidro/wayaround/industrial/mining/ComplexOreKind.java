package net.caravidro.wayaround.industrial.mining;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Geological identity stays registry-light: vanilla Items/Blocks are resolved
 * only when gameplay actually asks for drops or block states.
 */
public enum ComplexOreKind {
    IRON("iron", 1.00F),
    GOLD("gold", 1.35F),
    COPPER("copper", 0.88F),
    COAL("coal", 0.72F);

    private final String id;
    private final float miningLoad;

    ComplexOreKind(String id, float miningLoad) {
        this.id = id;
        this.miningLoad = miningLoad;
    }

    public String id() { return id; }
    public float miningLoad() { return miningLoad; }

    public Item drop() {
        return switch (this) {
            case IRON -> Items.RAW_IRON;
            case GOLD -> Items.RAW_GOLD;
            case COPPER -> Items.RAW_COPPER;
            case COAL -> Items.COAL;
        };
    }

    public BlockState oreState(int y) {
        boolean deep = y < 8;

        return switch (this) {
            case IRON -> (deep ? Blocks.DEEPSLATE_IRON_ORE : Blocks.IRON_ORE).defaultBlockState();
            case GOLD -> (deep ? Blocks.DEEPSLATE_GOLD_ORE : Blocks.GOLD_ORE).defaultBlockState();
            case COPPER -> (deep ? Blocks.DEEPSLATE_COPPER_ORE : Blocks.COPPER_ORE).defaultBlockState();
            case COAL -> (deep ? Blocks.DEEPSLATE_COAL_ORE : Blocks.COAL_ORE).defaultBlockState();
        };
    }

    public BlockState exhaustedState(int y) {
        return (y < 8 ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState();
    }
}
