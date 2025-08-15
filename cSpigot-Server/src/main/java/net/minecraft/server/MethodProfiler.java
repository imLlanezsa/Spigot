package net.minecraft.server;

import java.util.List;

// CraftBukkit start - Strip down to empty methods, performance cost
public class MethodProfiler {

    /**
     * cSpigot start - So basically, bukkit disabled this? but didn't remove its usage
     * and method calls. I got no idea why no one from paper, taco or any other fork didn't
     * see this, its just empty methods being called. In the original minecraft jar there is definite
     * code, which got override here with empty code by bukkit. Why? I can only guess its because timings
     * was made and this was rendered useless.
     *<p>
     * So cSpigot is removing all method calls for this class and its instance creations and stuff...
     *</p>
     * cSpigot end
     */

    public boolean a = false; // cSpigot - disable
    public MethodProfiler() {
    }



    public void a() {
    }
    public void enter() {
    }

    public void a(String s) {
    }
    public void enter(String s) {
    }

    public void b() {
    }
    public void exit() {
    }

    public List<MethodProfiler.ProfilerInfo> b(String s) {
        return null;
    }

    public void c(String s) {
    }

    public String c() {
        return "";
    }

    public static final class ProfilerInfo implements Comparable<MethodProfiler.ProfilerInfo> {

        public double a;
        public double b;
        public String c;

        public ProfilerInfo(String s, double d0, double d1) {
            this.c = s;
            this.a = d0;
            this.b = d1;
        }

        public int a(ProfilerInfo var1) {
            if (var1.a < this.a) {
                return -1;
            } else {
                return var1.a > this.a ? 1 : var1.c.compareTo(this.c);
            }
        }

        public int compareTo(ProfilerInfo object) {
            return this.a(object);
        }
    }
}
