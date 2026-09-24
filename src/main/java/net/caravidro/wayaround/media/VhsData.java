package net.caravidro.wayaround.media;

import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class VhsData {

    private VhsData() {
    }

    private static final String ID =
            "WayAroundRecordingId";

    private static final String DURATION =
            "WayAroundRecordingDurationMs";

    private static final String STARTED_AT =
            "WayAroundRecordingStartedAt";

    public record Info(
            String recordingId,
            long durationMillis,
            long startedAtMillis
    ) {
    }

    public static void write(
            ItemStack stack,
            String recordingId,
            long durationMillis,
            long startedAtMillis
    ) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    tag.putString(
                            ID,
                            recordingId
                    );

                    tag.putLong(
                            DURATION,
                            durationMillis
                    );

                    tag.putLong(
                            STARTED_AT,
                            startedAtMillis
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

        String id =
                tag.getString(ID);

        long duration =
                tag.getLong(DURATION);

        long startedAt =
                tag.getLong(STARTED_AT);

        if (id.isBlank()
                || duration <= 0L) {

            return Optional.empty();
        }

        return Optional.of(
                new Info(
                        id,
                        duration,
                        startedAt
                )
        );
    }
}
