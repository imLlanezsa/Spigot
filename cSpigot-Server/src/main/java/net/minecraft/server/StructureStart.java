package net.minecraft.server;

import com.google.common.collect.Lists;

import java.util.*;

public abstract class StructureStart {

    public LinkedList<StructurePiece> a = new LinkedList();
    public StructureBoundingBox b;
    private int c;
    private int d;

    public StructureStart() {
    }

    public StructureStart(int var1, int var2) {
        this.c = var1;
        this.d = var2;
    }

    public StructureBoundingBox a() {
        return this.b;
    }

    public LinkedList<StructurePiece> b() {
        return this.a;
    }

    public void a(World var1, Random var2, StructureBoundingBox var3) {
        Iterator var4 = this.a.iterator();

        synchronized (this.a) { // Paper - Synchronized
            while(var4.hasNext()) {
                StructurePiece var5 = (StructurePiece)var4.next();
                if (var5.c().a(var3) && !var5.a(var1, var2, var3)) {
                    var4.remove();
                }
            }
        } // Paper
    }

    protected void c() {
        this.b = StructureBoundingBox.a();
        Iterator var1 = this.a.iterator();
        synchronized (this.a) { // Paper - Synchronized
            while(var1.hasNext()) {
                StructurePiece var2 = (StructurePiece)var1.next();
                this.b.b(var2.c());
            }
        } // Paper
    }

    public NBTTagCompound a(int var1, int var2) {
        NBTTagCompound var3 = new NBTTagCompound();
        var3.setString("id", WorldGenFactory.a(this));
        var3.setInt("ChunkX", var1);
        var3.setInt("ChunkZ", var2);
        var3.set("BB", this.b.g());
        NBTTagList var4 = new NBTTagList();
        Iterator var5 = this.a.iterator();

        synchronized (this.a) { // Paper - Synchronized
            while(var5.hasNext()) {
                StructurePiece var6 = (StructurePiece)var5.next();
                var4.add(var6.b());
            }
        } // Paper

        var3.set("Children", var4);
        this.a(var3);
        return var3;
    }

    public void a(NBTTagCompound var1) {
    }

    public void a(World var1, NBTTagCompound var2) {
        this.c = var2.getInt("ChunkX");
        this.d = var2.getInt("ChunkZ");
        if (var2.hasKey("BB")) {
            this.b = new StructureBoundingBox(var2.getIntArray("BB"));
        }

        NBTTagList var3 = var2.getList("Children", 10);

        for(int var4 = 0; var4 < var3.size(); ++var4) {
            this.a.add(WorldGenFactory.b(var3.get(var4), var1));
        }

        this.b(var2);
    }

    public void b(NBTTagCompound var1) {
    }

    protected void a(World var1, Random var2, int var3) {
        int var4 = var1.F() - var3;
        int var5 = this.b.d() + 1;
        if (var5 < var4) {
            var5 += var2.nextInt(var4 - var5);
        }

        int var6 = var5 - this.b.e;
        this.b.a(0, var6, 0);
        Iterator var7 = this.a.iterator();

        synchronized (this.a) { // Paper - Synchronized
            while (var7.hasNext()) {
                StructurePiece var8 = (StructurePiece) var7.next();
                var8.a(0, var6, 0);
            }
        } // Paper

    }

    protected void a(World var1, Random var2, int var3, int var4) {
        int var5 = var4 - var3 + 1 - this.b.d();
        boolean var6 = true;
        int var10;
        if (var5 > 1) {
            var10 = var3 + var2.nextInt(var5);
        } else {
            var10 = var3;
        }

        int var7 = var10 - this.b.b;
        this.b.a(0, var7, 0);
        Iterator var8 = this.a.iterator();

        synchronized (this.a) { // Paper - Synchronized
            while (var8.hasNext()) {
                StructurePiece var9 = (StructurePiece) var8.next();
                var9.a(0, var7, 0);
            }
        } // Paper

    }

    public boolean d() {
        return true;
    }

    public boolean a(ChunkCoordIntPair var1) {
        return true;
    }

    public void b(ChunkCoordIntPair var1) {
    }

    public int e() {
        return this.c;
    }

    public int f() {
        return this.d;
    }
}
