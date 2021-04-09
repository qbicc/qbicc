package org.qbicc.type.annotation;

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

    public Kind getKind() {
        return Kind.ARRAY;
    }
}
