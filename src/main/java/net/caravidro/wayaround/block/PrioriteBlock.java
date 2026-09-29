package net.caravidro.wayaround.block;

import net.caravidro.wayaround.advancement.WayAroundAdvancements;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.particle.WayAroundParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

public final class PrioriteBlock extends Block {
    public static final IntegerProperty AMOUNT = IntegerProperty.create("amount", 1, 4);
    // 0 = idle, 1 = small bubble, 2 = large bubble. Saved with the scheduled tick.
    public static final IntegerProperty BUBBLE = IntegerProperty.create("bubble", 0, 2);
    public static final int BUBBLE_GROW_TICKS = 40;
    private static final String LAST_CONTACT = "wayaround_priorite_contact";

    public PrioriteBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AMOUNT, 4).setValue(BUBBLE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AMOUNT, BUBBLE);
    }

    private static boolean affected(LivingEntity living) {
        return living.isAlive() && !living.isSpectator()
                && !(living instanceof Player player && player.isCreative());
    }

    private static void applyContact(Level level, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living) || !affected(living)) return;
        long now = level.getGameTime();
        var data = living.getPersistentData();
        long previous = data.getLong(LAST_CONTACT);
        // Multiple neighboring blocks cannot multiply contact damage in the same tick.
        if (data.contains(LAST_CONTACT) && now >= previous && now - previous < 20) return;
        data.putLong(LAST_CONTACT, now);

        if (living instanceof net.minecraft.server.level.ServerPlayer player) {
            WayAroundAdvancements.vistaPriorite(
                    player
            );
        }

        living.hurt(level.damageSources().magic(), 2.0F);
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        // A solid cube does not call entityInside when an entity walks on its top.
        applyContact(level, entity);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof LivingEntity living && affected(living)) {
            entity.makeStuckInBlock(state, new Vec3(0.35, 1.0, 0.35));
        }
        applyContact(level, entity);
    }

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

        if (!level.isClientSide
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            WayAroundAdvancements.vistaPriorite(
                    serverPlayer
            );
        }

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
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockState(pos.above()).isAir() && random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.SMOKE,
                    pos.getX() + random.nextDouble(), pos.getY() + 1.02,
                    pos.getZ() + random.nextDouble(), 0, 0.025, 0);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(BUBBLE) != 0 || !level.getBlockState(pos.above()).isAir()
                || random.nextInt(3) != 0) return;
        boolean giant = random.nextInt(10) == 0;
        level.setBlock(pos, state.setValue(BUBBLE, giant ? 2 : 1), 2);
        level.scheduleTick(pos, this, BUBBLE_GROW_TICKS);
        // count=0 delivers the size in the particle's x-speed field to every client.
        level.sendParticles(WayAroundParticles.PRIORITE_BUBBLE.get(),
                pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5,
                0, giant ? 0.8 : 0.38, 0, 0, 1.0);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int bubble = state.getValue(BUBBLE);
        if (bubble == 0) return;
        level.setBlock(pos, state.setValue(BUBBLE, 0), 2);
        if (!level.getBlockState(pos.above()).isAir()) return;
        boolean giant = bubble == 2;
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 1.25, pos.getZ() + 0.5);
        double radius = giant ? 3.0 : 1.5;
        level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS,
                giant ? 1.2F : 0.5F, giant ? 0.55F : 0.9F);
        level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z,
                giant ? 30 : 12, radius * 0.3, 0.3, radius * 0.3, 0.045);
        level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z,
                giant ? 16 : 6, 0.35, 0.2, 0.35, 0.06);
        if (giant) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z,
                    10, 0.6, 0.3, 0.6, 0.04);
        }
        AABB area = new AABB(center, center).inflate(radius);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, PrioriteBlock::affected)) {
            AABB bounds = living.getBoundingBox();
            double x = Math.clamp(center.x, bounds.minX, bounds.maxX);
            double y = Math.clamp(center.y, bounds.minY, bounds.maxY);
            double z = Math.clamp(center.z, bounds.minZ, bounds.maxZ);
            if (center.distanceToSqr(x, y, z) > radius * radius) continue;
            living.hurt(level.damageSources().magic(), giant ? 4.0F : 1.5F);
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
        }
    }
}
