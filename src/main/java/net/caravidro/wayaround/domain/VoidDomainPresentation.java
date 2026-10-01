package net.caravidro.wayaround.domain;

import net.caravidro.wayaround.WayAround;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Temporary/debug-facing Void Domain progression state.
 *
 * There is intentionally no natural unlock route yet:
 *
 * INNATE   -> the player only has the innate domain; no Expansion UI/cast.
 * SIMPLE   -> regular Domain Expansion; text-only intro.
 * ABSOLUTE -> Absolute Expansion; supplied cinematic image intro.
 *
 * Later progression can replace the command as the writer without changing the
 * renderer/network contract.
 */
public enum VoidDomainPresentation {

    INNATE(
            0,
            "inato"
    ),

    SIMPLE(
            1,
            "simples"
    ),

    ABSOLUTE(
            2,
            "absoluto"
    );

    private static final String MOD_KEY =
            WayAround.MODID;

    private static final String KEY =
            "VoidDomainPresentation";

    private final int id;
    private final String label;

    VoidDomainPresentation(
            int id,
            String label
    ) {
        this.id =
                id;

        this.label =
                label;
    }

    public int id() {
        return id;
    }

    public byte networkId() {
        return (byte) id;
    }

    public String label() {
        return label;
    }

    public boolean canExpand() {
        return this != INNATE;
    }

    public static VoidDomainPresentation get(
            ServerPlayer player
    ) {
        CompoundTag persisted =
                player.getPersistentData()
                        .getCompound(
                                Player.PERSISTED_NBT_TAG
                        );

        CompoundTag mod =
                persisted.getCompound(
                        MOD_KEY
                );

        return byId(
                mod.getInt(
                        KEY
                )
        );
    }

    public static void set(
            ServerPlayer player,
            VoidDomainPresentation presentation
    ) {
        CompoundTag persistent =
                player.getPersistentData();

        CompoundTag persisted =
                persistent.getCompound(
                        Player.PERSISTED_NBT_TAG
                );

        CompoundTag mod =
                persisted.getCompound(
                        MOD_KEY
                );

        mod.putInt(
                KEY,
                presentation.id
        );

        persisted.put(
                MOD_KEY,
                mod
        );

        persistent.put(
                Player.PERSISTED_NBT_TAG,
                persisted
        );
    }

    public static VoidDomainPresentation byId(
            int id
    ) {
        for (VoidDomainPresentation value :
                values()) {
            if (value.id
                    == id) {
                return value;
            }
        }

        return INNATE;
    }
}
