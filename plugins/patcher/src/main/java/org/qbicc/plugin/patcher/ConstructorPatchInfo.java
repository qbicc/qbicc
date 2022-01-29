package org.qbicc.plugin.patcher;

import org.qbicc.context.Locatable;
import org.qbicc.context.Location;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.definition.ConstructorResolver;
import org.qbicc.type.descriptor.MethodDescriptor;

import java.util.List;

/**
 *
 */
final class ConstructorPatchInfo extends ExecutableMemberPatchInfo implements Locatable {
    private final ConstructorResolver constructorResolver;
    private final MethodDescriptor descriptor;

    ConstructorPatchInfo(int index, int modifiers, ConstructorResolver constructorResolver, MethodDescriptor descriptor, String internalName, Annotation annotation, List<Annotation> addedAnnotations) {
        super(index, modifiers, internalName, annotation, addedAnnotations);
        this.constructorResolver = constructorResolver;
        this.descriptor = descriptor;
    }

    ConstructorResolver getConstructorResolver() {
        return constructorResolver;
    }

    MethodDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public Location getLocation() {
        return ClassContextPatchInfo.getMethodLocation(getInternalName(), "<init>");
    }
}
