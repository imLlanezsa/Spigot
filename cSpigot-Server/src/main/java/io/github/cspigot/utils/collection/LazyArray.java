package io.github.cspigot.utils.collection;

import java.util.function.Function;

// Stackoverflow
public class LazyArray<E> {
    private final Function<Integer, E> supplier;
    private final Object[] backingArray;

    public LazyArray(Function<Integer, E> supplier, int size) {
        this.supplier = supplier;
        this.backingArray = new Object[size];
    }

    public E get(int index) {
        this.boundsCheck(index);
        Object obj = this.backingArray[index];
        if (obj == null) {
            this.backingArray[index] = obj = this.supplier.apply(index);
            return (E)obj;
        }
        return (E)obj;
    }

    public int size() {
        return this.backingArray.length;
    }

    private void boundsCheck(int index) {
        if (index >= 0 && index < this.backingArray.length) {
            return;
        }
        throw new ArrayIndexOutOfBoundsException(String.format((String)"Index %s is out of bounds!", (Object[])new Object[]{index}));
    }
}
