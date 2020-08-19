package cc.quarkus.qcc.type.annotation;

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
}
