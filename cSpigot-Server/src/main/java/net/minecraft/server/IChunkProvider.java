package net.minecraft.server;

import java.util.List;
import java.util.function.BooleanSupplier;

public interface IChunkProvider {

    boolean isChunkLoaded(int i, int j);

    Chunk getOrCreateChunk(int i, int j);

    Chunk getChunkAt(BlockPosition blockposition);

    void getChunkAt(IChunkProvider ichunkprovider, int i, int j);

    boolean a(IChunkProvider ichunkprovider, Chunk chunk, int i, int j);

    boolean saveChunks(boolean flag, IProgressUpdate iprogressupdate);

    default boolean unloadChunks(BooleanSupplier supplier) {
        return unloadChunks();
    }

    boolean unloadChunks();

    boolean canSave();

    String getName();

    List<BiomeBase.BiomeMeta> getMobsFor(EnumCreatureType enumcreaturetype, BlockPosition blockposition);

    BlockPosition findNearestMapFeature(World world, String s, BlockPosition blockposition);

    int getLoadedChunks();

    void recreateStructures(Chunk chunk, int i, int j);

    void c();
}
