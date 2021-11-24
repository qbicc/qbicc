package org.qbicc.plugin.patcher;

import org.qbicc.context.Locatable;
import org.qbicc.context.Location;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.descriptor.MethodDescriptor;

final class MethodDeleteInfo implements Locatable {
    private final String internalName;
    private final String name;
    private final MethodDescriptor descriptor;
    private final Annotation annotation;

    MethodDeleteInfo(String internalName, String name, MethodDescriptor descriptor, Annotation annotation) {
        this.internalName = internalName;
        this.name = name;
        this.descriptor = descriptor;
        this.annotation = annotation;
    }

    String getName() {
        return name;
    }

    MethodDescriptor getDescriptor() {
        return descriptor;
    }

    Annotation getAnnotation() {
        return annotation;
    }

    @Override
    public Location getLocation() {
        return ClassContextPatchInfo.getMethodLocation(internalName, name);
    }
}
