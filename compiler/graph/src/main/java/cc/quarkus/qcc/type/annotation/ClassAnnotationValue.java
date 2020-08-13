package cc.quarkus.qcc.type.annotation;

/**
 * A {@link Class} annotation value.
 */
public final class ClassAnnotationValue extends AnnotationValue {
    private final String className;

    ClassAnnotationValue(final String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public Kind getKind() {
        return Kind.CLASS;
    }
}
