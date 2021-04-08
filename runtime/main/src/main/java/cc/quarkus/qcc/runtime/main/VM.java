package cc.quarkus.qcc.runtime.main;

import static cc.quarkus.qcc.runtime.CNative.*;

import cc.quarkus.qcc.runtime.ThreadScoped;

/**
 * VM Utilities.
 */
public final class VM {

    /**
     * Internal holder for the pointer to the current thread.  Thread objects are not allowed to move in memory
     * after being constructed.
     * <p>
     * GC must take care to include this object in the root set of each thread.
     */
    @ThreadScoped
    @export
    @SuppressWarnings("unused")
    static void_ptr _qcc_bound_thread;

    // Temporary manual implementation
    @SuppressWarnings("ManualArrayCopy")
    static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
        if (src instanceof Object[] && dest instanceof Object[]) {
            Object[] srcArray = (Object[]) src;
            Object[] destArray = (Object[]) dest;
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else if (src instanceof byte[] && dest instanceof byte[]) {
            byte[] srcArray = (byte[]) src;
            byte[] destArray = (byte[]) dest;
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else if (src instanceof short[] && dest instanceof short[]) {
            short[] srcArray = (short[]) src;
            short[] destArray = (short[]) dest;
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else if (src instanceof int[] && dest instanceof int[]) {
            int[] srcArray = (int[]) src;
            int[] destArray = (int[]) dest;
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else if (src instanceof long[] && dest instanceof long[]) {
            long[] srcArray = (long[]) src;
            long[] destArray = (long[]) dest;
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else if (src instanceof char[] && dest instanceof char[]) {
            char[] srcArray = (char[]) src;
            char[] destArray = (char[]) dest;
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else if (src instanceof float[] && dest instanceof float[]) {
            float[] srcArray = (float[]) src;
            float[] destArray = (float[]) dest;
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else if (src instanceof double[] && dest instanceof double[]) {
            double[] srcArray = (double[]) src;
            double[] destArray = (double[]) dest;
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else if (src instanceof boolean[] && dest instanceof boolean[]) {
            boolean[] srcArray = (boolean[]) src;
            boolean[] destArray = (boolean[]) dest;
            for (int i = 0; i < length; i ++) {
                destArray[destPos + i] = srcArray[srcPos + i];
            }
        } else {
            throw new ClassCastException("Invalid array types for copy");
        }
    }
}
