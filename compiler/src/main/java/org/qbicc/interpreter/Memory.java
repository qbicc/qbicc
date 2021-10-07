package org.qbicc.interpreter;


import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.type.ValueType;

/**
 * A base for a relatively-indexed piece of memory.
 */
public interface Memory {
    int load8(int index, MemoryAtomicityMode mode);

    int load16(int index, MemoryAtomicityMode mode);

    int load32(int index, MemoryAtomicityMode mode);

    default float loadFloat(int index, MemoryAtomicityMode mode) {
        return Float.intBitsToFloat(load32(index, mode));
    }

    long load64(int index, MemoryAtomicityMode mode);

    VmObject loadRef(int index, MemoryAtomicityMode mode);

    ValueType loadType(int index, MemoryAtomicityMode mode);

    default double loadDouble(int index, MemoryAtomicityMode mode) {
        return Double.longBitsToDouble(load64(index, mode));
    }

    void store8(int index, int value, MemoryAtomicityMode mode);

    void store16(int index, int value, MemoryAtomicityMode mode);

    void store32(int index, int value, MemoryAtomicityMode mode);

    default void store32(int index, float value, MemoryAtomicityMode mode) {
        store32(index, Float.floatToRawIntBits(value), mode);
    }

    void store64(int index, long value, MemoryAtomicityMode mode);

    default void store64(int index, double value, MemoryAtomicityMode mode) {
        store64(index, Double.doubleToRawLongBits(value), mode);
    }

    void storeRef(int index, VmObject value, MemoryAtomicityMode mode);

    void storeType(int index, ValueType value, MemoryAtomicityMode mode);

    void storeMemory(int destIndex, Memory src, int srcIndex, int size);

    void storeMemory(int destIndex, byte[] src, int srcIndex, int size);

    int compareAndExchange8(int index, int expect, int update, MemoryAtomicityMode mode);

    int compareAndExchange16(int index, int expect, int update, MemoryAtomicityMode mode);

    int compareAndExchange32(int index, int expect, int update, MemoryAtomicityMode mode);

    long compareAndExchange64(int index, long expect, long update, MemoryAtomicityMode mode);

    VmObject compareAndExchangeRef(int index, VmObject expect, VmObject update, MemoryAtomicityMode mode);

    ValueType compareAndExchangeType(int index, ValueType expect, ValueType update, MemoryAtomicityMode mode);

    int getAndSet8(int index, int value, MemoryAtomicityMode mode);

    int getAndSet16(int index, int value, MemoryAtomicityMode mode);

    int getAndSet32(int index, int value, MemoryAtomicityMode mode);

    long getAndSet64(int index, long value, MemoryAtomicityMode mode);

    VmObject getAndSetRef(int index, VmObject value, MemoryAtomicityMode mode);

    ValueType getAndSetType(int index, ValueType value, MemoryAtomicityMode mode);

    int getAndAdd8(int index, int value, MemoryAtomicityMode mode);

    int getAndAdd16(int index, int value, MemoryAtomicityMode mode);

    int getAndAdd32(int index, int value, MemoryAtomicityMode mode);

    long getAndAdd64(int index, long value, MemoryAtomicityMode mode);

    Memory copy(int newSize);
}
