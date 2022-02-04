package org.qbicc.interpreter.memory;

import static org.qbicc.graph.atomic.AccessModes.*;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.impl.InvalidMemoryAccessException;
import org.qbicc.pointer.Pointer;
import org.qbicc.type.ValueType;

/**
 * A memory region which is backed by a {@code byte} array which can be directly accessed.
 */
public abstract class ByteArrayMemory implements Memory {
    private static final VarHandle h8 = ConstantBootstraps.arrayVarHandle(MethodHandles.lookup(), "ignored", VarHandle.class, byte[].class);

    final byte[] array;

    ByteArrayMemory(byte[] array) {
        this.array = array;
    }

    public byte[] getArray() {
        return array;
    }

    abstract VarHandle h16();
    abstract VarHandle h32();
    abstract VarHandle h64();

    @Override
    public int load8(long index, ReadAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            return array[Math.toIntExact(index)] & 0xff;
        } else if (SingleOpaque.includes(mode)) {
            return (int) h8.getOpaque(array, Math.toIntExact(index)) & 0xff;
        } else if (GlobalAcquire.includes(mode)) {
            return (int) h8.getAcquire(array, Math.toIntExact(index)) & 0xff;
        } else {
            return (int) h8.getVolatile(array, Math.toIntExact(index)) & 0xff;
        }
    }

    @Override
    public int load16(long index, ReadAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            return (int) h16().get(array, Math.toIntExact(index)) & 0xff;
        } else if (SingleOpaque.includes(mode)) {
            return (int) h16().getOpaque(array, Math.toIntExact(index)) & 0xff;
        } else if (GlobalAcquire.includes(mode)) {
            return (int) h16().getAcquire(array, Math.toIntExact(index)) & 0xff;
        } else {
            return (int) h16().getVolatile(array, Math.toIntExact(index)) & 0xff;
        }
    }

    @Override
    public int load32(long index, ReadAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            return (int) h32().get(array, Math.toIntExact(index)) & 0xff;
        } else if (SingleOpaque.includes(mode)) {
            return (int) h32().getOpaque(array, Math.toIntExact(index)) & 0xff;
        } else if (GlobalAcquire.includes(mode)) {
            return (int) h32().getAcquire(array, Math.toIntExact(index)) & 0xff;
        } else {
            return (int) h32().getVolatile(array, Math.toIntExact(index)) & 0xff;
        }
    }

    @Override
    public long load64(long index, ReadAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            return (long) h64().get(array, Math.toIntExact(index)) & 0xff;
        } else if (SingleOpaque.includes(mode)) {
            return (long) h64().getOpaque(array, Math.toIntExact(index)) & 0xff;
        } else if (GlobalAcquire.includes(mode)) {
            return (long) h64().getAcquire(array, Math.toIntExact(index)) & 0xff;
        } else {
            return (long) h64().getVolatile(array, Math.toIntExact(index)) & 0xff;
        }
    }

    @Override
    public VmObject loadRef(long index, ReadAccessMode mode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public ValueType loadType(long index, ReadAccessMode mode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public Pointer loadPointer(long index, ReadAccessMode mode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public void store8(long index, int value, WriteAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            array[(int) index] = (byte) value;
        } else if (SingleOpaque.includes(mode)) {
            h8.setOpaque(array, Math.toIntExact(index), (byte) value);
        } else if (GlobalRelease.includes(mode)) {
            h8.setRelease(array, Math.toIntExact(index), (byte) value);
        } else {
            h8.setVolatile(array, Math.toIntExact(index), (byte) value);
        }
    }

    @Override
    public void store16(long index, int value, WriteAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            h16().set(array, Math.toIntExact(index), (short) value);
        } else if (SingleOpaque.includes(mode)) {
            h16().setOpaque(array, Math.toIntExact(index), (short) value);
        } else if (GlobalRelease.includes(mode)) {
            h16().setRelease(array, Math.toIntExact(index), (short) value);
        } else {
            h16().setVolatile(array, Math.toIntExact(index), (short) value);
        }
    }

    @Override
    public void store32(long index, int value, WriteAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            h32().set(array, Math.toIntExact(index), value);
        } else if (SingleOpaque.includes(mode)) {
            h32().setOpaque(array, Math.toIntExact(index), value);
        } else if (GlobalRelease.includes(mode)) {
            h32().setRelease(array, Math.toIntExact(index), value);
        } else {
            h32().setVolatile(array, Math.toIntExact(index), value);
        }
    }

    @Override
    public void store64(long index, long value, WriteAccessMode mode) {
        if (GlobalPlain.includes(mode)) {
            h64().set(array, Math.toIntExact(index), value);
        } else if (SingleOpaque.includes(mode)) {
            h64().setOpaque(array, Math.toIntExact(index), value);
        } else if (GlobalRelease.includes(mode)) {
            h64().setRelease(array, Math.toIntExact(index), value);
        } else {
            h64().setVolatile(array, Math.toIntExact(index), value);
        }
    }

    @Override
    public void storeRef(long index, VmObject value, WriteAccessMode mode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public void storeType(long index, ValueType value, WriteAccessMode mode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public void storePointer(long index, Pointer value, WriteAccessMode mode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public void storeMemory(long destIndex, Memory src, long srcIndex, long size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeMemory(long destIndex, byte[] src, int srcIndex, int size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareAndExchange8(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            int val = load8(index, readMode) & 0xff;
            if (val == (expect & 0xff)) {
                store8(index, update, writeMode);
            }
            return val;
        } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
            return (int) h8.compareAndExchangeAcquire(array, Math.toIntExact(index), (byte) expect, (byte) update) & 0xff;
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h8.compareAndExchangeRelease(array, Math.toIntExact(index), (byte) expect, (byte) update) & 0xff;
        } else {
            return (int) h8.compareAndExchange(array, Math.toIntExact(index), (byte) expect, (byte) update) & 0xff;
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
            return (int) h16().compareAndExchangeAcquire(array, Math.toIntExact(index), (short) expect, (short) update) & 0xffff;
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h16().compareAndExchangeRelease(array, Math.toIntExact(index), (short) expect, (short) update) & 0xffff;
        } else {
            return (int) h16().compareAndExchange(array, Math.toIntExact(index), (short) expect, (short) update) & 0xffff;
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
            return (int) h32().compareAndExchangeAcquire(array, Math.toIntExact(index), expect, update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (int) h32().compareAndExchangeRelease(array, Math.toIntExact(index), expect, update);
        } else {
            return (int) h32().compareAndExchange(array, Math.toIntExact(index), expect, update);
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
            return (long) h64().compareAndExchangeAcquire(array, Math.toIntExact(index), expect, update);
        } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
            return (long) h64().compareAndExchangeRelease(array, Math.toIntExact(index), expect, update);
        } else {
            return (long) h64().compareAndExchange(array, Math.toIntExact(index), expect, update);
        }
    }

    @Override
    public VmObject compareAndExchangeRef(long index, VmObject expect, VmObject update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public ValueType compareAndExchangeType(long index, ValueType expect, ValueType update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public Pointer compareAndExchangePointer(long index, Pointer expect, Pointer update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        throw new InvalidMemoryAccessException();
    }

    @Override
    public abstract Memory clone();

    @Override
    public long getSize() {
        return array.length;
    }

    static final class BE extends ByteArrayMemory {
        private static final VarHandle h16 = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);
        private static final VarHandle h32 = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
        private static final VarHandle h64 = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);

        BE(byte[] array) {
            super(array);
        }

        @Override
        VarHandle h16() {
            return h16;
        }

        @Override
        VarHandle h32() {
            return h32;
        }

        @Override
        VarHandle h64() {
            return h64;
        }

        @Override
        public Memory copy(long newSize) {
            return new BE(Arrays.copyOf(array, Math.toIntExact(newSize)));
        }

        @Override
        public Memory clone() {
            return new BE(array.clone());
        }

        @Override
        public Memory cloneZeroed() {
            return new BE(new byte[array.length]);
        }
    }

    static final class LE extends ByteArrayMemory {
        private static final VarHandle h16 = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
        private static final VarHandle h32 = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
        private static final VarHandle h64 = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

        LE(byte[] array) {
            super(array);
        }

        @Override
        VarHandle h16() {
            return h16;
        }

        @Override
        VarHandle h32() {
            return h32;
        }

        @Override
        VarHandle h64() {
            return h64;
        }

        @Override
        public Memory copy(long newSize) {
            return new LE(Arrays.copyOf(array, Math.toIntExact(newSize)));
        }

        @Override
        public Memory clone() {
            return new LE(array.clone());
        }

        @Override
        public Memory cloneZeroed() {
            return new LE(new byte[array.length]);
        }
    }
}
