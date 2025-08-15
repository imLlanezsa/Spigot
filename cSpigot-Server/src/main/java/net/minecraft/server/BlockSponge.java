package net.minecraft.server;

import com.google.common.collect.Lists;
import io.github.cspigot.config.SettingsConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

public class BlockSponge extends Block {

    public static final BlockStateBoolean WET = BlockStateBoolean.of("wet");
    private boolean place;

    protected BlockSponge() {
        super(Material.SPONGE);
        this.j(this.blockStateList.getBlockData().set(BlockSponge.WET, Boolean.valueOf(false)));
        this.a(CreativeModeTab.b);
    }

    public String getName() {
        return LocaleI18n.get(this.a() + ".dry.name");
    }

    public int getDropData(IBlockData iblockdata) {
        return ((Boolean) iblockdata.get(BlockSponge.WET)).booleanValue() ? 1 : 0;
    }

    public void onPlace(World world, BlockPosition blockposition, IBlockData iblockdata) {
        this.place = true;
        this.e(world, blockposition, iblockdata);
        this.place = false;
    }

    // cSpigot start - Optimize Sponges
    public void doPhysics(World world, BlockPosition blockposition, IBlockData iblockdata, Block block) {
        this.e(world, blockposition, iblockdata);
        super.doPhysics(world, blockposition, iblockdata, block);
    }

    protected void e(World world, BlockPosition blockposition, IBlockData iblockdata) {
        if (SettingsConfig.SPONGE_RADIUS > 0) {
            if (this.place) {
                world.a(blockposition, iblockdata.getBlock(), 2, 0);
            } else {
                world.a(blockposition, iblockdata.getBlock(), 20, 0);
            }
            return;
        }

        if (SettingsConfig.DISABLE_VANILLA_SPONGE) return;

        if (!((Boolean) iblockdata.get(BlockSponge.WET)).booleanValue() && this.e(world, blockposition)) {
            world.setTypeAndData(blockposition, iblockdata.set(BlockSponge.WET, Boolean.valueOf(true)), 2);
            world.triggerEffect(2001, blockposition, Block.getId(Blocks.WATER));
        }
    }

    public void b(World world, BlockPosition blockposition, IBlockData iblockdata, Random random) {
        this.place = true;
        this.actAsSponge(world, blockposition);
        this.place = false;
    }

    private void actAsSponge(World world, BlockPosition blockPosition) {
        if (world.e) {
            return;
        }
        for (int mx = blockPosition.getX() - SettingsConfig.SPONGE_RADIUS; mx <= blockPosition.getX() + SettingsConfig.SPONGE_RADIUS; ++mx) {
            for (int my = blockPosition.getY() - SettingsConfig.SPONGE_RADIUS; my <= blockPosition.getY() + SettingsConfig.SPONGE_RADIUS; ++my) {
                for (int mz = blockPosition.getZ() - SettingsConfig.SPONGE_RADIUS; mz <= blockPosition.getZ() + SettingsConfig.SPONGE_RADIUS; ++mz) {
                    BlockPosition blockposition;
                    Block block;
                    if (mx == blockPosition.getX() && my == blockPosition.getY() && mz == blockPosition.getZ() || !(block = world.getType(blockposition = new BlockPosition(mx, my, mz)).getBlock()).getMaterial().isLiquid() && block != Blocks.AIR) continue;
                    world.setTypeAndData(blockposition, Blocks.AIR.getBlockData(), SettingsConfig.SPONGE_DOES_BLOCK_UPDATES && this.place ? 3 : 2, false);
                }
            }
        }
    }
    // cSpigot end


    private boolean e(World world, BlockPosition blockposition) {
        LinkedList linkedlist = Lists.newLinkedList();
        ArrayList arraylist = Lists.newArrayList();

        linkedlist.add(new Tuple(blockposition, Integer.valueOf(0)));
        int i = 0;

        BlockPosition blockposition1;

        while (!linkedlist.isEmpty()) {
            Tuple tuple = (Tuple) linkedlist.poll();

            blockposition1 = (BlockPosition) tuple.a();
            int j = ((Integer) tuple.b()).intValue();
            EnumDirection[] aenumdirection = EnumDirection.values();
            int k = aenumdirection.length;

            for (int l = 0; l < k; ++l) {
                EnumDirection enumdirection = aenumdirection[l];
                BlockPosition blockposition2 = blockposition1.shift(enumdirection);

                if (world.getType(blockposition2).getBlock().getMaterial() == Material.WATER) {
                    world.setTypeAndData(blockposition2, Blocks.AIR.getBlockData(), 2);
                    arraylist.add(blockposition2);
                    ++i;
                    if (j < 6) {
                        linkedlist.add(new Tuple(blockposition2, Integer.valueOf(j + 1)));
                    }
                }
            }

            if (i > 64) {
                break;
            }
        }

        Iterator iterator = arraylist.iterator();

        while (iterator.hasNext()) {
            blockposition1 = (BlockPosition) iterator.next();
            world.applyPhysics(blockposition1, Blocks.AIR);
        }

        return i > 0;
    }

    public IBlockData fromLegacyData(int i) {
        return this.getBlockData().set(BlockSponge.WET, Boolean.valueOf((i & 1) == 1));
    }

    public int toLegacyData(IBlockData iblockdata) {
        return ((Boolean) iblockdata.get(BlockSponge.WET)).booleanValue() ? 1 : 0;
    }

    protected BlockStateList getStateList() {
        return new BlockStateList(this, new IBlockState[] { BlockSponge.WET});
    }
}
