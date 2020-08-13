package cc.quarkus.qcc.type.annotation;

/**
 * An {@code enum} annotation value.
 */
public class EnumConstantAnnotationValue extends AnnotationValue {
    private final String typeName;
    private final String valueName;

    EnumConstantAnnotationValue(final String typeName, final String valueName) {
        this.typeName = typeName;
        this.valueName = valueName;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getValueName() {
        return valueName;
    }

    public Kind getKind() {
        return Kind.ENUM;
    }
}
