package net.caravidro.wayaround.domain;

import javax.annotation.Nullable;

import net.caravidro.wayaround.WayAround;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class DomainPlayerData {

    private static final String DOMAIN_KEY =
            "Domain";

    private static final String AWAKENED_KEY =
            "Awakened";

    private static final String SEED_KEY =
            "Seed";

    private DomainPlayerData() {
    }

    public static boolean awakened(
            ServerPlayer player
    ) {
        return domainTag(
                player
        ).getBoolean(
                AWAKENED_KEY
        );
    }

    @Nullable
    public static DomainProfile profile(
            ServerPlayer player
    ) {
        CompoundTag tag =
                domainTag(
                        player
                );

        if (!tag.getBoolean(
                AWAKENED_KEY
        )) {
            return null;
        }

        return DomainProfile.fromSeed(
                tag.getLong(
                        SEED_KEY
                )
        );
    }

    public static DomainProfile awaken(
            ServerPlayer player
    ) {
        DomainProfile existing =
                profile(
                        player
                );

        if (existing != null) {
            return existing;
        }

        long seed =
                mix64(
                        player.getUUID()
                                .getMostSignificantBits()
                        ^ Long.rotateLeft(
                                player.getUUID()
                                        .getLeastSignificantBits(),
                                17
                        )
                        ^ player.serverLevel()
                                .getSeed()
                );

        CompoundTag domain =
                domainTag(
                        player
                );

        domain.putBoolean(
                AWAKENED_KEY,
                true
        );

        domain.putLong(
                SEED_KEY,
                seed
        );

        saveDomainTag(
                player,
                domain
        );

        return DomainProfile.fromSeed(
                seed
        );
    }

    private static CompoundTag domainTag(
            ServerPlayer player
    ) {
        CompoundTag persistent =
                player.getPersistentData();

        CompoundTag playerPersisted =
                persistent.getCompound(
                        Player.PERSISTED_NBT_TAG
                );

        CompoundTag mod =
                playerPersisted.getCompound(
                        WayAround.MODID
                );

        return mod.getCompound(
                DOMAIN_KEY
        );
    }

    private static void saveDomainTag(
            ServerPlayer player,
            CompoundTag domain
    ) {
        CompoundTag persistent =
                player.getPersistentData();

        CompoundTag playerPersisted =
                persistent.getCompound(
                        Player.PERSISTED_NBT_TAG
                );

        CompoundTag mod =
                playerPersisted.getCompound(
                        WayAround.MODID
                );

        mod.put(
                DOMAIN_KEY,
                domain
        );

        playerPersisted.put(
                WayAround.MODID,
                mod
        );

        persistent.put(
                Player.PERSISTED_NBT_TAG,
                playerPersisted
        );
    }

    private static long mix64(
            long value
    ) {
        value ^=
                value >>> 30;

        value *=
                0xbf58476d1ce4e5b9L;

        value ^=
                value >>> 27;

        value *=
                0x94d049bb133111ebL;

        value ^=
                value >>> 31;

        return value;
    }
}
