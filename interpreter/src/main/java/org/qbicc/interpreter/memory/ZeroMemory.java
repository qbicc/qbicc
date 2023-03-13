package org.qbicc.interpreter.memory;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmObject;
import org.qbicc.pointer.Pointer;
import org.qbicc.type.ArrayType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.ValueType;

/**
 * A memory which is always zero and cannot be written to (other than to write zero).
 */
public final class ZeroMemory extends AbstractMemory {
    private final long size;

    ZeroMemory(final long size) {
        this.size = size;
    }

    @Override
    public int load8(long index, ReadAccessMode mode) {
        return 0;
    }

    @Override
    public int load16(long index, ReadAccessMode mode) {
        return 0;
    }

    @Override
    public int load32(long index, ReadAccessMode mode) {
        return 0;
    }

    @Override
    public long load64(long index, ReadAccessMode mode) {
        return 0;
    }

    @Override
    public VmObject loadRef(long index, ReadAccessMode mode) {
        return null;
    }

    @Override
    public ValueType loadType(long index, ReadAccessMode mode) {
        // todo: type zero
        return null;
    }

    @Override
    public Pointer loadPointer(long index, ReadAccessMode mode) {
        return null;
    }

    @Override
    public void store8(long index, int value, WriteAccessMode mode) {
        if (value == 0) {
            return;
        }
        super.store8(index, value, mode);
    }

    @Override
    public void store16(long index, int value, WriteAccessMode mode) {
        if (value == 0) {
            return;
        }
        super.store16(index, value, mode);
    }

    @Override
    public void store32(long index, int value, WriteAccessMode mode) {
        if (value == 0) {
            return;
        }
        super.store32(index, value, mode);
    }

    @Override
    public void store64(long index, long value, WriteAccessMode mode) {
        if (value == 0) {
            return;
        }
        super.store64(index, value, mode);
    }

    @Override
    public void storeRef(long index, VmObject value, WriteAccessMode mode) {
        if (value == null) {
            return;
        }
        super.storeRef(index, value, mode);
    }

    @Override
    public void storeType(long index, ValueType value, WriteAccessMode mode) {
        if (value == null) {
            // todo: type zero
            return;
        }
        super.storeType(index, value, mode);
    }

    @Override
    public void storePointer(long index, Pointer value, WriteAccessMode mode) {
        if (value == null) {
            return;
        }
        super.storePointer(index, value, mode);
    }

    @Override
    public int compareAndExchange8(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (expect != 0 || update == 0) {
            return 0;
        }
        return super.compareAndExchange8(index, expect, update, readMode, writeMode);
    }

    @Override
    public int compareAndExchange16(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (expect != 0 || update == 0) {
            return 0;
        }
        return super.compareAndExchange16(index, expect, update, readMode, writeMode);
    }

    @Override
    public int compareAndExchange32(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (expect != 0 || update == 0) {
            return 0;
        }
        return super.compareAndExchange32(index, expect, update, readMode, writeMode);
    }

    @Override
    public long compareAndExchange64(long index, long expect, long update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (expect != 0 || update == 0) {
            return 0;
        }
        return super.compareAndExchange64(index, expect, update, readMode, writeMode);
    }

    @Override
    public VmObject compareAndExchangeRef(long index, VmObject expect, VmObject update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (expect != null || update == null) {
            return null;
        }
        return super.compareAndExchangeRef(index, expect, update, readMode, writeMode);
    }

    @Override
    public ValueType compareAndExchangeType(long index, ValueType expect, ValueType update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (expect != null || update == null) {
            // todo: type zero
            return null;
        }
        return super.compareAndExchangeType(index, expect, update, readMode, writeMode);
    }

    @Override
    public Pointer compareAndExchangePointer(long index, Pointer expect, Pointer update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (expect != null || update == null) {
            return null;
        }
        return super.compareAndExchangePointer(index, expect, update, readMode, writeMode);
    }

    @Override
    public Memory copy(long newSize) {
        return new ZeroMemory(newSize);
    }

    @Override
    public Memory cloneZeroed() {
        return this;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public Memory clone() {
        return this;
    }
}
