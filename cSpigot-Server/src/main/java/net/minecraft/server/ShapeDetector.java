package net.minecraft.server;

import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Iterator;

public class ShapeDetector {

    private final Predicate<ShapeDetectorBlock>[][][] a;
    private final int b;
    private final int c;
    private final int d;

    public ShapeDetector(Predicate<ShapeDetectorBlock>[][][] apredicate) {
        this.a = apredicate;
        this.b = apredicate.length;
        if (this.b > 0) {
            this.c = apredicate[0].length;
            if (this.c > 0) {
                this.d = apredicate[0][0].length;
            } else {
                this.d = 0;
            }
        } else {
            this.c = 0;
            this.d = 0;
        }

    }

    public int b() {
        return this.c;
    }

    public int c() {
        return this.d;
    }

    private ShapeDetector.ShapeDetectorCollection a(BlockPosition blockposition, EnumDirection enumdirection, EnumDirection enumdirection1, LoadingCache<BlockPosition, ShapeDetectorBlock> loadingcache) {
        for (int i = 0; i < this.d; ++i) {
            for (int j = 0; j < this.c; ++j) {
                for (int k = 0; k < this.b; ++k) {
                    if (!this.a[k][j][i].apply(loadingcache.getUnchecked(a(blockposition, enumdirection, enumdirection1, i, j, k)))) {
                        return null;
                    }
                }
            }
        }

        return new ShapeDetector.ShapeDetectorCollection(blockposition, enumdirection, enumdirection1, loadingcache, this.d, this.c, this.b);
    }

    // cSpigot Start: Optimize
    public ShapeDetector.ShapeDetectorCollection a(World world, BlockPosition blockposition) {
        LoadingCache<BlockPosition, ShapeDetectorBlock> loadingcache = a(world, false);
        int i = Math.max(Math.max(this.d, this.c), this.b);

        Iterator<BlockPosition> iterator = BlockPosition.a(blockposition, blockposition.a(i - 1, i - 1, i - 1)).iterator();

        while (iterator.hasNext()) {
            BlockPosition blockposition1 = iterator.next();
            EnumDirection[] aenumdirection = EnumDirection.values();

            for (EnumDirection enumdirection : aenumdirection) {
                EnumDirection[] aenumdirection1 = EnumDirection.values();

                for (EnumDirection enumdirection1 : aenumdirection1) {
                    if (enumdirection1 != enumdirection && enumdirection1 != enumdirection.opposite()) {
                        ShapeDetectorCollection shapedetector_shapedetectorcollection = this.a(blockposition1, enumdirection, enumdirection1, loadingcache);

                        if (shapedetector_shapedetectorcollection != null) {
                            return shapedetector_shapedetectorcollection;
                        }
                    }
                }
            }
        }

        return null;
    }
    // cSpigot End

    public static LoadingCache<BlockPosition, ShapeDetectorBlock> a(World world, boolean flag) {
        return CacheBuilder.newBuilder().build(new ShapeDetector.BlockLoader(world, flag));
    }

    protected static BlockPosition a(BlockPosition blockposition, EnumDirection enumdirection, EnumDirection enumdirection1, int i, int j, int k) {
        if (enumdirection != enumdirection1 && enumdirection != enumdirection1.opposite()) {
            BaseBlockPosition baseblockposition = new BaseBlockPosition(enumdirection.getAdjacentX(), enumdirection.getAdjacentY(), enumdirection.getAdjacentZ());
            BaseBlockPosition baseblockposition1 = new BaseBlockPosition(enumdirection1.getAdjacentX(), enumdirection1.getAdjacentY(), enumdirection1.getAdjacentZ());
            BaseBlockPosition baseblockposition2 = baseblockposition.d(baseblockposition1);

            return blockposition.a(baseblockposition1.getX() * -j + baseblockposition2.getX() * i + baseblockposition.getX() * k, baseblockposition1.getY() * -j + baseblockposition2.getY() * i + baseblockposition.getY() * k, baseblockposition1.getZ() * -j + baseblockposition2.getZ() * i + baseblockposition.getZ() * k);
        } else {
            throw new IllegalArgumentException("Invalid forwards & up combination");
        }
    }

    public static class ShapeDetectorCollection {

        private final BlockPosition a;
        private final EnumDirection b;
        private final EnumDirection c;
        private final LoadingCache<BlockPosition, ShapeDetectorBlock> d;
        private final int e;
        private final int f;
        private final int g;

        public ShapeDetectorCollection(BlockPosition blockposition, EnumDirection enumdirection, EnumDirection enumdirection1, LoadingCache<BlockPosition, ShapeDetectorBlock> loadingcache, int i, int j, int k) {
            this.a = blockposition;
            this.b = enumdirection;
            this.c = enumdirection1;
            this.d = loadingcache;
            this.e = i;
            this.f = j;
            this.g = k;
        }

        public BlockPosition a() {
            return this.a;
        }

        public EnumDirection b() {
            return this.b;
        }

        public EnumDirection c() {
            return this.c;
        }

        public int d() {
            return this.e;
        }

        public int e() {
            return this.f;
        }

        public ShapeDetectorBlock a(int i, int j, int k) {
            return (ShapeDetectorBlock) this.d.getUnchecked(ShapeDetector.a(this.a, this.b(), this.c(), i, j, k));
        }

        public String toString() {
            return MoreObjects.toStringHelper(this).add("up", this.c).add("forwards", this.b).add("frontTopLeft", this.a).toString();
        }
    }

    static class BlockLoader extends CacheLoader<BlockPosition, ShapeDetectorBlock> {

        private final World a;
        private final boolean b;

        public BlockLoader(World world, boolean flag) {
            this.a = world;
            this.b = flag;
        }

        public ShapeDetectorBlock a(BlockPosition blockposition) throws Exception {
            return new ShapeDetectorBlock(this.a, blockposition, this.b);
        }

        public ShapeDetectorBlock load(BlockPosition object) throws Exception {
            return this.a(object);
        }
    }
}
