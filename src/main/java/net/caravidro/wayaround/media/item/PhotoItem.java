package net.caravidro.wayaround.media.item;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import net.caravidro.wayaround.media.MediaClientBridge;
import net.caravidro.wayaround.media.PhotoData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class PhotoItem
        extends Item {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm"
            );

    public PhotoItem(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(
            UseOnContext context
    ) {
        Direction face =
                context.getClickedFace();

        if (face.getAxis()
                .isVertical()) {
            return InteractionResult.PASS;
        }

        Level level =
                context.getLevel();

        BlockPos support =
                context.getClickedPos();

        if (!level.getBlockState(
                support
        ).isFaceSturdy(
                level,
                support,
                face
        )) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            Vec3 normal =
                    new Vec3(
                            face.getStepX(),
                            face.getStepY(),
                            face.getStepZ()
                    );

            Vec3 surface =
                    Vec3.atCenterOf(
                                    support
                            )
                            .add(
                                    normal.scale(
                                            0.57
                                    )
                            );

            ItemEntity photoEntity =
                    new ItemEntity(
                            level,
                            surface.x,
                            surface.y,
                            surface.z,
                            context.getItemInHand()
                                    .copyWithCount(
                                            1
                                    )
                    );

            /*
             * The photo is the object on the wall now. No hidden Item Frame,
             * no frame rotation state, no metadata swallowing. It simply sits
             * just outside the supporting face until somebody walks up and
             * takes it.
             */
            photoEntity.setNoGravity(
                    true
            );

            photoEntity.setDeltaMovement(
                    Vec3.ZERO
            );

            photoEntity.setPickUpDelay(
                    12
            );

            photoEntity.setUnlimitedLifetime();

            level.addFreshEntity(
                    photoEntity
            );

            Player player =
                    context.getPlayer();

            if (player == null
                    || !player.getAbilities()
                    .instabuild) {
                context.getItemInHand()
                        .shrink(
                                1
                        );
            }
        }

        return level.isClientSide
                ? InteractionResult.SUCCESS
                : InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack =
                player.getItemInHand(hand);

        if (level.isClientSide) {
            MediaClientBridge.openPhoto(
                    stack.copy()
            );
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        PhotoData.read(stack)
                .ifPresent(
                        info -> tooltip.add(
                                Component.translatable(
                                                "tooltip.wayaround.photo.taken",
                                                FORMAT.format(
                                                        Instant.ofEpochMilli(
                                                                        info.takenAt()
                                                                )
                                                                .atZone(
                                                                        ZoneId.systemDefault()
                                                                )
                                                )
                                        )
                                        .withStyle(
                                                ChatFormatting.GRAY
                                        )
                        )
                );
    }
}
