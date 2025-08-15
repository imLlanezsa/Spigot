package net.minecraft.server;

public class BlockBeacon extends BlockContainer {
    public BlockBeacon() {
        super(Material.SHATTERABLE, MaterialMapColor.G);
        this.c(3.0F);
        this.a(CreativeModeTab.f);
    }

    public TileEntity a(World var1, int var2) {
        return new TileEntityBeacon();
    }

    public boolean interact(World var1, BlockPosition var2, IBlockData var3, EntityHuman var4, EnumDirection var5, float var6, float var7, float var8) {
        if (var1.isClientSide) {
            return true;
        } else {
            TileEntity var9 = var1.getTileEntity(var2);
            if (var9 instanceof TileEntityBeacon) {
                var4.openContainer((TileEntityBeacon)var9);
                var4.b(StatisticList.N);
            }

            return true;
        }
    }

    public boolean c() {
        return false;
    }

    public boolean d() {
        return false;
    }

    public int b() {
        return 3;
    }

    public void postPlace(World var1, BlockPosition var2, IBlockData var3, EntityLiving var4, ItemStack var5) {
        super.postPlace(var1, var2, var3, var4, var5);
        if (var5.hasName()) {
            TileEntity var6 = var1.getTileEntity(var2);
            if (var6 instanceof TileEntityBeacon) {
                ((TileEntityBeacon)var6).a(var5.getName());
            }
        }

    }

    public void doPhysics(World var1, BlockPosition var2, IBlockData var3, Block var4) {
        TileEntity var5 = var1.getTileEntity(var2);
        if (var5 instanceof TileEntityBeacon) {
            ((TileEntityBeacon)var5).m();
            var1.playBlockAction(var2, this, 1, 0);
        }

    }

    public static void f(World world, BlockPosition blockposition) {
        Chunk chunk = world.getChunkAtWorldCoords(blockposition);
        for (int i = blockposition.getY() - 1; i >= 0; --i) {
            final BlockPosition blockposition1 = new BlockPosition(blockposition.getX(), i, blockposition.getZ());
            if (!chunk.d(blockposition1)) break;

            IBlockData iblockdata = world.getType(blockposition1);
            if (iblockdata.getBlock() == Blocks.BEACON) {
                TileEntity tileentity = world.getTileEntity(blockposition1);
                if (tileentity  instanceof TileEntityBeacon) {
                    ((TileEntityBeacon)tileentity ).m();
                    world.playBlockAction(blockposition1, Blocks.BEACON, 1, 0);
                }
            }
        }
    }
}

