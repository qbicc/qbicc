package org.qbicc.type.annotation;

import static org.qbicc.type.annotation.Annotation.writeShort;

import java.io.ByteArrayOutputStream;

import org.qbicc.type.definition.classfile.ConstantPool;

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

    @Override
    public void deparseValueTo(ByteArrayOutputStream os, ConstantPool cp) {
        os.write(switch (getKind()) {
            case BYTE -> 'B';
            case CHAR -> 'C';
            case DOUBLE -> 'D';
            case FLOAT -> 'F';
            case INT -> 'I';
            case LONG -> 'J';
            case SHORT -> 'S';
            case BOOLEAN -> 'Z';
            default -> throw new IllegalStateException();
        });
        writeShort(os, switch (getKind()) {
            case LONG -> cp.getOrAddLongConstant(longValue());
            case FLOAT -> cp.getOrAddIntConstant(Float.floatToRawIntBits(floatValue()));
            case DOUBLE -> cp.getOrAddLongConstant(Double.doubleToRawLongBits(doubleValue()));
            default -> cp.getOrAddIntConstant(intValue());
        });
    }

}
