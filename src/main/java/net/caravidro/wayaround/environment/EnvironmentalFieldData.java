package net.caravidro.wayaround.environment;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Sparse persistent 64x64 regional environmental memory.
 */
public final class EnvironmentalFieldData
        extends SavedData {

    public static final int CELL_SIZE =
            64;

    public static final int MAX_CELLS =
            8192;

    public static final class Cell {
        public float humidity;
        public float cloudWater;
        public float soilMoisture;
        public float snowBudget;
        public float smoke;
        public float pollution;
        public float waterAvailability;
        public long lastUpdate;
        public long lastWaterSample;

        public Cell copy() {
            Cell copy =
                    new Cell();

            copy.humidity =
                    humidity;

            copy.cloudWater =
                    cloudWater;

            copy.soilMoisture =
                    soilMoisture;

            copy.snowBudget =
                    snowBudget;

            copy.smoke =
                    smoke;

            copy.pollution =
                    pollution;

            copy.waterAvailability =
                    waterAvailability;

            copy.lastUpdate =
                    lastUpdate;

            copy.lastWaterSample =
                    lastWaterSample;

            return copy;
        }

        public void clamp() {
            humidity =
                    EnvironmentalFieldMath.unitF(
                            humidity
                    );

            cloudWater =
                    EnvironmentalFieldMath.unitF(
                            cloudWater
                    );

            soilMoisture =
                    EnvironmentalFieldMath.unitF(
                            soilMoisture
                    );

            snowBudget =
                    EnvironmentalFieldMath.unitF(
                            snowBudget
                    );

            smoke =
                    EnvironmentalFieldMath.unitF(
                            smoke
                    );

            pollution =
                    EnvironmentalFieldMath.unitF(
                            pollution
                    );

            waterAvailability =
                    EnvironmentalFieldMath.unitF(
                            waterAvailability
                    );
        }
    }

    private final LinkedHashMap<Long, Cell> cells =
            new LinkedHashMap<>();

    public static EnvironmentalFieldData get(
            ServerLevel level
    ) {
        return level.getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(
                                EnvironmentalFieldData::new,
                                EnvironmentalFieldData::load
                        ),
                        "wayaround_environmental_fields"
                );
    }

    public Cell get(
            long key
    ) {
        return cells.get(
                key
        );
    }

    public Cell putIfAbsent(
            long key,
            Cell cell
    ) {
        Cell old =
                cells.get(
                        key
                );

        if (old != null) {
            return old;
        }

        if (cells.size()
                >= MAX_CELLS) {
            cells.remove(
                    cells.keySet()
                            .iterator()
                            .next()
            );
        }

        cells.put(
                key,
                cell
        );

        setDirty();

        return cell;
    }

    public Map<Long, Cell> cells() {
        return Map.copyOf(
                cells
        );
    }

    public void changed() {
        setDirty();
    }

    private static EnvironmentalFieldData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        EnvironmentalFieldData data =
                new EnvironmentalFieldData();

        ListTag list =
                tag.getList(
                        "cells",
                        Tag.TAG_COMPOUND
                );

        for (int index = 0;
             index < list.size();
             index++) {

            CompoundTag entry =
                    list.getCompound(
                            index
                    );

            Cell cell =
                    new Cell();

            cell.humidity =
                    entry.getFloat(
                            "humidity"
                    );

            cell.cloudWater =
                    entry.getFloat(
                            "cloudWater"
                    );

            cell.soilMoisture =
                    entry.getFloat(
                            "soilMoisture"
                    );

            cell.snowBudget =
                    entry.getFloat(
                            "snowBudget"
                    );

            cell.smoke =
                    entry.getFloat(
                            "smoke"
                    );

            cell.pollution =
                    entry.getFloat(
                            "pollution"
                    );

            cell.waterAvailability =
                    entry.getFloat(
                            "waterAvailability"
                    );

            cell.lastUpdate =
                    entry.getLong(
                            "lastUpdate"
                    );

            cell.lastWaterSample =
                    entry.getLong(
                            "lastWaterSample"
                    );

            cell.clamp();

            data.cells.put(
                    entry.getLong(
                            "key"
                    ),
                    cell
            );
        }

        data.setDirty(
                false
        );

        return data;
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        ListTag list =
                new ListTag();

        for (var entry :
                cells.entrySet()) {

            CompoundTag value =
                    new CompoundTag();

            Cell cell =
                    entry.getValue();

            value.putLong(
                    "key",
                    entry.getKey()
            );

            value.putFloat(
                    "humidity",
                    cell.humidity
            );

            value.putFloat(
                    "cloudWater",
                    cell.cloudWater
            );

            value.putFloat(
                    "soilMoisture",
                    cell.soilMoisture
            );

            value.putFloat(
                    "snowBudget",
                    cell.snowBudget
            );

            value.putFloat(
                    "smoke",
                    cell.smoke
            );

            value.putFloat(
                    "pollution",
                    cell.pollution
            );

            value.putFloat(
                    "waterAvailability",
                    cell.waterAvailability
            );

            value.putLong(
                    "lastUpdate",
                    cell.lastUpdate
            );

            value.putLong(
                    "lastWaterSample",
                    cell.lastWaterSample
            );

            list.add(
                    value
            );
        }

        tag.put(
                "cells",
                list
        );

        return tag;
    }
}
