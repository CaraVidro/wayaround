package net.caravidro.wayaround.dream;

import java.io.IOException;
import java.nio.file.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.IOUtilities;

/** Synchronous, atomic write-ahead backup; separate from autosaves and the player's copied inventory. */
public final class DreamJournal {
    public static final String MARKER="WayAroundDreamRollback";
    private DreamJournal() {}
    public static Path path(ServerPlayer player) {
        return player.server.getWorldPath(LevelResource.ROOT).resolve("wayaround-dream-backups").resolve(player.getStringUUID()+".dat");
    }
    public static CompoundTag capture(ServerPlayer player) throws IOException {
        player.getPersistentData();
        CompoundTag backup=new CompoundTag();
        backup.put("player",player.saveWithoutId(new CompoundTag()));
        backup.putString("dimension",player.level().dimension().location().toString());
        Files.createDirectories(path(player).getParent());
        IOUtilities.writeNbtCompressed(backup,path(player));
        return backup;
    }
    public static CompoundTag read(ServerPlayer player) throws IOException {
        return Files.isRegularFile(path(player))?NbtIo.readCompressed(path(player),NbtAccounter.create(32L*1024*1024)):null;
    }
    public static void finish(ServerPlayer player) throws IOException {
        // Vanilla's player save is synchronous. Verify its completion before removing the rollback journal.
        player.server.getPlayerList().saveAll();
        if(player.server.isSingleplayerOwner(player.getGameProfile())){
            // Integrated servers may reload the owner from level.dat instead of playerdata/<uuid>.dat.
            player.server.saveEverything(true,true,true);
            CompoundTag world=NbtIo.readCompressed(player.server.getWorldPath(LevelResource.ROOT).resolve("level.dat"),
                    NbtAccounter.create(64L*1024*1024));
            CompoundTag owner=world.getCompound("Data").getCompound("Player");
            if(owner.isEmpty()||owner.getCompound("NeoForgeData").getBoolean(MARKER)
                    ||!owner.getString("Dimension").equals(player.level().dimension().location().toString()))
                throw new IOException("Singleplayer owner save has not committed the restored state");
        }
        Path saved=player.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getStringUUID()+".dat");
        CompoundTag disk=NbtIo.readCompressed(saved,NbtAccounter.create(32L*1024*1024));
        if(disk.getCompound("NeoForgeData").getBoolean(MARKER)||!disk.getString("Dimension").equals(player.level().dimension().location().toString()))
            throw new IOException("Player save did not commit the restored state");
        Files.deleteIfExists(path(player));
    }
}
