package org.qbicc.type.generic;

import java.nio.ByteBuffer;

import org.qbicc.type.definition.ClassContext;

/**
 *
 */
public abstract class ThrowsSignature extends ReferenceTypeSignature {
    ThrowsSignature(final int hashCode) {
        super(hashCode);
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
