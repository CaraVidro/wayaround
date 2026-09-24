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

    private static final String TITLE =
            "WayAroundRecordingTitle";

    private static final String SERIAL =
            "WayAroundTapeSerial";

    private static final String SHOW_COORDS =
            "WayAroundShowCoordinates";

    private static final String SHOW_DATETIME =
            "WayAroundShowDateTime";

    private static final String X =
            "WayAroundRecordingX";

    private static final String Y =
            "WayAroundRecordingY";

    private static final String Z =
            "WayAroundRecordingZ";

    public record Info(
            String recordingId,
            long durationMillis,
            long startedAtMillis,
            String title,
            int serial,
            boolean showCoordinates,
            boolean showDateTime,
            int x,
            int y,
            int z
    ) {
    }

    public static void write(
            ItemStack stack,
            String recordingId,
            long durationMillis,
            long startedAtMillis,
            String title,
            int serial,
            int x,
            int y,
            int z
    ) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    tag.putString(ID, recordingId);
                    tag.putLong(DURATION, durationMillis);
                    tag.putLong(STARTED_AT, startedAtMillis);
                    tag.putString(TITLE, title == null ? "" : title);
                    tag.putInt(SERIAL, serial);
                    tag.putBoolean(SHOW_COORDS, false);
                    tag.putBoolean(SHOW_DATETIME, false);
                    tag.putInt(X, x);
                    tag.putInt(Y, y);
                    tag.putInt(Z, z);
                }
        );
    }

    public static void copy(
            ItemStack source,
            ItemStack target
    ) {
        read(source).ifPresent(
                info -> {
                    write(
                            target,
                            info.recordingId(),
                            info.durationMillis(),
                            info.startedAtMillis(),
                            info.title(),
                            info.serial(),
                            info.x(),
                            info.y(),
                            info.z()
                    );

                    setPresentation(
                            target,
                            info.title(),
                            info.showCoordinates(),
                            info.showDateTime()
                    );
                }
        );
    }

    public static void setPresentation(
            ItemStack stack,
            String title,
            boolean showCoordinates,
            boolean showDateTime
    ) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    tag.putString(
                            TITLE,
                            sanitizeTitle(title)
                    );

                    tag.putBoolean(
                            SHOW_COORDS,
                            showCoordinates
                    );

                    tag.putBoolean(
                            SHOW_DATETIME,
                            showDateTime
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

        String id = tag.getString(ID);
        long duration = tag.getLong(DURATION);
        long startedAt = tag.getLong(STARTED_AT);

        if (id.isBlank()
                || duration <= 0L) {

            return Optional.empty();
        }

        int serial =
                Math.max(
                        1,
                        tag.getInt(SERIAL)
                );

        String title =
                tag.getString(TITLE);

        if (title.isBlank()) {
            title =
                    "Fita #"
                            + serial;
        }

        return Optional.of(
                new Info(
                        id,
                        duration,
                        startedAt,
                        title,
                        serial,
                        tag.getBoolean(SHOW_COORDS),
                        tag.getBoolean(SHOW_DATETIME),
                        tag.getInt(X),
                        tag.getInt(Y),
                        tag.getInt(Z)
                )
        );
    }

    private static String sanitizeTitle(
            String title
    ) {
        if (title == null) {
            return "";
        }

        String cleaned =
                title.trim()
                        .replaceAll(
                                "[\\r\\n\\t]",
                                " "
                        );

        if (cleaned.length() > 32) {
            cleaned =
                    cleaned.substring(
                            0,
                            32
                    );
        }

        return cleaned;
    }
}
