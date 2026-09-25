package net.caravidro.wayaround.justice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public final class JusticeSenseData extends SavedData {

    private static final String DATA_NAME = "wayaround_justice_sense";
    private static final int MAX_INCIDENTS_PER_PLAYER = 96;

    private final Map<UUID, List<JusticeIncident>> incidents =
            new HashMap<>();

    private final Map<String, UUID> chestOwners =
            new HashMap<>();

    private final Map<String, UUID> structureOwners =
            new HashMap<>();

    private final Map<UUID, Float> credibility =
            new HashMap<>();

    public static JusticeSenseData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(
                                JusticeSenseData::new,
                                JusticeSenseData::load
                        ),
                        DATA_NAME
                );
    }

    public void record(JusticeIncident incident) {
        List<JusticeIncident> list =
                incidents.computeIfAbsent(
                        incident.offender(),
                        ignored -> new ArrayList<>()
                );

        list.add(incident);

        while (list.size() > MAX_INCIDENTS_PER_PLAYER) {
            list.remove(0);
        }

        setDirty();
    }

    public List<JusticeIncident> recent(
            UUID offender,
            int max
    ) {
        List<JusticeIncident> source =
                incidents.getOrDefault(
                        offender,
                        List.of()
                );

        int from =
                Math.max(
                        0,
                        source.size() - Math.max(1, max)
                );

        ArrayList<JusticeIncident> result =
                new ArrayList<>(
                        source.subList(
                                from,
                                source.size()
                        )
                );

        java.util.Collections.reverse(result);
        return result;
    }

    public float credibility(UUID player) {
        return credibility.getOrDefault(
                player,
                0.50F
        );
    }

    public void adjustCredibility(
            UUID player,
            float delta
    ) {
        credibility.put(
                player,
                Mth.clamp(
                        credibility(player) + delta,
                        0.05F,
                        0.95F
                )
        );

        setDirty();
    }

    public void claimChest(
            Level level,
            BlockPos pos,
            UUID owner
    ) {
        chestOwners.put(
                key(level, pos),
                owner
        );
        setDirty();
    }

    public void claimStructure(
            Level level,
            BlockPos pos,
            UUID owner
    ) {
        structureOwners.put(
                key(level, pos),
                owner
        );
        setDirty();
    }

    public void forgetPosition(
            Level level,
            BlockPos pos
    ) {
        boolean changed =
                chestOwners.remove(
                        key(level, pos)
                ) != null;

        changed |=
                structureOwners.remove(
                        key(level, pos)
                ) != null;

        if (changed) {
            setDirty();
        }
    }

    @Nullable
    public UUID chestOwner(
            Level level,
            BlockPos pos
    ) {
        return chestOwners.get(
                key(level, pos)
        );
    }

    @Nullable
    public UUID structureOwner(
            Level level,
            BlockPos pos
    ) {
        return structureOwners.get(
                key(level, pos)
        );
    }

    private static String key(
            Level level,
            BlockPos pos
    ) {
        return level.dimension()
                .location()
                .toString()
                + "|"
                + pos.asLong();
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        ListTag incidentList =
                new ListTag();

        for (List<JusticeIncident> list :
                incidents.values()) {

            for (JusticeIncident incident :
                    list) {
                incidentList.add(
                        incident.save()
                );
            }
        }

        tag.put(
                "Incidents",
                incidentList
        );

        tag.put(
                "ChestOwners",
                saveOwners(
                        chestOwners
                )
        );

        tag.put(
                "StructureOwners",
                saveOwners(
                        structureOwners
                )
        );

        ListTag trust =
                new ListTag();

        for (Map.Entry<UUID, Float> entry :
                credibility.entrySet()) {

            CompoundTag row =
                    new CompoundTag();

            row.putUUID(
                    "Player",
                    entry.getKey()
            );

            row.putFloat(
                    "Value",
                    entry.getValue()
            );

            trust.add(
                    row
            );
        }

        tag.put(
                "Credibility",
                trust
        );

        return tag;
    }

    public static JusticeSenseData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        JusticeSenseData data =
                new JusticeSenseData();

        ListTag incidentList =
                tag.getList(
                        "Incidents",
                        Tag.TAG_COMPOUND
                );

        for (int index = 0;
             index < incidentList.size();
             index++) {

            JusticeIncident incident =
                    JusticeIncident.load(
                            incidentList.getCompound(index)
                    );

            data.incidents
                    .computeIfAbsent(
                            incident.offender(),
                            ignored -> new ArrayList<>()
                    )
                    .add(
                            incident
                    );
        }

        loadOwners(
                tag.getList(
                        "ChestOwners",
                        Tag.TAG_COMPOUND
                ),
                data.chestOwners
        );

        loadOwners(
                tag.getList(
                        "StructureOwners",
                        Tag.TAG_COMPOUND
                ),
                data.structureOwners
        );

        ListTag trust =
                tag.getList(
                        "Credibility",
                        Tag.TAG_COMPOUND
                );

        for (int index = 0;
             index < trust.size();
             index++) {

            CompoundTag row =
                    trust.getCompound(
                            index
                    );

            if (row.hasUUID(
                    "Player"
            )) {
                data.credibility.put(
                        row.getUUID(
                                "Player"
                        ),
                        Mth.clamp(
                                row.getFloat(
                                        "Value"
                                ),
                                0.05F,
                                0.95F
                        )
                );
            }
        }

        return data;
    }

    private static ListTag saveOwners(
            Map<String, UUID> owners
    ) {
        ListTag list =
                new ListTag();

        for (Map.Entry<String, UUID> entry :
                owners.entrySet()) {

            CompoundTag row =
                    new CompoundTag();

            row.putString(
                    "Key",
                    entry.getKey()
            );

            row.putUUID(
                    "Owner",
                    entry.getValue()
            );

            list.add(
                    row
            );
        }

        return list;
    }

    private static void loadOwners(
            ListTag list,
            Map<String, UUID> target
    ) {
        for (int index = 0;
             index < list.size();
             index++) {

            CompoundTag row =
                    list.getCompound(
                            index
                    );

            if (!row.hasUUID(
                    "Owner"
            )) {
                continue;
            }

            target.put(
                    row.getString(
                            "Key"
                    ),
                    row.getUUID(
                            "Owner"
                    )
            );
        }
    }
}
