package net.caravidro.wayaround.worldgen.feature;

import com.mojang.serialization.Codec;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Abyssal decoration only; basin terrain is shaped before any flora is generated. */
public final class DeepOceanTrenchFeature extends Feature<NoneFeatureConfiguration> {
    public DeepOceanTrenchFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_VEGETATION)) {
            return false;
        }

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        int baseX = origin.getX() & ~15;
        int baseZ = origin.getZ() & ~15;
        int seaLevel = level.getSeaLevel();
        double chunkInterior = 0.0;
        // Terrain is already shaped at SURFACE, before neighboring chunks can
        // decorate kelp here. This feature only adds supported remnants.
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx, z = baseZ + dz;
                if (!isDeepOcean(level, x, z, seaLevel - 8)) continue;
                int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                chunkInterior = Math.max(chunkInterior,
                        Mth.clamp((seaLevel - floor - 24.0) / 80.0, 0.0, 1.0));
            }
        }

        /*
         * Wrecks are intentionally common in the abyss: roughly one proper
         * ruined hull per three strongly-deepened chunks, plus smaller debris.
         * They are procedural remnants rather than full vanilla structures,
         * so they do not force neighbouring chunks to load.
         */
        if (chunkInterior > 0.52) {
            if (random.nextFloat() < 0.34F) {
                placeWreck(level, baseX, baseZ, seaLevel, random);
            } else if (random.nextFloat() < 0.30F) {
                placeDebris(level, baseX, baseZ, seaLevel, random);
            }
        }

        if(chunkInterior>.6 && random.nextInt(12)==0) {
            int x=baseX+3+random.nextInt(10),z=baseZ+3+random.nextInt(10);
            int y=level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z);
            BlockPos p=new BlockPos(x,y,z);
            if(level.getBlockState(p).is(Blocks.WATER))level.setBlock(p,net.caravidro.wayaround.ecology.EcologyContent.ABYSSAL_SKELETON_SKULL.get().defaultBlockState().setValue(net.minecraft.world.level.block.SkullBlock.ROTATION,random.nextInt(16)),2);
        }
        if(chunkInterior>.6 && random.nextInt(8)==0) {
            int x=baseX+4+random.nextInt(8),z=baseZ+4+random.nextInt(8),y=level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z);
            var fish=net.caravidro.wayaround.ecology.EcologyContent.FISH_CARCASS.get().create(level.getLevel());
            if(fish!=null) { fish.initialize(net.caravidro.wayaround.ecology.FishProcessingProfile.SARDINE,1.2F,false);fish.setAbyssalSettled(true);fish.setPos(x+.5,y+.15,z+.5);level.addFreshEntity(fish); }
        }
        return chunkInterior > 0.0;
    }

    private static boolean isDeepOcean(
            WorldGenLevel level,
            int x,
            int z,
            int y
    ) {
        return level.getBiome(new BlockPos(x, y, z))
                .unwrapKey()
                .map(key -> {
                    String path = key.location().getPath();
                    return path.equals("deep_ocean")
                            || path.equals("deep_cold_ocean")
                            || path.equals("deep_frozen_ocean")
                            || path.equals("deep_lukewarm_ocean");
                })
                .orElse(false);
    }

    private static int findFloor(
            WorldGenLevel level,
            int x,
            int z,
            int seaLevel
    ) {
        BlockPos.MutableBlockPos cursor =
                new BlockPos.MutableBlockPos(x, seaLevel - 1, z);

        for (int y = seaLevel - 1;
             y > level.getMinBuildHeight() + 4;
             y--) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);

            if (state.blocksMotion() && !state.is(Blocks.WATER)) {
                return y;
            }
        }

        return level.getMinBuildHeight() + 5;
    }

    private static void placeWreck(
            WorldGenLevel level,
            int baseX,
            int baseZ,
            int seaLevel,
            RandomSource random
    ) {
        int cx = baseX + 5 + random.nextInt(6);
        int cz = baseZ + 5 + random.nextInt(6);
        int floor = findFloor(level, cx, cz, seaLevel);
        boolean alongX = random.nextBoolean();

        BlockState hull = random.nextBoolean()
                ? Blocks.DARK_OAK_PLANKS.defaultBlockState()
                : Blocks.SPRUCE_PLANKS.defaultBlockState();

        for (int along = -3; along <= 3; along++) {
            for (int across = -2; across <= 2; across++) {
                if (Math.abs(across) == 2 && Math.abs(along) > 2) continue;
                if (random.nextFloat() < 0.23F) continue;

                int x = cx + (alongX ? along : across);
                int z = cz + (alongX ? across : along);
                int y = floor + 1 + (Math.abs(across) == 2 ? 1 : 0);

                setIfWater(level, new BlockPos(x, y, z), hull);
            }
        }

        BlockPos mast = new BlockPos(cx, floor + 2, cz);
        for (int y = 0; y < 5; y++) {
            if (y == 3 && random.nextBoolean()) continue;
            setIfWater(
                    level,
                    mast.above(y),
                    Blocks.SPRUCE_LOG.defaultBlockState()
            );
        }

        for (int side : new int[]{-2, 2}) {
            BlockPos rail = alongX
                    ? new BlockPos(cx, floor + 3, cz + side)
                    : new BlockPos(cx + side, floor + 3, cz);
            setIfWater(level, rail, Blocks.OAK_FENCE.defaultBlockState());
        }

        if (random.nextBoolean()) {
            BlockPos chain = alongX
                    ? new BlockPos(cx + 2, floor + 2, cz)
                    : new BlockPos(cx, floor + 2, cz + 2);
            setIfWater(level, chain, Blocks.CHAIN.defaultBlockState());
            setIfWater(level, chain.below(), Blocks.CHAIN.defaultBlockState());
        }

        setIfWater(
                level,
                new BlockPos(cx + (alongX ? -3 : 0), floor + 1, cz + (alongX ? 0 : -3)),
                Blocks.MOSSY_COBBLESTONE.defaultBlockState()
        );
    }

    private static void placeDebris(
            WorldGenLevel level,
            int baseX,
            int baseZ,
            int seaLevel,
            RandomSource random
    ) {
        int cx = baseX + 4 + random.nextInt(8);
        int cz = baseZ + 4 + random.nextInt(8);
        int floor = findFloor(level, cx, cz, seaLevel);

        for (int i = 0; i < 7 + random.nextInt(8); i++) {
            int x = cx + random.nextInt(9) - 4;
            int z = cz + random.nextInt(9) - 4;
            BlockState state = switch (random.nextInt(4)) {
                case 0 -> Blocks.SPRUCE_PLANKS.defaultBlockState();
                case 1 -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
                case 2 -> Blocks.CHAIN.defaultBlockState();
                default -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            };
            setIfWater(level, new BlockPos(x, floor + 1, z), state);
        }
    }

    public static void setIfWater(
            net.minecraft.world.level.LevelAccessor level,
            BlockPos pos,
            BlockState state
    ) {
        if (pos.getY() < level.getMinBuildHeight()
                || pos.getY() >= level.getMaxBuildHeight()
                || level.getBlockEntity(pos) != null) {
            return;
        }

        BlockState existing = level.getBlockState(pos);
        if (existing.is(Blocks.WATER)) {
            level.setBlock(pos, state, 2);
        }
    }
}
