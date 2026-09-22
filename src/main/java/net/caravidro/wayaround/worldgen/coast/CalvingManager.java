package net.caravidro.wayaround.worldgen.coast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.resources.ResourceKey;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.minecraft.tags.FluidTags;

import net.minecraft.util.RandomSource;

import net.minecraft.world.entity.item.FallingBlockEntity;

import net.minecraft.world.level.Level;

import net.minecraft.world.level.block.Blocks;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.levelgen.Heightmap;

import net.minecraft.world.phys.Vec3;

public final class CalvingManager {

    private static final List<CalvingEvent> ACTIVE =
            new ArrayList<>();

    private static final Direction[] HORIZONTAL = {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST
    };

    private CalvingManager() {
    }

    /*
     * =========================================================
     * COMMAND ENTRY
     * =========================================================
     */

    public static boolean tryStartAt(
            ServerPlayer player
    ) {

        ServerLevel level =
                player.serverLevel();

        /*
         * Procura uma parede glacial
         * próxima do jogador.
         */
        WallHit hit =
                findWall(
                        level,
                        player.blockPosition(),
                        72
                );

        if (
                hit == null
        ) {
            return false;
        }

        /*
         * Evita dois calvings um em cima
         * do outro.
         */
        for (
                CalvingEvent event
                :
                ACTIVE
        ) {

            if (
                    event.dimension.equals(
                            level.dimension()
                    )

                    &&

                    event.center.distanceToSqr(
                            Vec3.atCenterOf(
                                    hit.base
                            )
                    )
                    <
                    48.0 * 48.0
            ) {

                return false;
            }
        }

        CalvingEvent event =
                CalvingEvent.create(
                        level,
                        hit
                );

        if (
                event == null
        ) {
            return false;
        }

        ACTIVE.add(
                event
        );

        return true;
    }

    /*
     * =========================================================
     * SERVER TICK
     * =========================================================
     */

    public static void tick(
            MinecraftServer server
    ) {

        Iterator<CalvingEvent> iterator =
                ACTIVE.iterator();

        while (
                iterator.hasNext()
        ) {

            CalvingEvent event =
                    iterator.next();

            ServerLevel level =
                    server.getLevel(
                            event.dimension
                    );

            if (
                    level == null
            ) {

                iterator.remove();

                continue;
            }

            if (
                    event.tick(
                            level
                    )
            ) {

                iterator.remove();
            }
        }
    }

    public static void clear() {

        ACTIVE.clear();
    }

    /*
     * =========================================================
     * FIND WALL
     * =========================================================
     */

    private static WallHit findWall(
            ServerLevel level,
            BlockPos origin,
            int radius
    ) {

        int sea =
                level.getSeaLevel();

        WallHit best =
                null;

        double bestScore =
                Double.MAX_VALUE;

        /*
         * Passo 2:
         * suficiente para comando/manual,
         * sem escanear 20 mil blocos.
         */
        for (
                int dx = -radius;
                dx <= radius;
                dx += 2
        ) {

            for (
                    int dz = -radius;
                    dz <= radius;
                    dz += 2
            ) {

                if (
                        dx * dx
                        +
                        dz * dz
                        >
                        radius * radius
                ) {
                    continue;
                }

                int x =
                        origin.getX()
                        +
                        dx;

                int z =
                        origin.getZ()
                        +
                        dz;

                /*
                 * Procuramos gelo logo acima
                 * da linha da água.
                 */
                BlockPos wall =
                        new BlockPos(
                                x,
                                sea + 2,
                                z
                        );

                if (
                        !isGlacial(
                                level.getBlockState(
                                        wall
                                )
                        )
                ) {
                    continue;
                }

                int top =
                        level.getHeight(
                                Heightmap.Types.WORLD_SURFACE,

                                x,
                                z
                        )
                        -
                        1;

                int wallHeight =
                        top
                        -
                        sea;

                /*
                 * Não vamos calvar uma pilha
                 * de gelo de 3 blocos.
                 */
                if (
                        wallHeight < 12
                ) {
                    continue;
                }

                for (
                        Direction direction
                        :
                        HORIZONTAL
                ) {

                    int fx =
                            x
                            +
                            direction.getStepX();

                    int fz =
                            z
                            +
                            direction.getStepZ();

                    BlockPos waterProbe =
                            new BlockPos(
                                    fx,
                                    sea - 1,
                                    fz
                            );

                    BlockPos airProbe =
                            new BlockPos(
                                    fx,
                                    sea + 2,
                                    fz
                            );

                    /*
                     * Temos parede de gelo
                     * com oceano do outro lado?
                     */
                    boolean water =
                            level
                                    .getFluidState(
                                            waterProbe
                                    )
                                    .is(
                                            FluidTags.WATER
                                    );

                    BlockState front =
                            level.getBlockState(
                                    airProbe
                            );

                    boolean openFront =
                            front.isAir()

                            ||

                            !front
                                    .getFluidState()
                                    .isEmpty();

                    if (
                            !water
                            ||
                            !openFront
                    ) {
                        continue;
                    }

                    double distance =
                            wall.distSqr(
                                    origin
                            );

                    /*
                     * Paredes altas ganham
                     * uma pequena preferência.
                     */
                    double score =
                            distance
                            -
                            wallHeight * 8.0;

                    if (
                            score < bestScore
                    ) {

                        bestScore =
                                score;

                        best =
                                new WallHit(
                                        wall,
                                        direction,
                                        top
                                );
                    }
                }
            }
        }

        return best;
    }

    /*
     * =========================================================
     * BLOCK TYPES
     * =========================================================
     */

    private static boolean isGlacial(
            BlockState state
    ) {

        return state.is(
                Blocks.PACKED_ICE
        )

                ||

                state.is(
                        Blocks.BLUE_ICE
                )

                ||

                state.is(
                        Blocks.SNOW_BLOCK
                )

                ||

                state.is(
                        Blocks.ICE
                );
    }

    /*
     * =========================================================
     * WALL RESULT
     * =========================================================
     */

    private record WallHit(
            BlockPos base,
            Direction oceanDirection,
            int topY
    ) {
    }

    /*
     * =========================================================
     * SNAPSHOT
     * =========================================================
     */

    private record IceBlock(
            BlockPos pos,
            BlockState state
    ) {
    }

    /*
     * =========================================================
     * EVENT
     * =========================================================
     */

    private static final class CalvingEvent {

        /*
         * 1.5 s rachando
         * depois destaca.
         */
        private static final int CRACK_TICK =
                22;

        private static final int DETACH_TICK =
                32;

        private static final int SPLASH_TICK =
                49;

        private static final int ICEBERG_TICK =
                62;

        private static final int END_TICK =
                105;

        /*
         * Não transforme 2000 blocos
         * em entidades.
         *
         * Java agradece.
         */
        private static final int MAX_DEBRIS =
                90;

        private final ResourceKey<Level> dimension;

        private final BlockPos base;

        private final Direction oceanDirection;

        private final Direction tangent;

        private final List<IceBlock> blocks;

        private final RandomSource random;

        private final Vec3 center;

        private final int seaLevel;

        private final int topY;

        private final int halfWidth;

        private final int slabDepth;

        private int age =
                0;

        private int detachIndex =
                0;

        private int debrisCount =
                0;

        private boolean icebergSpawned =
                false;

        private boolean crackCarved =
                false;

        private CalvingEvent(
                ServerLevel level,
                WallHit hit,
                List<IceBlock> blocks,

                int halfWidth,
                int slabDepth
        ) {

            this.dimension =
                    level.dimension();

            this.base =
                    hit.base;

            this.oceanDirection =
                    hit.oceanDirection;

            this.topY =
                    hit.topY;

            this.blocks =
                    blocks;

            this.halfWidth =
                    halfWidth;

            this.slabDepth =
                    slabDepth;

            this.seaLevel =
                    level.getSeaLevel();

            /*
             * Tangente da parede.
             */
            if (
                    oceanDirection
                            .getAxis()
                            ==
                            Direction.Axis.X
            ) {

                tangent =
                        Direction.NORTH;
            }

            else {

                tangent =
                        Direction.EAST;
            }

            this.center =
                    new Vec3(
                            base.getX() + 0.5,

                            (
                                    seaLevel
                                    +
                                    Math.min(
                                            topY,
                                            seaLevel + 32
                                    )
                            )
                            *
                            0.5,

                            base.getZ() + 0.5
                    );

            this.random =
                    RandomSource.create(
                            level.getSeed()

                            ^

                            base.asLong()

                            ^

                            level.getGameTime()
                    );
        }

        /*
         * =====================================================
         * CREATE
         * =====================================================
         */

        private static CalvingEvent create(
                ServerLevel level,
                WallHit hit
        ) {

            RandomSource random =
                    RandomSource.create(
                            level.getSeed()

                            ^

                            hit.base.asLong()

                            ^

                            level.getGameTime()
                    );

            int halfWidth =
                    4
                    +
                    random.nextInt(
                            4
                    );

            /*
             * largura total:
             *
             * 9 até 15 blocos
             */
            int depth =
                    3
                    +
                    random.nextInt(
                            3
                    );

            Direction tangent;

            if (
                    hit.oceanDirection
                            .getAxis()
                            ==
                            Direction.Axis.X
            ) {

                tangent =
                        Direction.NORTH;
            }

            else {

                tangent =
                        Direction.EAST;
            }

            int sea =
                    level.getSeaLevel();

            int bottomY =
                    sea - 2;

            int topY =
                    Math.min(
                            hit.topY,
                            sea + 34
                    );

            List<IceBlock> blocks =
                    new ArrayList<>();

            /*
             * =================================================
             * FAZ A "FATIA"
             * =================================================
             */

            for (
                    int side = -halfWidth;
                    side <= halfWidth;
                    side++
            ) {

                double sideFactor =
                        Math.abs(
                                side
                        )
                        /
                        (double) halfWidth;

                /*
                 * Bordas mais baixas.
                 *
                 * Não queremos um cubo perfeito.
                 */
                int localTop =
                        topY
                        -
                        (int) Math.round(
                                sideFactor
                                *
                                sideFactor
                                *
                                5.0
                        );

                localTop -=
                        random.nextInt(
                                2
                        );

                int localDepth =
                        depth;

                if (
                        Math.abs(
                                side
                        )
                        ==
                        halfWidth
                ) {

                    localDepth--;
                }

                for (
                        int d = 0;
                        d < localDepth;
                        d++
                ) {

                    int x =
                            hit.base.getX()

                            +
                            tangent.getStepX()
                            *
                            side

                            -

                            hit.oceanDirection
                                    .getStepX()
                            *
                            d;

                    int z =
                            hit.base.getZ()

                            +
                            tangent.getStepZ()
                            *
                            side

                            -

                            hit.oceanDirection
                                    .getStepZ()
                            *
                            d;

                    for (
                            int y = bottomY;
                            y <= localTop;
                            y++
                    ) {

                        BlockPos pos =
                                new BlockPos(
                                        x,
                                        y,
                                        z
                                );

                        BlockState state =
                                level.getBlockState(
                                        pos
                                );

                        if (
                                isGlacial(
                                        state
                                )
                        ) {

                            blocks.add(
                                    new IceBlock(
                                            pos.immutable(),
                                            state
                                    )
                            );
                        }
                    }
                }
            }

            /*
             * Não encontrou massa suficiente.
             */
            if (
                    blocks.size()
                    <
                    80
            ) {

                return null;
            }

            return new CalvingEvent(
                    level,
                    hit,
                    blocks,
                    halfWidth,
                    depth
            );
        }

        /*
         * =====================================================
         * TICK
         * =====================================================
         */

        private boolean tick(
                ServerLevel level
        ) {

            age++;

            /*
             * ================================================
             * ESTÁGIO 1 — ESTALOS
             * ================================================
             */

            if (
                    age < DETACH_TICK
            ) {

                crackingEffects(
                        level
                );
            }

            /*
             * Uma rachadura REAL aparece.
             */
            if (
                    age == CRACK_TICK
                    &&
                    !crackCarved
            ) {

                carveSideCracks(
                        level
                );

                crackCarved =
                        true;
            }

            /*
             * ================================================
             * ESTÁGIO 2 — DESPRENDIMENTO
             * ================================================
             */

            if (
                    age >= DETACH_TICK

                    &&

                    detachIndex
                    <
                    blocks.size()
            ) {

                detachBatch(
                        level
                );
            }

            /*
             * ================================================
             * ESTÁGIO 3 — SPLASH
             * ================================================
             */

            if (
                    age == SPLASH_TICK
            ) {

                splash(
                        level
                );
            }

            /*
             * ================================================
             * ESTÁGIO 4 — ICEBERG
             * ================================================
             */

            if (
                    age == ICEBERG_TICK
                    &&
                    !icebergSpawned
            ) {

                spawnDetachedIceberg(
                        level
                );

                icebergSpawned =
                        true;
            }

            /*
             * ================================================
             * FINAL
             * ================================================
             */

            if (
                    age >= END_TICK
            ) {

                fillOceanGap(
                        level
                );

                return true;
            }

            return false;
        }

        /*
         * =====================================================
         * CRACKING
         * =====================================================
         */

        private void crackingEffects(
                ServerLevel level
        ) {

            if (
                    age % 4
                    !=
                    0
            ) {
                return;
            }

            BlockParticleOption particle =
                    new BlockParticleOption(
                            ParticleTypes.BLOCK,

                            Blocks
                                    .PACKED_ICE
                                    .defaultBlockState()
                    );

            int maxY =
                    Math.min(
                            topY,
                            seaLevel + 32
                    );

            for (
                    int i = 0;
                    i < 9;
                    i++
            ) {

                int y =
                        seaLevel + 2

                        +

                        random.nextInt(
                                Math.max(
                                        1,
                                        maxY
                                        -
                                        seaLevel
                                        -
                                        1
                                )
                        );

                double side =
                        (
                                random.nextDouble()
                                -
                                0.5
                        )
                        *
                        2.0;

                double x =
                        base.getX()
                        +
                        0.5

                        +
                        tangent.getStepX()
                        *
                        side

                        +
                        oceanDirection
                                .getStepX()
                        *
                        0.6;

                double z =
                        base.getZ()
                        +
                        0.5

                        +
                        tangent.getStepZ()
                        *
                        side

                        +
                        oceanDirection
                                .getStepZ()
                        *
                        0.6;

                level.sendParticles(
                        particle,

                        x,
                        y,
                        z,

                        3,

                        0.18,
                        0.30,
                        0.18,

                        0.03
                );
            }

            if (
                    age % 12
                    ==
                    0
            ) {

                level.playSound(
                        null,

                        base,

                        SoundEvents.GLASS_BREAK,

                        SoundSource.BLOCKS,

                        2.4F,

                        0.45F
                                +
                                random.nextFloat()
                                *
                                0.12F
                );
            }
        }

        /*
         * =====================================================
         * REAL CRACKS
         * =====================================================
         */

        private void carveSideCracks(
                ServerLevel level
        ) {

            int maxY =
                    Math.min(
                            topY,
                            seaLevel + 32
                    );

            /*
             * Uma rachadura de cada lado
             * da futura fatia.
             */
            carveOneSide(
                    level,
                    -halfWidth - 1,
                    maxY
            );

            carveOneSide(
                    level,
                    halfWidth + 1,
                    maxY
            );

            level.playSound(
                    null,

                    base,

                    SoundEvents.GLASS_BREAK,

                    SoundSource.BLOCKS,

                    4.0F,

                    0.32F
            );
        }

        private void carveOneSide(
                ServerLevel level,
                int side,
                int maxY
        ) {

            for (
                    int y = seaLevel + 1;
                    y <= maxY;
                    y++
            ) {

                /*
                 * Não completamente reta.
                 */
                int wobble =
                        (
                                y % 7 == 0
                                        ?
                                        1
                                        :
                                        0
                        );

                int x =
                        base.getX()

                        +
                        tangent.getStepX()
                        *
                        (
                                side
                                +
                                wobble
                        );

                int z =
                        base.getZ()

                        +
                        tangent.getStepZ()
                        *
                        (
                                side
                                +
                                wobble
                        );

                BlockPos pos =
                        new BlockPos(
                                x,
                                y,
                                z
                        );

                BlockState state =
                        level.getBlockState(
                                pos
                        );

                if (
                        isGlacial(
                                state
                        )
                        &&
                        random.nextFloat()
                        <
                        0.78F
                ) {

                    level.setBlock(
                            pos,

                            Blocks.AIR
                                    .defaultBlockState(),

                            2
                    );
                }
            }
        }

        /*
         * =====================================================
         * DETACH
         * =====================================================
         */

        private void detachBatch(
                ServerLevel level
        ) {

            /*
             * Divide a destruição ao longo
             * de vários ticks.
             */
            int budget =
                    190;

            while (
                    budget-- > 0

                    &&

                    detachIndex
                    <
                    blocks.size()
            ) {

                IceBlock snapshot =
                        blocks.get(
                                detachIndex++
                        );

                BlockPos pos =
                        snapshot.pos;

                BlockState current =
                        level.getBlockState(
                                pos
                        );

                if (
                        !isGlacial(
                                current
                        )
                ) {
                    continue;
                }

                /*
                 * Só alguns blocos viram
                 * entidades físicas.
                 *
                 * O restante simplesmente
                 * sai da parede.
                 */
                boolean debris =
                        debrisCount
                        <
                        MAX_DEBRIS

                        &&

                        random.nextInt(
                                17
                        )
                        ==
                        0;

                if (
                        debris
                ) {

                    FallingBlockEntity falling =
                            FallingBlockEntity.fall(
                                    level,
                                    pos,
                                    current
                            );

                    /*
                     * Nada de 90 Packed Ice
                     * dropando como item.
                     */
                    falling.dropItem =
                            false;

                    falling.disableDrop();

                    double sideways =
                            (
                                    random.nextDouble()
                                    -
                                    0.5
                            )
                            *
                            0.11;

                    double outward =
                            0.08
                            +
                            random.nextDouble()
                            *
                            0.09;

                    double vx =
                            oceanDirection
                                    .getStepX()
                            *
                            outward

                            +

                            tangent.getStepX()
                            *
                            sideways;

                    double vz =
                            oceanDirection
                                    .getStepZ()
                            *
                            outward

                            +

                            tangent.getStepZ()
                            *
                            sideways;

                    falling.setDeltaMovement(
                            vx,

                            -0.03
                                    -
                                    random.nextDouble()
                                    *
                                    0.06,

                            vz
                    );

                    debrisCount++;
                }

                else {

                    level.setBlock(
                            pos,

                            Blocks.AIR
                                    .defaultBlockState(),

                            2
                    );
                }
            }
        }

        /*
         * =====================================================
         * SPLASH
         * =====================================================
         */

        private void splash(
                ServerLevel level
        ) {

            double x =
                    base.getX()
                    +
                    0.5

                    +
                    oceanDirection
                            .getStepX()
                    *
                    4.0;

            double z =
                    base.getZ()
                    +
                    0.5

                    +
                    oceanDirection
                            .getStepZ()
                    *
                    4.0;

            double y =
                    seaLevel
                    +
                    0.5;

            level.sendParticles(
                    ParticleTypes.SPLASH,

                    x,
                    y,
                    z,

                    170,

                    halfWidth * 0.65,
                    3.5,
                    halfWidth * 0.65,

                    0.40
            );

            level.sendParticles(
                    ParticleTypes.CLOUD,

                    x,
                    y + 1.0,
                    z,

                    75,

                    halfWidth * 0.55,
                    2.0,
                    halfWidth * 0.55,

                    0.14
            );

            level.sendParticles(
                    ParticleTypes.SNOWFLAKE,

                    x,
                    y + 2.5,
                    z,

                    120,

                    halfWidth * 0.8,
                    5.0,
                    halfWidth * 0.8,

                    0.18
            );

            level.playSound(
                    null,

                    BlockPos.containing(
                            x,
                            y,
                            z
                    ),

                    SoundEvents.GENERIC_EXPLODE.value(),

                    SoundSource.BLOCKS,

                    4.5F,

                    0.62F
            );
        }

        /*
         * =====================================================
         * DETACHED ICEBERG
         * =====================================================
         */

        private void spawnDetachedIceberg(
                ServerLevel level
        ) {

            /*
             * Alguns blocos além da parede.
             */
            int distance =
                    slabDepth
                    +
                    8;

            int centerX =
                    base.getX()

                    +
                    oceanDirection
                            .getStepX()
                    *
                    distance;

            int centerZ =
                    base.getZ()

                    +
                    oceanDirection
                            .getStepZ()
                    *
                    distance;

            int tangentRadius =
                    Math.max(
                            3,
                            halfWidth - 1
                    );

            int normalRadius =
                    2
                    +
                    random.nextInt(
                            2
                    );

            int above =
                    3;

            int below =
                    5
                    +
                    random.nextInt(
                            3
                    );

            int centerY =
                    seaLevel - 1;

            for (
                    int t = -tangentRadius;
                    t <= tangentRadius;
                    t++
            ) {

                for (
                        int n = -normalRadius;
                        n <= normalRadius;
                        n++
                ) {

                    for (
                            int dy = -below;
                            dy <= above;
                            dy++
                    ) {

                        double nt =
                                t
                                /
                                (double) tangentRadius;

                        double nn =
                                n
                                /
                                (double) normalRadius;

                        double ny;

                        if (
                                dy >= 0
                        ) {

                            ny =
                                    dy
                                    /
                                    (double) above;
                        }

                        else {

                            ny =
                                    dy
                                    /
                                    (double) below;
                        }

                        /*
                         * Elipsoide irregular.
                         */
                        double shape =
                                nt * nt
                                +
                                nn * nn
                                +
                                ny * ny;

                        shape +=
                                (
                                        random.nextDouble()
                                        -
                                        0.5
                                )
                                *
                                0.18;

                        if (
                                shape > 1.0
                        ) {
                            continue;
                        }

                        int x =
                                centerX

                                +
                                tangent.getStepX()
                                *
                                t

                                +
                                oceanDirection
                                        .getStepX()
                                *
                                n;

                        int z =
                                centerZ

                                +
                                tangent.getStepZ()
                                *
                                t

                                +
                                oceanDirection
                                        .getStepZ()
                                *
                                n;

                        int y =
                                centerY
                                +
                                dy;

                        BlockPos pos =
                                new BlockPos(
                                        x,
                                        y,
                                        z
                                );

                        BlockState existing =
                                level.getBlockState(
                                        pos
                                );

                        /*
                         * Não enterra o iceberg
                         * em terreno sólido.
                         */
                        if (
                                !existing.isAir()

                                &&

                                existing
                                        .getFluidState()
                                        .isEmpty()
                        ) {
                            continue;
                        }

                        BlockState state;

                        if (
                                random.nextInt(
                                        11
                                )
                                ==
                                0
                        ) {

                            state =
                                    Blocks.BLUE_ICE
                                            .defaultBlockState();
                        }

                        else {

                            state =
                                    Blocks.PACKED_ICE
                                            .defaultBlockState();
                        }

                        /*
                         * Parte superior branca.
                         */
                        if (
                                dy >= above - 1

                                &&

                                random.nextFloat()
                                <
                                0.72F
                        ) {

                            state =
                                    Blocks.SNOW_BLOCK
                                            .defaultBlockState();
                        }

                        level.setBlock(
                                pos,
                                state,
                                2
                        );
                    }
                }
            }
        }

        /*
         * =====================================================
         * WATER CLEANUP
         * =====================================================
         */

        private void fillOceanGap(
                ServerLevel level
        ) {

            for (
                    IceBlock snapshot
                    :
                    blocks
            ) {

                BlockPos pos =
                        snapshot.pos;

                if (
                        pos.getY()
                        >
                        seaLevel
                ) {
                    continue;
                }

                if (
                        level
                                .getBlockState(
                                        pos
                                )
                                .isAir()
                ) {

                    level.setBlock(
                            pos,

                            Blocks.WATER
                                    .defaultBlockState(),

                            3
                    );
                }
            }
        }
    }
}
