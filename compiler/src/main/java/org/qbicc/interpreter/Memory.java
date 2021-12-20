package org.qbicc.interpreter;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.type.ValueType;

/**
 * A base for a relatively-indexed piece of memory.
 */
public interface Memory {
    int load8(int index, ReadAccessMode mode);

    int load16(int index, ReadAccessMode mode);

    int load32(int index, ReadAccessMode mode);

    default float loadFloat(int index, ReadAccessMode mode) {
        return Float.intBitsToFloat(load32(index, mode));
    }

    long load64(int index, ReadAccessMode mode);

    VmObject loadRef(int index, ReadAccessMode mode);

    ValueType loadType(int index, ReadAccessMode mode);

    default double loadDouble(int index, ReadAccessMode mode) {
        return Double.longBitsToDouble(load64(index, mode));
    }

    void store8(int index, int value, WriteAccessMode mode);

    void store16(int index, int value, WriteAccessMode mode);

    void store32(int index, int value, WriteAccessMode mode);

    default void store32(int index, float value, WriteAccessMode mode) {
        store32(index, Float.floatToRawIntBits(value), mode);
    }

    default void store32(int index, long value, WriteAccessMode mode) {
        store32(index, (int)value, mode);
    }

    void store64(int index, long value, WriteAccessMode mode);

    default void store64(int index, double value, WriteAccessMode mode) {
        store64(index, Double.doubleToRawLongBits(value), mode);
    }

    void storeRef(int index, VmObject value, WriteAccessMode mode);

    void storeType(int index, ValueType value, WriteAccessMode mode);

    void storeMemory(int destIndex, Memory src, int srcIndex, int size);

    void storeMemory(int destIndex, byte[] src, int srcIndex, int size);

    int compareAndExchange8(int index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode);

    int compareAndExchange16(int index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode);

    int compareAndExchange32(int index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode);

    long compareAndExchange64(int index, long expect, long update, ReadAccessMode readMode, WriteAccessMode writeMode);

    VmObject compareAndExchangeRef(int index, VmObject expect, VmObject update, ReadAccessMode readMode, WriteAccessMode writeMode);

    ValueType compareAndExchangeType(int index, ValueType expect, ValueType update, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndSet8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndSet16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndSet32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    long getAndSet64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    VmObject getAndSetRef(int index, VmObject value, ReadAccessMode readMode, WriteAccessMode writeMode);

    ValueType getAndSetType(int index, ValueType value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndAdd8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndAdd16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndAdd32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    long getAndAdd64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndBitwiseAnd8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndBitwiseAnd16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndBitwiseAnd32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    long getAndBitwiseAnd64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndBitwiseNand8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndBitwiseNand16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndBitwiseNand32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    long getAndBitwiseNand64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndBitwiseOr8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndBitwiseOr16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndBitwiseOr32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    long getAndBitwiseOr64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndBitwiseXor8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndBitwiseXor16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndBitwiseXor32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    long getAndBitwiseXor64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndSetMaxSigned8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndSetMaxSigned16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndSetMaxSigned32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    long getAndSetMaxSigned64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndSetMaxUnsigned8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndSetMaxUnsigned16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndSetMaxUnsigned32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    long getAndSetMaxUnsigned64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndSetMinSigned8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndSetMinSigned16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndSetMinSigned32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    long getAndSetMinSigned64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndSetMinUnsigned8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndSetMinUnsigned16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndSetMinUnsigned32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    long getAndSetMinUnsigned64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    Memory copy(int newSize);
}
