package cc.quarkus.qcc.type.annotation;

/**
 * A {@link String} annotation value.
 */
public final class StringAnnotationValue extends AnnotationValue {
    private final String string;

    StringAnnotationValue(final String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    public Kind getKind() {
        return Kind.STRING;
    }
}
