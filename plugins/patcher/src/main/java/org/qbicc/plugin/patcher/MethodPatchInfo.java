package org.qbicc.plugin.patcher;

import org.qbicc.type.definition.MethodResolver;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 *
 */
final class MethodPatchInfo extends ExecutableMemberPatchInfo {
    private final MethodResolver methodResolver;
    private final MethodDescriptor descriptor;
    private final String name;

    MethodPatchInfo(int index, int modifiers, MethodResolver methodResolver, MethodDescriptor descriptor, String name) {
        super(index, modifiers);
        this.methodResolver = methodResolver;
        this.descriptor = descriptor;
        this.name = name;
    }

    MethodResolver getMethodResolver() {
        return methodResolver;
    }

    MethodDescriptor getDescriptor() {
        return descriptor;
    }

    String getName() {
        return name;
    }
}
