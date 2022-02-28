package org.qbicc.type.generic;

import java.nio.ByteBuffer;

import org.eclipse.collections.api.map.ImmutableMap;
import org.qbicc.context.ClassContext;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

/**
 *
 */
public abstract class ThrowsSignature extends ReferenceTypeSignature {
    ThrowsSignature(final int hashCode, ImmutableMap<ClassTypeDescriptor, Annotation> annotations) {
        super(hashCode, annotations);
    }

    public final boolean equals(final ReferenceTypeSignature other) {
        return other instanceof ThrowsSignature && equals((ThrowsSignature) other);
    }

    public boolean equals(final ThrowsSignature other) {
        return super.equals(other);
    }

    public static ThrowsSignature parse(ClassContext classContext, ByteBuffer buf) {
        int i = next(buf);
        if (i != '^') {
            throw parseError();
        }
        i = peek(buf);
        if (i == 'L') {
            return ClassTypeSignature.parse(classContext, buf);
        } else if (i == 'T') {
            return TypeVariableSignature.parse(classContext, buf);
        } else {
            throw parseError();
        }
    }

}
