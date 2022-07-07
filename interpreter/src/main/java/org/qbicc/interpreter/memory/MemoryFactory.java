package org.qbicc.interpreter.memory;

import java.nio.ByteOrder;
import java.util.List;

import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmObject;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VoidType;

/**
 * Factory methods for producing memory instances.
 */
public final class MemoryFactory {
    private MemoryFactory() {}

    private static final Memory EMPTY = new ByteArrayMemory.LE(new byte[0]);

    /**
     * Get an empty memory.  The memory may have any type (or none) and is not guaranteed to be extendable or copyable.
     *
     * @return an empty memory (not {@code null})
     */
    public static Memory getEmpty() {
        return EMPTY;
    }

    public static Memory replicate(Memory first, int nCopies) {
        if (nCopies == 1) {
            return first;
        } else {
            return new VectorMemory(first, nCopies);
        }
    }

    public static Memory compose(Memory... memories) {
        if (memories == null || memories.length == 0) {
            return EMPTY;
        } else if (memories.length == 1) {
            return memories[0];
        } else {
            return new CompositeMemory(memories);
        }
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static ByteArrayMemory wrap(byte[] array, ByteOrder byteOrder) {
        return byteOrder == ByteOrder.BIG_ENDIAN ? new ByteArrayMemory.BE(array) : new ByteArrayMemory.LE(array);
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static ShortArrayMemory wrap(short[] array) {
        return new ShortArrayMemory(array);
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static IntArrayMemory wrap(int[] array) {
        return new IntArrayMemory(array);
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static LongArrayMemory wrap(long[] array) {
        return new LongArrayMemory(array);
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static CharArrayMemory wrap(char[] array) {
        return new CharArrayMemory(array);
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static FloatArrayMemory wrap(float[] array) {
        return new FloatArrayMemory(array);
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static DoubleArrayMemory wrap(double[] array) {
        return new DoubleArrayMemory(array);
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static BooleanArrayMemory wrap(boolean[] array) {
        return new BooleanArrayMemory(array);
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     * The size of a reference is acquired from the given type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @param refType the reference type (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static ReferenceArrayMemory wrap(VmObject[] array, ReferenceType refType) {
        return new ReferenceArrayMemory(array, refType);
    }

    /**
     * Allocate a strongly typed memory with the given type and count.
     *
     * @param type the type (must not be {@code null})
     * @param count the number of sequential elements to allocate
     * @return the memory
     */
    public static Memory allocate(ValueType type, long count) {
        ByteOrder byteOrder = type.getTypeSystem().getEndianness();
        if (type instanceof VoidType || count == 0) {
            return getEmpty();
        }
        if (type instanceof ArrayType at) {
            long ec = count * at.getElementCount();
            if (ec > (1 << 30)) {
                // todo: we could vector some memories together...
                throw new IllegalArgumentException("too big");
            }
            ValueType et = at.getElementType();
            if (ec == 1) {
                return allocate(et, 1);
            } else {
                return allocate(et, ec);
            }
        }
        int intCount = Math.toIntExact(count);
        // vectored
        if (type instanceof CompoundType ct) {
            // todo: dynamically produce more efficient memory impls
            int cnt = ct.getMemberCount();
            if (cnt == 1 && ct.getMember(0).getOffset() == 0) {
                return allocate(ct.getMember(0).getType(), 1);
            }
            List<CompoundType.Member> padded = ct.getPaddedMembers();
            int pc = padded.size();
            Memory[] memories = new Memory[pc];
            for (int i = 0; i < pc; i ++) {
                memories[i] = allocate(padded.get(i).getType(), 1);
            }
            Memory result = compose(memories);
            // create a vector of memories if there is more than one
            if (count > 1) {
                result = replicate(result, intCount);
            }
            return result;
        } else if (type instanceof IntegerType it) {
            // todo: more compact impls
            return switch (it.getMinBits()) {
                case 8 -> wrap(new byte[intCount], byteOrder);
                case 16 -> it instanceof SignedIntegerType ? wrap(new short[intCount]) : wrap(new char[intCount]);
                case 32 -> wrap(new int[intCount]);
                case 64 -> wrap(new long[intCount]);
                default -> throw new IllegalArgumentException();
            };
        } else if (type instanceof FloatType ft) {
            return switch (ft.getMinBits()) {
                case 32 -> wrap(new float[intCount]);
                case 64 -> wrap(new double[intCount]);
                default -> throw new IllegalArgumentException();
            };
        } else if (type instanceof BooleanType) {
            return wrap(new boolean[intCount]);
        } else {
            // todo: pointers, types
            throw new IllegalArgumentException();
        }
    }
}
