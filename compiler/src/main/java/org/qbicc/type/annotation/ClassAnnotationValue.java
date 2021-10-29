package org.qbicc.type.annotation;

import io.smallrye.common.constraint.Assert;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 * A {@link Class} annotation value.
 */
public final class ClassAnnotationValue extends AnnotationValue {
    private final TypeDescriptor descriptor;

    ClassAnnotationValue(final TypeDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public TypeDescriptor getDescriptor() {
        return descriptor;
    }

    public Kind getKind() {
        return Kind.CLASS;
    }

    public static ClassAnnotationValue of(final TypeDescriptor  typeName) {
        return new ClassAnnotationValue(Assert.checkNotNullParam("typeName", typeName));
    }
}
