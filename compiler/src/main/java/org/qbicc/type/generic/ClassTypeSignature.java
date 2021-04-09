package org.qbicc.type.generic;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.qbicc.type.definition.ClassContext;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

/**
 * Some class (or interface).  Does not specify whether the class is accessed by reference or by value.
 */
public abstract class ClassTypeSignature extends ThrowsSignature {
    private final String identifier;
    private final List<TypeArgument> typeArguments;

    ClassTypeSignature(final int hashCode, final String identifier, final List<TypeArgument> typeArguments) {
        super(Objects.hash(identifier, typeArguments) * 19 + hashCode);
        this.identifier = identifier;
        this.typeArguments = typeArguments;
    }

    public String getIdentifier() {
        return identifier;
    }

    public List<TypeArgument> getTypeArguments() {
        return typeArguments;
    }

    public final boolean equals(final ThrowsSignature other) {
        return other instanceof ClassTypeSignature && equals((ClassTypeSignature) other);
    }

    public boolean equals(final ClassTypeSignature other) {
        return super.equals(other) && identifier.equals(other.identifier) && typeArguments.equals(other.typeArguments);
    }

    public ClassTypeDescriptor asDescriptor(final ClassContext classContext) {
        return (ClassTypeDescriptor) super.asDescriptor(classContext);
    }

    abstract StringBuilder prefixString(final StringBuilder target);

    final StringBuilder simpleString(final StringBuilder target) {
        target.append(identifier);
        final Iterator<TypeArgument> iterator = typeArguments.iterator();
        if (iterator.hasNext()) {
            target.append('<');
            do {
                iterator.next().toString(target);
            } while (iterator.hasNext());
            target.append('>');
        }
        return target;
    }

    public final StringBuilder toString(final StringBuilder target) {
        return prefixString(target).append(';');
    }

    public static ClassTypeSignature parse(ClassContext classContext, ByteBuffer buf) {
        expect(buf, 'L');
        ClassTypeSignature sig = TopLevelClassTypeSignature.parse(classContext, buf);
        int i;
        for (;;) {
            i = peek(buf);
            if (i == ';') {
                expect(buf, ';');
                return sig;
            } else if (i == '.') {
                sig = NestedClassTypeSignature.parse(sig, classContext, buf);
            }
        }
    }

}
