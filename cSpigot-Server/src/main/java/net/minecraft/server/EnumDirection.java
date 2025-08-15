package net.minecraft.server;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import lombok.Getter;

import java.util.*;

public enum EnumDirection implements INamable {

    DOWN(0, 1, -1, 1,"down", EnumDirection.EnumAxisDirection.NEGATIVE, EnumDirection.EnumAxis.Y, new BaseBlockPosition(0, -1, 0)),
    UP(1, 0, -1, 2,"up", EnumDirection.EnumAxisDirection.POSITIVE, EnumDirection.EnumAxis.Y, new BaseBlockPosition(0, 1, 0)),
    NORTH(2, 3, 2, 4,"north", EnumDirection.EnumAxisDirection.NEGATIVE, EnumDirection.EnumAxis.Z, new BaseBlockPosition(0, 0, -1)),
    SOUTH(3, 2, 0, 8,"south", EnumDirection.EnumAxisDirection.POSITIVE, EnumDirection.EnumAxis.Z, new BaseBlockPosition(0, 0, 1)),
    WEST(4, 5, 1, 16,"west", EnumDirection.EnumAxisDirection.NEGATIVE, EnumDirection.EnumAxis.X, new BaseBlockPosition(-1, 0, 0)),
    EAST(5, 4, 3, 32, "east", EnumDirection.EnumAxisDirection.POSITIVE, EnumDirection.EnumAxis.X, new BaseBlockPosition(1, 0, 0));

    private static final EnumDirection[] n = new EnumDirection[6];
    private static final EnumDirection[] o = new EnumDirection[4];
    private static final List<EnumDirection>[] bitCombinations = new List[64];
    private static final Map<String, EnumDirection> p = new Object2ObjectArrayMap<>(); // cSpigot: Changed map type

    static {
        // cSpigot Start
        EnumDirection[] values = values();
        for (EnumDirection enumdirection : values) {
            EnumDirection.n[enumdirection.g] = enumdirection;
            if (enumdirection.k().c()) {
                EnumDirection.o[enumdirection.i] = enumdirection;
            }

            EnumDirection.p.put(enumdirection.j().toLowerCase(), enumdirection);
        }

        int bit = 0;
        while (bit < 64) {
            List<EnumDirection> directions = new ArrayList<>();
            for (EnumDirection ed : values) {
                if ((ed.getDirectionBit() & bit) <= 0) continue;
                directions.add(ed);
            }
            EnumDirection.bitCombinations[bit] = directions;
            ++bit;
        }
        // cSpigot End
    }

    private final int g;
    private final int h;
    private final int i;
    private final int directionBit;
    private final String j;
    private final EnumDirection.EnumAxis k;
    private final EnumDirection.EnumAxisDirection l;
    @Getter // cSpigot: Added getter
    private final BaseBlockPosition m;

    EnumDirection(int i, int j, int k, int directionBit, String s, EnumDirection.EnumAxisDirection enumdirection_enumaxisdirection, EnumDirection.EnumAxis enumdirection_enumaxis, BaseBlockPosition baseblockposition) {
        this.g = i;
        this.i = k;
        this.h = j;
        this.directionBit = directionBit;
        this.j = s;
        this.k = enumdirection_enumaxis;
        this.l = enumdirection_enumaxisdirection;
        this.m = baseblockposition;
    }

    // cSpigot - Added direction bit for RedstoneProvider
    public int getDirectionBit() {
        return this.directionBit;
    }
    // cSpigot end

    public static EnumDirection fromType1(int i) {
        return EnumDirection.n[MathHelper.a(i % EnumDirection.n.length)];
    }

    public static EnumDirection fromType2(int i) {
        return EnumDirection.o[MathHelper.a(i % EnumDirection.o.length)];
    }

    public static EnumDirection fromAngle(double d0) {
        return fromType2(MathHelper.floor(d0 / 90.0D + 0.5D) & 3);
    }

    public static EnumDirection a(Random random) {
        return values()[random.nextInt(values().length)];
    }

    public static EnumDirection a(EnumDirection.EnumAxisDirection enumdirection_enumaxisdirection, EnumDirection.EnumAxis enumdirection_enumaxis) {
        EnumDirection[] aenumdirection = values();
        int i = aenumdirection.length;

        for (int j = 0; j < i; ++j) {
            EnumDirection enumdirection = aenumdirection[j];

            if (enumdirection.c() == enumdirection_enumaxisdirection && enumdirection.k() == enumdirection_enumaxis) {
                return enumdirection;
            }
        }

        throw new IllegalArgumentException("No such direction: " + enumdirection_enumaxisdirection + " " + enumdirection_enumaxis);
    }

    public int a() {
        return this.g;
    }

    public int b() {
        return this.i;
    }

    public EnumDirection.EnumAxisDirection c() {
        return this.l;
    }

    public EnumDirection opposite() {
        return fromType1(this.h);
    }

    public EnumDirection e() {
        switch (EnumDirection.SyntheticClass_1.b[this.ordinal()]) {
            case 1:
                return EnumDirection.EAST;

            case 2:
                return EnumDirection.SOUTH;

            case 3:
                return EnumDirection.WEST;

            case 4:
                return EnumDirection.NORTH;

            default:
                throw new IllegalStateException("Unable to get Y-rotated facing of " + this);
        }
    }

    public EnumDirection f() {
        switch (EnumDirection.SyntheticClass_1.b[this.ordinal()]) {
            case 1:
                return EnumDirection.WEST;

            case 2:
                return EnumDirection.NORTH;

            case 3:
                return EnumDirection.EAST;

            case 4:
                return EnumDirection.SOUTH;

            default:
                throw new IllegalStateException("Unable to get CCW facing of " + this);
        }
    }

    // cSpigot start
    public static List<EnumDirection> getAll(int bits) {
        return bitCombinations[bits];
    }
    // cSpigot end

    public int getAdjacentX() {
        return this.k == EnumDirection.EnumAxis.X ? this.l.a() : 0;
    }

    public int getAdjacentY() {
        return this.k == EnumDirection.EnumAxis.Y ? this.l.a() : 0;
    }

    public int getAdjacentZ() {
        return this.k == EnumDirection.EnumAxis.Z ? this.l.a() : 0;
    }

    public String j() {
        return this.j;
    }

    public EnumDirection.EnumAxis k() {
        return this.k;
    }

    public String toString() {
        return this.j;
    }

    public String getName() {
        return this.j;
    }

    public enum EnumDirectionLimit implements Predicate<EnumDirection>, Iterable<EnumDirection> {

        HORIZONTAL, VERTICAL, HORIZONTALFIRST, VERTICALFIRST;

        EnumDirectionLimit() {
        }

        private EnumDirection[] directions;

        public EnumDirection[] getDirections() {
            if (this.directions == null) {
                this.generateArray();
            }

            return this.directions;
        }

        private void generateArray() {
            switch (this.ordinal()) {
                case 0:
                    this.directions = new EnumDirection[]{EnumDirection.NORTH, EnumDirection.EAST, EnumDirection.SOUTH, EnumDirection.WEST};
                    break;
                case 1:
                    this.directions = new EnumDirection[]{EnumDirection.UP, EnumDirection.DOWN};
                    break;
                case 2:
                    this.directions = new EnumDirection[]{EnumDirection.SOUTH, EnumDirection.WEST, EnumDirection.NORTH, EnumDirection.EAST, EnumDirection.UP, EnumDirection.DOWN};
                    break;
                case 3:
                    this.directions = new EnumDirection[]{EnumDirection.UP, EnumDirection.DOWN, EnumDirection.SOUTH, EnumDirection.WEST, EnumDirection.NORTH, EnumDirection.EAST};
                    break;
                default:
                    throw new Error("Someone's been tampering with the universe! " + this.ordinal());
            }

        }

        public EnumDirection[] a() {
            switch (SyntheticClass_1.c[this.ordinal()]) {
                case 1:
                    return new EnumDirection[]{EnumDirection.NORTH, EnumDirection.EAST, EnumDirection.SOUTH, EnumDirection.WEST};
                case 2:
                    return new EnumDirection[]{EnumDirection.UP, EnumDirection.DOWN};
                case 3:
                    return new EnumDirection[]{EnumDirection.SOUTH, EnumDirection.WEST, EnumDirection.NORTH, EnumDirection.EAST, EnumDirection.UP, EnumDirection.DOWN};
                case 4:
                    return new EnumDirection[]{EnumDirection.UP, EnumDirection.DOWN, EnumDirection.SOUTH, EnumDirection.WEST, EnumDirection.NORTH, EnumDirection.EAST};
                default:
                    throw new Error("Someone's been tampering with the universe!");
            }
        }


        public EnumDirection a(Random random) {
            EnumDirection[] aenumdirection = this.a();

            return aenumdirection[random.nextInt(aenumdirection.length)];
        }

        public boolean a(EnumDirection enumdirection) {
            return enumdirection != null && enumdirection.k().d() == this;
        }

        public Iterator<EnumDirection> iterator() {
            return Iterators.forArray(this.a());
        }

        public boolean apply(EnumDirection object) {
            return this.a(object);
        }
    }

    public enum EnumAxisDirection {

        POSITIVE(1, "Towards positive"), NEGATIVE(-1, "Towards negative");

        private final int c;
        private final String d;

        EnumAxisDirection(int i, String s) {
            this.c = i;
            this.d = s;
        }

        public int a() {
            return this.c;
        }

        public String toString() {
            return this.d;
        }
    }

    public enum EnumAxis implements Predicate<EnumDirection>, INamable {

        X("x", EnumDirection.EnumDirectionLimit.HORIZONTAL), Y("y", EnumDirection.EnumDirectionLimit.VERTICAL), Z("z", EnumDirection.EnumDirectionLimit.HORIZONTAL);

        private static final Map<String, EnumDirection.EnumAxis> d = Maps.newHashMap();

        static {
            EnumDirection.EnumAxis[] aenumdirection_enumaxis = values();
            int i = aenumdirection_enumaxis.length;

            for (int j = 0; j < i; ++j) {
                EnumDirection.EnumAxis enumdirection_enumaxis = aenumdirection_enumaxis[j];

                EnumDirection.EnumAxis.d.put(enumdirection_enumaxis.a().toLowerCase(), enumdirection_enumaxis);
            }

        }

        private final String e;
        private final EnumDirection.EnumDirectionLimit f;

        EnumAxis(String s, EnumDirection.EnumDirectionLimit enumdirection_enumdirectionlimit) {
            this.e = s;
            this.f = enumdirection_enumdirectionlimit;
        }

        public String a() {
            return this.e;
        }

        public boolean b() {
            return this.f == EnumDirection.EnumDirectionLimit.VERTICAL;
        }

        public boolean c() {
            return this.f == EnumDirection.EnumDirectionLimit.HORIZONTAL;
        }

        public String toString() {
            return this.e;
        }

        public boolean a(EnumDirection enumdirection) {
            return enumdirection != null && enumdirection.k() == this;
        }

        public EnumDirection.EnumDirectionLimit d() {
            return this.f;
        }

        public String getName() {
            return this.e;
        }

        public boolean apply(EnumDirection object) {
            return this.a(object);
        }
    }

    static class SyntheticClass_1 {

        static final int[] a;
        static final int[] b;
        static final int[] c = new int[EnumDirection.EnumDirectionLimit.values().length];

        static {
            try {
                EnumDirection.SyntheticClass_1.c[EnumDirection.EnumDirectionLimit.HORIZONTAL.ordinal()] = 1;
            } catch (NoSuchFieldError nosuchfielderror) {
            }

            try {
                EnumDirection.SyntheticClass_1.c[EnumDirection.EnumDirectionLimit.VERTICAL.ordinal()] = 2;
            } catch (NoSuchFieldError nosuchfielderror1) {
            }

            b = new int[EnumDirection.values().length];

            try {
                EnumDirection.SyntheticClass_1.b[EnumDirection.NORTH.ordinal()] = 1;
            } catch (NoSuchFieldError nosuchfielderror2) {
            }

            try {
                EnumDirection.SyntheticClass_1.b[EnumDirection.EAST.ordinal()] = 2;
            } catch (NoSuchFieldError nosuchfielderror3) {
            }

            try {
                EnumDirection.SyntheticClass_1.b[EnumDirection.SOUTH.ordinal()] = 3;
            } catch (NoSuchFieldError nosuchfielderror4) {
            }

            try {
                EnumDirection.SyntheticClass_1.b[EnumDirection.WEST.ordinal()] = 4;
            } catch (NoSuchFieldError nosuchfielderror5) {
            }

            try {
                EnumDirection.SyntheticClass_1.b[EnumDirection.UP.ordinal()] = 5;
            } catch (NoSuchFieldError nosuchfielderror6) {
            }

            try {
                EnumDirection.SyntheticClass_1.b[EnumDirection.DOWN.ordinal()] = 6;
            } catch (NoSuchFieldError nosuchfielderror7) {
            }

            a = new int[EnumDirection.EnumAxis.values().length];

            try {
                EnumDirection.SyntheticClass_1.a[EnumDirection.EnumAxis.X.ordinal()] = 1;
            } catch (NoSuchFieldError nosuchfielderror8) {
            }

            try {
                EnumDirection.SyntheticClass_1.a[EnumDirection.EnumAxis.Y.ordinal()] = 2;
            } catch (NoSuchFieldError nosuchfielderror9) {
            }

            try {
                EnumDirection.SyntheticClass_1.a[EnumDirection.EnumAxis.Z.ordinal()] = 3;
            } catch (NoSuchFieldError nosuchfielderror10) {
            }

        }
    }
}
