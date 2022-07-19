package org.qbicc.interpreter.memory;

import static org.qbicc.graph.atomic.AccessModes.*;

import java.lang.invoke.VarHandle;
import java.util.Objects;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.interpreter.InvalidMemoryAccessException;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmObject;
import org.qbicc.pointer.IntegerAsPointer;
import org.qbicc.pointer.Pointer;
import org.qbicc.type.PointerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;

/**
 * A memory implementation which uses {@link java.lang.invoke.VarHandle} to access its members.
 */
public abstract class VarHandleMemory extends AbstractMemory {

    /**
     * Construct a new instance.
     */
    protected VarHandleMemory() {
    }

    protected abstract CompilationContext getCompilationContext();

    protected Memory getDelegateMemory(int offset) {
        return null;
    }

    protected VarHandle getHandle(int offset) {
        throw invalidMemoryAccess();
    }

    @Override
    public int load8(long offset, ReadAccessMode mode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.load8(offset, mode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == boolean.class) {
            if (GlobalPlain.includes(mode)) {
                return (boolean) handle.get(this) ? 1 : 0;
            } else if (SingleOpaque.includes(mode)) {
                return (boolean) handle.getOpaque(this) ? 1 : 0;
            } else if (GlobalAcquire.includes(mode)) {
                return (boolean) handle.getAcquire(this) ? 1 : 0;
            } else {
                return (boolean) handle.getVolatile(this) ? 1 : 0;
            }
        } else if (handle.varType() == byte.class) {
            if (GlobalPlain.includes(mode)) {
                return (int) handle.get(this) & 0xff;
            } else if (SingleOpaque.includes(mode)) {
                return (int) handle.getOpaque(this) & 0xff;
            } else if (GlobalAcquire.includes(mode)) {
                return (int) handle.getAcquire(this) & 0xff;
            } else {
                return (int) handle.getVolatile(this) & 0xff;
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int load16(long offset, ReadAccessMode mode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.load16(offset, mode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == short.class || handle.varType() == char.class) {
            if (GlobalPlain.includes(mode)) {
                return (int) handle.get(this) & 0xffff;
            } else if (SingleOpaque.includes(mode)) {
                return (int) handle.getOpaque(this) & 0xffff;
            } else if (GlobalAcquire.includes(mode)) {
                return (int) handle.getAcquire(this) & 0xffff;
            } else {
                return (int) handle.getVolatile(this) & 0xffff;
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int load32(long offset, ReadAccessMode mode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.load32(offset, mode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == float.class) {
            if (GlobalPlain.includes(mode)) {
                return Float.floatToRawIntBits((float) handle.get(this));
            } else if (SingleOpaque.includes(mode)) {
                return Float.floatToRawIntBits((float) handle.getOpaque(this));
            } else if (GlobalAcquire.includes(mode)) {
                return Float.floatToRawIntBits((float) handle.getAcquire(this));
            } else {
                return Float.floatToRawIntBits((float) handle.getVolatile(this));
            }
        } else if (handle.varType() == int.class) {
            if (GlobalPlain.includes(mode)) {
                return (int) handle.get(this);
            } else if (SingleOpaque.includes(mode)) {
                return (int) handle.getOpaque(this);
            } else if (GlobalAcquire.includes(mode)) {
                return (int) handle.getAcquire(this);
            } else {
                return (int) handle.getVolatile(this);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public long load64(long offset, ReadAccessMode mode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.load64(offset, mode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == Pointer.class) {
            Pointer pointer = loadPointer(offset, mode);
            if (pointer == null) {
                return 0;
            } else if (pointer instanceof IntegerAsPointer iap) {
                return iap.getValue();
            } else {
                throw pointerAsInteger();
            }
        } else if (handle.varType() == double.class) {
            if (GlobalPlain.includes(mode)) {
                return Double.doubleToRawLongBits((double) handle.get(this));
            } else if (SingleOpaque.includes(mode)) {
                return Double.doubleToRawLongBits((double) handle.getOpaque(this));
            } else if (GlobalAcquire.includes(mode)) {
                return Double.doubleToRawLongBits((double) handle.getAcquire(this));
            } else {
                return Double.doubleToRawLongBits((double) handle.getVolatile(this));
            }
        } else if (handle.varType() == long.class) {
            if (GlobalPlain.includes(mode)) {
                return (long) handle.get(this);
            } else if (SingleOpaque.includes(mode)) {
                return (long) handle.getOpaque(this);
            } else if (GlobalAcquire.includes(mode)) {
                return (long) handle.getAcquire(this);
            } else {
                return (long) handle.getVolatile(this);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public VmObject loadRef(long offset, ReadAccessMode mode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.loadRef(offset, mode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == VmObject.class) {
            if (GlobalPlain.includes(mode)) {
                return (VmObject) handle.get(this);
            } else if (SingleOpaque.includes(mode)) {
                return (VmObject) handle.getOpaque(this);
            } else if (GlobalAcquire.includes(mode)) {
                return (VmObject) handle.getAcquire(this);
            } else {
                return (VmObject) handle.getVolatile(this);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public ValueType loadType(long offset, ReadAccessMode mode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.loadType(offset, mode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == ValueType.class) {
            if (GlobalPlain.includes(mode)) {
                return (ValueType) handle.get(this);
            } else if (SingleOpaque.includes(mode)) {
                return (ValueType) handle.getOpaque(this);
            } else if (GlobalAcquire.includes(mode)) {
                return (ValueType) handle.getAcquire(this);
            } else {
                return (ValueType) handle.getVolatile(this);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public Pointer loadPointer(long offset, ReadAccessMode mode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.loadPointer(offset, mode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == Pointer.class) {
            if (GlobalPlain.includes(mode)) {
                return (Pointer) handle.get(this);
            } else if (SingleOpaque.includes(mode)) {
                return (Pointer) handle.getOpaque(this);
            } else if (GlobalAcquire.includes(mode)) {
                return (Pointer) handle.getAcquire(this);
            } else {
                return (Pointer) handle.getVolatile(this);
            }
        } else if (handle.varType() == long.class) {
            PointerType voidPointerType = getCompilationContext().getTypeSystem().getVoidType().getPointer();
            return new IntegerAsPointer(voidPointerType, load64(offset, mode));
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public void store8(long offset, int value, WriteAccessMode mode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            delegateMemory.store8(offset, value, mode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == boolean.class) {
            if (GlobalPlain.includes(mode)) {
                handle.set(this, (value & 1) != 0);
            } else if (SingleOpaque.includes(mode)) {
                handle.setOpaque(this, (value & 1) != 0);
            } else if (GlobalRelease.includes(mode)) {
                handle.setRelease(this, (value & 1) != 0);
            } else {
                handle.setVolatile(this, (value & 1) != 0);
            }
        } else if (handle.varType() == byte.class) {
            if (GlobalPlain.includes(mode)) {
                handle.set(this, (byte) value);
            } else if (SingleOpaque.includes(mode)) {
                handle.setOpaque(this, (byte) value);
            } else if (GlobalRelease.includes(mode)) {
                handle.setRelease(this, (byte) value);
            } else {
                handle.setVolatile(this, (byte) value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public void store16(long offset, int value, WriteAccessMode mode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            delegateMemory.store16(offset, value, mode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == char.class) {
            if (GlobalPlain.includes(mode)) {
                handle.set(this, (char) value);
            } else if (SingleOpaque.includes(mode)) {
                handle.setOpaque(this, (char) value);
            } else if (GlobalRelease.includes(mode)) {
                handle.setRelease(this, (char) value);
            } else {
                handle.setVolatile(this, (char) value);
            }
        } else if (handle.varType() == short.class) {
            if (GlobalPlain.includes(mode)) {
                handle.set(this, (short) value);
            } else if (SingleOpaque.includes(mode)) {
                handle.setOpaque(this, (short) value);
            } else if (GlobalRelease.includes(mode)) {
                handle.setRelease(this, (short) value);
            } else {
                handle.setVolatile(this, (short) value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public void store32(long offset, int value, WriteAccessMode mode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            delegateMemory.store32(offset, value, mode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == float.class) {
            if (GlobalPlain.includes(mode)) {
                handle.set(this, Float.intBitsToFloat(value));
            } else if (SingleOpaque.includes(mode)) {
                handle.setOpaque(this, Float.intBitsToFloat(value));
            } else if (GlobalRelease.includes(mode)) {
                handle.setRelease(this, Float.intBitsToFloat(value));
            } else {
                handle.setVolatile(this, Float.intBitsToFloat(value));
            }
        } else if (handle.varType() == int.class) {
            if (GlobalPlain.includes(mode)) {
                handle.set(this, value);
            } else if (SingleOpaque.includes(mode)) {
                handle.setOpaque(this, value);
            } else if (GlobalRelease.includes(mode)) {
                handle.setRelease(this, value);
            } else {
                handle.setVolatile(this, value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public void store64(long offset, long value, WriteAccessMode mode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            delegateMemory.store64(offset, value, mode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == Pointer.class) {
            TypeSystem ts = getCompilationContext().getTypeSystem();
            IntegerAsPointer ptr = new IntegerAsPointer(ts.getVoidType().getPointer(), value);
            storePointer(offset, ptr, mode);
        } else if (handle.varType() == double.class) {
            if (GlobalPlain.includes(mode)) {
                handle.set(this, Double.longBitsToDouble(value));
            } else if (SingleOpaque.includes(mode)) {
                handle.setOpaque(this, Double.longBitsToDouble(value));
            } else if (GlobalRelease.includes(mode)) {
                handle.setRelease(this, Double.longBitsToDouble(value));
            } else {
                handle.setVolatile(this, Double.longBitsToDouble(value));
            }
        } else if (handle.varType() == long.class) {
            if (GlobalPlain.includes(mode)) {
                handle.set(this, value);
            } else if (SingleOpaque.includes(mode)) {
                handle.setOpaque(this, value);
            } else if (GlobalRelease.includes(mode)) {
                handle.setRelease(this, value);
            } else {
                handle.setVolatile(this, value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public void storeRef(long offset, VmObject value, WriteAccessMode mode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            delegateMemory.storeRef(offset, value, mode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == VmObject.class) {
            if (GlobalPlain.includes(mode)) {
                handle.set(this, value);
            } else if (SingleOpaque.includes(mode)) {
                handle.setOpaque(this, value);
            } else if (GlobalRelease.includes(mode)) {
                handle.setRelease(this, value);
            } else {
                handle.setVolatile(this, value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public void storeType(long offset, ValueType value, WriteAccessMode mode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            delegateMemory.storeType(offset, value, mode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == ValueType.class) {
            if (GlobalPlain.includes(mode)) {
                handle.set(this, value);
            } else if (SingleOpaque.includes(mode)) {
                handle.setOpaque(this, value);
            } else if (GlobalRelease.includes(mode)) {
                handle.setRelease(this, value);
            } else {
                handle.setVolatile(this, value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public void storePointer(long offset, Pointer value, WriteAccessMode mode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            delegateMemory.storePointer(offset, value, mode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == Pointer.class) {
            if (GlobalPlain.includes(mode)) {
                handle.set(this, value);
            } else if (SingleOpaque.includes(mode)) {
                handle.setOpaque(this, value);
            } else if (GlobalRelease.includes(mode)) {
                handle.setRelease(this, value);
            } else {
                handle.setVolatile(this, value);
            }
        } else if (handle.varType() == long.class) {
            if (value == null) {
                store64(offset, 0, mode);
            } else if (value instanceof IntegerAsPointer iap) {
                store64(offset, iap.getValue(), mode);
            } else {
                throw invalidMemoryAccess();
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int compareAndExchange8(long offset, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.compareAndExchange8(offset, expect, update, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == boolean.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (boolean) handle.get(this) ? 1 : 0;
                if (val == (expect & 0x1)) {
                    handle.set(this, (update & 1) != 0);
                }
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (boolean) handle.compareAndExchangeAcquire(this, (expect & 1) != 0, (update & 1) != 0) ? 1 : 0;
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (boolean) handle.compareAndExchangeRelease(this, (expect & 1) != 0, (update & 1) != 0) ? 1 : 0;
            } else {
                return (boolean) handle.compareAndExchange(this, (expect & 1) != 0, (update & 1) != 0) ? 1 : 0;
            }
        } else if (handle.varType() == byte.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this);
                if (val == (expect & 0xff)) {
                    handle.set(this, (byte) update);
                }
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.compareAndExchangeAcquire(this, (byte) expect, (byte) update) & 0xff;
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.compareAndExchangeRelease(this, (byte) expect, (byte) update) & 0xff;
            } else {
                return (int) handle.compareAndExchange(this, (byte) expect, (byte) update) & 0xff;
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int compareAndExchange16(long offset, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.compareAndExchange16(offset, expect, update, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == char.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this);
                if (val == (expect & 0xffff)) {
                    handle.set(this, (char) update);
                }
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.compareAndExchangeAcquire(this, (char) expect, (char) update);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.compareAndExchangeRelease(this, (char) expect, (char) update);
            } else {
                return (int) handle.compareAndExchange(this, (char) expect, (char) update);
            }
        } else if (handle.varType() == short.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this) & 0xffff;
                if (val == (expect & 0xffff)) {
                    handle.set(this, (short) update);
                }
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.compareAndExchangeAcquire(this, (short) expect, (short) update) & 0xffff;
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.compareAndExchangeRelease(this, (short) expect, (short) update) & 0xffff;
            } else {
                return (int) handle.compareAndExchange(this, (short) expect, (short) update) & 0xffff;
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int compareAndExchange32(long offset, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.compareAndExchange32(offset, expect, update, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == float.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = Float.floatToRawIntBits((float) handle.get(this));
                if (val == expect) {
                    handle.set(this, Float.intBitsToFloat(update));
                }
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return Float.floatToRawIntBits((float) handle.compareAndExchangeAcquire(this, Float.intBitsToFloat(expect), Float.intBitsToFloat(update)));
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return Float.floatToRawIntBits((float) handle.compareAndExchangeRelease(this, Float.intBitsToFloat(expect), Float.intBitsToFloat(update)));
            } else {
                return Float.floatToRawIntBits((float) handle.compareAndExchange(this, Float.intBitsToFloat(expect), Float.intBitsToFloat(update)));
            }
        } else if (handle.varType() == int.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this);
                if (val == expect) {
                    handle.set(this, update);
                }
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.compareAndExchangeAcquire(this, expect, update);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.compareAndExchangeRelease(this, expect, update);
            } else {
                return (int) handle.compareAndExchange(this, expect, update);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public long compareAndExchange64(long offset, long expect, long update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.compareAndExchange64(offset, expect, update, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == Pointer.class) {
            TypeSystem ts = getCompilationContext().getTypeSystem();
            PointerType voidPointerType = ts.getVoidType().getPointer();
            Pointer witness = loadPointer(offset, readMode);
            if (witness == null && expect == 0 || witness instanceof IntegerAsPointer iap && expect == iap.getValue()) {
                // OK, try the real CAS
                IntegerAsPointer updatePtr = new IntegerAsPointer(voidPointerType, update);
                witness = compareAndExchangePointer(offset, witness, updatePtr, readMode, writeMode);
            }
            if (witness instanceof IntegerAsPointer newIap) {
                return newIap.getValue();
            } else if (witness == null) {
                return 0;
            } else {
                throw pointerAsInteger();
            }
        } else if (handle.varType() == double.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                long val = Double.doubleToRawLongBits((double) handle.get(this));
                if (val == expect) {
                    handle.set(this, Double.longBitsToDouble(update));
                }
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return Double.doubleToRawLongBits((double) handle.compareAndExchangeAcquire(this, Double.longBitsToDouble(expect), Double.longBitsToDouble(update)));
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return Double.doubleToRawLongBits((double) handle.compareAndExchangeRelease(this, Double.longBitsToDouble(expect), Double.longBitsToDouble(update)));
            } else {
                return Double.doubleToRawLongBits((double) handle.compareAndExchange(this, Double.longBitsToDouble(expect), Double.longBitsToDouble(update)));
            }
        } else if (handle.varType() == long.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                long val = (long) handle.get(this);
                if (val == expect) {
                    handle.set(this, update);
                }
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (long) handle.compareAndExchangeAcquire(this, expect, update);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (long) handle.compareAndExchangeRelease(this, expect, update);
            } else {
                return (long) handle.compareAndExchange(this, expect, update);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public VmObject compareAndExchangeRef(long offset, VmObject expect, VmObject update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.compareAndExchangeRef(offset, expect, update, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == VmObject.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                VmObject val = (VmObject) handle.get(this);
                if (val == expect) {
                    handle.set(this, update);
                }
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (VmObject) handle.compareAndExchangeAcquire(this, expect, update);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (VmObject) handle.compareAndExchangeRelease(this, expect, update);
            } else {
                return (VmObject) handle.compareAndExchange(this, expect, update);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public ValueType compareAndExchangeType(long offset, ValueType expect, ValueType update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.compareAndExchangeType(offset, expect, update, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == ValueType.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                ValueType val = (ValueType) handle.get(this);
                if (val == expect) {
                    handle.set(this, update);
                }
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (ValueType) handle.compareAndExchangeAcquire(this, expect, update);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (ValueType) handle.compareAndExchangeRelease(this, expect, update);
            } else {
                return (ValueType) handle.compareAndExchange(this, expect, update);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public Pointer compareAndExchangePointer(long offset, Pointer expect, Pointer update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.compareAndExchangePointer(offset, expect, update, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == long.class) {
            long expectLong, updateLong;
            if (expect == null) {
                expectLong = 0;
            } else if (expect instanceof IntegerAsPointer iap) {
                expectLong = iap.getValue();
            } else {
                throw pointerAsInteger();
            }
            if (update == null) {
                updateLong = 0;
            } else if (update instanceof IntegerAsPointer iap) {
                updateLong = iap.getValue();
            } else {
                throw pointerAsInteger();
            }
            long result = compareAndExchange64(offset, expectLong, updateLong, readMode, writeMode);
            if (result == 0) {
                return null;
            }
            PointerType voidPointerType = getCompilationContext().getTypeSystem().getVoidType().getPointer();
            return new IntegerAsPointer(voidPointerType, result);
        } else if (handle.varType() == Pointer.class) {
            Pointer witness;
            for (;;) {
                if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                    Pointer val = (Pointer) handle.get(this);
                    if (val == expect) {
                        handle.set(this, update);
                    }
                    return val;
                } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                    witness = (Pointer) handle.compareAndExchangeAcquire(this, expect, update);
                } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                    witness = (Pointer) handle.compareAndExchangeRelease(this, expect, update);
                } else {
                    witness = (Pointer) handle.compareAndExchange(this, expect, update);
                }
                if (witness != expect && Objects.equals(witness, expect)) {
                    // try again so we have `equals` semantics
                    expect = witness;
                } else {
                    break;
                }
            }
            return witness;
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int getAndSet8(long offset, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndSet8(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == boolean.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (boolean) handle.get(this) ? 1 : 0;
                handle.set(this, (value & 1) != 0);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (boolean) handle.getAndSetAcquire(this, (value & 1) != 0) ? 1 : 0;
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (boolean) handle.getAndSetRelease(this, (value & 1) != 0) ? 1 : 0;
            } else {
                return (boolean) handle.getAndSet(this, (value & 1) != 0) ? 1 : 0;
            }
        } else if (handle.varType() == byte.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this) & 0xff;
                handle.set(this, (byte) value);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndSetAcquire(this, (byte) value) & 0xff;
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndSetRelease(this, (byte) value) & 0xff;
            } else {
                return (int) handle.getAndSet(this, (byte) value) & 0xff;
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int getAndSet16(long offset, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndSet16(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == char.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this);
                handle.set(this, (char) value);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndSetAcquire(this, (char) value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndSetRelease(this, (char) value);
            } else {
                return (int) handle.getAndSet(this, (char) value);
            }
        } else if (handle.varType() == short.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this) & 0xffff;
                handle.set(this, (short) value);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndSetAcquire(this, (short) value) & 0xffff;
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndSetRelease(this, (short) value) & 0xffff;
            } else {
                return (int) handle.getAndSet(this, (short) value) & 0xffff;
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int getAndSet32(long offset, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndSet32(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == float.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = Float.floatToRawIntBits((float) handle.get(this));
                handle.set(this, Float.intBitsToFloat(value));
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return Float.floatToRawIntBits((float) handle.getAndSetAcquire(this, Float.intBitsToFloat(value)));
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return Float.floatToRawIntBits((float) handle.getAndSetRelease(this, Float.intBitsToFloat(value)));
            } else {
                return Float.floatToRawIntBits((float) handle.getAndSet(this, Float.intBitsToFloat(value)));
            }
        } else if (handle.varType() == int.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this);
                handle.set(this, value);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndSetAcquire(this, value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndSetRelease(this, value);
            } else {
                return (int) handle.getAndSet(this, value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public long getAndSet64(long offset, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndSet64(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == Pointer.class) {
            Pointer val;
            if (value == 0) {
                val = getAndSetPointer(offset, null, readMode, writeMode);
            } else {
                PointerType voidPointerType = getCompilationContext().getTypeSystem().getVoidType().getPointer();
                val = getAndSetPointer(offset, new IntegerAsPointer(voidPointerType, value), readMode, writeMode);
            }
            if (val == null) {
                return 0;
            } else if (val instanceof IntegerAsPointer iap) {
                return iap.getValue();
            } else {
                throw pointerAsInteger();
            }
        } else if (handle.varType() == double.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                long val = Double.doubleToRawLongBits((double) handle.get(this));
                handle.set(this, Double.longBitsToDouble(value));
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return Double.doubleToRawLongBits((double) handle.getAndSetAcquire(this, Double.longBitsToDouble(value)));
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return Double.doubleToRawLongBits((double) handle.getAndSetRelease(this, Double.longBitsToDouble(value)));
            } else {
                return Double.doubleToRawLongBits((double) handle.getAndSet(this, Double.longBitsToDouble(value)));
            }
        } else if (handle.varType() == long.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                long val = (long) handle.get(this);
                handle.set(this, value);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (long) handle.getAndSetAcquire(this, value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (long) handle.getAndSetRelease(this, value);
            } else {
                return (long) handle.getAndSet(this, value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public VmObject getAndSetRef(long offset, VmObject value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndSetRef(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == VmObject.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                VmObject val = (VmObject) handle.get(this);
                handle.set(this, value);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (VmObject) handle.getAndSetAcquire(this, value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (VmObject) handle.getAndSetRelease(this, value);
            } else {
                return (VmObject) handle.getAndSet(this, value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public ValueType getAndSetType(long offset, ValueType value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndSetType(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == ValueType.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                ValueType val = (ValueType) handle.get(this);
                handle.set(this, value);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (ValueType) handle.getAndSetAcquire(this, value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (ValueType) handle.getAndSetRelease(this, value);
            } else {
                return (ValueType) handle.getAndSet(this, value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public Pointer getAndSetPointer(long offset, Pointer value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndSetPointer(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == long.class) {
            long val;
            if (value == null) {
                val = getAndSet64(offset, 0, readMode, writeMode);
            } else if (value instanceof IntegerAsPointer iap) {
                val = getAndSet64(offset, iap.getValue(), readMode, writeMode);
            } else {
                throw pointerAsInteger();
            }
            PointerType voidPointerType = getCompilationContext().getTypeSystem().getVoidType().getPointer();
            return new IntegerAsPointer(voidPointerType, val);
        } else if (handle.varType() == Pointer.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                Pointer val = (Pointer) handle.get(this);
                handle.set(this, value);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (Pointer) handle.getAndSetAcquire(this, value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (Pointer) handle.getAndSetRelease(this, value);
            } else {
                return (Pointer) handle.getAndSet(this, value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int getAndAdd8(long offset, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndAdd8(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == byte.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this) & 0xff;
                handle.set(this, (byte) (val + value));
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndAddAcquire(this, (byte) value) & 0xff;
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndAddRelease(this, (byte) value) & 0xff;
            } else {
                return (int) handle.getAndAdd(this, (byte) value) & 0xff;
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int getAndAdd16(long offset, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndAdd16(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == char.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this);
                handle.set(this, (char) (val + value));
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndAddAcquire(this, (char) value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndAddRelease(this, (char) value);
            } else {
                return (int) handle.getAndAdd(this, (char) value);
            }
        } else if (handle.varType() == short.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this) & 0xffff;
                handle.set(this, (short) (val + value));
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndAddAcquire(this, (short) value) & 0xffff;
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndAddRelease(this, (short) value) & 0xffff;
            } else {
                return (int) handle.getAndAdd(this, (short) value) & 0xffff;
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int getAndAdd32(long offset, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndAdd32(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == int.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this);
                handle.set(this, val + value);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndAddAcquire(this, value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndAddRelease(this, value);
            } else {
                return (int) handle.getAndAdd(this, value);
            }
        } else if (handle.varType() == float.class) {
            // TODO: float behavior?
            throw new UnsupportedOperationException("float getAndAdd");
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public long getAndAdd64(long offset, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndAdd64(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == Pointer.class) {
            // use default CAS-based implementation
            return super.getAndAdd64(offset, value, readMode, writeMode);
        } else if (handle.varType() == long.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                long val = (long) handle.get(this);
                handle.set(this, val + value);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (long) handle.getAndAddAcquire(this, value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (long) handle.getAndAddRelease(this, value);
            } else {
                return (long) handle.getAndAdd(this, value);
            }
        } else if (handle.varType() == double.class) {
            // TODO: double behavior?
            throw new UnsupportedOperationException("double getAndAdd");
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int getAndBitwiseAnd8(long offset, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndBitwiseAnd8(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == byte.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this) & 0xff;
                handle.set(this, (byte) (val & value));
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndBitwiseAndAcquire(this, (byte) value) & 0xff;
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndBitwiseAndRelease(this, (byte) value) & 0xff;
            } else {
                return (int) handle.getAndBitwiseAnd(this, (byte) value) & 0xff;
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int getAndBitwiseAnd16(long offset, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndBitwiseAnd16(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == char.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this);
                handle.set(this, (char) (val & value));
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndBitwiseAndAcquire(this, (char) value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndBitwiseAndRelease(this, (char) value);
            } else {
                return (int) handle.getAndBitwiseAnd(this, (char) value);
            }
        } else if (handle.varType() == short.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this) & 0xffff;
                handle.set(this, (short) (val & value));
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndBitwiseAndAcquire(this, (short) value) & 0xffff;
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndBitwiseAndRelease(this, (short) value) & 0xffff;
            } else {
                return (int) handle.getAndBitwiseAnd(this, (short) value) & 0xffff;
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int getAndBitwiseAnd32(long offset, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndBitwiseAnd32(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == int.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this);
                handle.set(this, val & value);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndBitwiseAndAcquire(this, value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndBitwiseAndRelease(this, value);
            } else {
                return (int) handle.getAndBitwiseAnd(this, value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public long getAndBitwiseAnd64(long offset, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndBitwiseAnd64(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == Pointer.class) {
            // use default CAS-based implementation
            return super.getAndBitwiseAnd64(offset, value, readMode, writeMode);
        } else if (handle.varType() == long.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                long val = (long) handle.get(this);
                handle.set(this, val & value);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (long) handle.getAndBitwiseAndAcquire(this, value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (long) handle.getAndBitwiseAndRelease(this, value);
            } else {
                return (long) handle.getAndBitwiseAnd(this, value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int getAndBitwiseOr8(long offset, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndBitwiseOr8(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == byte.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this) & 0xff;
                handle.set(this, (byte) (val & value));
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndBitwiseOrAcquire(this, (byte) value) & 0xff;
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndBitwiseOrRelease(this, (byte) value) & 0xff;
            } else {
                return (int) handle.getAndBitwiseOr(this, (byte) value) & 0xff;
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int getAndBitwiseOr16(long offset, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndBitwiseOr16(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == char.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this);
                handle.set(this, (char) (val & value));
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndBitwiseOrAcquire(this, (char) value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndBitwiseOrRelease(this, (char) value);
            } else {
                return (int) handle.getAndBitwiseOr(this, (char) value);
            }
        } else if (handle.varType() == short.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this) & 0xffff;
                handle.set(this, (short) (val & value));
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndBitwiseOrAcquire(this, (short) value) & 0xffff;
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndBitwiseOrRelease(this, (short) value) & 0xffff;
            } else {
                return (int) handle.getAndBitwiseOr(this, (short) value) & 0xffff;
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int getAndBitwiseOr32(long offset, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndBitwiseOr32(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == int.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this);
                handle.set(this, val & value);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndBitwiseOrAcquire(this, value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndBitwiseOrRelease(this, value);
            } else {
                return (int) handle.getAndBitwiseOr(this, value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public long getAndBitwiseOr64(long offset, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndBitwiseOr64(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == Pointer.class) {
            // use default CAS-based implementation
            return super.getAndBitwiseOr64(offset, value, readMode, writeMode);
        } else if (handle.varType() == long.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                long val = (long) handle.get(this);
                handle.set(this, val & value);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (long) handle.getAndBitwiseOrAcquire(this, value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (long) handle.getAndBitwiseOrRelease(this, value);
            } else {
                return (long) handle.getAndBitwiseOr(this, value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int getAndBitwiseXor8(long offset, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndBitwiseXor8(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == byte.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this) & 0xff;
                handle.set(this, (byte) (val & value));
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndBitwiseXorAcquire(this, (byte) value) & 0xff;
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndBitwiseXorRelease(this, (byte) value) & 0xff;
            } else {
                return (int) handle.getAndBitwiseXor(this, (byte) value) & 0xff;
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int getAndBitwiseXor16(long offset, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndBitwiseXor16(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == char.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this);
                handle.set(this, (char) (val & value));
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndBitwiseXorAcquire(this, (char) value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndBitwiseXorRelease(this, (char) value);
            } else {
                return (int) handle.getAndBitwiseXor(this, (char) value);
            }
        } else if (handle.varType() == short.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this) & 0xffff;
                handle.set(this, (short) (val & value));
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndBitwiseXorAcquire(this, (short) value) & 0xffff;
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndBitwiseXorRelease(this, (short) value) & 0xffff;
            } else {
                return (int) handle.getAndBitwiseXor(this, (short) value) & 0xffff;
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public int getAndBitwiseXor32(long offset, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndBitwiseXor32(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == int.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                int val = (int) handle.get(this);
                handle.set(this, val & value);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (int) handle.getAndBitwiseXorAcquire(this, value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (int) handle.getAndBitwiseXorRelease(this, value);
            } else {
                return (int) handle.getAndBitwiseXor(this, value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public long getAndBitwiseXor64(long offset, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (offset > Integer.MAX_VALUE) {
            throw invalidMemoryAccess();
        }
        Memory delegateMemory = getDelegateMemory((int) offset);
        if (delegateMemory != null) {
            return delegateMemory.getAndBitwiseXor64(offset, value, readMode, writeMode);
        }
        VarHandle handle = getHandle((int) offset);
        if (handle.varType() == Pointer.class) {
            // use default CAS-based implementation
            return super.getAndBitwiseXor64(offset, value, readMode, writeMode);
        } else if (handle.varType() == long.class) {
            if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
                long val = (long) handle.get(this);
                handle.set(this, val & value);
                return val;
            } else if (GlobalAcquire.includes(readMode) && GlobalPlain.includes(writeMode)) {
                return (long) handle.getAndBitwiseXorAcquire(this, value);
            } else if (GlobalPlain.includes(readMode) && GlobalRelease.includes(writeMode)) {
                return (long) handle.getAndBitwiseXorRelease(this, value);
            } else {
                return (long) handle.getAndBitwiseXor(this, value);
            }
        } else {
            throw invalidMemoryAccess();
        }
    }

    @Override
    public Memory copy(long newSize) {
        if (newSize == getSize()) {
            return clone();
        } else {
            throw new IllegalArgumentException("Fixed memory cannot be resized");
        }
    }

    private static InvalidMemoryAccessException pointerAsInteger() {
        return new InvalidMemoryAccessException("Pointer as integer");
    }

    private static InvalidMemoryAccessException invalidMemoryAccess() {
        return new InvalidMemoryAccessException();
    }
}
