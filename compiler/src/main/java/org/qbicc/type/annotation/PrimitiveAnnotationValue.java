package org.qbicc.type.annotation;

/**
 * An annotation value that is a primitive type of some sort.
 */
public abstract class PrimitiveAnnotationValue extends AnnotationValue {
    PrimitiveAnnotationValue() {}

    public abstract boolean booleanValue();

    public abstract byte byteValue();

    public abstract short shortValue();

    public abstract int intValue();

    public abstract long longValue();

    public abstract char charValue();

    public abstract float floatValue();

    public abstract double doubleValue();
}
