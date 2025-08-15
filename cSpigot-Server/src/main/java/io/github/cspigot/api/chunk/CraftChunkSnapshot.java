package io.github.cspigot.api.chunk;

import net.minecraft.server.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

public class CraftChunkSnapshot implements ChunkSnapshot {

    private final ChunkSectionSnapshot[] sections = new ChunkSectionSnapshot[16];

    private final List<NBTTagCompound> tileEntities = new ArrayList<>();

    /**
     * Get all {@link ChunkSectionSnapshot} of a Snapshot
     *
     * @return {@link ChunkSectionSnapshot[] sections}
     */
    public ChunkSectionSnapshot[] getSections() {
        return this.sections;
    }

    /**
     * Get a list of all title entities in the snapshot
     * in nbt-tag form.
     *
     * @return {@link List list of tile entities}
     */
    public List<NBTTagCompound> getTileEntities() {
        return this.tileEntities;
    }
}
