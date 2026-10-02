package net.caravidro.wayaround.industrial.mining;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public enum ComplexOreKind {
    IRON("iron", Items.RAW_IRON, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, 1.00F),
    GOLD("gold", Items.RAW_GOLD, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, 1.35F),
    COPPER("copper", Items.RAW_COPPER, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, 0.88F),
    COAL("coal", Items.COAL, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, 0.72F);

    private final String id;
    private final Item drop;
    private final Block ore;
    private final Block deepOre;
    private final float miningLoad;

    ComplexOreKind(String id, Item drop, Block ore, Block deepOre, float miningLoad) {
        this.id = id;
        this.drop = drop;
        this.ore = ore;
        this.deepOre = deepOre;
        this.miningLoad = miningLoad;
    }

    public String id() { return id; }
    public Item drop() { return drop; }
    public float miningLoad() { return miningLoad; }

    public BlockState oreState(int y) {
        return (y < 8 ? deepOre : ore).defaultBlockState();
    }

    public BlockState exhaustedState(int y) {
        return (y < 8 ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState();
    }
}
