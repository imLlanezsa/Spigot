package io.github.cspigot.api.chunk;

import net.minecraft.server.NibbleArray;

public class ChunkSectionSnapshot implements Cloneable {

    boolean isDirty; // PaperSpigot
    private int yPos;
    private int nonEmptyBlockCount;
    private int tickingBlockCount;
    private char[] blockIds;
    private NibbleArray emittedLight;
    private NibbleArray skyLight;

    public ChunkSectionSnapshot(int yPos, int nonEmptyBlockCount, int tickingBlockCount, char[] blockIds, NibbleArray emittedLight, NibbleArray skyLight, boolean isDirty) {
        this.yPos = yPos;
        this.nonEmptyBlockCount = nonEmptyBlockCount;
        this.tickingBlockCount = tickingBlockCount;
        this.blockIds = blockIds;
        this.emittedLight = emittedLight;
        this.skyLight = skyLight;
        this.isDirty = isDirty;
    }

    public int getyPos() {
        return yPos;
    }

    public int getNonEmptyBlockCount() {
        return nonEmptyBlockCount;
    }

    public int getTickingBlockCount() {
        return tickingBlockCount;
    }

    public char[] getBlockIds() {
        return blockIds;
    }

    public NibbleArray getEmittedLight() {
        return emittedLight;
    }

    public NibbleArray getSkyLight() {
        return skyLight;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public ChunkSectionSnapshot clone() {
        try {
            ChunkSectionSnapshot sectionSnapshot = (ChunkSectionSnapshot) super.clone();
            sectionSnapshot.blockIds = this.blockIds.clone();
            sectionSnapshot.skyLight = this.skyLight.cloneArray();
            sectionSnapshot.emittedLight = this.emittedLight.cloneArray();
            sectionSnapshot.yPos = this.yPos;
            sectionSnapshot.nonEmptyBlockCount = this.nonEmptyBlockCount;
            sectionSnapshot.tickingBlockCount = this.tickingBlockCount;
            return sectionSnapshot;
        } catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }

}
