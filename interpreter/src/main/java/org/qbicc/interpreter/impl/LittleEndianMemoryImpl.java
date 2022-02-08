package org.qbicc.interpreter.impl;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.qbicc.graph.atomic.AccessModes.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;

final class LittleEndianMemoryImpl extends MemoryImpl {
    static final LittleEndianMemoryImpl EMPTY = new LittleEndianMemoryImpl(0);

    private static final VarHandle h16 = MethodHandles.byteArrayViewVarHandle(short[].class, LITTLE_ENDIAN);
    private static final VarHandle h32 = MethodHandles.byteArrayViewVarHandle(int[].class, LITTLE_ENDIAN);
    private static final VarHandle h64 = MethodHandles.byteArrayViewVarHandle(long[].class, LITTLE_ENDIAN);

    LittleEndianMemoryImpl(int dataSize) {
        super(dataSize);
    }

    LittleEndianMemoryImpl(final LittleEndianMemoryImpl original) {
        super(original);
    }

    @Override
    public int load16(long index, ReadAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            return (int) h16.get(data, Math.toIntExact(index));
        } else if (SingleOpaque.includes(mode)) {
            return (int) h16.getOpaque(data, Math.toIntExact(index));
        } else if (GlobalAcquire.includes(mode)) {
            return (int) h16.getAcquire(data, Math.toIntExact(index));
        } else {
            return (int) h16.getVolatile(data, Math.toIntExact(index));
        }
    }

    @Override
    public int load32(long index, ReadAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            return (int) h32.get(data, Math.toIntExact(index));
        } else if (SingleOpaque.includes(mode)) {
            return (int) h32.getOpaque(data, Math.toIntExact(index));
        } else if (GlobalAcquire.includes(mode)) {
            return (int) h32.getAcquire(data, Math.toIntExact(index));
        } else {
            return (int) h32.getVolatile(data, Math.toIntExact(index));
        }
    }

    @Override
    public long load64(long index, ReadAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            return (long) h64.get(data, Math.toIntExact(index));
        } else if (SingleOpaque.includes(mode)) {
            return (long) h64.getOpaque(data, Math.toIntExact(index));
        } else if (GlobalAcquire.includes(mode)) {
            return (long) h64.getAcquire(data, Math.toIntExact(index));
        } else {
            return (long) h64.getVolatile(data, Math.toIntExact(index));
        }
    }

    @Override
    public void store16(long index, int value, WriteAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            h16.set(data, Math.toIntExact(index), (short) value);
        } else if (SingleOpaque.includes(mode)) {
            h16.setOpaque(data, Math.toIntExact(index), (short) value);
        } else if (GlobalRelease.includes(mode)) {
            h16.setRelease(data, Math.toIntExact(index), (short) value);
        } else {
            h16.setVolatile(data, Math.toIntExact(index), (short) value);
        }
    }

    @Override
    public void store32(long index, int value, WriteAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            h32.set(data, Math.toIntExact(index), value);
        } else if (SingleOpaque.includes(mode)) {
            h32.setOpaque(data, Math.toIntExact(index), value);
        } else if (GlobalRelease.includes(mode)) {
            h32.setRelease(data, Math.toIntExact(index), value);
        } else {
            h32.setVolatile(data, Math.toIntExact(index), value);
        }
    }

    @Override
    public void store64(long index, long value, WriteAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            h64.set(data, Math.toIntExact(index), value);
        } else if (SingleOpaque.includes(mode)) {
            h64.setOpaque(data, Math.toIntExact(index), value);
        } else if (GlobalRelease.includes(mode)) {
            h64.setRelease(data, Math.toIntExact(index), value);
        } else {
            h64.setVolatile(data, Math.toIntExact(index), value);
        }
    }

    @Override
    public int compareAndExchange16(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load16(index, readMode) & 0xffff;
            if (val == (expect & 0xffff)) {
                store16(index, update, writeMode);
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h16.compareAndExchangeAcquire(data, Math.toIntExact(index), (short) expect, (short) update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h16.compareAndExchangeRelease(data, Math.toIntExact(index), (short) expect, (short) update);
        } else {
            return (int) h16.compareAndExchange(data, Math.toIntExact(index), (short) expect, (short) update);
        }
    }

    @Override
    public int compareAndExchange32(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load32(index, readMode);
            if (val == expect) {
                store32(index, update, writeMode);
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h32.compareAndExchangeAcquire(data, Math.toIntExact(index), expect, update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h32.compareAndExchangeRelease(data, Math.toIntExact(index), expect, update);
        } else {
            return (int) h32.compareAndExchange(data, Math.toIntExact(index), expect, update);
        }
    }

    @Override
    public long compareAndExchange64(long index, long expect, long update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            long val = load64(index, readMode);
            if (val == expect) {
                store64(index, update, writeMode);
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (long) h64.compareAndExchangeAcquire(data, Math.toIntExact(index), expect, update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (long) h64.compareAndExchangeRelease(data, Math.toIntExact(index), expect, update);
        } else {
            return (long) h64.compareAndExchange(data, Math.toIntExact(index), expect, update);
        }
    }

    @Override
    public int getAndSet16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load16(index, readMode);
            store16(index, value, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h16.getAndSetAcquire(data, Math.toIntExact(index), (short) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h16.getAndSetRelease(data, Math.toIntExact(index), (short) value);
        } else {
            return (int) h16.getAndSet(data, Math.toIntExact(index), (short) value);
        }
    }

    @Override
    public int getAndSet32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load32(index, readMode);
            store32(index, value, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h32.getAndSetAcquire(data, Math.toIntExact(index), value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h32.getAndSetRelease(data, Math.toIntExact(index), value);
        } else {
            return (int) h32.getAndSet(data, Math.toIntExact(index), value);
        }
    }

    @Override
    public long getAndSet64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            long val = load64(index, readMode);
            store64(index, value, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (long) h64.getAndSetAcquire(data, Math.toIntExact(index), value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (long) h64.getAndSetRelease(data, Math.toIntExact(index), value);
        } else {
            return (long) h64.getAndSet(data, Math.toIntExact(index), value);
        }
    }

    @Override
    public int getAndAdd16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load16(index, readMode);
            store16(index, value + val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h16.getAndAddAcquire(data, Math.toIntExact(index), (short) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h16.getAndAddRelease(data, Math.toIntExact(index), (short) value);
        } else {
            return (int) h16.getAndAdd(data, Math.toIntExact(index), (short) value);
        }
    }

    @Override
    public int getAndAdd32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load32(index, readMode);
            store32(index, value + val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h32.getAndAddAcquire(data, Math.toIntExact(index), value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h32.getAndAddRelease(data, Math.toIntExact(index), value);
        } else {
            return (int) h32.getAndAdd(data, Math.toIntExact(index), value);
        }
    }

    @Override
    public long getAndAdd64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            long val = load64(index, readMode);
            store64(index, value + val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (long) h64.getAndAddAcquire(data, Math.toIntExact(index), value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (long) h64.getAndAddRelease(data, Math.toIntExact(index), value);
        } else {
            return (long) h64.getAndAdd(data, Math.toIntExact(index), value);
        }
    }

    @Override
    public int getAndBitwiseAnd16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load16(index, readMode);
            store16(index, value & val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h16.getAndBitwiseAndAcquire(data, Math.toIntExact(index), (short) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h16.getAndBitwiseAndRelease(data, Math.toIntExact(index), (short) value);
        } else {
            return (int) h16.getAndBitwiseAnd(data, Math.toIntExact(index), (short) value);
        }
    }

    @Override
    public int getAndBitwiseAnd32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load32(index, readMode);
            store32(index, value & val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h32.getAndBitwiseAndAcquire(data, Math.toIntExact(index), value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h32.getAndBitwiseAndRelease(data, Math.toIntExact(index), value);
        } else {
            return (int) h32.getAndBitwiseAnd(data, Math.toIntExact(index), value);
        }
    }

    @Override
    public long getAndBitwiseAnd64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            long val = load64(index, readMode);
            store64(index, value & val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (long) h64.getAndBitwiseAndAcquire(data, Math.toIntExact(index), value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (long) h64.getAndBitwiseAndRelease(data, Math.toIntExact(index), value);
        } else {
            return (long) h64.getAndBitwiseAnd(data, Math.toIntExact(index), value);
        }
    }

    @Override
    public int getAndBitwiseOr16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load16(index, readMode);
            store16(index, value | val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h16.getAndBitwiseOrAcquire(data, Math.toIntExact(index), (short) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h16.getAndBitwiseOrRelease(data, Math.toIntExact(index), (short) value);
        } else {
            return (int) h16.getAndBitwiseOr(data, Math.toIntExact(index), (short) value);
        }
    }

    @Override
    public int getAndBitwiseOr32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load32(index, readMode);
            store32(index, value | val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h32.getAndBitwiseOrAcquire(data, Math.toIntExact(index), value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h32.getAndBitwiseOrRelease(data, Math.toIntExact(index), value);
        } else {
            return (int) h32.getAndBitwiseOr(data, Math.toIntExact(index), value);
        }
    }

    @Override
    public long getAndBitwiseOr64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            long val = load64(index, readMode);
            store64(index, value | val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (long) h64.getAndBitwiseOrAcquire(data, Math.toIntExact(index), value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (long) h64.getAndBitwiseOrRelease(data, Math.toIntExact(index), value);
        } else {
            return (long) h64.getAndBitwiseOr(data, Math.toIntExact(index), value);
        }
    }

    @Override
    public int getAndBitwiseXor16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load16(index, readMode);
            store16(index, value ^ val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h16.getAndBitwiseXorAcquire(data, Math.toIntExact(index), (short) value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h16.getAndBitwiseXorRelease(data, Math.toIntExact(index), (short) value);
        } else {
            return (int) h16.getAndBitwiseXor(data, Math.toIntExact(index), (short) value);
        }
    }

    @Override
    public int getAndBitwiseXor32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load32(index, readMode);
            store32(index, value ^ val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h32.getAndBitwiseXorAcquire(data, Math.toIntExact(index), value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h32.getAndBitwiseXorRelease(data, Math.toIntExact(index), value);
        } else {
            return (int) h32.getAndBitwiseXor(data, Math.toIntExact(index), value);
        }
    }

    @Override
    public long getAndBitwiseXor64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            long val = load64(index, readMode);
            store64(index, value ^ val, writeMode);
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (long) h64.getAndBitwiseXorAcquire(data, Math.toIntExact(index), value);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (long) h64.getAndBitwiseXorRelease(data, Math.toIntExact(index), value);
        } else {
            return (long) h64.getAndBitwiseXor(data, Math.toIntExact(index), value);
        }
    }

    @Override
    public LittleEndianMemoryImpl copy(long newSize) {
        if (newSize == 0) {
            return EMPTY;
        }
        LittleEndianMemoryImpl newMemory = new LittleEndianMemoryImpl(Math.toIntExact(newSize));
        newMemory.storeMemory(0, this, 0, Math.min(data.length, newMemory.data.length));
        return newMemory;
    }

    protected MemoryImpl clone() {
        return new LittleEndianMemoryImpl(this);
    }
}
