package net.caravidro.wayaround.industrial.engineering;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class EngineeringBlueprintData {

    private static final String ID = "WayAroundBlueprintId";
    private static final String TITLE = "WayAroundBlueprintTitle";
    private static final String CREATED_AT = "WayAroundBlueprintCreatedAt";
    private static final String SHOW_COORDS = "WayAroundBlueprintShowCoordinates";
    private static final String SHOW_DATETIME = "WayAroundBlueprintShowDateTime";
    private static final String X = "WayAroundBlueprintX";
    private static final String Y = "WayAroundBlueprintY";
    private static final String Z = "WayAroundBlueprintZ";
    private static final String PROJECT = "WayAroundEngineeringProject";

    private static final int MAX_NODES = 256;

    private EngineeringBlueprintData() {
    }

    public record Info(
            String blueprintId,
            String title,
            long createdAtMillis,
            boolean showCoordinates,
            boolean showDateTime,
            int x,
            int y,
            int z,
            CompoundTag project
    ) {
        public int nodeCount() {
            return project.getList(
                    "Nodes",
                    CompoundTag.TAG_COMPOUND
            ).size();
        }
    }

    public static Info writeProject(
            ItemStack stack,
            CompoundTag rawProject,
            BlockPos createdAt
    ) {
        Info old =
                read(stack)
                        .orElse(null);

        String id =
                old == null
                        ? UUID.randomUUID().toString()
                        : old.blueprintId();

        String title =
                old == null
                        ? ""
                        : old.title();

        long time =
                old == null
                        ? System.currentTimeMillis()
                        : old.createdAtMillis();

        boolean showCoordinates =
                old != null
                        && old.showCoordinates();

        boolean showDateTime =
                old != null
                        && old.showDateTime();

        int x =
                old == null
                        ? createdAt.getX()
                        : old.x();

        int y =
                old == null
                        ? createdAt.getY()
                        : old.y();

        int z =
                old == null
                        ? createdAt.getZ()
                        : old.z();

        CompoundTag project =
                sanitizeProject(
                        rawProject
                );

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    tag.putString(ID, id);
                    tag.putString(TITLE, sanitizeTitle(title));
                    tag.putLong(CREATED_AT, time);
                    tag.putBoolean(SHOW_COORDS, showCoordinates);
                    tag.putBoolean(SHOW_DATETIME, showDateTime);
                    tag.putInt(X, x);
                    tag.putInt(Y, y);
                    tag.putInt(Z, z);
                    tag.put(PROJECT, project.copy());
                }
        );

        return read(stack)
                .orElseThrow();
    }

    public static void setPresentation(
            ItemStack stack,
            String title,
            boolean showCoordinates,
            boolean showDateTime
    ) {
        if (read(stack).isEmpty()) {
            return;
        }

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
                ).copyTag();

        String id =
                tag.getString(ID);

        if (id.isBlank()
                || !tag.contains(
                PROJECT,
                CompoundTag.TAG_COMPOUND
        )) {
            return Optional.empty();
        }

        CompoundTag project =
                sanitizeProject(
                        tag.getCompound(PROJECT)
                );

        String title =
                tag.getString(TITLE);

        if (title.isBlank()) {
            title =
                    "Blueprint "
                            + shortId(id);
        }

        return Optional.of(
                new Info(
                        id,
                        title,
                        Math.max(
                                0L,
                                tag.getLong(CREATED_AT)
                        ),
                        tag.getBoolean(SHOW_COORDS),
                        tag.getBoolean(SHOW_DATETIME),
                        tag.getInt(X),
                        tag.getInt(Y),
                        tag.getInt(Z),
                        project
                )
        );
    }

    public static CompoundTag sanitizeProject(
            CompoundTag raw
    ) {
        CompoundTag project =
                raw == null
                        ? new CompoundTag()
                        : raw.copy();

        ListTag nodes =
                project.getList(
                        "Nodes",
                        CompoundTag.TAG_COMPOUND
                );

        if (nodes.size() > MAX_NODES) {
            ListTag trimmed =
                    new ListTag();

            for (int index =
                         0;
                 index < MAX_NODES;
                 index++) {
                trimmed.add(
                        nodes.get(index)
                                .copy()
                );
            }

            project.put(
                    "Nodes",
                    trimmed
            );
        }

        project.putDouble(
                "Zoom",
                Math.clamp(
                        project.getDouble("Zoom"),
                        0.45,
                        2.40
                )
        );

        project.putDouble(
                "PanX",
                Math.clamp(
                        project.getDouble("PanX"),
                        -100000.0,
                        100000.0
                )
        );

        project.putDouble(
                "PanY",
                Math.clamp(
                        project.getDouble("PanY"),
                        -100000.0,
                        100000.0
                )
        );

        return project;
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

        if (cleaned.length() > 40) {
            cleaned =
                    cleaned.substring(
                            0,
                            40
                    );
        }

        return cleaned;
    }

    private static String shortId(
            String id
    ) {
        if (id.length() <= 8) {
            return id;
        }

        return id.substring(
                0,
                8
        );
    }
}
