package org.qbicc.type.annotation;

import static org.qbicc.type.annotation.Annotation.*;

import java.io.ByteArrayOutputStream;

import io.smallrye.common.constraint.Assert;
import org.qbicc.type.definition.classfile.ConstantPool;
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

    public void deparseValueTo(final ByteArrayOutputStream os, final ConstantPool cp) {
        os.write('c');
        writeShort(os, cp.getOrAddUtf8Constant(descriptor.toString()));
    }

    public Kind getKind() {
        return Kind.CLASS;
    }

    public static ClassAnnotationValue of(final TypeDescriptor  typeName) {
        return new ClassAnnotationValue(Assert.checkNotNullParam("typeName", typeName));
    }
}
