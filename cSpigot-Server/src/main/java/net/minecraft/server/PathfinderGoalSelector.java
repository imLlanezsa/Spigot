package net.minecraft.server;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bukkit.craftbukkit.util.UnsafeList; // CraftBukkit

public class PathfinderGoalSelector {

    private static final Logger a = LogManager.getLogger();
    private List<PathfinderGoalSelectorItem> b = new UnsafeList<PathfinderGoalSelectorItem>();
    private List<PathfinderGoalSelectorItem> c = new UnsafeList<PathfinderGoalSelectorItem>();
    private final MethodProfiler d;
    private int e;
    private int f = 3;

    public PathfinderGoalSelector(MethodProfiler methodprofiler) {
        this.d = methodprofiler;
    }

    public void a(int i, PathfinderGoal pathfindergoal) {
        this.b.add(new PathfinderGoalSelectorItem(i, pathfindergoal));
    }

    public void a(PathfinderGoal pathfindergoal) {
        Iterator iterator = this.b.iterator();

        while (iterator.hasNext()) {
            PathfinderGoalSelectorItem pathfindergoalselector_pathfindergoalselectoritem = (PathfinderGoalSelectorItem) iterator.next();
            PathfinderGoal pathfindergoal1 = pathfindergoalselector_pathfindergoalselectoritem.a;

            if (pathfindergoal1 == pathfindergoal) {
                if (this.c.contains(pathfindergoalselector_pathfindergoalselectoritem)) {
                    pathfindergoal1.d();
                    this.c.remove(pathfindergoalselector_pathfindergoalselectoritem);
                }

                iterator.remove();
            }
        }

    }

    public void a() {
        Iterator iterator;
        PathfinderGoalSelectorItem pathfindergoalselector_pathfindergoalselectoritem;

        if (this.e++ % this.f == 0) {
            iterator = this.b.iterator();

            while (iterator.hasNext()) {
                pathfindergoalselector_pathfindergoalselectoritem = (PathfinderGoalSelectorItem) iterator.next();
                boolean flag = this.c.contains(pathfindergoalselector_pathfindergoalselectoritem);

                if (flag) {
                    if (this.b(pathfindergoalselector_pathfindergoalselectoritem) && this.a(pathfindergoalselector_pathfindergoalselectoritem)) {
                        continue;
                    }

                    pathfindergoalselector_pathfindergoalselectoritem.a.d();
                    this.c.remove(pathfindergoalselector_pathfindergoalselectoritem);
                }

                if (this.b(pathfindergoalselector_pathfindergoalselectoritem) && pathfindergoalselector_pathfindergoalselectoritem.a.a()) {
                    pathfindergoalselector_pathfindergoalselectoritem.a.c();
                    this.c.add(pathfindergoalselector_pathfindergoalselectoritem);
                }
            }
        } else {
            iterator = this.c.iterator();

            while (iterator.hasNext()) {
                pathfindergoalselector_pathfindergoalselectoritem = (PathfinderGoalSelectorItem) iterator.next();
                if (!this.a(pathfindergoalselector_pathfindergoalselectoritem)) {
                    pathfindergoalselector_pathfindergoalselectoritem.a.d();
                    iterator.remove();
                }
            }
        }

        iterator = this.c.iterator();

        while (iterator.hasNext()) {
            pathfindergoalselector_pathfindergoalselectoritem = (PathfinderGoalSelectorItem) iterator.next();
            pathfindergoalselector_pathfindergoalselectoritem.a.e();
        }
    }

    private boolean a(PathfinderGoalSelectorItem pathfindergoalselector_pathfindergoalselectoritem) {
        boolean flag = pathfindergoalselector_pathfindergoalselectoritem.a.b();

        return flag;
    }

    private boolean b(PathfinderGoalSelectorItem pathfindergoalselector_pathfindergoalselectoritem) {
        Iterator iterator = this.b.iterator();

        while (iterator.hasNext()) {
            PathfinderGoalSelectorItem pathfindergoalselector_pathfindergoalselectoritem1 = (PathfinderGoalSelectorItem) iterator.next();

            if (pathfindergoalselector_pathfindergoalselectoritem1 != pathfindergoalselector_pathfindergoalselectoritem) {
                if (pathfindergoalselector_pathfindergoalselectoritem.b >= pathfindergoalselector_pathfindergoalselectoritem1.b) {
                    if (!this.a(pathfindergoalselector_pathfindergoalselectoritem, pathfindergoalselector_pathfindergoalselectoritem1) && this.c.contains(pathfindergoalselector_pathfindergoalselectoritem1)) {
                        ((UnsafeList.Itr) iterator).valid = false; // CraftBukkit - mark iterator for reuse
                        return false;
                    }
                } else if (!pathfindergoalselector_pathfindergoalselectoritem1.a.i() && this.c.contains(pathfindergoalselector_pathfindergoalselectoritem1)) {
                    ((UnsafeList.Itr) iterator).valid = false; // CraftBukkit - mark iterator for reuse
                    return false;
                }
            }
        }

        return true;
    }

    private boolean a(PathfinderGoalSelectorItem pathfindergoalselector_pathfindergoalselectoritem, PathfinderGoalSelectorItem pathfindergoalselector_pathfindergoalselectoritem1) {
        return (pathfindergoalselector_pathfindergoalselectoritem.a.j() & pathfindergoalselector_pathfindergoalselectoritem1.a.j()) == 0;
    }

    class PathfinderGoalSelectorItem {

        public PathfinderGoal a;
        public int b;

        public PathfinderGoalSelectorItem(int i, PathfinderGoal pathfindergoal) {
            this.b = i;
            this.a = pathfindergoal;
        }
    }
}
