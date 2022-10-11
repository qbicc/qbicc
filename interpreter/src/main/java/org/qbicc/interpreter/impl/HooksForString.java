package org.qbicc.interpreter.impl;

import static org.qbicc.graph.atomic.AccessModes.*;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmString;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
final class HooksForString {
    HooksForString() {}

    @Hook
    static VmString intern(VmThreadImpl thread, VmStringImpl string) {
        return thread.vm.intern(string);
    }

    @Hook(name = "hashCode")
    static int hashCodeImpl(VmThread thread, VmString string) {
        // for performance:
        // String.hashCode is well-defined
        return string.getContent().hashCode();
    }

    @Hook(name = "equals")
    static boolean equalsImpl(VmThread thread, VmString string, VmObject other) {
        return other instanceof VmString otherStr && string.contentEquals(otherStr.getContent());
    }

    @Hook
    static byte coder(VmThreadImpl thread, VmString string) {
        return (byte) string.getMemory().load8(thread.vm.stringCoderOffset, SinglePlain);
    }

    @Hook
    static boolean isLatin1(VmThreadImpl thread, VmString string) {
        return coder(thread, string) == 0;
    }

    @Hook
    static int length(VmThread thread, VmString string) {
        return string.getContent().length();
    }

    @Hook
    static char charAt(VmThreadImpl thread, VmString string, int index) {
        try {
            return string.getContent().charAt(index);
        } catch (StringIndexOutOfBoundsException e) {
            VmThrowableClassImpl ioobe = (VmThrowableClassImpl) thread.vm.bootstrapClassLoader.loadClass("java/lang/StringIndexOutOfBoundsException");
            throw new Thrown(ioobe.newInstance(e.getMessage()));
        }
    }
}
