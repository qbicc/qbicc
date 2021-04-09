package org.qbicc.type.annotation.type;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.definition.ClassContext;
import org.qbicc.type.definition.classfile.ClassFile;

/**
 * A type annotation.
 */
public final class TypeAnnotation {
    private final TargetInfo targetType;
    private final Annotation annotation;
    private final byte[] path;
    final int arg0;
    final int arg1;
    final short[] table;

    private TypeAnnotation(final TargetInfo targetType, final int arg0, final int arg1, final short[] table, final byte[] path, final Annotation annotation) {
        this.targetType = targetType;
        this.annotation = annotation;
        this.path = path;
        this.arg0 = arg0;
        this.arg1 = arg1;
        this.table = table;
    }

    TypeAnnotation(TargetInfo.Catch targetType, int arg, ClassFile classFile, ClassContext classContext, ByteBuffer buf) {
        this(targetType, arg, 0, null, parsePath(classContext, buf), Annotation.parse(classFile, classContext, buf));
    }

    TypeAnnotation(TargetInfo.Empty targetType, ClassFile classFile, ClassContext classContext, ByteBuffer buf) {
        this(targetType, 0, 0, null, parsePath(classContext, buf), Annotation.parse(classFile, classContext, buf));
    }

    TypeAnnotation(TargetInfo.FormalParameter targetType, int arg, ClassFile classFile, ClassContext classContext, ByteBuffer buf) {
        this(targetType, arg, 0, null, parsePath(classContext, buf), Annotation.parse(classFile, classContext, buf));
    }

    TypeAnnotation(TargetInfo.LocalVar targetType, short[] table, ClassFile classFile, ClassContext classContext, ByteBuffer buf) {
        this(targetType, 0, 0, table, parsePath(classContext, buf), Annotation.parse(classFile, classContext, buf));
    }

    TypeAnnotation(TargetInfo.Offset targetType, int arg, ClassFile classFile, ClassContext classContext, ByteBuffer buf) {
        this(targetType, arg, 0, null, parsePath(classContext, buf), Annotation.parse(classFile, classContext, buf));
    }

    TypeAnnotation(TargetInfo.SuperType targetType, int arg, ClassFile classFile, ClassContext classContext, ByteBuffer buf) {
        this(targetType, arg, 0, null, parsePath(classContext, buf), Annotation.parse(classFile, classContext, buf));
    }

    TypeAnnotation(TargetInfo.Throws targetType, int arg, ClassFile classFile, ClassContext classContext, ByteBuffer buf) {
        this(targetType, arg, 0, null, parsePath(classContext, buf), Annotation.parse(classFile, classContext, buf));
    }

    TypeAnnotation(TargetInfo.TypeArgument targetType, int arg0, int arg1, ClassFile classFile, ClassContext classContext, ByteBuffer buf) {
        this(targetType, arg0, arg1, null, parsePath(classContext, buf), Annotation.parse(classFile, classContext, buf));
    }

    TypeAnnotation(TargetInfo.TypeParameter targetType, int arg, ClassFile classFile, ClassContext classContext, ByteBuffer buf) {
        this(targetType, arg, 0, null, parsePath(classContext, buf), Annotation.parse(classFile, classContext, buf));
    }

    TypeAnnotation(TargetInfo.TypeParameterBound targetType, int arg0, int arg1, ClassFile classFile, ClassContext classContext, ByteBuffer buf) {
        this(targetType, arg0, arg1, null, parsePath(classContext, buf), Annotation.parse(classFile, classContext, buf));
    }

    public TargetInfo getTargetType() {
        return targetType;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public int getPathLength() {
        return path.length >> 1;
    }

    public TypePathKind getPathKind(int index) throws IndexOutOfBoundsException {
        return TypePathKind.of(path[index << 1] & 0xff);
    }

    public int getPathArgumentIndex(int index) throws IndexOutOfBoundsException {
        return path[1 + (index << 1)] & 0xff;
    }

    public boolean pathsAreEqual(TypeAnnotation other) {
        return Arrays.equals(path, other.path);
    }

    public boolean pathPrefixes(TypeAnnotation other) {
        int len = path.length;
        return other.path.length >= len && Arrays.equals(path, 0, len, other.path, 0, len);
    }

    boolean pathEndsWith(final TypePathKind pathKind, final int pathIdx) {
        int pi = getPathLength() - 1;
        return pathKind == getPathKind(pi) && pathIdx == getPathArgumentIndex(pi);
    }

    public static TypeAnnotation parse(ClassFile classFile, ClassContext classContext, ByteBuffer buffer) {
        return TargetTypes.getTargetInfo(next(buffer)).parse(classFile, classContext, buffer);
    }

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

    private static byte[] parsePath(ClassContext classContext, ByteBuffer buf) {
        int cnt = next(buf);
        byte[] path = new byte[cnt << 1];
        for (int i = 0; i < path.length; i += 2) {
            path[i] = (byte) next(buf);
            path[i + 1] = (byte) next(buf);
        }
        return path;
    }

    static IllegalArgumentException parseError() {
        return new IllegalArgumentException("Invalid generic signature string");
    }
}
