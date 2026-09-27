package net.caravidro.wayaround.war;

import com.mojang.serialization.MapCodec;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Creative test fixture: fixed aim, persistent selection, no player-owner exemption. */
public final class DebugTurretBlock extends DirectionalBlock {
    public static final MapCodec<DebugTurretBlock> CODEC = simpleCodec(DebugTurretBlock::new);
    public static final IntegerProperty AMMO = IntegerProperty.create("ammo", 0, 5);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final String[] NAMES = {"arrow", "snowball", "glock", "shotgun", "machine_gun", "rocket"};
    public DebugTurretBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(AMMO, 0).setValue(ACTIVE, false));
    }
    @Override public MapCodec<DebugTurretBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, AMMO, ACTIVE);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }
    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player) {
        if (!WorldFeatureRuntime.enabled(level, WorldFeature.WAR_WITHOUT_REASON)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) state = state.cycle(AMMO);
            else state = state.cycle(ACTIVE);
            level.setBlock(pos, state, 3);
            if (state.getValue(ACTIVE)) level.scheduleTick(pos, this, 10);
            player.displayClientMessage(Component.translatable("message.wayaround.debug_turret",
                    Component.translatable("turret.wayaround." + NAMES[state.getValue(AMMO)]),
                    Component.translatable(state.getValue(ACTIVE) ? "turret.wayaround.on" : "turret.wayaround.off")), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) { return interact(state, level, pos, player); }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        interact(state, level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) return;
        if (!WorldFeatureRuntime.enabled(level, WorldFeature.WAR_WITHOUT_REASON)) {
            level.setBlock(pos, state.setValue(ACTIVE, false), 3);
            return;
        }
        // An unattended test fixture must not fill a frozen field with unbounded entities.
        if (level.getEntities((net.minecraft.world.entity.Entity) null,
                new net.minecraft.world.phys.AABB(pos).inflate(64),
                entity -> entity instanceof net.minecraft.world.entity.projectile.Projectile
                        || entity instanceof WarProjectileEntity).size() >= 128) {
            level.scheduleTick(pos, this, 20);
            return;
        }
        Vec3 direction = Vec3.atLowerCornerOf(state.getValue(FACING).getNormal());
        Vec3 muzzle = Vec3.atCenterOf(pos).add(direction.scale(0.75));
        int ammo = state.getValue(AMMO);
        if (ammo == 0) {
            Arrow arrow = new Arrow(EntityType.ARROW, level);
            arrow.setPos(muzzle); arrow.setDeltaMovement(direction.scale(1.6));
            level.addFreshEntity(arrow);
        } else if (ammo == 1) {
            Snowball ball = new Snowball(EntityType.SNOWBALL, level);
            ball.setPos(muzzle); ball.setDeltaMovement(direction.scale(1.2));
            level.addFreshEntity(ball);
        } else {
            WarGunItem.Kind kind = WarGunItem.Kind.values()[ammo - 2];
            for (int i = 0; i < kind.pellets; i++) {
                WarProjectileEntity round = new WarProjectileEntity(WarContent.WAR_PROJECTILE.get(), level);
                Vec3 aim = direction.add(random.nextGaussian() * kind.spread,
                        random.nextGaussian() * kind.spread, random.nextGaussian() * kind.spread).normalize();
                round.setPos(muzzle);
                round.configure(null, kind, aim.scale(kind.speed));
                level.addFreshEntity(round);
            }
        }
        level.scheduleTick(pos, this, 20);
    }
}
