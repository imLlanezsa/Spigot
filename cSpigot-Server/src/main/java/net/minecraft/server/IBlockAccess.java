package net.minecraft.server;

public interface IBlockAccess {

    TileEntity getTileEntity(BlockPosition blockposition);

    IBlockData getType(BlockPosition blockposition);

    boolean isEmpty(BlockPosition blockposition);

    int getBlockPower(BlockPosition blockposition, EnumDirection enumdirection);

    IBlockData getTypeWithChunkSafe(BlockPosition var1, Chunk var2);

    IBlockData getTypeWithChunkSafe(BlockPosition var1, Chunk var2, boolean var3);
}
