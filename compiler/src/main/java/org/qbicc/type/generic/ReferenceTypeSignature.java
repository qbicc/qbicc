package org.qbicc.type.generic;

import java.nio.ByteBuffer;

import org.eclipse.collections.api.map.ImmutableMap;
import org.qbicc.context.ClassContext;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

/**
 *
 */
public abstract class ReferenceTypeSignature extends TypeSignature {
    ReferenceTypeSignature(final int hashCode, ImmutableMap<ClassTypeDescriptor, Annotation> annotations) {
        super(hashCode, annotations);
    }

    public final boolean equals(final TypeSignature other) {
        return other instanceof ReferenceTypeSignature && equals((ReferenceTypeSignature) other);
    }

    public boolean equals(final ReferenceTypeSignature other) {
        return super.equals(other);
    }

    public static ReferenceTypeSignature parse(ClassContext classContext, ByteBuffer buf) {
        int i = peek(buf);
        if (i == 'L') {
            return ClassTypeSignature.parse(classContext, buf);
        } else if (i == 'T') {
            return TypeVariableSignature.parse(classContext, buf);
        } else if (i == '[') {
            return ArrayTypeSignature.parse(classContext, buf);
        } else {
            throw parseError();
        }
    }
}
