package net.caravidro.wayaround.justice;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public record JusticeIncident(
        Type type,
        UUID offender,
        String offenderName,
        @Nullable UUID victim,
        String victimName,
        String dimension,
        long position,
        long gameTime,
        float confidence,
        String detail
) {

    public enum Type {
        CHEST_THEFT("roubo de baú"),
        DOG_KILL("morte de cachorro"),
        PLAYER_KILL("morte de jogador"),
        ARSON("incêndio criminoso");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.name());
        tag.putUUID("Offender", offender);
        tag.putString("OffenderName", offenderName);
        if (victim != null) tag.putUUID("Victim", victim);
        tag.putString("VictimName", victimName == null ? "" : victimName);
        tag.putString("Dimension", dimension);
        tag.putLong("Position", position);
        tag.putLong("GameTime", gameTime);
        tag.putFloat("Confidence", confidence);
        tag.putString("Detail", detail == null ? "" : detail);
        return tag;
    }

    public static JusticeIncident load(CompoundTag tag) {
        Type type;
        try {
            type = Type.valueOf(tag.getString("Type"));
        } catch (IllegalArgumentException ex) {
            type = Type.PLAYER_KILL;
        }

        return new JusticeIncident(
                type,
                tag.getUUID("Offender"),
                tag.getString("OffenderName"),
                tag.hasUUID("Victim") ? tag.getUUID("Victim") : null,
                tag.getString("VictimName"),
                tag.getString("Dimension"),
                tag.getLong("Position"),
                tag.getLong("GameTime"),
                Mth.clamp(tag.getFloat("Confidence"), 0.0F, 1.0F),
                tag.getString("Detail")
        );
    }

    public BlockPos blockPos() {
        return BlockPos.of(position);
    }

    public String summary() {
        BlockPos pos = blockPos();
        String victimText = victimName == null || victimName.isBlank()
                ? ""
                : " | vítima: " + victimName;

        return type.label()
                + victimText
                + " | " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                + (detail == null || detail.isBlank() ? "" : " | " + detail);
    }
}
