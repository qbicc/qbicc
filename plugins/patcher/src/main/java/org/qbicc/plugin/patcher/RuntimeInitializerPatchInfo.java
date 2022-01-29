package org.qbicc.plugin.patcher;

import org.qbicc.context.Locatable;
import org.qbicc.context.Location;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.InitializerResolver;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
final class RuntimeInitializerPatchInfo extends MemberPatchInfo implements Locatable {
    private final InitializerResolver initializerResolver;
    private final int initializerResolverIndex;
    private final TypeDescriptor descriptor;
    private final String name;

    RuntimeInitializerPatchInfo(String internalName, int index, InitializerResolver initializerResolver, int initializerResolverIndex, TypeDescriptor descriptor, String name, Annotation annotation) {
        super(index, 0, internalName, annotation, null);
        this.initializerResolver = initializerResolver;
        this.initializerResolverIndex = initializerResolverIndex;
        this.descriptor = descriptor;
        this.name = name;
    }

    InitializerResolver getInitializerResolver() {
        return initializerResolver;
    }

    int getInitializerResolverIndex() {
        return initializerResolverIndex;
    }

    TypeDescriptor getDescriptor() {
        return descriptor;
    }

    public String getName() {
        return name;
    }

    @Override
    public Location getLocation() {
        return ClassContextPatchInfo.getFieldLocation(getInternalName(), name);
    }
}
