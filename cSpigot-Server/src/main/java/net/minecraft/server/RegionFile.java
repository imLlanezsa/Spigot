package net.minecraft.server;

import com.google.common.collect.Lists;

import java.io.*;

import org.github.paperspigot.exception.ServerInternalException;

import javax.annotation.Nullable;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class RegionFile {

    private static final byte[] a = new byte[4096];
    private final File b;
    private RandomAccessFile c;
    private final int[] d = new int[1024];
    private final int[] e = new int[1024];
    private List<Boolean> f;
    private int g;
    private long h;
    private final boolean[] existingChunkCache = new boolean[1024];

    private boolean checkExistingChunkCache(int n, int n2) {
        return this.existingChunkCache[n + n2 * 32];
    }

    private void addCoordinatesToCache(int n, int n2) {
        this.existingChunkCache[n + n2 * 32] = true;
    }

    public RegionFile(File file) {
        this.b = file;
        this.g = 0;

        try {
            if (file.exists()) {
                this.h = file.lastModified();
            }

            this.c = new RandomAccessFile(file, "rw");
            if (this.c.length() < 4096L) {
                this.c.write(RegionFile.a);
                this.c.write(RegionFile.a);
                this.g += 8192;
            }

            int i;

            if ((this.c.length() & 4095L) != 0L) {
                for (i = 0; (long) i < (this.c.length() & 4095L); ++i) {
                    this.c.write(0);
                }
            }

            i = (int) this.c.length() / 4096;
            this.f = Lists.newArrayListWithCapacity(i);

            int j;

            for (j = 0; j < i; ++j) {
                this.f.add(true);
            }

            this.f.set(0, false);
            this.f.set(1, false);
            this.c.seek(0L);

            int k;
//            // Paper Start
//            java.nio.ByteBuffer header = java.nio.ByteBuffer.allocate(4096);
//            while (header.hasRemaining())  {
//                if (this.c.getChannel().read(header) == -1) throw new java.io.EOFException();
//            }
//            ClassUtils.clear(header);
//            java.nio.IntBuffer headerAsInts = header.asIntBuffer();
//            // Paper End

            for (j = 0; j < 1024; ++j) {
                this.d[j] = k = this.c.readInt();
                if (k == 0 || (k >> 8) + (k & 0xFF) > this.f.size()) continue;
                for (int l = 0; l < (k & 0xFF); ++l) {
                    this.f.set((k >> 8) + l, false);
                }
            }
            for (j = 0; j < 1024; ++j) {
                k = this.c.readInt();
                this.e[j] = k;
            }
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
            ServerInternalException.reportInternalException(ioexception); // Paper
        }

    }

    // CraftBukkit start - This is a copy (sort of) of the method below it, make sure they stay in sync
    public synchronized boolean chunkExists(int i, int j) {
        if (this.d(i, j)) {
            return false;
        }

        if (this.checkExistingChunkCache(i, j)) {
            return true;
        }

        try {
            int k = this.getOffset(i, j);

            if (k == 0) {
                return false;
            } else {
                int l = k >> 8;
                int i1 = k & 255;

                if (l + i1 > this.f.size()) {
                    return false;
                }

                this.c.seek(l * 4096L);
                int j1 = this.c.readInt();

                if (j1 > 4096 * i1 || j1 <= 0) {
                    return false;
                }

                byte b0 = this.c.readByte();
                if (b0 == 1 || b0 == 2) {
                    this.addCoordinatesToCache(i, j);
                    return true;
                }
            }
        } catch (IOException ioexception) {
            return false;
        }

        return false;
    }
    // CraftBukkit end

    @Nullable
    public synchronized DataInputStream getReadStream(int i, int j) { return a(i, j); }
    @Nullable public synchronized DataInputStream a(int i, int j) { // Paper - OBFHELPER
        if (this.d(i, j)) {
            return null;
        }

        try {
            int k = this.getOffset(i, j);
            if (k == 0) {
                return null;
            }

            int l = k >> 8;
            int i1 = k & 255;
            if (l + i1 > this.f.size()) {
                return null;
            }

            this.c.seek(l * 4096L);
            int j1 = this.c.readInt();

            if (j1 > 4096 * i1) {
                org.bukkit.Bukkit.getLogger().log(java.util.logging.Level.SEVERE, "Invalid chunk: ({0}, {1}) Offset: {2} Invalid Size: {3}>{4} {5}", new Object[]{i, j, l, j1, i1 * 4096, this.b}); // Spigot
                return null;
            }
            if (j1 <= 0) {
                org.bukkit.Bukkit.getLogger().log(java.util.logging.Level.SEVERE, "Invalid chunk: ({0}, {1}) Offset: {2} Invalid Size: {3} {4}", new Object[]{i, j, l, j1, this.b}); // Spigot
                return null;
            }

            byte b0 = this.c.readByte();
            byte[] abyte;

            if (b0 == 1) {
                this.addCoordinatesToCache(i, j);
                abyte = new byte[j1 - 1];
                this.c.read(abyte);
                return new DataInputStream(new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(abyte))));
            } else if (b0 == 2) {
                this.addCoordinatesToCache(i, j);
                abyte = new byte[j1 - 1];
                this.c.read(abyte);
                return new DataInputStream(new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(abyte))));
            }

            return null;
        } catch (IOException ioexception) {
            return null;
        }
    }

    public DataOutputStream writeStream(int i, int j) { return b(i, j); }
    public DataOutputStream b(int i, int j) { // PAIL: getChunkOutputStream
        // PAIL: isInvalidRegion
        return this.d(i, j) ? null : new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(new ChunkBuffer(i, j))));
    }

    protected synchronized void a(int i, int j, byte[] abyte, int k) {
        try {
            int l = this.getOffset(i, j);
            int i1 = l >> 8;  final int oldSectorOffset = i1; // Paper - store variable for later
            int j1 = l & 255; final int oldSectorCount; // Paper - store variable for later
            oldSectorCount = j1; // Paper - store variable for later (watch out for re-assignments of j1)
            int k1 = (k + 5) / 4096 + 1;

            if (k1 >= 256) {
                return;
            }

            int l1 = this.f.indexOf(true);
            int i2 = 0;
            int j2;

            if (l1 != -1) {
                for ( j2 = l1; j2 < this.f.size(); ++j2 ) {
                    if (i2 != 0) {
                        if ((Boolean) this.f.get(j2)) {
                            ++i2;
                        } else {
                            i2 = 0;
                        }
                    } else if ((Boolean) this.f.get(j2)) {
                        l1 = j2;
                        i2 = 1;
                    }

                    if (i2 >= k1) {
                        break;
                    }
                }
            }

            if (i2 >= k1) {
                i1 = l1;
                //this.a(i, j, l1 << 8 | (k1 > 255 ? 255 : k1)); // Spigot // Paper - We only write to header after we've written chunk data

                for ( j2 = 0; j2 < k1; ++j2 ) {
                    this.f.set(i1 + j2, false);
                }

                this.writeChunk(i, j, i1 << 8 | (k1 > 255 ? 255 : k1), i1, abyte, k); // Paper - Ensure we do not corrupt region files
            } else {
                this.c.seek(this.c.length());
                i1 = this.f.size();

                for ( j2 = 0; j2 < k1; ++j2 ) {
                    this.c.write(RegionFile.a);
                    this.f.add(false);
                }

                this.g += 4096 * k1;
                this.writeChunk(i, j, i1 << 8 | (k1 > 255 ? 255 : k1), i1, abyte, k); // Paper - Ensure we do not corrupt region files
            }

            // Paper start - Now that we've written the new chunk we can free the old data
            for (int off = 0; off < oldSectorCount; ++off) {
                this.f.set(oldSectorOffset + off, true);
            }
            // Paper end
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
            ServerInternalException.reportInternalException(ioexception); // Paper
        }
    }

    private void writeChunkData(final int sectorOffset, final byte[] data, final int dataLength) throws IOException { this.a(sectorOffset, data, dataLength); } // Paper - OBFHELPER
    private void a(int i, byte[] abyte, int j) throws IOException {
        this.c.seek(i * 4096);
        this.writeIntAndByte(j + 1, (byte)2); // Paper - Avoid 4 io write calls
        this.c.write(abyte, 0, j);
    }

    public boolean d(int i, int j) {
        return i < 0 || i >= 32 || j < 0 || j >= 32;
    }

    private int getOffset(int i, int j) {
        return this.d[i + j * 32];
    }

    public boolean c(int i, int j) {
        return this.getOffset(i, j) != 0;
    }

    private void updateChunkHeader(final int x, final int z, final int offset) throws IOException { this.a(x, z, offset); } // Paper - OBFHELPER
    private void a(int i, int j, int k) throws IOException {
        // Paper - move this to after the write
        this.c.seek((i + j * 32L) * 4);
        this.writeInt(k); // Paper - Avoid 3 io write calls
        this.d[i + j * 32] = k; // Paper - move this to after the write
    }

    private void updateChunkTime(final int x, final int z, final int time) throws IOException { this.b(x, z, time); } // Paper - OBFHELPER
    private void b(int i, int j, int k) throws IOException {
        // Paper - move this to after the write
        this.c.seek(4096 + (i + j * 32L) * 4);
        this.writeInt(k); // Paper - Avoid 3 io write calls
        this.e[i + j * 32] = k; // Paper - move this to after the write
    }

    public void c() throws IOException {
        if (this.c != null) {
            this.c.close();
        }

    }
    // Paper start
    private static final boolean FLUSH_ON_SAVE = Boolean.getBoolean("paper.flush-on-save");
    private void syncRegionFile() throws IOException {
        if (!FLUSH_ON_SAVE) {
            return;
        }
        this.c.getFD().sync(); // rethrow exception as we want to avoid corrupting a regionfile
    }

    private final java.nio.ByteBuffer scratchBuffer = java.nio.ByteBuffer.allocate(8);

    private void writeInt(final int value) throws IOException {
        synchronized (scratchBuffer) {
            this.scratchBuffer.putInt(0, value);
            this.c.write(this.scratchBuffer.array(), 0, 4);
        }
    }

    // writes v1 then v2
    private void writeIntAndByte(final int v1, final byte v2) throws IOException {
        synchronized (scratchBuffer) {
            this.scratchBuffer.putInt(0, v1);
            this.scratchBuffer.put(4, v2);
            this.c.write(this.scratchBuffer.array(), 0, 5);
        }
    }

    private void writeChunk(final int x, final int z, final int chunkHeaderData,
                            final int chunkOffset, final byte[] chunkData, final int chunkDataLength) throws IOException {
        this.writeChunkData(chunkOffset, chunkData, chunkDataLength);
        this.syncRegionFile(); // Sync is required to ensure the previous data is written successfully
        this.updateChunkTime(x, z, (int) (SystemUtils.getTimeMillis() / 1000L));
        this.updateChunkHeader(x, z, chunkHeaderData);
        this.syncRegionFile(); // Ensure header changes go through
    }
    // Paper end

    public class ChunkBuffer extends ByteArrayOutputStream {

        private int b;
        private int c;

        public ChunkBuffer(int i, int j) {
            super(8096);
            this.b = i;
            this.c = j;
        }

        public void close() {
            RegionFile.this.a(this.b, this.c, this.buf, this.count);
        }
    }
}
