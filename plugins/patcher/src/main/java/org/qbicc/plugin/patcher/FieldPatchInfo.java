package org.qbicc.plugin.patcher;

import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.InitializerResolver;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
final class FieldPatchInfo extends MemberPatchInfo {
    private final InitializerResolver initializerResolver;
    private final int initializerResolverIndex;
    private final FieldResolver fieldResolver;
    private final TypeDescriptor descriptor;
    private final String name;

    FieldPatchInfo(int index, int modifiers, InitializerResolver initializerResolver, int initializerResolverIndex, FieldResolver fieldResolver, TypeDescriptor descriptor, String name) {
        super(index, modifiers);
        this.initializerResolver = initializerResolver;
        this.initializerResolverIndex = initializerResolverIndex;
        this.fieldResolver = fieldResolver;
        this.descriptor = descriptor;
        this.name = name;
    }

    InitializerResolver getInitializerResolver() {
        return initializerResolver;
    }

    int getInitializerResolverIndex() {
        return initializerResolverIndex;
    }

    FieldResolver getFieldResolver() {
        return fieldResolver;
    }

    TypeDescriptor getDescriptor() {
        return descriptor;
    }

    public String getName() {
        return name;
    }
}
