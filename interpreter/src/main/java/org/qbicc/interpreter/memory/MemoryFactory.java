package org.qbicc.interpreter.memory;

import java.nio.ByteOrder;

import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmObject;
import org.qbicc.type.ReferenceType;

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
}
