package cc.quarkus.qcc.type.definition.classfile;

import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.MethodHandle;
import cc.quarkus.qcc.type.definition.ResolutionFailedException;

/**
 *
 */
final class VirtualMethodHandleImpl implements MethodHandle {
    VirtualMethodHandleImpl(final ClassFileImpl classFile, final int index) {
    }

    public int getModifiers() {
        throw new UnsupportedOperationException();
    }

    public int getParameterCount() {
        throw new UnsupportedOperationException();
    }

    public MethodBody getResolvedMethodBody() throws ResolutionFailedException {
        throw new UnsupportedOperationException();
    }
}
