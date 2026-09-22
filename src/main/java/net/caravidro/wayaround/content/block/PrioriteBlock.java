package net.caravidro.wayaround.content.block;

import net.caravidro.wayaround.content.WayAroundContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class PrioriteBlock extends Block {

    /*
     * =========================================================
     * amount = 1 até 4
     *
     * 1 = bem pouco
     * 4 = poça cheia
     * =========================================================
     */

    public static final IntegerProperty AMOUNT =
            IntegerProperty.create(
                    "amount",
                    1,
                    4
            );

    public PrioriteBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any().setValue(
                        AMOUNT,
                        4
                )
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(AMOUNT);
    }

    /*
     * =========================================================
     * FÍSICA DO SOFRIMENTO
     * =========================================================
     */

   @Override
protected void entityInside(
        BlockState state,
        Level level,
        BlockPos pos,
        Entity entity
) {

    /*
     * =========================================================
     * VISCOSIDADE
     * =========================================================
     */

    Vec3 motion =
            entity.getDeltaMovement();

    entity.setDeltaMovement(

            motion.x * 0.22,

            motion.y * 0.35,

            motion.z * 0.22

    );


    /*
     * É difícil subir através da Priorita.
     */

    Vec3 current =
            entity.getDeltaMovement();

    if (
            current.y > 0.12
    ) {

        entity.setDeltaMovement(

                current.x,

                0.12,

                current.z

        );
    }


    /*
     * =========================================================
     * CONTATO QUÍMICO
     * =========================================================
     *
     * Chamamos hurt() sempre que existir contato.
     *
     * LivingEntity já possui os próprios
     * invulnerability frames do Minecraft,
     * então não toma 20 corações por segundo.
     *
     * A primeira encostada já machuca.
     */

    if (
            !level.isClientSide

                    &&

            entity instanceof LivingEntity living
    ) {

        int amount =
                state.getValue(
                        AMOUNT
                );


        /*
         * Poças mais concentradas fazem
         * um pouco mais de dano.
         */

        float damage =
                1.0F

                        +

                amount * 0.35F;


        living.hurt(

                level.damageSources()
                        .magic(),

                damage

        );
    }
}

    /*
     * =========================================================
     * COLETA COM GARRAFA E BALDE
     * =========================================================
     */

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {

        int amount = state.getValue(AMOUNT);

        /*
         * -----------------------------------------------------
         * BALDE VAZIO
         * Só pega se a poça estiver cheia.
         * -----------------------------------------------------
         */
        if (stack.is(Items.BUCKET)) {

            if (amount < 4) {
                return ItemInteractionResult.FAIL;
            }

            if (!level.isClientSide) {

                level.removeBlock(pos, false);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);

                    ItemStack filled =
                            new ItemStack(
                                    WayAroundContent.PRIORITE_BUCKET.get()
                            );

                    if (stack.isEmpty()) {
                        player.setItemInHand(hand, filled);
                    } else if (!player.addItem(filled)) {
                        player.drop(filled, false);
                    }
                }

                level.playSound(
                        null,
                        pos,
                        SoundEvents.BUCKET_FILL_LAVA,
                        SoundSource.BLOCKS,
                        1.0F,
                        0.75F
                );
            }

            return ItemInteractionResult.SUCCESS;
        }

        /*
         * -----------------------------------------------------
         * GARRAFA DE VIDRO
         * tira 1 do amount
         * -----------------------------------------------------
         */
        if (stack.is(Items.GLASS_BOTTLE)) {

            if (!level.isClientSide) {

                if (amount <= 1) {
                    level.removeBlock(pos, false);
                } else {
                    level.setBlock(
                            pos,
                            state.setValue(
                                    AMOUNT,
                                    amount - 1
                            ),
                            3
                    );
                }

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);

                    ItemStack filled =
                            new ItemStack(
                                    WayAroundContent.PRIORITE_BOTTLE.get()
                            );

                    if (stack.isEmpty()) {
                        player.setItemInHand(hand, filled);
                    } else if (!player.addItem(filled)) {
                        player.drop(filled, false);
                    }
                }

                level.playSound(
                        null,
                        pos,
                        SoundEvents.BOTTLE_FILL,
                        SoundSource.BLOCKS,
                        1.0F,
                        0.70F
                );
            }

            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /*
     * =========================================================
     * FUMAÇA E BOLHAS VISUAIS
     * =========================================================
     */

    @Override
    public void animateTick(
            BlockState state,
            Level level,
            BlockPos pos,
            RandomSource random
    ) {

        int amount = state.getValue(AMOUNT);

        double topHeight =
                0.20 + amount * 0.14;

        /*
         * Fumaça constante.
         */
        if (random.nextInt(2) == 0) {
            level.addParticle(
                    ParticleTypes.SMOKE,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + topHeight,
                    pos.getZ() + random.nextDouble(),
                    (random.nextDouble() - 0.5) * 0.015,
                    0.03 + random.nextDouble() * 0.03,
                    (random.nextDouble() - 0.5) * 0.015
            );
        }

        /*
         * Puff / borbulhada pequena.
         */
        if (random.nextInt(5) == 0) {
            level.addParticle(
                    ParticleTypes.POOF,
                    pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + topHeight,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                    0.0,
                    0.04,
                    0.0
            );
        }

        /*
         * Coluna de fumaça mais forte às vezes.
         */
        if (random.nextInt(14) == 0) {
            level.addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + topHeight,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                    0.0,
                    0.07,
                    0.0
            );
        }
    }

    /*
     * =========================================================
     * BOLHAS QUE ESTOURAM E DÃO DANO
     * =========================================================
     *
     * Aqui mora a maluquice boa.
     *
     * Às vezes:
     * - bolha normal
     * - raramente: bolha enorme
     */

    @Override
    protected void randomTick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {

        /*
         * Nem toda random tick vai borbulhar.
         */
        if (random.nextInt(6) != 0) {
            return;
        }

        int amount = state.getValue(AMOUNT);

        boolean giantBubble =
                random.nextInt(10) == 0;

        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.65;
        double centerZ = pos.getZ() + 0.5;

        float volume = giantBubble ? 1.2F : 0.45F;
        float pitch = giantBubble ? 0.55F : 0.85F;

        level.playSound(
                null,
                pos,
                SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                SoundSource.BLOCKS,
                volume,
                pitch
        );

        /*
         * Partículas no servidor para todo mundo ver.
         */
        if (giantBubble) {

            level.sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    centerX,
                    centerY,
                    centerZ,
                    12,
                    0.45,
                    0.18,
                    0.45,
                    0.03
            );

            level.sendParticles(
                    ParticleTypes.POOF,
                    centerX,
                    centerY,
                    centerZ,
                    18,
                    0.55,
                    0.20,
                    0.55,
                    0.06
            );
        } else {

            level.sendParticles(
                    ParticleTypes.POOF,
                    centerX,
                    centerY,
                    centerZ,
                    6,
                    0.20,
                    0.08,
                    0.20,
                    0.02
            );
        }

        /*
         * Área de dano da bolha.
         */
        double radius =
                giantBubble
                        ? 2.6
                        : 1.2;

        float damage =
                giantBubble
                        ? (2.0F + amount)
                        : 1.5F;

        AABB area =
                new AABB(pos).inflate(
                        radius,
                        1.2,
                        radius
                );

        List<LivingEntity> entities =
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        area
                );

        for (LivingEntity living : entities) {

            living.hurt(
                    level.damageSources().magic(),
                    damage
            );

            /*
             * leve empurrão da explosão química
             */
            double dx = living.getX() - centerX;
            double dz = living.getZ() - centerZ;

            double len = Math.sqrt(dx * dx + dz * dz);

            if (len > 0.0001) {
                dx /= len;
                dz /= len;

                living.push(
                        dx * (giantBubble ? 0.35 : 0.12),
                        giantBubble ? 0.12 : 0.04,
                        dz * (giantBubble ? 0.35 : 0.12)
                );
            }
        }
    }
}