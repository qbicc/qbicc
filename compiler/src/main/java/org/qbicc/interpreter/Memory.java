package org.qbicc.interpreter;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.pointer.Pointer;
import org.qbicc.type.ValueType;

/**
 * A base for a relatively-indexed piece of memory.
 */
public interface Memory {
    int load8(long index, ReadAccessMode mode);

    int load16(long index, ReadAccessMode mode);

    int load32(long index, ReadAccessMode mode);

    default float loadFloat(long index, ReadAccessMode mode) {
        return Float.intBitsToFloat(load32(index, mode));
    }

    long load64(long index, ReadAccessMode mode);

    VmObject loadRef(long index, ReadAccessMode mode);

    ValueType loadType(long index, ReadAccessMode mode);

    Pointer loadPointer(long index, ReadAccessMode mode);

    default double loadDouble(long index, ReadAccessMode mode) {
        return Double.longBitsToDouble(load64(index, mode));
    }

    void store8(long index, int value, WriteAccessMode mode);

    void store16(long index, int value, WriteAccessMode mode);

    void store32(long index, int value, WriteAccessMode mode);

    default void storeFloat(long index, float value, WriteAccessMode mode) {
        store32(index, Float.floatToRawIntBits(value), mode);
    }

    default void store32(long index, long value, WriteAccessMode mode) {
        store32(index, (int) value, mode);
    }

    void store64(long index, long value, WriteAccessMode mode);

    default void storeDouble(long index, double value, WriteAccessMode mode) {
        store64(index, Double.doubleToRawLongBits(value), mode);
    }

    void storeRef(long index, VmObject value, WriteAccessMode mode);

    void storeType(long index, ValueType value, WriteAccessMode mode);

    void storePointer(long index, Pointer value, WriteAccessMode mode);

    void storeMemory(long destIndex, Memory src, long srcIndex, long size);

    void storeMemory(long destIndex, byte[] src, int srcIndex, int size);

    int compareAndExchange8(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode);

    int compareAndExchange16(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode);

    int compareAndExchange32(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode);

    long compareAndExchange64(long index, long expect, long update, ReadAccessMode readMode, WriteAccessMode writeMode);

    VmObject compareAndExchangeRef(long index, VmObject expect, VmObject update, ReadAccessMode readMode, WriteAccessMode writeMode);

    ValueType compareAndExchangeType(long index, ValueType expect, ValueType update, ReadAccessMode readMode, WriteAccessMode writeMode);

    Pointer compareAndExchangePointer(long index, Pointer expect, Pointer update, ReadAccessMode readMode, WriteAccessMode writeMode);

    default int getAndSet8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load8(index, readMode);
        int witness;
        for (;;) {
            witness = compareAndExchange8(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndSet16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load16(index, readMode);
        int witness;
        for (;;) {
            witness = compareAndExchange16(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndSet32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load32(index, readMode);
        int witness;
        for (;;) {
            witness = compareAndExchange32(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default long getAndSet64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        long oldVal = load64(index, readMode);
        long witness;
        for (;;) {
            witness = compareAndExchange64(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default VmObject getAndSetRef(long index, VmObject value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        VmObject oldVal = loadRef(index, readMode);
        VmObject witness;
        for (;;) {
            witness = compareAndExchangeRef(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default ValueType getAndSetType(long index, ValueType value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        ValueType oldVal = loadType(index, readMode);
        ValueType witness;
        for (;;) {
            witness = compareAndExchangeType(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default Pointer getAndSetPointer(long index, Pointer value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        Pointer oldVal = loadPointer(index, readMode);
        Pointer witness;
        for (;;) {
            witness = compareAndExchangePointer(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndAdd8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load8(index, readMode);
        int newVal, witness;
        for (;;) {
            newVal = oldVal + value;
            witness = compareAndExchange8(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndAdd16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load16(index, readMode);
        int newVal, witness;
        for (;;) {
            newVal = oldVal + value;
            witness = compareAndExchange16(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndAdd32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load32(index, readMode);
        int newVal, witness;
        for (;;) {
            newVal = oldVal + value;
            witness = compareAndExchange32(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default long getAndAdd64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        long oldVal = load64(index, readMode);
        long newVal, witness;
        for (;;) {
            newVal = oldVal + value;
            witness = compareAndExchange64(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndBitwiseAnd8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load8(index, readMode);
        int newVal, witness;
        for (;;) {
            newVal = oldVal & value;
            witness = compareAndExchange8(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndBitwiseAnd16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load16(index, readMode);
        int newVal, witness;
        for (;;) {
            newVal = oldVal & value;
            witness = compareAndExchange16(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndBitwiseAnd32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load32(index, readMode);
        int newVal, witness;
        for (;;) {
            newVal = oldVal & value;
            if (newVal == oldVal) {
                return oldVal;
            }
            witness = compareAndExchange32(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default long getAndBitwiseAnd64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        long oldVal = load64(index, readMode);
        long newVal, witness;
        for (;;) {
            newVal = oldVal & value;
            if (newVal == oldVal) {
                return oldVal;
            }
            witness = compareAndExchange64(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndBitwiseNand8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load8(index, readMode);
        int newVal, witness;
        for (;;) {
            newVal = oldVal & ~value;
            witness = compareAndExchange8(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndBitwiseNand16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load16(index, readMode);
        int newVal, witness;
        for (;;) {
            newVal = oldVal & ~value;
            witness = compareAndExchange16(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndBitwiseNand32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load32(index, readMode);
        int newVal, witness;
        for (;;) {
            newVal = oldVal & ~value;
            if (newVal == oldVal) {
                return oldVal;
            }
            witness = compareAndExchange32(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default long getAndBitwiseNand64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        long oldVal = load64(index, readMode);
        long newVal, witness;
        for (;;) {
            newVal = oldVal & ~value;
            if (newVal == oldVal) {
                return oldVal;
            }
            witness = compareAndExchange64(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndBitwiseOr8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load8(index, readMode);
        int newVal, witness;
        for (;;) {
            newVal = oldVal | value;
            witness = compareAndExchange8(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndBitwiseOr16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load16(index, readMode);
        int newVal, witness;
        for (;;) {
            newVal = oldVal | value;
            witness = compareAndExchange16(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndBitwiseOr32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load32(index, readMode);
        int newVal, witness;
        for (;;) {
            newVal = oldVal | ~value;
            if (newVal == oldVal) {
                return oldVal;
            }
            witness = compareAndExchange32(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default long getAndBitwiseOr64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        long oldVal = load64(index, readMode);
        long newVal, witness;
        for (;;) {
            newVal = oldVal | value;
            if (newVal == oldVal) {
                return oldVal;
            }
            witness = compareAndExchange64(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndBitwiseXor8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load8(index, readMode);
        int newVal, witness;
        for (;;) {
            newVal = oldVal ^ value;
            witness = compareAndExchange8(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndBitwiseXor16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load16(index, readMode);
        int newVal, witness;
        for (;;) {
            newVal = oldVal ^ value;
            witness = compareAndExchange16(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndBitwiseXor32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load32(index, readMode);
        int newVal, witness;
        for (;;) {
            newVal = oldVal ^ value;
            if (newVal == oldVal) {
                return oldVal;
            }
            witness = compareAndExchange32(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default long getAndBitwiseXor64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        long oldVal = load64(index, readMode);
        long newVal, witness;
        for (;;) {
            newVal = oldVal & ~value;
            if (newVal == oldVal) {
                return oldVal;
            }
            witness = compareAndExchange64(index, oldVal, newVal, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    private int signExtend8(int val) {
        return (byte) val;
    }

    private int signExtend16(int val) {
        return (short) val;
    }

    default int getAndSetMaxSigned8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load8(index, readMode);
        int witness;
        value = signExtend8(value);
        for (;;) {
            if (signExtend8(oldVal) >= value) {
                return oldVal;
            }
            witness = compareAndExchange8(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndSetMaxSigned16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load16(index, readMode);
        int witness;
        value = signExtend16(value);
        for (;;) {
            if (signExtend16(oldVal) >= value) {
                return oldVal;
            }
            witness = compareAndExchange16(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndSetMaxSigned32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load32(index, readMode);
        int witness;
        for (;;) {
            if (oldVal >= value) {
                return oldVal;
            }
            witness = compareAndExchange32(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default long getAndSetMaxSigned64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        long oldVal = load64(index, readMode);
        long witness;
        for (;;) {
            if (oldVal >= value) {
                return oldVal;
            }
            witness = compareAndExchange64(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndSetMaxUnsigned8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load8(index, readMode) & 0xff;
        int witness;
        value &= 0xff;
        for (;;) {
            if (oldVal >= value) {
                return oldVal;
            }
            witness = compareAndExchange8(index, oldVal, value, readMode, writeMode) & 0xff;
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndSetMaxUnsigned16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load16(index, readMode) & 0xffff;
        int witness;
        value &= 0xffff;
        for (;;) {
            if (oldVal >= value) {
                return oldVal;
            }
            witness = compareAndExchange16(index, oldVal, value, readMode, writeMode) & 0xffff;
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndSetMaxUnsigned32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load32(index, readMode);
        int witness;
        for (;;) {
            if (Integer.compareUnsigned(oldVal, value) >= 0) {
                return oldVal;
            }
            witness = compareAndExchange32(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default long getAndSetMaxUnsigned64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        long oldVal = load64(index, readMode);
        long witness;
        for (;;) {
            if (Long.compareUnsigned(oldVal, value) >= 0) {
                return oldVal;
            }
            witness = compareAndExchange64(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndSetMinSigned8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load8(index, readMode);
        int witness;
        value = signExtend8(value);
        for (;;) {
            if (signExtend8(oldVal) <= value) {
                return oldVal;
            }
            witness = compareAndExchange8(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndSetMinSigned16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load16(index, readMode);
        int witness;
        value = signExtend16(value);
        for (;;) {
            if (signExtend16(oldVal) <= value) {
                return oldVal;
            }
            witness = compareAndExchange16(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndSetMinSigned32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load32(index, readMode);
        int witness;
        for (;;) {
            if (oldVal <= value) {
                return oldVal;
            }
            witness = compareAndExchange32(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default long getAndSetMinSigned64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        long oldVal = load64(index, readMode);
        long witness;
        for (;;) {
            if (oldVal <= value) {
                return oldVal;
            }
            witness = compareAndExchange64(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndSetMinUnsigned8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load8(index, readMode) & 0xff;
        int witness;
        value &= 0xff;
        for (;;) {
            if (oldVal <= value) {
                return oldVal;
            }
            witness = compareAndExchange8(index, oldVal, value, readMode, writeMode) & 0xff;
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndSetMinUnsigned16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load16(index, readMode) & 0xffff;
        int witness;
        value &= 0xffff;
        for (;;) {
            if (oldVal <= value) {
                return oldVal;
            }
            witness = compareAndExchange16(index, oldVal, value, readMode, writeMode) & 0xffff;
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default int getAndSetMinUnsigned32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int oldVal = load32(index, readMode);
        int witness;
        for (;;) {
            if (Integer.compareUnsigned(oldVal, value) <= 0) {
                return oldVal;
            }
            witness = compareAndExchange32(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    default long getAndSetMinUnsigned64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        long oldVal = load32(index, readMode);
        long witness;
        for (;;) {
            if (Long.compareUnsigned(oldVal, value) <= 0) {
                return oldVal;
            }
            witness = compareAndExchange64(index, oldVal, value, readMode, writeMode);
            if (witness == oldVal) {
                return oldVal;
            }
            oldVal = witness;
        }
    }

    Memory copy(long newSize);

    Memory clone();

    Memory cloneZeroed();

    long getSize();
}
