package net.minecraft.server;

import java.util.Iterator;

public interface Registry<T> extends Iterable<T> {

    @Override
    Iterator<T> iterator(); // Paper - decompile fix
}
