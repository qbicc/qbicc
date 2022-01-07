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
final class FieldPatchInfo extends MemberPatchInfo implements Locatable {
    private final FieldResolver fieldResolver;
    private final TypeDescriptor descriptor;
    private final String name;

    FieldPatchInfo(String internalName, int index, int modifiers, FieldResolver fieldResolver, TypeDescriptor descriptor, String name, Annotation annotation) {
        super(index, modifiers, internalName, annotation);
        this.fieldResolver = fieldResolver;
        this.descriptor = descriptor;
        this.name = name;
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

    @Override
    public Location getLocation() {
        return ClassContextPatchInfo.getFieldLocation(getInternalName(), name);
    }
}
