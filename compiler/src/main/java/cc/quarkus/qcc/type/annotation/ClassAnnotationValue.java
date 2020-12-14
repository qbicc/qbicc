package cc.quarkus.qcc.type.annotation;

import io.smallrye.common.constraint.Assert;

/**
 * A {@link Class} annotation value.
 */
public final class ClassAnnotationValue extends AnnotationValue {
    private final String classInternalName;

    ClassAnnotationValue(final String classInternalName) {
        this.classInternalName = classInternalName;
    }

    public String getClassInternalName() {
        return classInternalName;
    }

    public Kind getKind() {
        return Kind.CLASS;
    }

    public static ClassAnnotationValue of(final String typeName) {
        return new ClassAnnotationValue(Assert.checkNotNullParam("typeName", typeName));
    }
}
