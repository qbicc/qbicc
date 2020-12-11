package cc.quarkus.qcc.type.annotation.type;

import static cc.quarkus.qcc.type.annotation.type.TypeAnnotation.*;

import java.nio.ByteBuffer;

import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;

/**
 * The type annotation target information structures.
 */
public abstract class TargetInfo {
    TargetInfo() {}

    static IllegalArgumentException badTarget() {
        return new IllegalArgumentException("The target of the type annotation is of the wrong kind");
    }

    void check(TypeAnnotation annotation) {
        if (annotation.getTargetType() != this) {
            throw badTarget();
        }
    }

    abstract TypeAnnotation parse(final ClassFile classFile, final ClassContext classContext, final ByteBuffer buf);

    public static final class TypeParameter extends TargetInfo {
        TypeParameter() {}

        public int getTypeParameterIndex(TypeAnnotation annotation) {
            check(annotation);
            return annotation.arg0;
        }

        TypeAnnotation parse(final ClassFile classFile, final ClassContext classContext, final ByteBuffer buf) {
            return new TypeAnnotation(this, next(buf), classFile, classContext, buf);
        }
    }

    public static final class SuperType extends TargetInfo {
        SuperType() {}

        public int getSuperTypeIndex(TypeAnnotation annotation) {
            check(annotation);
            return annotation.arg0;
        }

        TypeAnnotation parse(final ClassFile classFile, final ClassContext classContext, final ByteBuffer buf) {
            return new TypeAnnotation(this, nextShort(buf), classFile, classContext, buf);
        }
    }

    public static final class TypeParameterBound extends TargetInfo {
        TypeParameterBound() {}

        public int getTypeParameterIndex(TypeAnnotation annotation) {
            check(annotation);
            return annotation.arg0;
        }

        public int getBoundIndex(TypeAnnotation annotation) {
            check(annotation);
            return annotation.arg1;
        }

        TypeAnnotation parse(final ClassFile classFile, final ClassContext classContext, final ByteBuffer buf) {
            return new TypeAnnotation(this, next(buf), next(buf), classFile, classContext, buf);
        }
    }

    public static final class Empty extends TargetInfo {
        Empty() {}

        TypeAnnotation parse(final ClassFile classFile, final ClassContext classContext, final ByteBuffer buf) {
            return new TypeAnnotation(this, classFile, classContext, buf);
        }
    }

    public static final class FormalParameter extends TargetInfo {
        FormalParameter() {}

        public int getFormalParameterIndex(TypeAnnotation annotation) {
            check(annotation);
            return annotation.arg0;
        }

        TypeAnnotation parse(final ClassFile classFile, final ClassContext classContext, final ByteBuffer buf) {
            return new TypeAnnotation(this, next(buf), classFile, classContext, buf);
        }
    }

    public static final class Throws extends TargetInfo {
        Throws() {}

        public int getThrowsTypeIndex(TypeAnnotation annotation) {
            check(annotation);
            return annotation.arg0;
        }

        TypeAnnotation parse(final ClassFile classFile, final ClassContext classContext, final ByteBuffer buf) {
            return new TypeAnnotation(this, nextShort(buf), classFile, classContext, buf);
        }
    }

    public static final class LocalVar extends TargetInfo {
        LocalVar() {}

        public int getTableLength(TypeAnnotation annotation) {
            check(annotation);
            return annotation.table.length;
        }

        public int getStartPc(TypeAnnotation annotation, int index) throws IndexOutOfBoundsException {
            check(annotation);
            return annotation.table[index * 3];
        }

        public int getLength(TypeAnnotation annotation, int index) throws IndexOutOfBoundsException {
            check(annotation);
            return annotation.table[index * 3 + 1];
        }

        public int getVarIndex(TypeAnnotation annotation, int index) throws IndexOutOfBoundsException {
            check(annotation);
            return annotation.table[index * 3 + 2];
        }

        TypeAnnotation parse(final ClassFile classFile, final ClassContext classContext, final ByteBuffer buf) {
            short[] table = new short[nextShort(buf) * 3];
            for (int i = 0; i < table.length; i += 3) {
                table[i] = (short) nextShort(buf);
                table[i + 1] = (short) nextShort(buf);
                table[i + 2] = (short) nextShort(buf);
            }
            return new TypeAnnotation(this, table, classFile, classContext, buf);
        }
    }

    public static final class Catch extends TargetInfo {
        Catch() {}

        public int getExceptionTableIndex(TypeAnnotation annotation) {
            check(annotation);
            return annotation.arg0;
        }

        TypeAnnotation parse(final ClassFile classFile, final ClassContext classContext, final ByteBuffer buf) {
            return new TypeAnnotation(this, nextShort(buf), classFile, classContext, buf);
        }
    }

    public static final class Offset extends TargetInfo {
        Offset() {}

        public int getOffset(TypeAnnotation annotation) {
            check(annotation);
            return annotation.arg0;
        }

        TypeAnnotation parse(final ClassFile classFile, final ClassContext classContext, final ByteBuffer buf) {
            return new TypeAnnotation(this, nextShort(buf), classFile, classContext, buf);
        }
    }

    public static final class TypeArgument extends TargetInfo {
        TypeArgument() {}

        public int getOffset(TypeAnnotation annotation) {
            check(annotation);
            return annotation.arg0;
        }

        public int getTypeArgumentIndex(TypeAnnotation annotation) {
            check(annotation);
            return annotation.arg1;
        }

        TypeAnnotation parse(final ClassFile classFile, final ClassContext classContext, final ByteBuffer buf) {
            return new TypeAnnotation(this, nextShort(buf), next(buf), classFile, classContext, buf);
        }
    }
}
