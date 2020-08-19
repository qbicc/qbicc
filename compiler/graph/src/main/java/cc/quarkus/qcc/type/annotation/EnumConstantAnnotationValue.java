package cc.quarkus.qcc.type.annotation;

import io.smallrye.common.constraint.Assert;

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

    public static EnumConstantAnnotationValue of(final String typeName, final String valueName) {
        return new EnumConstantAnnotationValue(Assert.checkNotNullParam("typeName", typeName), Assert.checkNotNullParam("valueName", valueName));
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
