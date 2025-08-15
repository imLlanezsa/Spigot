package net.minecraft.server;

public class ChunkCache implements IBlockAccess {
    private static final IBlockData DEFAULT_BLOCK = Blocks.AIR.getBlockData(); // cSpigot - Lithium: ai/pathing/ChunkCache

    protected int a;
    protected int b;
    protected Chunk[][] c;
    protected boolean d;
    protected World e;

    // cSpigot start - Lithium: ai/pathing/ChunkCache
    // A 1D view of the chunks available to this cache
    private Chunk[] chunksFlat;

    // The x/z length of this cache
    private int xLen;
    private int zLen;
    // cSpigot end

    public ChunkCache(World world, BlockPosition blockposition, BlockPosition blockposition1, int i) {
        this.e = world;
        this.a = blockposition.getX() - i >> 4;
        this.b = blockposition.getZ() - i >> 4;
        int j = blockposition1.getX() + i >> 4;
        int k = blockposition1.getZ() + i >> 4;

        this.c = new Chunk[j - this.a + 1][k - this.b + 1];
        this.d = true;

        int l;
        int i1;

        for (l = this.a; l <= j; ++l) {
            for (i1 = this.b; i1 <= k; ++i1) {
                this.c[l - this.a][i1 - this.b] = world.getChunkAt(l, i1);
            }
        }

        for (l = blockposition.getX() >> 4; l <= blockposition1.getX() >> 4; ++l) {
            for (i1 = blockposition.getZ() >> 4; i1 <= blockposition1.getZ() >> 4; ++i1) {
                Chunk chunk = this.c[l - this.a][i1 - this.b];

                if (chunk != null && !chunk.c(blockposition.getY(), blockposition1.getY())) {
                    this.d = false;
                }
            }
        }

        // cSpigot start - Lithium: ai/pathing/ChunkCache
        this.xLen = 1 + (blockposition1.getX() >> 4) - (blockposition.getX() >> 4);
        this.zLen = 1 + (blockposition1.getZ() >> 4) - (blockposition.getZ() >> 4);

        this.chunksFlat = new Chunk[this.xLen * this.zLen];

        // Flatten the 2D chunk array into our 1D array
        for (int x = 0; x < this.xLen; x++) {
            System.arraycopy(this.c[x], 0, this.chunksFlat, x * this.zLen, this.zLen);
        }
        // cSpigot end
    }

    // WindSpigot start - reduce usage of blockposition
    public ChunkCache(World world, int x, int y, int z, int x1, int y1, int z1, int var4) {
        this.e = world;
        this.a = x - var4 >> 4;
        this.b = z - var4 >> 4;
        int var5 = x1 + var4 >> 4;
        int var6 = z1 + var4 >> 4;
        this.c = new Chunk[var5 - this.a + 1][var6 - this.b + 1];
        this.d = true;

        int var7;
        int var8;
        for (var7 = this.a; var7 <= var5; ++var7) {
            for (var8 = this.b; var8 <= var6; ++var8) {
                this.c[var7 - this.a][var8 - this.b] = world.getChunkAt(var7, var8);
            }
        }

        for (var7 = x >> 4; var7 <= x1 >> 4; ++var7) {
            for (var8 = z >> 4; var8 <= z1 >> 4; ++var8) {
                Chunk var9 = this.c[var7 - this.a][var8 - this.b];
                if (var9 != null && !var9.c(y, y1)) {
                    this.d = false;
                }
            }
        }

    }
    // WindSpigot end

    public TileEntity getTileEntity(BlockPosition blockposition) {
        int i = (blockposition.getX() >> 4) - this.a;
        int j = (blockposition.getZ() >> 4) - this.b;

        return this.c[i][j].a(blockposition, Chunk.EnumTileEntityState.IMMEDIATE);
    }

    public IBlockData getType(BlockPosition blockposition) {
        // cSpigot start - Lithium: ai/pathing/ChunkCache
        int y = blockposition.getY();
        if (y >= 0 && y< 256) {
            int x = blockposition.getX();
            int z = blockposition.getZ();

            int chunkX = (x >> 4) - this.a;
            int chunkZ = (z >> 4) - this.b;

            if (chunkX >= 0 && chunkX < this.xLen && chunkZ >= 0 && chunkZ < this.zLen) {
                Chunk chunk = this.chunksFlat[(chunkX * this.zLen) + chunkZ];

                // Avoid going through Chunk#getBlockState
                if (chunk != null) {
                    ChunkSection section = chunk.getSections()[y >> 4];

                    if (section != null) {
                        return section.getType(x & 15, y & 15, z & 15);
                    }
                    //return chunk.getBlockData(blockposition); // cSpigot - Lithium
                }
            }
        }

        return DEFAULT_BLOCK;
        // cSpigot end
    }

    public IBlockData getTypeWithChunkSafe(BlockPosition blockposition, Chunk chunk) {
        return this.getTypeWithChunkSafe(blockposition, chunk, false);
    }

    public IBlockData getTypeWithChunkSafe(BlockPosition blockposition, Chunk chunk, boolean extendedLoad) {
        // cSpigot start - Lithium: ai/pathing/ChunkCache
        int y = blockposition.getY();
        if (y >= 0 && y< 256) {
            int x = blockposition.getX();
            int z = blockposition.getZ();

            int chunkX = (x >> 4) - this.a;
            int chunkZ = (z >> 4) - this.b;

            if (chunkX >= 0 && chunkX < this.xLen && chunkZ >= 0 && chunkZ < this.zLen) {
                if (extendedLoad) {
                    chunk.extendChunkUnloadLate();
                }
                // Avoid going through Chunk#getBlockState
                if (chunk != null) {
                    ChunkSection section = chunk.getSections()[y >> 4];

                    if (section != null) {
                        return section.getType(x & 15, y & 15, z & 15);
                    }
                    //return chunk.getBlockData(blockposition); // cSpigot - Lithium
                }
            }
        }

        return DEFAULT_BLOCK;
        // cSpigot end
    }

    public boolean isEmpty(BlockPosition blockposition) {
        return this.getType(blockposition).getBlock().getMaterial() == Material.AIR;
    }

    public int getBlockPower(BlockPosition blockposition, EnumDirection enumdirection) {
        IBlockData iblockdata = this.getType(blockposition);

        return iblockdata.getBlock().b((IBlockAccess) this, blockposition, iblockdata, enumdirection);
    }
}
