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

    default void storeFloat(int index, float value, WriteAccessMode mode) {
        store32(index, Float.floatToRawIntBits(value), mode);
    }

    default void store32(int index, long value, WriteAccessMode mode) {
        store32(index, (int)value, mode);
    }

    void store64(int index, long value, WriteAccessMode mode);

    default void storeDouble(int index, double value, WriteAccessMode mode) {
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

    default int getAndSet8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndSet16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndSet32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default long getAndSet64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default VmObject getAndSetRef(int index, VmObject value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default ValueType getAndSetType(int index, ValueType value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndAdd8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndAdd16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndAdd32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default long getAndAdd64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndBitwiseAnd8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndBitwiseAnd16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndBitwiseAnd32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default long getAndBitwiseAnd64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndBitwiseNand8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndBitwiseNand16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndBitwiseNand32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default long getAndBitwiseNand64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndBitwiseOr8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndBitwiseOr16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndBitwiseOr32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default long getAndBitwiseOr64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndBitwiseXor8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndBitwiseXor16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndBitwiseXor32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default long getAndBitwiseXor64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndSetMaxSigned8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndSetMaxSigned16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndSetMaxSigned32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default long getAndSetMaxSigned64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndSetMaxUnsigned8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndSetMaxUnsigned16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndSetMaxUnsigned32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default long getAndSetMaxUnsigned64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndSetMinSigned8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndSetMinSigned16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndSetMinSigned32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default long getAndSetMinSigned64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndSetMinUnsigned8(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndSetMinUnsigned16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default int getAndSetMinUnsigned32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    default long getAndSetMinUnsigned64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
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

    Memory copy(int newSize);
}
