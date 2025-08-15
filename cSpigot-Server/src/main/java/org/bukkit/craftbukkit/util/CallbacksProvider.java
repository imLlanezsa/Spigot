package org.bukkit.craftbukkit.util;

public interface CallbacksProvider<I, O, E extends Throwable> {

    O asyncStage(I input) throws E;

    void syncStage(I input, O output) throws E;

}
