package net.caravidro.wayaround.industrial.pipework;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public enum PipeProfile {

    COPPER_TUBE(
            "copper_tube",
            PipeClass.LIQUID,
            0.095F,
            1_000,
            40,
            180.0F,
            Blocks.COPPER_BLOCK.defaultBlockState(),
            Blocks.CUT_COPPER.defaultBlockState(),
            false
    ),

    IRON_SERVICE_PIPE(
            "iron_service_pipe",
            PipeClass.LIQUID,
            0.140F,
            2_000,
            110,
            420.0F,
            Blocks.IRON_BLOCK.defaultBlockState(),
            Blocks.SMOOTH_STONE.defaultBlockState(),
            true
    ),

    STEEL_WATER_MAIN(
            "steel_water_main",
            PipeClass.LIQUID,
            0.220F,
            8_000,
            340,
            900.0F,
            Blocks.IRON_BLOCK.defaultBlockState(),
            Blocks.DEEPSLATE_BRICKS.defaultBlockState(),
            true
    ),

    BRASS_GAS_LINE(
            "brass_gas_line",
            PipeClass.GAS,
            0.085F,
            1_500,
            85,
            650.0F,
            Blocks.CUT_COPPER.defaultBlockState(),
            Blocks.GOLD_BLOCK.defaultBlockState(),
            false
    ),

    REINFORCED_GAS_PIPE(
            "reinforced_gas_pipe",
            PipeClass.GAS,
            0.155F,
            4_000,
            220,
            1_800.0F,
            Blocks.IRON_BLOCK.defaultBlockState(),
            Blocks.COPPER_BLOCK.defaultBlockState(),
            true
    ),

    INSULATED_STEAM_PIPE(
            "insulated_steam_pipe",
            PipeClass.GAS,
            0.185F,
            5_000,
            260,
            2_400.0F,
            Blocks.SMOOTH_STONE.defaultBlockState(),
            Blocks.IRON_BLOCK.defaultBlockState(),
            true
    );

    public enum PipeClass {
        LIQUID,
        GAS
    }

    private final String id;
    private final PipeClass pipeClass;
    private final float radius;
    private final int capacity;
    private final int throughputPerTick;
    private final float maxPressureKpa;
    private final BlockState bodyMaterial;
    private final BlockState bandMaterial;
    private final boolean flanged;

    PipeProfile(
            String id,
            PipeClass pipeClass,
            float radius,
            int capacity,
            int throughputPerTick,
            float maxPressureKpa,
            BlockState bodyMaterial,
            BlockState bandMaterial,
            boolean flanged
    ) {
        this.id = id;
        this.pipeClass = pipeClass;
        this.radius = radius;
        this.capacity = capacity;
        this.throughputPerTick = throughputPerTick;
        this.maxPressureKpa = maxPressureKpa;
        this.bodyMaterial = bodyMaterial;
        this.bandMaterial = bandMaterial;
        this.flanged = flanged;
    }

    public String id() {
        return id;
    }

    public PipeClass pipeClass() {
        return pipeClass;
    }

    public float radius() {
        return radius;
    }

    public int capacity() {
        return capacity;
    }

    public int throughputPerTick() {
        return throughputPerTick;
    }

    public float maxPressureKpa() {
        return maxPressureKpa;
    }

    public BlockState bodyMaterial() {
        return bodyMaterial;
    }

    public BlockState bandMaterial() {
        return bandMaterial;
    }

    public boolean flanged() {
        return flanged;
    }

    public boolean accepts(
            PipeMedium medium
    ) {
        if (medium == PipeMedium.EMPTY) {
            return true;
        }

        return switch (pipeClass) {
            case LIQUID ->
                    medium == PipeMedium.WATER;

            case GAS ->
                    medium == PipeMedium.AIR
                            || medium == PipeMedium.STEAM;
        };
    }

    public boolean hotSteamRated() {
        return this == INSULATED_STEAM_PIPE
                || this == REINFORCED_GAS_PIPE;
    }
}
