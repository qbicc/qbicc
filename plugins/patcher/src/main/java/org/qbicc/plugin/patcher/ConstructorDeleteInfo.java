package org.qbicc.plugin.patcher;

import org.qbicc.context.Locatable;
import org.qbicc.context.Location;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.descriptor.MethodDescriptor;

final class ConstructorDeleteInfo implements Locatable {
    private final String internalName;
    private final MethodDescriptor descriptor;
    private final Annotation annotation;

    ConstructorDeleteInfo(String internalName, MethodDescriptor descriptor, Annotation annotation) {
        this.internalName = internalName;
        this.descriptor = descriptor;
        this.annotation = annotation;
    }

    MethodDescriptor getDescriptor() {
        return descriptor;
    }

    Annotation getAnnotation() {
        return annotation;
    }

    @Override
    public Location getLocation() {
        return ClassContextPatchInfo.getMethodLocation(internalName, "<init>");
    }
}
