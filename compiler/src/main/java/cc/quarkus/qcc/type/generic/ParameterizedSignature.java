package cc.quarkus.qcc.type.generic;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import cc.quarkus.qcc.type.definition.ClassContext;

/**
 *
 */
public abstract class ParameterizedSignature extends Signature {
    private final List<TypeParameter> typeParameters;

    ParameterizedSignature(final int hashCode, final List<TypeParameter> typeParameters) {
        super(hashCode * 19 + typeParameters.hashCode());
        this.typeParameters = typeParameters;
    }

    public List<TypeParameter> getTypeParameters() {
        return typeParameters;
    }

    public TypeParameter getTypeParameter(String name) {
        for (TypeParameter typeParameter : typeParameters) {
            if (typeParameter.getIdentifier().equals(name)) {
                return typeParameter;
            }
        }
        return null;
    }

    public final boolean equals(final Signature other) {
        return other instanceof ParameterizedSignature && equals((ParameterizedSignature) other);
    }

    public boolean equals(final ParameterizedSignature other) {
        return super.equals(other) && typeParameters.equals(other.typeParameters);
    }

    public StringBuilder toString(StringBuilder target) {
        final Iterator<TypeParameter> iterator = typeParameters.iterator();
        if (iterator.hasNext()) {
            target.append('<');
            do {
                iterator.next().toString(target);
            } while (iterator.hasNext());
            target.append('>');
        }
        return target;
    }

    static ParameterizedSignature parse(ClassContext classContext, ByteBuffer buf) {
        List<TypeParameter> typeParameters;
        int i = peek(buf);
        if (i == '<') {
            typeParameters = TypeParameter.parseList(classContext, buf);
            i = peek(buf);
        } else {
            typeParameters = List.of();
        }
        if (i == '(') {
            return MethodSignature.parse(classContext, buf, typeParameters);
        } else if (i == 'L') {
            return ClassSignature.parse(classContext, buf, typeParameters);
        } else {
            throw parseError();
        }
    }
}
