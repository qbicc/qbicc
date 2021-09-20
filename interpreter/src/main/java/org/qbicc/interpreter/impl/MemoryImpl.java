package org.qbicc.interpreter.impl;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmObject;

/**
 *
 */
abstract class MemoryImpl implements Memory {
    private static final byte[] NO_DATA = new byte[0];
    private static final Referenceable[] NO_REFS = new Referenceable[0];

    private static final VarHandle hr = MethodHandles.arrayElementVarHandle(Referenceable[].class);
    private static final VarHandle h8 = MethodHandles.arrayElementVarHandle(byte[].class);
    private static final VarHandle h16 = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.nativeOrder());
    private static final VarHandle h32 = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.nativeOrder());
    private static final VarHandle h64 = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.nativeOrder());
    private final byte[] data;
    private final Referenceable[] refs;

    MemoryImpl(int dataSize, int refShift) {
        int mask = (1 << refShift) - 1;
        dataSize = (dataSize + mask) & ~mask;
        int refSize = dataSize >> refShift;
        data = dataSize == 0 ? NO_DATA : new byte[dataSize];
        refs = dataSize == 0 ? NO_REFS : new Referenceable[refSize];
    }

    static int alignGap(final int align, final int offset) {
        int mask = align - 1;
        return align - offset & mask;
    }

    static void checkAlign(int offs, int align) {
        int mask = align - 1;
        if ((offs & mask) != 0) {
            throw new IllegalArgumentException("Invalid unaligned access: 0x" + Integer.toHexString(offs));
        }
    }

    @Override
    public int load8(int index, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            return (int) h8.get(data, index);
        } else if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h8.getAcquire(data, index);
        } else {
            return (int) h8.getVolatile(data, index);
        }
    }

    @Override
    public int load16(int index, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            return (int) h16.get(data, index);
        } else if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h16.getAcquire(data, index);
        } else {
            return (int) h16.getVolatile(data, index);
        }
    }

    @Override
    public int load32(int index, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            return (int) h32.get(data, index);
        } else if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h32.getAcquire(data, index);
        } else {
            return (int) h32.getVolatile(data, index);
        }
    }

    @Override
    public long load64(int index, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            return (long) h64.get(data, index);
        } else if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (long) h64.getAcquire(data, index);
        } else {
            return (long) h64.getVolatile(data, index);
        }
    }

    @Override
    public void store8(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            h8.set(data, index, (byte) value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            h8.setRelease(data, index, (byte) value);
        } else {
            h8.setVolatile(data, index, (byte) value);
        }
    }

    @Override
    public void store16(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            h16.set(data, index, (short) value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            h16.setRelease(data, index, (short) value);
        } else {
            h16.setVolatile(data, index, (short) value);
        }
    }

    @Override
    public void store32(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            h32.set(data, index, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            h32.setRelease(data, index, value);
        } else {
            h32.setVolatile(data, index, value);
        }
    }

    @Override
    public void store64(int index, long value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            h64.set(data, index, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            h64.setRelease(data, index, value);
        } else {
            h64.setVolatile(data, index, value);
        }
    }

    @Override
    public int compareAndExchange8(int index, int expect, int update, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h8.compareAndExchangeAcquire(data, index, (byte) expect, (byte) update);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h8.compareAndExchangeRelease(data, index, (byte) expect, (byte) update);
        } else {
            return (int) h8.compareAndExchange(data, index, (byte) expect, (byte) update);
        }
    }

    @Override
    public int compareAndExchange16(int index, int expect, int update, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h16.compareAndExchangeAcquire(data, index, (short) expect, (short) update);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h16.compareAndExchangeRelease(data, index, (short) expect, (short) update);
        } else {
            return (int) h16.compareAndExchange(data, index, (short) expect, (short) update);
        }
    }

    @Override
    public int compareAndExchange32(int index, int expect, int update, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h32.compareAndExchangeAcquire(data, index, expect, update);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h32.compareAndExchangeRelease(data, index, expect, update);
        } else {
            return (int) h32.compareAndExchange(data, index, expect, update);
        }
    }

    @Override
    public long compareAndExchange64(int index, long expect, long update, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (long) h64.compareAndExchangeAcquire(data, index, expect, update);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (long) h64.compareAndExchangeRelease(data, index, expect, update);
        } else {
            return (long) h64.compareAndExchange(data, index, expect, update);
        }
    }

    Referenceable compareAndExchangeRefAligned(int index, Referenceable expect, Referenceable update, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (Referenceable) hr.compareAndExchangeAcquire(refs, index, expect, update);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (Referenceable) hr.compareAndExchangeRelease(refs, index, expect, update);
        } else {
            return (Referenceable) hr.compareAndExchange(refs, index, expect, update);
        }
    }

    @Override
    public int getAndSet8(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h8.getAndSetAcquire(data, index, (byte) value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h8.getAndSetRelease(data, index, (byte) value);
        } else {
            return (int) h8.getAndSet(data, index, (byte) value);
        }
    }

    @Override
    public int getAndSet16(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h16.getAndSetAcquire(data, index, (short) value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h16.getAndSetRelease(data, index, (short) value);
        } else {
            return (int) h16.getAndSet(data, index, (short) value);
        }
    }

    @Override
    public int getAndSet32(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h32.getAndSetAcquire(data, index, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h32.getAndSetRelease(data, index, value);
        } else {
            return (int) h32.getAndSet(data, index, value);
        }
    }

    @Override
    public long getAndSet64(int index, long value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (long) h64.getAndSetAcquire(data, index, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (long) h64.getAndSetRelease(data, index, value);
        } else {
            return (long) h64.getAndSet(data, index, value);
        }
    }

    Referenceable getAndSetRefAligned(int index, Referenceable value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (Referenceable) hr.getAndSetAcquire(refs, index, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (Referenceable) hr.getAndSetRelease(refs, index, value);
        } else {
            return (Referenceable) hr.getAndSet(refs, index, value);
        }
    }

    @Override
    public int getAndAdd8(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h8.getAndAddAcquire(data, index, (byte) value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h8.getAndAddRelease(data, index, (byte) value);
        } else {
            return (int) h8.getAndAdd(data, index, (byte) value);
        }
    }

    @Override
    public int getAndAdd16(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h16.getAndAddAcquire(data, index, (short) value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h16.getAndAddRelease(data, index, (short) value);
        } else {
            return (int) h16.getAndAdd(data, index, (short) value);
        }
    }

    @Override
    public int getAndAdd32(int index, int value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (int) h32.getAndAddAcquire(data, index, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (int) h32.getAndAddRelease(data, index, value);
        } else {
            return (int) h32.getAndAdd(data, index, value);
        }
    }

    @Override
    public long getAndAdd64(int index, long value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (long) h64.getAndAddAcquire(data, index, value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            return (long) h64.getAndAddRelease(data, index, value);
        } else {
            return (long) h64.getAndAdd(data, index, value);
        }
    }

    @Override
    public void storeMemory(int destIndex, Memory src, int srcIndex, int size) {
        MemoryImpl srcImpl = (MemoryImpl) src;
        System.arraycopy(srcImpl.data, srcIndex, data, destIndex, size);
    }

    void storeMemoryAligned(int destIndex, MemoryImpl srcImpl, int srcIndex, int size) {
        System.arraycopy(srcImpl.refs, srcIndex, refs, destIndex, size);
    }

    @Override
    public void storeMemory(int destIndex, byte[] src, int srcIndex, int size) {
        // just data
        System.arraycopy(src, srcIndex, data, destIndex, size);
        clearRefs(destIndex, size);
    }

    abstract void clearRefs(int startIdx, int byteSize);

    void clearRefsAligned(int refIdx, int refSize) {
        Arrays.fill(refs, refIdx, refIdx + refSize, null);
    }

    VmObject loadRefAligned(int index, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            return (VmObject) (Referenceable) hr.get(refs, index);
        } else if (mode == MemoryAtomicityMode.ACQUIRE) {
            return (VmObject) (Referenceable) hr.getAcquire(refs, index);
        } else {
            return (VmObject) (Referenceable) hr.getVolatile(refs, index);
        }
    }

    void storeRefAligned(int index, VmObject value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.NONE || mode == MemoryAtomicityMode.UNORDERED) {
            hr.set(refs, index, (Referenceable) value);
        } else if (mode == MemoryAtomicityMode.RELEASE) {
            hr.setRelease(refs, index, (Referenceable) value);
        } else {
            hr.setVolatile(refs, index, (Referenceable) value);
        }
    }

    byte[] getArray() {
        return data;
    }
}
