package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
final class HooksForSystem {
    HooksForSystem() {}

    @Hook
    static long nanoTime(VmThread thread) {
        return System.nanoTime();
    }

    @Hook
    static long currentTimeMillis(VmThread thread) {
        return System.currentTimeMillis();
    }

    @Hook
    static void arraycopy(VmThreadImpl thread, VmArray src, int srcPos, VmArray dest, int destPos, int length) {
        final VmImpl vm = thread.vm;
        try {
            //noinspection SuspiciousSystemArraycopy
            System.arraycopy(src.getArray(), srcPos, dest.getArray(), destPos, length);
        } catch (ClassCastException ex) {
            VmThrowableClassImpl exClass = (VmThrowableClassImpl) vm.bootstrapClassLoader.loadClass("java/lang/ClassCastException");
            throw new Thrown(exClass.newInstance());
        } catch (ArrayStoreException ex) {
            VmThrowableClassImpl exClass = (VmThrowableClassImpl) vm.bootstrapClassLoader.loadClass("java/lang/ArrayStoreException");
            throw new Thrown(exClass.newInstance());
        } catch (NullPointerException ex) {
            VmThrowableClassImpl exClass = (VmThrowableClassImpl) vm.bootstrapClassLoader.loadClass("java/lang/NullPointerException");
            throw new Thrown(exClass.newInstance());
        }
    }
}
