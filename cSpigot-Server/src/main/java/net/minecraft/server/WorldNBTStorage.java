package net.minecraft.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.eatthepath.uuid.FastUUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.github.paperspigot.exception.ServerInternalException;
import io.github.cspigot.config.SettingsConfig;
// CraftBukkit end

public class WorldNBTStorage implements IDataManager, IPlayerFileData {

    private static final Logger a = LogManager.getLogger();
    private final File baseDir;
    private final File playerDir;
    private final File dataDir;
    private final long sessionId = MinecraftServer.az();
    private final String f;
    private UUID uuid = null; // CraftBukkit

    public WorldNBTStorage(File file, String s, boolean flag) {
        this.baseDir = new File(file, s);
        this.baseDir.mkdirs();
        this.playerDir = new File(this.baseDir, "playerdata");
        this.dataDir = new File(this.baseDir, "data");
        this.dataDir.mkdirs();
        this.f = s;
        if (flag) {
            this.playerDir.mkdirs();
        }

        this.h();
    }

    private void h() {
        try {
            File file = new File(this.baseDir, "session.lock");

            try (DataOutputStream dataoutputstream = new DataOutputStream(Files.newOutputStream(file.toPath()))) {
                dataoutputstream.writeLong(this.sessionId);
            }

        } catch (IOException ioexception) {
            ioexception.printStackTrace();
            throw new RuntimeException("Failed to check session lock for world located at " + this.baseDir + ", aborting. Stop the server and delete the session.lock in this world to prevent further issues."); // Spigot
        }
    }

    public File getDirectory() {
        return this.baseDir;
    }

    public void checkSession() throws ExceptionWorldConflict {
        try {
            File file = new File(this.baseDir, "session.lock");

            try (DataInputStream datainputstream = new DataInputStream(Files.newInputStream(file.toPath()))) {
                if (datainputstream.readLong() != this.sessionId) {
                    throw new ExceptionWorldConflict("The save for world located at " + this.baseDir + " is being accessed from another location, aborting");  // Spigot
                }
            }

        } catch (IOException ioexception) {
            throw new ExceptionWorldConflict("Failed to check session lock for world located at " + this.baseDir + ", aborting. Stop the server and delete the session.lock in this world to prevent further issues."); // Spigot
        }
    }

    public IChunkLoader createChunkLoader(WorldProvider worldprovider) {
        throw new RuntimeException("Old Chunk Storage is no longer supported.");
    }

    public WorldData getWorldData() {
        File file = new File(this.baseDir, "level.dat");
        NBTTagCompound nbttagcompound;
        NBTTagCompound nbttagcompound1;

        if (file.exists()) {
            try {
                nbttagcompound = NBTCompressedStreamTools.a(Files.newInputStream(file.toPath()));
                nbttagcompound1 = nbttagcompound.getCompound("Data");
                return new WorldData(nbttagcompound1);
            } catch (Exception exception) {
                exception.printStackTrace();
                ServerInternalException.reportInternalException(exception); // Paper
            }
        }

        file = new File(this.baseDir, "level.dat_old");
        if (file.exists()) {
            try {
                nbttagcompound = NBTCompressedStreamTools.a(Files.newInputStream(file.toPath()));
                nbttagcompound1 = nbttagcompound.getCompound("Data");
                return new WorldData(nbttagcompound1);
            } catch (Exception exception1) {
                exception1.printStackTrace();
                ServerInternalException.reportInternalException(exception1); // Paper
            }
        }

        return null;
    }

    public void saveWorldData(WorldData worlddata, NBTTagCompound nbttagcompound) {
        NBTTagCompound nbttagcompound1 = worlddata.a(nbttagcompound);
        NBTTagCompound nbttagcompound2 = new NBTTagCompound();

        nbttagcompound2.set("Data", nbttagcompound1);

        try {
            File file = new File(this.baseDir, "level.dat_new");
            File file1 = new File(this.baseDir, "level.dat_old");
            File file2 = new File(this.baseDir, "level.dat");

            NBTCompressedStreamTools.a(nbttagcompound2, (OutputStream) (new FileOutputStream(file)));
            if (file1.exists()) {
                file1.delete();
            }

            file2.renameTo(file1);
            if (file2.exists()) {
                file2.delete();
            }

            file.renameTo(file2);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            ServerInternalException.reportInternalException(exception); // Paper
        }

    }

    public void saveWorldData(WorldData worlddata) {
        NBTTagCompound nbttagcompound = worlddata.a();
        NBTTagCompound nbttagcompound1 = new NBTTagCompound();

        nbttagcompound1.set("Data", nbttagcompound);

        try {
            File file = new File(this.baseDir, "level.dat_new");
            File file1 = new File(this.baseDir, "level.dat_old");
            File file2 = new File(this.baseDir, "level.dat");

            NBTCompressedStreamTools.a(nbttagcompound1, Files.newOutputStream(file.toPath()));
            if (file1.exists()) {
                file1.delete();
            }

            file2.renameTo(file1);
            if (file2.exists()) {
                file2.delete();
            }

            file.renameTo(file2);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            ServerInternalException.reportInternalException(exception); // Paper
        }

    }
    // cSpigot end

    public void save(EntityHuman entityhuman) {
        if (SettingsConfig.DISABLE_PLAYER_SAVE) return;

        try {
            NBTTagCompound nbttagcompound = new NBTTagCompound();

            entityhuman.e(nbttagcompound);

            File file = new File(this.playerDir, entityhuman.getUniqueID().toString() + ".dat.tmp");
            File file1 = new File(this.playerDir, entityhuman.getUniqueID().toString() + ".dat");

            NBTCompressedStreamTools.a(nbttagcompound, (OutputStream) (new FileOutputStream(file)));
            if (file1.exists()) {
                file1.delete();
            }

            file.renameTo(file1);
        } catch (Exception exception) {
            WorldNBTStorage.a.warn("Failed to save player data for " + entityhuman.getName());
        }

    }

    public NBTTagCompound load(EntityHuman entityhuman) {
        NBTTagCompound nbttagcompound = null;

        try {
            File file = new File(this.playerDir, FastUUID.toString(entityhuman.getUniqueID()) + ".dat");
            // Spigot Start
            boolean usingWrongFile = false;
            boolean normalFile = file.isFile(); // Akarin - ensures normal file // Nacho - Remove file.exists, Thanks bob7l :)

            if ( org.bukkit.Bukkit.getOnlineMode() && !normalFile) // Paper - Check online mode first // Akarin - ensures normal file
            {
                file = new File( this.playerDir, FastUUID.toString(UUID.nameUUIDFromBytes( ( "OfflinePlayer:" + entityhuman.getName() ).getBytes(StandardCharsets.UTF_8) )) + ".dat");
                if ( file.exists() )
                {
                    usingWrongFile = true;
                    org.bukkit.Bukkit.getServer().getLogger().warning( "Using offline mode UUID file for player " + entityhuman.getName() + " as it is the only copy we can find." );
                }
            }
            // Spigot End

            if (normalFile) { // Akarin - Avoid Double IO Operation
                nbttagcompound = NBTCompressedStreamTools.a(Files.newInputStream(file.toPath()));
            }
            // Spigot Start
            if ( usingWrongFile )
            {
                file.renameTo( new File( file.getPath() + ".offline-read" ) );
            }
            // Spigot End
        } catch (Exception exception) {
            WorldNBTStorage.a.warn("Failed to load player data for " + entityhuman.getName());
        }

        if (nbttagcompound != null) {
            // CraftBukkit start
            if (entityhuman instanceof EntityPlayer) {
                CraftPlayer player = (CraftPlayer) entityhuman.getBukkitEntity();
                // Only update first played if it is older than the one we have
                long modified = new File(this.playerDir, FastUUID.toString(entityhuman.getUniqueID()) + ".dat").lastModified();
                if (modified < player.getFirstPlayed()) {
                    player.setFirstPlayed(modified);
                }
            }
            // CraftBukkit end

            entityhuman.f(nbttagcompound);
        }

        return nbttagcompound;
    }

    // CraftBukkit start
    public NBTTagCompound getPlayerData(String s) {
        try {
            File file1 = new File(this.playerDir, s + ".dat");

            if (file1.exists()) {
                return NBTCompressedStreamTools.a((InputStream) (new FileInputStream(file1)));
            }
        } catch (Exception exception) {
            a.warn("Failed to load player data for " + s);
        }

        return null;
    }
    // CraftBukkit end

    public IPlayerFileData getPlayerFileData() {
        return this;
    }

    public String[] getSeenPlayers() {
        String[] astring = this.playerDir.list();

        if (astring == null) {
            astring = new String[0];
        }

        for (int i = 0; i < astring.length; ++i) {
            if (astring[i].endsWith(".dat")) {
                astring[i] = astring[i].substring(0, astring[i].length() - 4);
            }
        }

        return astring;
    }

    public void a() {}

    public File getDataFile(String s) {
        return new File(this.dataDir, s + ".dat");
    }

    public String g() {
        return this.f;
    }

    // CraftBukkit start
    public UUID getUUID() {
        if (uuid != null) return uuid;
        File file1 = new File(this.baseDir, "uid.dat");
        Path path = file1.toPath();
        if (file1.exists()) {
            try (DataInputStream dis = new DataInputStream(Files.newInputStream(path))) {
                return uuid = new UUID(dis.readLong(), dis.readLong());
            } catch (IOException ex) {
                a.warn("Failed to read " + file1 + ", generating new random UUID", ex);
            }
            // NOOP
        }
        uuid = UUID.randomUUID();
        try (DataOutputStream dos = new DataOutputStream(Files.newOutputStream(path))) {
            dos.writeLong(uuid.getMostSignificantBits());
            dos.writeLong(uuid.getLeastSignificantBits());
        } catch (IOException ex) {
            a.warn("Failed to write " + file1, ex);
        }
        // NOOP
        return uuid;
    }

    public File getPlayerDir() {
        return playerDir;
    }
    // CraftBukkit end
}
