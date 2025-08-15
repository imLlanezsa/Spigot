package net.minecraft.server;

import java.util.Random;

public class InventoryUtils {

    private static final Random a = new Random();

    public static void dropInventory(World world, BlockPosition blockposition, IInventory iinventory) {
        dropInventory(world, (double)blockposition.getX(), (double)blockposition.getY(), (double)blockposition.getZ(), iinventory);
    }

    public static void dropInventory(World world, BlockPosition blockposition, IInventory iinventory, boolean exploded) {
        dropInventory(world, (double)blockposition.getX(), (double)blockposition.getY(), (double)blockposition.getZ(), iinventory, exploded);
    }

    public static void dropEntity(World world, Entity entity, IInventory iinventory) {
        dropInventory(world, entity.locX, entity.locY, entity.locZ, iinventory);
    }

    private static void dropInventory(World world, double d0, double d1, double d2, IInventory iinventory) {
        dropInventory(world, d0, d1, d2, iinventory, false);
    }

    private static void dropInventory(World world, double d0, double d1, double d2, IInventory iinventory, boolean exploded) {
        for(int i = 0; i < iinventory.getSize(); ++i) {
            ItemStack itemstack = iinventory.getItem(i);
            if (itemstack != null && (!exploded || !(itemstack.getItem() instanceof ItemBlock) || ((ItemBlock)itemstack.getItem()).a != Blocks.TNT)) {
                dropItem(world, d0, d1, d2, itemstack);
            }
        }

    }

    private static void dropItem(World world, double d0, double d1, double d2, ItemStack itemstack) {
        dropItem(world, d0, d1, d2, itemstack, false);
    }

    private static void dropItem(World world, double d0, double d1, double d2, ItemStack itemstack, boolean fromDispenser) {
        float f = a.nextFloat() * 0.8F + 0.1F;
        float f1 = a.nextFloat() * 0.8F + 0.1F;
        float f2 = a.nextFloat() * 0.8F + 0.1F;

        while(itemstack.count > 0) {
            int i = a.nextInt(21) + 10;
            if (i > itemstack.count) {
                i = itemstack.count;
            }

            itemstack.count -= i;
            EntityItem entityitem = new EntityItem(world, d0 + (double)f, d1 + (double)f1, d2 + (double)f2, new ItemStack(itemstack.getItem(), i, itemstack.getData()));
            entityitem.fromDispenser = fromDispenser;
            if (itemstack.hasTag()) {
                entityitem.getItemStack().setTag((NBTTagCompound)itemstack.getTag().clone());
            }

            float f3 = 0.05F;
            entityitem.motX = a.nextGaussian() * (double)f3;
            entityitem.motY = a.nextGaussian() * (double)f3 + 0.20000000298023224;
            entityitem.motZ = a.nextGaussian() * (double)f3;
            world.addEntity(entityitem);
        }

    }
}
