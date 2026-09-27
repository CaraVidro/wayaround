package net.caravidro.wayaround.media.blackbox;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class BlackBoxData {

    private static final String SEALED = "WayAroundBlackBoxSealed";
    private static final String RECORDING_ID = "WayAroundBlackBoxRecordingId";
    private static final String STARTED_AT = "WayAroundBlackBoxStartedAt";
    private static final String DURATION_TICKS = "WayAroundBlackBoxDurationTicks";
    private static final String DIMENSION = "WayAroundBlackBoxDimension";
    private static final String X = "WayAroundBlackBoxX";
    private static final String Y = "WayAroundBlackBoxY";
    private static final String Z = "WayAroundBlackBoxZ";

    private BlackBoxData() {
    }

    public record Info(
            boolean sealed,
            UUID recordingId,
            long startedAtMillis,
            long durationTicks,
            String dimension,
            int x,
            int y,
            int z
    ) {
    }

    public static void write(
            ItemStack stack,
            Info info
    ) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    tag.putBoolean(
                            SEALED,
                            info.sealed()
                    );
                    tag.putUUID(
                            RECORDING_ID,
                            info.recordingId()
                    );
                    tag.putLong(
                            STARTED_AT,
                            info.startedAtMillis()
                    );
                    tag.putLong(
                            DURATION_TICKS,
                            Math.max(
                                    0L,
                                    info.durationTicks()
                            )
                    );
                    tag.putString(
                            DIMENSION,
                            info.dimension() == null
                                    ? ""
                                    : info.dimension()
                    );
                    tag.putInt(
                            X,
                            info.x()
                    );
                    tag.putInt(
                            Y,
                            info.y()
                    );
                    tag.putInt(
                            Z,
                            info.z()
                    );
                }
        );
    }

    public static Optional<Info> read(
            ItemStack stack
    ) {
        if (stack == null
                || stack.isEmpty()) {
            return Optional.empty();
        }

        CompoundTag tag =
                stack.getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY
                )
                        .copyTag();

        if (!tag.hasUUID(
                RECORDING_ID
        )) {
            return Optional.empty();
        }

        return Optional.of(
                new Info(
                        tag.getBoolean(
                                SEALED
                        ),
                        tag.getUUID(
                                RECORDING_ID
                        ),
                        tag.getLong(
                                STARTED_AT
                        ),
                        tag.getLong(
                                DURATION_TICKS
                        ),
                        tag.getString(
                                DIMENSION
                        ),
                        tag.getInt(
                                X
                        ),
                        tag.getInt(
                                Y
                        ),
                        tag.getInt(
                                Z
                        )
                )
        );
    }
}
