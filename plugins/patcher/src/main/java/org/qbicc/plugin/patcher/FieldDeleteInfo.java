package org.qbicc.plugin.patcher;

import org.qbicc.context.Locatable;
import org.qbicc.context.Location;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
final class FieldDeleteInfo implements Locatable {
    private final String internalName;
    private final TypeDescriptor descriptor;
    private final String name;
    private final Annotation annotation;

    FieldDeleteInfo(String internalName, TypeDescriptor descriptor, String name, Annotation annotation) {
        this.internalName = internalName;
        this.descriptor = descriptor;
        this.name = name;
        this.annotation = annotation;
    }

    TypeDescriptor getDescriptor() {
        return descriptor;
    }

    String getName() {
        return name;
    }

    Annotation getAnnotation() {
        return annotation;
    }

    @Override
    public Location getLocation() {
        return ClassContextPatchInfo.getFieldLocation(internalName, name);
    }
}
