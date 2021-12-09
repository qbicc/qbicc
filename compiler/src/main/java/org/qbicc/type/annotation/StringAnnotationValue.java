package org.qbicc.type.annotation;

import static org.qbicc.type.annotation.Annotation.*;

import java.io.ByteArrayOutputStream;

import io.smallrye.common.constraint.Assert;
import org.qbicc.type.definition.classfile.ConstantPool;

/**
 * A {@link String} annotation value.
 */
public final class StringAnnotationValue extends AnnotationValue {
    private final String string;

    StringAnnotationValue(final String string) {
        this.string = string;
    }

    public static StringAnnotationValue of(final String value) {
        return new StringAnnotationValue(Assert.checkNotNullParam("value", value));
    }

    public String getString() {
        return string;
    }

    public void deparseValueTo(final ByteArrayOutputStream os, final ConstantPool cp) {
        os.write('s');
        writeShort(os, cp.getOrAddUtf8Constant(string));
    }

    public Kind getKind() {
        return Kind.STRING;
    }
}
