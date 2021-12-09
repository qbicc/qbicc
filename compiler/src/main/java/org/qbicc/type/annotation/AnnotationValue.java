package org.qbicc.type.annotation;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.classfile.ConstantPool;
import org.qbicc.type.definition.classfile.InvalidAnnotationValueException;
import io.smallrye.common.constraint.Assert;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
public abstract class AnnotationValue {
    private static final AnnotationValue[] NO_VALUES = new AnnotationValue[0];

    AnnotationValue() {}

    public static ArrayAnnotationValue array(AnnotationValue... values) {
        return new ArrayAnnotationValue(Assert.checkNotNullParam("values", values).clone());
    }

    public static ArrayAnnotationValue array(Collection<AnnotationValue> values) {
        return new ArrayAnnotationValue(Assert.checkNotNullParam("values", values).toArray(NO_VALUES));
    }

    public abstract void deparseValueTo(final ByteArrayOutputStream os, final ConstantPool cp);

    public enum Kind {
        BYTE,
        CHAR,
        DOUBLE,
        FLOAT,
        INT,
        LONG,
        SHORT,
        BOOLEAN,
        STRING,
        ENUM,
        CLASS,
        ANNOTATION,
        ARRAY,
        ;
    }

    public abstract Kind getKind();

    static int next(ByteBuffer buf) {
        return buf.get() & 0xff;
    }

    static int nextShort(ByteBuffer buf) {
        return buf.getShort() & 0xffff;
    }

    static int peek(ByteBuffer buf) {
        return buf.get(buf.position()) & 0xff;
    }

    static void expect(ByteBuffer buf, int val) {
        if (next(buf) != val) {
            throw parseError();
        }
    }

    static int codePoint(ByteBuffer buf) {
        int a = next(buf);
        if (a < 0x80) {
            return a;
        } else if (a < 0xc0) {
            throw parseError();
        } else if (a < 0xe0) {
            int b = next(buf);
            if (b < 0x80 || 0xbf < b) {
                throw parseError();
            }
            return (a & 0b0001_1111) << 6 | (b & 0b0011_1111);
        } else if (a < 0xf0) {
            int b = next(buf);
            if (b < 0x80 || 0xbf < b) {
                throw parseError();
            }
            int c = next(buf);
            if (c < 0x80 || 0xbf < c) {
                throw parseError();
            }
            return (a & 0b0000_1111) << 12 | (b & 0b0011_1111) << 6 | c & 0b0011_1111;
        } else {
            throw parseError();
        }
    }

    static IllegalArgumentException parseError() {
        return new IllegalArgumentException("Invalid generic signature string");
    }

    public static AnnotationValue parse(ClassFile classFile, ClassContext classContext, ByteBuffer buf) {
        // tag
        switch (next(buf)) {
            case 'B': {
                return ByteAnnotationValue.of(classFile.getIntConstant(nextShort(buf)));
            }
            case 'C': {
                return CharAnnotationValue.of(classFile.getIntConstant(nextShort(buf)));
            }
            case 'D': {
                return DoubleAnnotationValue.of(classFile.getDoubleConstant(nextShort(buf)));
            }
            case 'F': {
                return FloatAnnotationValue.of(classFile.getFloatConstant(nextShort(buf)));
            }
            case 'I': {
                return IntAnnotationValue.of(classFile.getIntConstant(nextShort(buf)));
            }
            case 'J': {
                return LongAnnotationValue.of(classFile.getLongConstant(nextShort(buf)));
            }
            case 'S': {
                return ShortAnnotationValue.of(classFile.getIntConstant(nextShort(buf)));
            }
            case 'Z': {
                return BooleanAnnotationValue.of(classFile.getIntConstant(nextShort(buf)) != 0);
            }
            case 's': {
                return StringAnnotationValue.of(classFile.getUtf8Constant(nextShort(buf)));
            }
            case 'e': {
                return EnumConstantAnnotationValue.of(classFile.getUtf8Constant(nextShort(buf)), classFile.getUtf8Constant(nextShort(buf)));
            }
            case 'c': {
                return ClassAnnotationValue.of((TypeDescriptor) classFile.getDescriptorConstant(nextShort(buf)));
            }
            case '@': {
                return Annotation.parse(classFile, classContext, buf);
            }
            case '[': {
                int count = nextShort(buf);
                AnnotationValue[] array = new AnnotationValue[count];
                for (int j = 0; j < count; j ++) {
                    array[j] = parse(classFile, classContext, buf);
                }
                return AnnotationValue.array(array);
            }
            default: {
                throw new InvalidAnnotationValueException("Invalid annotation value tag");
            }
        }
    }

}
