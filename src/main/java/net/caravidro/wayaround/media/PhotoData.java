package net.caravidro.wayaround.media;

import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class PhotoData {

    private PhotoData() {
    }

    private static final String ID =
            "WayAroundPhotoId";

    private static final String TAKEN_AT =
            "WayAroundPhotoTakenAt";

    private static final String X =
            "WayAroundPhotoX";

    private static final String Y =
            "WayAroundPhotoY";

    private static final String Z =
            "WayAroundPhotoZ";

    public record Info(
            String photoId,
            long takenAt,
            int x,
            int y,
            int z
    ) {
    }

    public static void write(
            ItemStack stack,
            String photoId,
            long takenAt,
            int x,
            int y,
            int z
    ) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    tag.putString(ID, photoId);
                    tag.putLong(TAKEN_AT, takenAt);
                    tag.putInt(X, x);
                    tag.putInt(Y, y);
                    tag.putInt(Z, z);
                }
        );
    }

    public static Optional<Info> read(
            ItemStack stack
    ) {
        CompoundTag tag =
                stack.getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY
                )
                        .copyTag();

        String id =
                tag.getString(ID);

        if (id.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(
                new Info(
                        id,
                        tag.getLong(TAKEN_AT),
                        tag.getInt(X),
                        tag.getInt(Y),
                        tag.getInt(Z)
                )
        );
    }
}
