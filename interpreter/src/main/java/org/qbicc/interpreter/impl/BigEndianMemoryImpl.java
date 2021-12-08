package org.qbicc.interpreter.impl;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static org.qbicc.graph.atomic.AccessModes.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;

final class BigEndianMemoryImpl extends MemoryImpl {
    static final BigEndianMemoryImpl EMPTY = new BigEndianMemoryImpl(0);

    private static final VarHandle h16 = MethodHandles.byteArrayViewVarHandle(short[].class, BIG_ENDIAN);
    private static final VarHandle h32 = MethodHandles.byteArrayViewVarHandle(int[].class, BIG_ENDIAN);
    private static final VarHandle h64 = MethodHandles.byteArrayViewVarHandle(long[].class, BIG_ENDIAN);

    BigEndianMemoryImpl(int dataSize) {
        super(dataSize);
    }

    BigEndianMemoryImpl(final BigEndianMemoryImpl original) {
        super(original);
    }

    @Override
    public int load16(int index, ReadAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            return (int) h16.get(data, index);
        } else if (SingleOpaque.includes(mode)) {
            return (int) h16.getOpaque(data, index);
        } else if (GlobalAcquire.includes(mode)) {
            return (int) h16.getAcquire(data, index);
        } else {
            return (int) h16.getVolatile(data, index);
        }
    }

    @Override
    public int load32(int index, ReadAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            return (int) h32.get(data, index);
        } else if (SingleOpaque.includes(mode)) {
            return (int) h32.getOpaque(data, index);
        } else if (GlobalAcquire.includes(mode)) {
            return (int) h32.getAcquire(data, index);
        } else {
            return (int) h32.getVolatile(data, index);
        }
    }

    @Override
    public long load64(int index, ReadAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            return (long) h64.get(data, index);
        } else if (SingleOpaque.includes(mode)) {
            return (long) h64.getOpaque(data, index);
        } else if (GlobalAcquire.includes(mode)) {
            return (long) h64.getAcquire(data, index);
        } else {
            return (long) h64.getVolatile(data, index);
        }
    }

    @Override
    public void store16(int index, int value, WriteAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            h16.set(data, index, (short) value);
        } else if (SingleOpaque.includes(mode)) {
            h16.setOpaque(data, index, (short) value);
        } else if (GlobalRelease.includes(mode)) {
            h16.setRelease(data, index, (short) value);
        } else {
            h16.setVolatile(data, index, (short) value);
        }
    }

    @Override
    public void store32(int index, int value, WriteAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            h32.set(data, index, value);
        } else if (SingleOpaque.includes(mode)) {
            h32.setOpaque(data, index, value);
        } else if (GlobalRelease.includes(mode)) {
            h32.setRelease(data, index, value);
        } else {
            h32.setVolatile(data, index, value);
        }
    }

    @Override
    public void store64(int index, long value, WriteAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            h64.set(data, index, value);
        } else if (SingleOpaque.includes(mode)) {
            h64.setOpaque(data, index, value);
        } else if (GlobalRelease.includes(mode)) {
            h64.setRelease(data, index, value);
        } else {
            h64.setVolatile(data, index, value);
        }
    }

    @Override
    public int compareAndExchange16(int index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load16(index, readMode) & 0xffff;
            if (val == (expect & 0xffff)) {
                store16(index, update, writeMode);
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h16.compareAndExchangeAcquire(data, index, (short) expect, (short) update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h16.compareAndExchangeRelease(data, index, (short) expect, (short) update);
        } else {
            return (int) h16.compareAndExchange(data, index, (short) expect, (short) update);
        }
    }

    @Override
    public int compareAndExchange32(int index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load32(index, readMode);
            if (val == expect) {
                store32(index, update, writeMode);
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h32.compareAndExchangeAcquire(data, index, expect, update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h32.compareAndExchangeRelease(data, index, expect, update);
        } else {
            return (int) h32.compareAndExchange(data, index, expect, update);
        }
    }

    @Override
    public long compareAndExchange64(int index, long expect, long update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            long val = load64(index, readMode);
            if (val == expect) {
                store64(index, update, writeMode);
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (long) h64.compareAndExchangeAcquire(data, index, expect, update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (long) h64.compareAndExchangeRelease(data, index, expect, update);
        } else {
            return (long) h64.compareAndExchange(data, index, expect, update);
        }
    }

    @Override
    public int getAndSet16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load16(index, readMode);
            store16(index, value, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h16.getAndSetAcquire(data, index, (short) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h16.getAndSetRelease(data, index, (short) value);
        } else {
            return (int) h16.getAndSet(data, index, (short) value);
        }
    }

    @Override
    public int getAndSet32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load32(index, readMode);
            store32(index, value, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h32.getAndSetAcquire(data, index, value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h32.getAndSetRelease(data, index, value);
        } else {
            return (int) h32.getAndSet(data, index, value);
        }
    }

    @Override
    public long getAndSet64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            long val = load64(index, readMode);
            store64(index, value, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (long) h64.getAndSetAcquire(data, index, value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (long) h64.getAndSetRelease(data, index, value);
        } else {
            return (long) h64.getAndSet(data, index, value);
        }
    }

    @Override
    public int getAndAdd16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load16(index, readMode);
            store16(index, value + val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h16.getAndAddAcquire(data, index, (short) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h16.getAndAddRelease(data, index, (short) value);
        } else {
            return (int) h16.getAndAdd(data, index, (short) value);
        }
    }

    @Override
    public int getAndAdd32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load32(index, readMode);
            store32(index, value + val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h32.getAndAddAcquire(data, index, value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h32.getAndAddRelease(data, index, value);
        } else {
            return (int) h32.getAndAdd(data, index, value);
        }
    }

    @Override
    public long getAndAdd64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            long val = load64(index, readMode);
            store64(index, value + val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (long) h64.getAndAddAcquire(data, index, value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (long) h64.getAndAddRelease(data, index, value);
        } else {
            return (long) h64.getAndAdd(data, index, value);
        }
    }

    @Override
    public int getAndBitwiseAnd16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load16(index, readMode);
            store16(index, value & val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h16.getAndBitwiseAndAcquire(data, index, (short) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h16.getAndBitwiseAndRelease(data, index, (short) value);
        } else {
            return (int) h16.getAndBitwiseAnd(data, index, (short) value);
        }
    }

    @Override
    public int getAndBitwiseAnd32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load32(index, readMode);
            store32(index, value & val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h32.getAndBitwiseAndAcquire(data, index, value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h32.getAndBitwiseAndRelease(data, index, value);
        } else {
            return (int) h32.getAndBitwiseAnd(data, index, value);
        }
    }

    @Override
    public long getAndBitwiseAnd64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            long val = load64(index, readMode);
            store64(index, value & val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (long) h64.getAndBitwiseAndAcquire(data, index, value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (long) h64.getAndBitwiseAndRelease(data, index, value);
        } else {
            return (long) h64.getAndBitwiseAnd(data, index, value);
        }
    }

    @Override
    public int getAndBitwiseOr16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load16(index, readMode);
            store16(index, value | val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h16.getAndBitwiseOrAcquire(data, index, (short) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h16.getAndBitwiseOrRelease(data, index, (short) value);
        } else {
            return (int) h16.getAndBitwiseOr(data, index, (short) value);
        }
    }

    @Override
    public int getAndBitwiseOr32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load32(index, readMode);
            store32(index, value | val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h32.getAndBitwiseOrAcquire(data, index, value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h32.getAndBitwiseOrRelease(data, index, value);
        } else {
            return (int) h32.getAndBitwiseOr(data, index, value);
        }
    }

    @Override
    public long getAndBitwiseOr64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            long val = load64(index, readMode);
            store64(index, value | val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (long) h64.getAndBitwiseOrAcquire(data, index, value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (long) h64.getAndBitwiseOrRelease(data, index, value);
        } else {
            return (long) h64.getAndBitwiseOr(data, index, value);
        }
    }

    @Override
    public int getAndBitwiseXor16(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load16(index, readMode);
            store16(index, value ^ val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h16.getAndBitwiseXorAcquire(data, index, (short) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h16.getAndBitwiseXorRelease(data, index, (short) value);
        } else {
            return (int) h16.getAndBitwiseXor(data, index, (short) value);
        }
    }

    @Override
    public int getAndBitwiseXor32(int index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load32(index, readMode);
            store32(index, value ^ val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h32.getAndBitwiseXorAcquire(data, index, value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h32.getAndBitwiseXorRelease(data, index, value);
        } else {
            return (int) h32.getAndBitwiseXor(data, index, value);
        }
    }

    @Override
    public long getAndBitwiseXor64(int index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            long val = load64(index, readMode);
            store64(index, value ^ val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (long) h64.getAndBitwiseXorAcquire(data, index, value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (long) h64.getAndBitwiseXorRelease(data, index, value);
        } else {
            return (long) h64.getAndBitwiseXor(data, index, value);
        }
    }

    @Override
    public BigEndianMemoryImpl copy(int newSize) {
        if (newSize == 0) {
            return EMPTY;
        }
        BigEndianMemoryImpl newMemory = new BigEndianMemoryImpl(newSize);
        newMemory.storeMemory(0, this, 0, Math.min(data.length, newMemory.data.length));
        return newMemory;
    }

    protected MemoryImpl clone() {
        return new BigEndianMemoryImpl(this);
    }
}
