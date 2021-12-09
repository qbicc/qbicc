package org.qbicc.type.annotation;

import static org.qbicc.type.annotation.Annotation.writeShort;

import java.io.ByteArrayOutputStream;

import org.qbicc.type.definition.classfile.ConstantPool;

/**
 *
 */
public final class ArrayAnnotationValue extends AnnotationValue {
    private final AnnotationValue[] values;

    ArrayAnnotationValue(final AnnotationValue[] values) {
        this.values = values;
    }

    public int getElementCount() {
        return values.length;
    }

    public AnnotationValue getValue(int index) {
        return values[index];
    }

    @Override
    public void deparseValueTo(ByteArrayOutputStream os, ConstantPool cp) {
        os.write('[');
        writeShort(os, values.length);
        for (AnnotationValue value : values) {
            value.deparseValueTo(os, cp);
        }
    }

    public Kind getKind() {
        return Kind.ARRAY;
    }
}
