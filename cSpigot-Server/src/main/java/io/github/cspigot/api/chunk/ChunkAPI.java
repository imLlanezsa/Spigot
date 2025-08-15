package io.github.cspigot.api.chunk;

import lombok.Getter;
import org.bukkit.Chunk;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 9/4/2023
 * @version cSpigot
 */
public class ChunkAPI {

    /**
     * Static instance that ppl can use for their plugins
     */
    @Getter
    private static ChunkAPI instance;

    public ChunkAPI() {
        ChunkAPI.instance = this;
    }

    /**
     * Take a Snapshot of all 16 sections of specified {@link Chunk}
     *
     * @param chunk {@link Chunk}
     * @return      {@link ChunkSnapshot Snapshot}
     */
    public ChunkSnapshot takeSnapshot(Chunk chunk) {
        return chunk.takeSnapshot();
    }

    /**
     * Restore the state of the specified {@link Chunk} with the
     * specified {@link ChunkSnapshot}
     *
     * @param chunk         {@link Chunk}
     * @param chunkSnapshot {@link ChunkSnapshot Snapshot}
     */
    public void restoreSnapshot(Chunk chunk, ChunkSnapshot chunkSnapshot) {
        chunk.restoreSnapshot(chunkSnapshot);
    }

}
