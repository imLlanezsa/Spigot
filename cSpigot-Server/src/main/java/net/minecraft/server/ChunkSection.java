package net.minecraft.server;

import io.github.cspigot.api.chunk.ChunkSectionSnapshot;
import io.github.cspigot.feature.block.DataPaletteGlobal;

public class ChunkSection {

    public static final DataPaletteGlobal<IBlockData> GLOBAL_PALETTE = new DataPaletteGlobal<>(Block.d, Blocks.AIR.getBlockData());

    private int yPos;
    private int nonEmptyBlockCount;
    private int tickingBlockCount;
    private char[] blockIds;
    private NibbleArray emittedLight;
    private NibbleArray skyLight;
    boolean isDirty; // PaperSpigot

    final com.destroystokyo.paper.util.IBlockDataList tickingList = new com.destroystokyo.paper.util.IBlockDataList(); // Paper

    public ChunkSection(int i, boolean flag) {
        this.yPos = i;
        this.blockIds = new char[4096];
        this.emittedLight = new NibbleArray();
        if (flag) {
            this.skyLight = new NibbleArray();
        }
    }

    // CraftBukkit start
    public ChunkSection(int y, boolean flag, char[] blockIds) {
        this.yPos = y;
        this.blockIds = blockIds;
        this.emittedLight = new NibbleArray();
        if (flag) {
            this.skyLight = new NibbleArray();
        }
        recalcBlockCounts();
    }
    // CraftBukkit end

    public IBlockData getType(int i, int j, int k) {
        IBlockData iblockdata = Block.d.a(this.blockIds[j << 8 | k << 4 | i]);

        return iblockdata != null ? iblockdata : Blocks.AIR.getBlockData();
    }

    public void setType(int i, int j, int k, IBlockData iblockdata) {
        IBlockData iblockdata1 = this.getType(i, j, k);
        Block block = iblockdata1.getBlock();
        Block block1 = iblockdata.getBlock();

        if (block != Blocks.AIR) {
            --this.nonEmptyBlockCount;
            if (block.isTicking()) {
                --this.tickingBlockCount;
                // Paper start
                this.tickingList.remove(i, j, k);
                // Paper end
            }
        }

        if (block1 != Blocks.AIR) {
            ++this.nonEmptyBlockCount;
            if (block1.isTicking()) {
                ++this.tickingBlockCount;
                // Paper start
                this.tickingList.add(i, j, k, iblockdata);
                // Paper end
            }
        }

        this.blockIds[j << 8 | k << 4 | i] = (char) Block.d.b(iblockdata);
        isDirty = true; // PaperSpigot
    }

    public Block b(int i, int j, int k) {
        return this.getType(i, j, k).getBlock();
    }

    public int c(int i, int j, int k) {
        IBlockData iblockdata = this.getType(i, j, k);

        return iblockdata.getBlock().toLegacyData(iblockdata);
    }

    public boolean a() {
        return this.nonEmptyBlockCount == 0;
    }

    public boolean shouldTick() {
        return this.tickingBlockCount > 0;
    }

    public int getYPosition() {
        return this.yPos;
    }

    public void a(int i, int j, int k, int l) {
        this.skyLight.a(i, j, k, l);
        isDirty = true; // PaperSpigot
    }

    public int d(int i, int j, int k) {
        return this.skyLight.a(i, j, k);
    }

    public void b(int i, int j, int k, int l) {
        this.emittedLight.a(i, j, k, l);
        isDirty = true; // PaperSpigot
    }

    public int e(int i, int j, int k) {
        return this.emittedLight.a(i, j, k);
    }

    public void recalcBlockCounts() {
        // Paper start
        this.tickingList.clear();
        // Paper end
        this.nonEmptyBlockCount = 0;
        this.tickingBlockCount = 0;

        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                for (int k = 0; k < 16; ++k) {
                    IBlockData blockData = this.getType(i, j, k);
                    Block block = blockData.getBlock();

                    if (block != Blocks.AIR) {
                        this.nonEmptyBlockCount = (short) (this.nonEmptyBlockCount + 1);
                        if (block.isTicking()) {
                            this.tickingBlockCount = (short) (this.tickingBlockCount + 1);
                            // Paper start
                            this.tickingList.add(i, j, k, blockData);
                            // Paper end
                        }
                    }
                }
            }
        }

    }

    public char[] getIdArray() {
        return this.blockIds;
    }

    public void a(char[] achar) {
        this.blockIds = achar;
    }

    public NibbleArray getEmittedLightArray() {
        return this.emittedLight;
    }

    public NibbleArray getSkyLightArray() {
        return this.skyLight;
    }

    public void a(NibbleArray nibblearray) {
        this.emittedLight = nibblearray;
    }

    public void b(NibbleArray nibblearray) {
        this.skyLight = nibblearray;
    }

    // Kohi start - chunk snapshot api
    public ChunkSectionSnapshot createSnapshot() {
        return new ChunkSectionSnapshot(
                yPos,
                nonEmptyBlockCount,
                tickingBlockCount,
                clone(blockIds),
                clone(emittedLight),
                clone(skyLight),
                isDirty);
    }

    public void restoreSnapshot(ChunkSectionSnapshot snap) {
        yPos = snap.getyPos();
        nonEmptyBlockCount = snap.getNonEmptyBlockCount();
        tickingBlockCount = snap.getTickingBlockCount();
        blockIds = clone(snap.getBlockIds());
        emittedLight = clone(snap.getEmittedLight());
        skyLight = clone(snap.getSkyLight());
        isDirty = snap.isDirty();
    }
    // kohi end

    private static byte[] clone(byte[] array) {
        if (array != null) {
            return array.clone();
        }
        return null;
    }

    private static char[] clone(char[] array) {
        if (array != null) {
            return array.clone();
        }
        return null;
    }

    private static NibbleArray clone(NibbleArray array) {
        if (array != null) {
            return array.cloneArray();
        }
        return null;
    }
}
