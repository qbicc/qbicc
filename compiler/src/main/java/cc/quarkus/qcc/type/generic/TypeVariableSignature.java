package cc.quarkus.qcc.type.generic;

import java.nio.ByteBuffer;
import java.util.Objects;

import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

/**
 *
 */
public final class TypeVariableSignature extends ThrowsSignature {
    private final String identifier;

    TypeVariableSignature(final String identifier) {
        super(Objects.hash(TypeVariableSignature.class, identifier));
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean equals(final ThrowsSignature other) {
        return other instanceof TypeVariableSignature && equals((TypeVariableSignature) other);
    }

    public boolean equals(final TypeVariableSignature other) {
        return super.equals(other) && identifier.equals(other.identifier);
    }

    public StringBuilder toString(final StringBuilder target) {
        return target.append('T').append(identifier).append(';');
    }

    public ClassTypeDescriptor asDescriptor(final ClassContext classContext) {
        return (ClassTypeDescriptor) super.asDescriptor(classContext);
    }

    TypeDescriptor makeDescriptor(final ClassContext classContext) {
        return ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
    }

    public static TypeVariableSignature parse(ClassContext classContext, ByteBuffer buf) {
        expect(buf, 'T');
        StringBuilder sb = new StringBuilder();
        int i = peek(buf);
        while (i != ';') {
            sb.appendCodePoint(codePoint(buf));
            i = peek(buf);
        }
        buf.get(); // consume ';'
        return Cache.get(classContext).getTypeVariableSignature(classContext.deduplicate(sb.toString()));
    }
}
