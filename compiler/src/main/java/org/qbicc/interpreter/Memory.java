package org.qbicc.interpreter;


import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.type.ValueType;

/**
 * A base for a relatively-indexed piece of memory.
 */
public interface Memory {
    @Deprecated
    default int load8(int index, MemoryAtomicityMode mode) {
        return load8(index, mode.getAccessMode().getReadAccess());
    }

    int load8(int index, ReadAccessMode mode);

    @Deprecated
    default int load16(int index, MemoryAtomicityMode mode) {
        return load16(index, mode.getAccessMode().getReadAccess());
    }

    int load16(int index, ReadAccessMode mode);

    @Deprecated
    default int load32(int index, MemoryAtomicityMode mode) {
        return load32(index, mode.getAccessMode().getReadAccess());
    }

    int load32(int index, ReadAccessMode mode);

    @Deprecated
    default float loadFloat(int index, MemoryAtomicityMode mode) {
        return Float.intBitsToFloat(load32(index, mode));
    }

    default float loadFloat(int index, ReadAccessMode mode) {
        return Float.intBitsToFloat(load32(index, mode));
    }

    @Deprecated
    default long load64(int index, MemoryAtomicityMode mode) {
        return load64(index, mode.getAccessMode().getReadAccess());
    }

    long load64(int index, ReadAccessMode mode);

    @Deprecated
    default VmObject loadRef(int index, MemoryAtomicityMode mode) {
        return loadRef(index, mode.getAccessMode().getReadAccess());
    }

    VmObject loadRef(int index, ReadAccessMode mode);

    @Deprecated
    default ValueType loadType(int index, MemoryAtomicityMode mode) {
        return loadType(index, mode.getAccessMode().getReadAccess());
    }

    ValueType loadType(int index, ReadAccessMode mode);

    @Deprecated
    default double loadDouble(int index, MemoryAtomicityMode mode) {
        return Double.longBitsToDouble(load64(index, mode));
    }

    default double loadDouble(int index, ReadAccessMode mode) {
        return Double.longBitsToDouble(load64(index, mode));
    }

    @Deprecated
    default void store8(int index, int value, MemoryAtomicityMode mode) {
        store8(index, value, mode.getAccessMode().getWriteAccess());
    }

    void store8(int index, int value, WriteAccessMode mode);

    @Deprecated
    default void store16(int index, int value, MemoryAtomicityMode mode) {
        store16(index, value, mode.getAccessMode().getWriteAccess());
    }

    void store16(int index, int value, WriteAccessMode mode);

    @Deprecated
    default void store32(int index, int value, MemoryAtomicityMode mode) {
        store32(index, value, mode.getAccessMode().getWriteAccess());
    }

    void store32(int index, int value, WriteAccessMode mode);

    @Deprecated
    default void store32(int index, float value, MemoryAtomicityMode mode) {
        store32(index, Float.floatToRawIntBits(value), mode);
    }

    default void store32(int index, float value, WriteAccessMode mode) {
        store32(index, Float.floatToRawIntBits(value), mode);
    }

    @Deprecated
    default void store32(int index, long value, MemoryAtomicityMode mode) {
        store32(index, (int)value, mode);
    }

    default void store32(int index, long value, WriteAccessMode mode) {
        store32(index, (int)value, mode);
    }

    @Deprecated
    default void store64(int index, long value, MemoryAtomicityMode mode) {
        store64(index, value, mode.getAccessMode().getWriteAccess());
    }

    void store64(int index, long value, WriteAccessMode mode);

    @Deprecated
    default void store64(int index, double value, MemoryAtomicityMode mode) {
        store64(index, Double.doubleToRawLongBits(value), mode);
    }

    default void store64(int index, double value, WriteAccessMode mode) {
        store64(index, Double.doubleToRawLongBits(value), mode);
    }

    @Deprecated
    default void storeRef(int index, VmObject value, MemoryAtomicityMode mode) {
        storeRef(index, value, mode.getAccessMode().getWriteAccess());
    }

    void storeRef(int index, VmObject value, WriteAccessMode mode);

    @Deprecated
    default void storeType(int index, ValueType value, MemoryAtomicityMode mode) {
        storeType(index, value, mode.getAccessMode().getWriteAccess());
    }

    void storeType(int index, ValueType value, WriteAccessMode mode);

    void storeMemory(int destIndex, Memory src, int srcIndex, int size);

    void storeMemory(int destIndex, byte[] src, int srcIndex, int size);

    @Deprecated
    default int compareAndExchange8(int index, int expect, int update, MemoryAtomicityMode mode) {
        return compareAndExchange8(index, expect, update, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int compareAndExchange8(int index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int compareAndExchange16(int index, int expect, int update, MemoryAtomicityMode mode) {
        return compareAndExchange16(index, expect, update, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int compareAndExchange16(int index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int compareAndExchange32(int index, int expect, int update, MemoryAtomicityMode mode) {
        return compareAndExchange32(index, expect, update, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int compareAndExchange32(int index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default long compareAndExchange64(int index, long expect, long update, MemoryAtomicityMode mode) {
        return compareAndExchange64(index, expect, update, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    long compareAndExchange64(int index, long expect, long update, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default VmObject compareAndExchangeRef(int index, VmObject expect, VmObject update, MemoryAtomicityMode mode) {
        return compareAndExchangeRef(index, expect, update, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    VmObject compareAndExchangeRef(int index, VmObject expect, VmObject update, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default ValueType compareAndExchangeType(int index, ValueType expect, ValueType update, MemoryAtomicityMode mode) {
        return compareAndExchangeType(index, expect, update, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    ValueType compareAndExchangeType(int index, ValueType expect, ValueType update, ReadAccessMode readMode, WriteAccessMode writeMode);

    int getAndSet8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndSet8(int index, int value, MemoryAtomicityMode mode) {
        return getAndSet8(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndSet16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndSet16(int index, int value, MemoryAtomicityMode mode) {
        return getAndSet16(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndSet32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndSet32(int index, int value, MemoryAtomicityMode mode) {
        return getAndSet32(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    long getAndSet64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default long getAndSet64(int index, long value, MemoryAtomicityMode mode) {
        return getAndSet64(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    VmObject getAndSetRef(int index, VmObject value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default VmObject getAndSetRef(int index, VmObject value, MemoryAtomicityMode mode) {
        return getAndSetRef(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    ValueType getAndSetType(int index, ValueType value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default ValueType getAndSetType(int index, ValueType value, MemoryAtomicityMode mode) {
        return getAndSetType(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndAdd8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndAdd8(int index, int value, MemoryAtomicityMode mode) {
        return getAndAdd8(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndAdd16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndAdd16(int index, int value, MemoryAtomicityMode mode) {
        return getAndAdd16(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndAdd32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndAdd32(int index, int value, MemoryAtomicityMode mode) {
        return getAndAdd32(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    long getAndAdd64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default long getAndAdd64(int index, long value, MemoryAtomicityMode mode) {
        return getAndAdd64(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndBitwiseAnd8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndBitwiseAnd8(int index, int value, MemoryAtomicityMode mode) {
        return getAndBitwiseAnd8(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndBitwiseAnd16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndBitwiseAnd16(int index, int value, MemoryAtomicityMode mode) {
        return getAndBitwiseAnd16(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndBitwiseAnd32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndBitwiseAnd32(int index, int value, MemoryAtomicityMode mode) {
        return getAndBitwiseAnd32(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    long getAndBitwiseAnd64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default long getAndBitwiseAnd64(int index, long value, MemoryAtomicityMode mode) {
        return getAndBitwiseAnd64(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndBitwiseNand8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndBitwiseNand8(int index, int value, MemoryAtomicityMode mode) {
        return getAndBitwiseNand8(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndBitwiseNand16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndBitwiseNand16(int index, int value, MemoryAtomicityMode mode) {
        return getAndBitwiseNand16(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndBitwiseNand32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndBitwiseNand32(int index, int value, MemoryAtomicityMode mode) {
        return getAndBitwiseNand32(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    long getAndBitwiseNand64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default long getAndBitwiseNand64(int index, long value, MemoryAtomicityMode mode) {
        return getAndBitwiseNand64(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndBitwiseOr8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndBitwiseOr8(int index, int value, MemoryAtomicityMode mode) {
        return getAndBitwiseOr8(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndBitwiseOr16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndBitwiseOr16(int index, int value, MemoryAtomicityMode mode) {
        return getAndBitwiseOr16(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndBitwiseOr32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndBitwiseOr32(int index, int value, MemoryAtomicityMode mode) {
        return getAndBitwiseOr32(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    long getAndBitwiseOr64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default long getAndBitwiseOr64(int index, long value, MemoryAtomicityMode mode) {
        return getAndBitwiseOr64(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndBitwiseXor8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndBitwiseXor8(int index, int value, MemoryAtomicityMode mode) {
        return getAndBitwiseXor8(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndBitwiseXor16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndBitwiseXor16(int index, int value, MemoryAtomicityMode mode) {
        return getAndBitwiseXor16(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndBitwiseXor32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndBitwiseXor32(int index, int value, MemoryAtomicityMode mode) {
        return getAndBitwiseXor32(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    long getAndBitwiseXor64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default long getAndBitwiseXor64(int index, long value, MemoryAtomicityMode mode) {
        return getAndBitwiseXor64(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndSetMaxSigned8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndSetMaxSigned8(int index, int value, MemoryAtomicityMode mode) {
        return getAndSetMaxSigned8(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndSetMaxSigned16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndSetMaxSigned16(int index, int value, MemoryAtomicityMode mode) {
        return getAndSetMaxSigned16(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndSetMaxSigned32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndSetMaxSigned32(int index, int value, MemoryAtomicityMode mode) {
        return getAndSetMaxSigned32(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    long getAndSetMaxSigned64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default long getAndSetMaxSigned64(int index, long value, MemoryAtomicityMode mode) {
        return getAndSetMaxSigned64(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndSetMaxUnsigned8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndSetMaxUnsigned8(int index, int value, MemoryAtomicityMode mode) {
        return getAndSetMaxUnsigned8(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndSetMaxUnsigned16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndSetMaxUnsigned16(int index, int value, MemoryAtomicityMode mode) {
        return getAndSetMaxUnsigned16(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndSetMaxUnsigned32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndSetMaxUnsigned32(int index, int value, MemoryAtomicityMode mode) {
        return getAndSetMaxUnsigned32(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    long getAndSetMaxUnsigned64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default long getAndSetMaxUnsigned64(int index, long value, MemoryAtomicityMode mode) {
        return getAndSetMaxUnsigned64(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndSetMinSigned8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndSetMinSigned8(int index, int value, MemoryAtomicityMode mode) {
        return getAndSetMinSigned8(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndSetMinSigned16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndSetMinSigned16(int index, int value, MemoryAtomicityMode mode) {
        return getAndSetMinSigned16(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndSetMinSigned32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndSetMinSigned32(int index, int value, MemoryAtomicityMode mode) {
        return getAndSetMinSigned32(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    long getAndSetMinSigned64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default long getAndSetMinSigned64(int index, long value, MemoryAtomicityMode mode) {
        return getAndSetMinSigned64(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndSetMinUnsigned8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndSetMinUnsigned8(int index, int value, MemoryAtomicityMode mode) {
        return getAndSetMinUnsigned8(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndSetMinUnsigned16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndSetMinUnsigned16(int index, int value, MemoryAtomicityMode mode) {
        return getAndSetMinUnsigned16(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    int getAndSetMinUnsigned32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default int getAndSetMinUnsigned32(int index, int value, MemoryAtomicityMode mode) {
        return getAndSetMinUnsigned32(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }

    long getAndSetMinUnsigned64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode);

    @Deprecated
    default long getAndSetMinUnsigned64(int index, long value, MemoryAtomicityMode mode) {
        return getAndSetMinUnsigned64(index, value, mode.getAccessMode().getReadAccess(), mode.getAccessMode().getWriteAccess());
    }


    Memory copy(int newSize);
}
