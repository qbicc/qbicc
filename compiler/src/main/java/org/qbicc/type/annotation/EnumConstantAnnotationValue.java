package org.qbicc.type.annotation;

import static org.qbicc.type.annotation.Annotation.*;

import java.io.ByteArrayOutputStream;

import io.smallrye.common.constraint.Assert;
import org.qbicc.type.definition.classfile.ConstantPool;

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

    public void deparseValueTo(final ByteArrayOutputStream os, final ConstantPool cp) {
        os.write('e');
        writeShort(os, cp.getOrAddUtf8Constant(typeName));
        writeShort(os, cp.getOrAddUtf8Constant(valueName));
    }

    public Kind getKind() {
        return Kind.ENUM;
    }
}
