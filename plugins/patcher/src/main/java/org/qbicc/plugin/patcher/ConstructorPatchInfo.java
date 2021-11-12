package org.qbicc.plugin.patcher;

import org.qbicc.type.definition.ConstructorResolver;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 *
 */
final class ConstructorPatchInfo extends ExecutableMemberPatchInfo {
    private final ConstructorResolver constructorResolver;
    private final MethodDescriptor descriptor;

    ConstructorPatchInfo(int index, int modifiers, ConstructorResolver constructorResolver, MethodDescriptor descriptor) {
        super(index, modifiers);
        this.constructorResolver = constructorResolver;
        this.descriptor = descriptor;
    }

    ConstructorResolver getConstructorResolver() {
        return constructorResolver;
    }

    MethodDescriptor getDescriptor() {
        return descriptor;
    }
}
