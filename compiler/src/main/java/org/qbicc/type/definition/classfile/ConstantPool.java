package org.qbicc.type.definition.classfile;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;

/**
 * A synthetic constant pool which can be used by the run time to handle reflection and annotation information.  This
 * constant pool may have fewer entries than the backing class file, and it may comprise constants from several (or no)
 * class file(s).
 * <p>
 * Constant pools are not thread-safe.
 */
public final class ConstantPool {

    private final MutableObjectIntMap<String> utf8;
    private String[] objConstants;
    private int[] intConstants;
    private int cnt;

    public ConstantPool() {
        this(32);
    }

    public ConstantPool(int initialCapacity) {
        utf8 = new ObjectIntHashMap<>(initialCapacity);
        objConstants = new String[initialCapacity];
        intConstants = new int[initialCapacity];
    }

    public int getOrAddUtf8Constant(final String value) {
        int idx = utf8.getIfAbsent(value, -1);
        if (idx == -1) {
            idx = cnt++;
            if (idx >= objConstants.length) {
                objConstants = Arrays.copyOf(objConstants, Math.max(idx+1, objConstants.length >>> 1));
            }
            utf8.put(value, idx);
            objConstants[idx] = value;
        }
        return idx;
    }

    public int getOrAddIntConstant(final int value) {
        // it's one slot no matter the actual type
        for (int i = 0; i < cnt; i ++) {
            if (intConstants[i] == value) {
                return i;
            }
        }
        if (cnt >= intConstants.length) {
            intConstants = Arrays.copyOf(intConstants, Math.max(cnt+1, intConstants.length >>> 1));
        }
        intConstants[cnt] = value;
        return cnt ++;
    }

    public int getOrAddLongConstant(final long value) {
        // it's two slots, always
        for (int i = 0; i < cnt - 1; i ++) {
            if (getLongConstant(i) == value) {
                return i;
            }
        }
        if (cnt >= intConstants.length - 1) {
            intConstants = Arrays.copyOf(intConstants, Math.max(cnt+2, intConstants.length >>> 1));
        }
        int idx = cnt;
        intConstants[cnt++] = (int) (value >>> 32L);
        intConstants[cnt++] = (int) value;
        return idx;
    }

    public String getUtf8Constant(int index) {
        return objConstants[index];
    }

    public int getIntConstant(int index) {
        return intConstants[index];
    }

    public long getLongConstant(int index) {
        // longs and doubles use two slots
        return (long) intConstants[index] << 32L | intConstants[index + 1] & 0xFFFF_FFFFL;
    }

    public double getDoubleConstant(int index) {
        return Double.longBitsToDouble(getLongConstant(index));
    }
}
