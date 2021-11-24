package org.qbicc.plugin.patcher;

import org.qbicc.context.Locatable;
import org.qbicc.context.Location;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.definition.MethodResolver;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 *
 */
final class MethodPatchInfo extends ExecutableMemberPatchInfo implements Locatable {
    private final MethodResolver methodResolver;
    private final MethodDescriptor descriptor;
    private final String name;

    MethodPatchInfo(int index, int modifiers, MethodResolver methodResolver, MethodDescriptor descriptor, String name, String internalName, Annotation annotation) {
        super(index, modifiers, internalName, annotation);
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

    @Override
    public Location getLocation() {
        return ClassContextPatchInfo.getMethodLocation(getInternalName(), name);
    }
}
