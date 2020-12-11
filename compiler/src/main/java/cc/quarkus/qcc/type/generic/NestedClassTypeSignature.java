package cc.quarkus.qcc.type.generic;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;

/**
 *
 */
public final class NestedClassTypeSignature extends ClassTypeSignature {
    private final ClassTypeSignature enclosing;

    NestedClassTypeSignature(final ClassTypeSignature enclosing, final String identifier, final List<TypeArgument> typeArguments) {
        super(Objects.hash(NestedClassTypeSignature.class, enclosing), identifier, typeArguments);
        this.enclosing = enclosing;
    }

    public ClassTypeSignature getEnclosing() {
        return enclosing;
    }

    public boolean equals(final ClassTypeSignature other) {
        return other instanceof NestedClassTypeSignature && equals((NestedClassTypeSignature) other);
    }

    public boolean equals(final NestedClassTypeSignature other) {
        return super.equals(other) && enclosing.equals(other.enclosing);
    }

    StringBuilder prefixString(final StringBuilder target) {
        return simpleString(enclosing.prefixString(target).append('.'));
    }

    public static NestedClassTypeSignature parse(ClassTypeSignature outer, ClassContext classContext, ByteBuffer buf) {
        expect(buf, '.');
        int lastIdx = -1;
        StringBuilder b = new StringBuilder();
        int i;
        for (;;) {
            i = peek(buf);
            if (i == '/') {
                throw parseError();
            } else if (i == '.' || i == ';' || i == '<') {
                String identifier = classContext.deduplicate(b.toString());
                List<TypeArgument> typeArgs;
                if (i == '<') {
                    typeArgs = TypeArgument.parseList(classContext, buf);
                    i = next(buf);
                    if (i != '.' && i != ';') {
                        throw parseError();
                    }
                } else {
                    typeArgs = List.of();
                }
                return Cache.get(classContext).getNestedTypeSignature(outer, identifier, typeArgs);
            } else {
                b.appendCodePoint(codePoint(buf));
            }
        }
    }

    ClassTypeDescriptor makeDescriptor(final ClassContext classContext) {
        ClassTypeDescriptor encDesc = enclosing.asDescriptor(classContext);
        return ClassTypeDescriptor.synthesize(classContext, encDesc.getPackageName() + '/' + encDesc.getClassName() + '$' + getIdentifier());
    }
}
