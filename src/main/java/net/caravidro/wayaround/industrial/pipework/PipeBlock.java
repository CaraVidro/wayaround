package net.caravidro.wayaround.industrial.pipework;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PipeBlock
        extends BaseEntityBlock {

    private final PipeProfile profile;
    private final MapCodec<PipeBlock> codec;

    public PipeBlock(
            Properties properties,
            PipeProfile profile
    ) {
        super(properties);

        this.profile =
                profile;

        this.codec =
                simpleCodec(
                        properties -> new PipeBlock(
                                properties,
                                profile
                        )
                );
    }

    public PipeProfile profile() {
        return profile;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new PipeBlockEntity(
                pos,
                state
        );
    }

    @Nullable
    @Override
    public <T extends BlockEntity>
            BlockEntityTicker<T> getTicker(
                    Level level,
                    BlockState state,
                    BlockEntityType<T> type
            ) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                        type,
                        PipeworkContent.PIPE_ENTITY.get(),
                        PipeBlockEntity::serverTick
                );
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        double radius =
                profile.radius();

        double min =
                0.5 - radius;

        double max =
                0.5 + radius;

        VoxelShape shape =
                box(
                        min * 16.0,
                        min * 16.0,
                        min * 16.0,
                        max * 16.0,
                        max * 16.0,
                        max * 16.0
                );

        for (Direction direction :
                Direction.values()) {

            if (!connects(
                    level,
                    pos,
                    direction
            )) {
                continue;
            }

            VoxelShape arm =
                    switch (direction) {
                        case EAST ->
                                box(
                                        max * 16.0,
                                        min * 16.0,
                                        min * 16.0,
                                        16.0,
                                        max * 16.0,
                                        max * 16.0
                                );

                        case WEST ->
                                box(
                                        0.0,
                                        min * 16.0,
                                        min * 16.0,
                                        min * 16.0,
                                        max * 16.0,
                                        max * 16.0
                                );

                        case UP ->
                                box(
                                        min * 16.0,
                                        max * 16.0,
                                        min * 16.0,
                                        max * 16.0,
                                        16.0,
                                        max * 16.0
                                );

                        case DOWN ->
                                box(
                                        min * 16.0,
                                        0.0,
                                        min * 16.0,
                                        max * 16.0,
                                        min * 16.0,
                                        max * 16.0
                                );

                        case SOUTH ->
                                box(
                                        min * 16.0,
                                        min * 16.0,
                                        max * 16.0,
                                        max * 16.0,
                                        max * 16.0,
                                        16.0
                                );

                        case NORTH ->
                                box(
                                        min * 16.0,
                                        min * 16.0,
                                        0.0,
                                        max * 16.0,
                                        max * 16.0,
                                        min * 16.0
                                );
                    };

            shape =
                    Shapes.or(
                            shape,
                            arm
                    );
        }

        return shape;
    }

    private static boolean connects(
            BlockGetter level,
            BlockPos pos,
            Direction direction
    ) {
        return level.getBlockState(
                pos.relative(
                        direction
                )
        ).getBlock()
                instanceof PipeBlock;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos)
                instanceof PipeBlockEntity pipe)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            if (stack.is(Items.WATER_BUCKET)
                    && profile.accepts(
                    PipeMedium.WATER
            )) {

                int accepted =
                        pipe.insert(
                                PipeMedium.WATER,
                                1_000
                        );

                if (accepted == 1_000
                        && !player.isCreative()) {
                    player.setItemInHand(
                            hand,
                            new ItemStack(
                                    Items.BUCKET
                            )
                    );
                }

                return InteractionResult.SUCCESS;
            }

            if (stack.is(Items.BUCKET)
                    && pipe.medium()
                            == PipeMedium.WATER
                    && pipe.amount() >= 1_000) {

                int extracted =
                        pipe.extract(
                                1_000
                        );

                if (extracted == 1_000
                        && !player.isCreative()) {
                    stack.shrink(1);

                    ItemStack filled =
                            new ItemStack(
                                    Items.WATER_BUCKET
                            );

                    if (stack.isEmpty()) {
                        player.setItemInHand(
                                hand,
                                filled
                        );
                    } else if (!player.getInventory()
                            .add(
                                    filled
                            )) {
                        player.drop(
                                filled,
                                false
                        );
                    }
                }

                return InteractionResult.SUCCESS;
            }

            if (stack.is(
                    PipeworkContent.COMPRESSED_AIR_CANISTER.get()
            )) {
                int accepted =
                        pipe.insert(
                                PipeMedium.AIR,
                                1_000
                        );

                if (accepted == 1_000
                        && !player.isCreative()) {
                    stack.shrink(1);
                    giveCanister(
                            player,
                            hand,
                            stack,
                            new ItemStack(
                                    PipeworkContent.EMPTY_CANISTER.get()
                            )
                    );
                }

                return InteractionResult.SUCCESS;
            }

            if (stack.is(
                    PipeworkContent.STEAM_CANISTER.get()
            )) {
                if (!profile.hotSteamRated()) {
                    player.displayClientMessage(
                            Component.translatable(
                                    "message.wayaround.pipe.steam_rating"
                            ),
                            true
                    );

                    return InteractionResult.SUCCESS;
                }

                int accepted =
                        pipe.insert(
                                PipeMedium.STEAM,
                                1_000
                        );

                if (accepted == 1_000
                        && !player.isCreative()) {
                    stack.shrink(1);
                    giveCanister(
                            player,
                            hand,
                            stack,
                            new ItemStack(
                                    PipeworkContent.EMPTY_CANISTER.get()
                            )
                    );
                }

                return InteractionResult.SUCCESS;
            }

            if (stack.is(
                    PipeworkContent.EMPTY_CANISTER.get()
            )
                    && pipe.amount() >= 1_000
                    && (
                    pipe.medium()
                            == PipeMedium.AIR
                            || pipe.medium()
                            == PipeMedium.STEAM
            )) {

                PipeMedium medium =
                        pipe.medium();

                if (pipe.extract(
                        1_000
                ) == 1_000
                        && !player.isCreative()) {

                    stack.shrink(1);

                    ItemStack filled =
                            new ItemStack(
                                    medium
                                            == PipeMedium.STEAM
                                            ? PipeworkContent.STEAM_CANISTER.get()
                                            : PipeworkContent.COMPRESSED_AIR_CANISTER.get()
                            );

                    giveCanister(
                            player,
                            hand,
                            stack,
                            filled
                    );
                }

                return InteractionResult.SUCCESS;
            }

            player.displayClientMessage(
                    pipe.status(),
                    true
            );
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide
        );
    }

    private static void giveCanister(
            Player player,
            InteractionHand hand,
            ItemStack originalStack,
            ItemStack result
    ) {
        if (originalStack.isEmpty()) {
            player.setItemInHand(
                    hand,
                    result
            );
        } else if (!player.getInventory()
                .add(
                        result
                )) {
            player.drop(
                    result,
                    false
            );
        }
    }
}
