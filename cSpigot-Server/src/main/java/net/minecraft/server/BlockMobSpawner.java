package net.minecraft.server;

import io.github.cspigot.config.SettingsConfig;

import java.util.Random;

public class BlockMobSpawner extends BlockContainer {

    private boolean place;

    protected BlockMobSpawner() {
        super(Material.STONE);
    }

    public TileEntity a(World world, int i) {
        return new TileEntityMobSpawner();
    }

    public Item getDropType(IBlockData iblockdata, Random random, int i) {
        return null;
    }

    public int a(Random random) {
        return 0;
    }

    public void dropNaturally(World world, BlockPosition blockposition, IBlockData iblockdata, float f, int i) {
        super.dropNaturally(world, blockposition, iblockdata, f, i);
        /* CraftBukkit start - Delegate to getExpDrop
        int j = 15 + world.random.nextInt(15) + world.random.nextInt(15);

        this.dropExperience(world, blockposition, j);
        */
    }


    @Override
    public void onPlace(World world, BlockPosition blockposition, IBlockData iblockdata) {
        super.onPlace(world, blockposition, iblockdata);
        if (SettingsConfig.SPAWNERS_ACT_AS_SPONGE) {
            world.a(blockposition, this, 0, 0);
        }
    }

    @Override
    public void doPhysics(World world, BlockPosition blockposition, IBlockData iblockdata, Block block) {
        if (SettingsConfig.SPAWNERS_ACT_AS_SPONGE && SettingsConfig.SPONGES_DISABLE_WATER_FLOW) {
            world.a(blockposition, this, 0, 0);
        }
    }

    @Override
    public void b(World world, BlockPosition blockposition, IBlockData iblockdata, Random random) {
        this.place = true;
        this.actAsSponge(world, blockposition);
        this.place = false;
    }

    private void actAsSponge(World world, BlockPosition blockPosition) {
        if (world.e) {
            return;
        }
        for ( int mx = blockPosition.getX() - SettingsConfig.SPONGE_RADIUS; mx <= blockPosition.getX() + SettingsConfig.SPONGE_RADIUS; ++mx ) {
            for ( int my = blockPosition.getY() - SettingsConfig.SPONGE_RADIUS; my <= blockPosition.getY() + SettingsConfig.SPONGE_RADIUS; ++my ) {
                for ( int mz = blockPosition.getZ() - SettingsConfig.SPONGE_RADIUS; mz <= blockPosition.getZ() + SettingsConfig.SPONGE_RADIUS; ++mz ) {
                    BlockPosition blockposition;
                    Block block;
                    if (mx == blockPosition.getX() && my == blockPosition.getY() && mz == blockPosition.getZ() || !(block = world.getType(blockposition = new BlockPosition(mx, my, mz)).getBlock()).getMaterial().isLiquid() && block != Blocks.AIR)
                        continue;
                    world.setTypeAndData(blockposition, Blocks.AIR.getBlockData(), this.place ? 3 : 2, false);
                }
            }
        }
    }


    @Override
    public int getExpDrop(World world, IBlockData iblockdata, int enchantmentLevel) {
        int j = 15 + world.random.nextInt(15) + world.random.nextInt(15);

        return j;
        // CraftBukkit end
    }

    public boolean c() {
        return false;
    }

    public int b() {
        return 3;
    }
}